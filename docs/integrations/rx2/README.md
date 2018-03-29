---
layout: docs
title: Rx2
permalink: /docs/integrations/rx2/
---

## RxJava 2

Arrow aims to enhance the user experience when using RxJava. While providing other datatypes that are capable of handling effects, like IO, the style of programming encouraged by the library allows users to generify behavior for any existing abstractions.

One of such abstractions is RxJava, a library focused on providing composable streams that enable reactive programming. Observable streams are created by chaining operators into what are called observable chains.

```kotlin
Observable.from(7, 4, 11, 3)
  .map { it + 1 }
  .filter { it % 2 == 0 }
  .scan { acc, value -> acc + value }
  .toList()
  .subscribeOn(Schedulers.computation())
  .blockingFirst()
//[8, 20, 24]
```

### Integration with your existing Observable chains

The largest quality of life improvement when using Observables in Arrow is the introduction of the [Monad Comprehension]({{ '/docs/patterns/monadcomprehensions' | relative_url }}). This library construct allows expressing asynchronous Observable sequences as synchronous code using binding/bind.

#### Arrow Wrapper

To wrap any existing Observable in its Arrow Wrapper counterpart you can use the extension function `k()`.

```kotlin
import arrow.effects.*
import io.reactivex.*
import io.reactivex.subjects.*

val obs = Observable.fromArray(1, 2, 3, 4, 5).k()
obs
// ObservableK(observable=io.reactivex.internal.operators.observable.ObservableFromArray@1f76ef66)
```

```kotlin
val flow = Flowable.fromArray(1, 2, 3, 4, 5).k()
flow
// FlowableK(flowable=io.reactivex.internal.operators.flowable.FlowableFromArray@1c0519e5)
```

```kotlin
val subject = PublishSubject.create<Int>().k()
subject
// ObservableK(observable=io.reactivex.subjects.PublishSubject@3136025b)
```

You can return to their regular forms using the function `value()`.

```kotlin
obs.value()
// io.reactivex.internal.operators.observable.ObservableFromArray@1f76ef66
```

```kotlin
flow.value()
// io.reactivex.internal.operators.flowable.FlowableFromArray@1c0519e5
```

```kotlin
subject.value()
// io.reactivex.subjects.PublishSubject@3136025b
```

### Observable comprehensions

The library provides instances of [`MonadError`]({{ '/docs/typeclasses/monaderror' | relative_url }}) and [`MonadSuspend`]({{ '/docs/effects/monadsuspend' | relative_url }}).

[`MonadSuspend`]({{ '/docs/effects/async' | relative_url }}) allows you to generify over datatypes that can run asynchronous code. You can use it with `ObservableK` and `FlowableK`.

```kotlin
fun <F> getSongUrlAsync(MS: MonadSuspend<F> = monadSuspend()) =
  MS { getSongUrl() }

val songObservable: ObservableK<Url> = getSongUrlAsync().fix()
val songFlowable: FlowableK<Url> = getSongUrlAsync().fix()
```

[`MonadError`]({{ '/docs/typeclasses/monaderror' | relative_url }}) can be used to start a [Monad Comprehension]({{ '/docs/patterns/monadcomprehensions' | relative_url }}) using the method `bindingCatch`, with all its benefits.

Let's take an example and convert it to a comprehension. We'll create an observable that loads a song from a remote location, and then reports the current play % every 100 milliseconds until the percentage reaches 100%:

```kotlin
getSongUrlAsync()
  .map { songUrl -> MediaPlayer.load(songUrl) }
  .flatMap {
    val totalTime = musicPlayer.getTotaltime()
    Observable.interval(100, Milliseconds)
      .flatMap { 
        Observable.create { musicPlayer.getCurrentTime() }
          .subscribeOn(AndroidSchedulers.mainThread())
          .map { tick -> (tick / totalTime * 100).toInt() }
      }
      .takeUntil { percent -> percent >= 100 }
      .observeOn(Schedulers.immediate())
  }
```

When rewritten using `bindingCatch` it becomes:

```kotlin
ObservableK.monadError().bindingCatch {
  val songUrl = getSongUrlAsync().bind()
  val musicPlayer = MediaPlayer.load(songUrl)
  val totalTime = musicPlayer.getTotaltime()

  val end = PublishSubject.create<Unit>()
  Observable.interval(100, Milliseconds).takeUntil(end).bind()

  val tick = bindIn(UI) { musicPlayer.getCurrentTime() }
  val percent = (tick / totalTime * 100).toInt()
  if (percent >= 100) {
    end.onNext(Unit)
  }

  percent
}.fix()
```

Note that any unexpected exception, like `AritmeticException` when `totalTime` is 0, is automatically caught and wrapped inside the observable. 

### Subscription and cancellation

Observables created with comprehensions like `bindingCatch` behave the same way regular observables do, including cancellation by disposing the subscription.

```kotlin
val disposable = 
  songObservable.value()
    .subscribe({ Log.d("Song $it") } , { prinln("Error $it") })

disposable.dispose()
```

Note that [`MonadSuspend`]({{ '/docs/effects/monadsuspend' | relative_url }}) provides an alternative to `bindingCatch` called `bindingCancellable` returning a `arrow.Disposable`.
Invoking this `Disposable` causes an `BindingCancellationException` in the chain which needs to be handled by the subscriber, similarly to what `Deferred` does.

```kotlin
val (observable, disposable) = 
  ObservableK.monadSuspend().bindingCancellable {
    val userProfile = Observable.create { getUserProfile("123") }
    val friendProfiles = userProfile.friends().map { friend ->
        bindAsync(observableAsync) { getProfile(friend.id) }
    }
    listOf(userProfile) + friendProfiles
  }

observable.value()
  .subscribe({ Log.d("User $it") } , { prinln("Boom! caused by $it") })

disposable()
// Boom! caused by BindingCancellationException
```
