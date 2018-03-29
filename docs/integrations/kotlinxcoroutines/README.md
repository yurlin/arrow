---
layout: docs
title: kotlinx.coroutines
permalink: /docs/integrations/kotlinxcoroutines/
---

## kotlinx.coroutines

Arrow wants to provide an abstraction over multiple concurrency frameworks, in a way where their semantics match and they become interchangeable.

Working towards this purpose, it's only natural that we'd add support for the framework Jetbrains provides over coroutines.
This framework is called `kotlinx.coroutines`, whereas the machinery necessary to create coroutines is called `kotlin.coroutines`.

The most important datatype provided by Jetbrains is `Deferred`. `Deferred` is an abstraction capable of returning 1 result and cancellation.
Its constructor is called `async`, and takes one suspended execution block where you can `await()` suspended functions.

```kotlin
async {
  val userProfile = getProfile("userId").await()
  val friendProfiles = userProfile.friends().map { friend ->
     getProfile(friend.id).await()
  }
}
```

Does it look familiar? Yes! It's the same as our [comprehensions]({{ '/docs/patterns/monad_comprehensions' | relative_url }})!

### Improvements over the library

Unlike [RxJava]({{ '/docs/integrations/rx2' | relative_url }}), `Deferred` doesn't come with a natural set of operations for error handling and recovery,
requiring users to use imperative try/catch blocks.
Luckily, Arrow comes with its own set of error handling functions in its integration with [`MonadError`]({{ '/docs/typeclasses/monaderror' | relative_url }}).

See this faulty block
```kotlin
import kotlinx.coroutines.*

val deferred = async { throw RuntimeException("BOOM!") }
runBlocking { deferred.await() }
// CRASH! Exception!
```

What if we convert it to Arrow using `k()`?
```kotlin
import arrow.effects.*

val errorArrowWrapper = async { throw RuntimeException("BOOM!") }.k()
deferredWrapper.unsafeAttemptSync()
// Failure(RuntimeException("BOOM!"))
```

And how about adding some nice error recovery!
```kotlin
val recoveryArrowWrapper = async { throw RuntimeException("BOOM!") }.k()
                                 .handleError { 0 }
recoveryArrowWrapper.unsafeAttemptSync()
// Success(0)
```

The second advantage is that we're providing all the instances required to create an architecture that's agnostic to the framework, so you can mix and match multiple frameworks
in a way that feels idiomatic, while not having to worry about the semantics of each implementation.

You can read more about FP architectures in the section on [Monad Transformers]({{ '/docs/patterns/monad_transformers' | relative_url }}).

### Bringing Deferred to Arrow

To create a Deferred Arrow Wrapper you can invoke the constructor with any synchronous non-suspending function, the same way you'd use `async`.

```kotlin
val deferredK = DeferredK { throw RuntimeException("BOOM!") }
```

To wrap any existing `Deferred` in its Arrow Wrapper counterpart you can use the extension function `k()`.

```kotlin
val deferredWrapped = async { throw RuntimeException("BOOM!") }.k()
```

All the other usual constructors like `pure()`, `suspend()`, and `async()` are available too, in versions that accept different values for `CoroutineStart` and `CoroutineContext`.

To unwrap the value of a `DeferredK` we provide a synchronous method called `unsafeAttemptSync()` that returns a `Try<A>`.

```kotlin
deferredK.unsafeAttemptSync()
// Failure(RuntimeException("BOOM!"))
```

For unwrapping the values asynchronously you can use `unsafeRunAsync()`  and `runAsync()`.

The safe version takes as a parameter a callback from a result of `Either<Throwable, A>` to a new `Deferred<Unit>` instance.
All exceptions that would happen on the function parameter are automatically captured and propagated to the `Deferred<Unit>` return.

```kotlin
DeferredK { throw RuntimeException("Boom!") }
  .runAsync { result ->
    result.fold({ DeferredK { println("Error found") } }, { res -> DeferredK { println(res.toString()) } })
  }
// Error found
```

The unsafe version requires a callback to `Unit` and is assumed to never throw any internal exceptions.

```kotlin
DeferredK { throw RuntimeException("Boom!") }
  .unsafeRunAsync { result ->
    result.fold({ println("Error found") }, { println(it.toString()) })
  }
// Error found
```

Note that the function `unsafeRunSync` returns a value that's not wrapped on a `Try<A>`. This means that, like `async`, this function can crash your program.
Use it with SEVERE CAUTION.

It is also posible to `await()` on the wrapper like you would on `Deferred`, but losing all the benefits of Arrow.

### Error handling & recovery

[`MonadError`]({{ '/docs/typeclasses/monaderror' | relative_url }}) can be used to start a [Monad Comprehension]({{ '/docs/patterns/monadcomprehensions' | relative_url }}) using the method `bindingCatch`, with all its benefits.
These benefits include capturing all exceptions that happen inside the block.

```kotlin
DeferredK.monadError().bindingCatch {
  val songUrl = getSongUrlAsync().bind()
  val musicPlayer = MediaPlayer.load(songUrl)
  val totalTime = musicPlayer.getTotaltime() // Oh oh, total time is 0
  
  val timelineClick = audioTimeline.click().bind()

  val percent = (timelineClick / totalTime * 100).toInt()

  percent
}.unsafeAttemptSync()
 // Failure(ArithmeticException("/ by zero"))
```

Several recovery methods are provided, which you can find in the documentation for [`ApplicativeError`]({{ '/docs/typeclasses/applicativeerror' | relative_url }}).
The most common ones are `handleError` and `handleErrorWith`.

The former allows you to return a single value from a faulty block

```kotlin
val recoveryArrowWrapper = DeferredK { getUserListByIdRange(-1, 2) }
                                 .handleError { listOf() }
recoveryArrowWrapper.unsafeAttemptSync()
// Success(List())
```

whereas the later allows for any `DeferredK` to be returned

```kotlin
val recoveryArrowWrapper = DeferredK { getUserListByIdRange(-1, 2) }
                                 .handleErrorWith { getUserListByIdRange(1, 3) }
recoveryArrowWrapper.unsafeAttemptSync()
// Success(List(User(1), User(2), User(3)))
```

### Subscription and cancellation

`DeferredK` created with `bindingCatch` behave the same way regular `Deferred` do, including cancellation by disposing the subscription.

Note that [`MonadSuspend`]({{ '/docs/effects/monadsuspend' | relative_url }}) provides an alternative to `bindingCatch` called `bindingCancellable` returning a `arrow.Disposable`.
Invoking this `Disposable` causes an `BindingCancellationException` in the chain which needs to be handled by the subscriber, similarly to what `Deferred` does.

```kotlin
val (deferred, unsafeCancel) = 
  DeferredK.monadSuspend().bindingCancellable {
    val userProfile = DeferredK { getUserProfile("123") }.bind()
    val friendProfiles = userProfile.friends().map { friend ->
        DeferredK { getProfile(friend.id) }.bind()
    }
    listOf(userProfile) + friendProfiles
  }

deferred.unsafeRunAsync { result ->
  result.fold({ println("Boom! caused by $it") }, { println(it.toString()) })
}
  
unsafeCancel()
// Boom! caused by BindingCancellationException
```

### Instances

You can see all the type classes `DeferredK` implements below:

```kotlin
import arrow.*
import arrow.effects.*
import arrow.debug.*

showInstances<ForDeferredK, Throwable>()
// [Applicative, ApplicativeError, Functor, Monad, MonadError, MonadSuspend, Async, Effect]
```
