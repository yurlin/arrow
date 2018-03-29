---
layout: docs
title: OptionT
permalink: /docs/datatypes/optiont/
video: EWfxL9yBUJo
---

## OptionT

`OptionT` also known as the `Option` monad transformer allows to compute inside the context when `Option` is nested in a different monad.

One issue we face with monads is that they don't compose. This can cause your code to get really hairy when trying to combine structures like `ObservableK` and `Option`. But there's a simple solution, and we're going to explain how you can use Monad Transformers to alleviate this problem.

For our purposes here, we're going to utilize a monad that serves as a container that may hold a value and where a computation can be performed.

Given that both `ObservableK<A>` and `Option<A>` would be examples of datatypes that provide instances for the `Monad` typeclasses.

Because [monads don't compose](http://tonymorris.github.io/blog/posts/monads-do-not-compose/), we may end up with nested structures such as `ObservableK<Option<ObservableK<Option<A>>>` when using `ObservableK` and `Option` together. Using Monad Transformers can help us to reduce this boilerplate.

In the most basic of scenarios, we'll only be dealing with one monad at a time making our lives nice and easy. However, it's not uncommon to get into scenarios where some function calls will return `ObservableK<A>`, and others will return `Option<A>`.

So let's test this out with an example:

```kotlin
import arrow.*
import arrow.core.*

data class Country(val code: Option<String>)
data class Address(val id: Int, val country: Option<Country>)
data class Person(val name: String, val address: Option<Address>)

fun getCountryCode(maybePerson : Option<Person>): Option<String> =
  maybePerson.flatMap { person ->
    person.address.flatMap { address ->
      address.country.flatMap { country ->
        country.code
      }
    }
  }
```

Nested flatMap calls flatten the `Option` but the resulting function starts looking like a pyramid and can easily lead to callback hell.

We can further simplify this case by using Arrow `binding` facilities
that enables monad comprehensions for all datatypes for which a monad instance is available.

```kotlin
import arrow.typeclasses.*

fun getCountryCode(maybePerson : Option<Person>): Option<String> =
  Option.monad().binding {
    val person = maybePerson.bind()
    val address = person.address.bind()
    val country = address.country.bind()
    val code = country.code.bind()
    code
  }.fix()
```

Alright, a piece of cake right? That's because we were dealing with a simple type `Option`. But here's where things can get more complicated. Let's introduce another monad in the middle of the computation. For example what happens when we need to load a person by id, then their address and country to obtain the country code from a remote service?

Consider this simple database representation:

```kotlin
val personDB: Map<Int, Person> = mapOf(
  1 to Person(
        name = "Alfredo Lambda",
        address = Some(
          Address(
            id = 1,
            country = Some(
              Country(
                code = Some("ES")
              )
            )
          )
        )
      )
)

val adressDB: Map<Int, Address> = mapOf(
  1 to Address(
    id = 1,
    country = Some(
      Country(
        code = Some("ES")
      )
    )
  )
)
```

Now we've got two new functions in the mix that are going to call a remote service, and they return a `ObservableK`. This is common in most APIs that handle loading asynchronously.

```kotlin
import arrow.effects.*

fun findPerson(personId : Int) : ObservableK<Option<Person>> =
  ObservableK.pure(Option.fromNullable(personDB.get(personId))) //mock impl for simplicity

fun findCountry(addressId : Int) : ObservableK<Option<Country>> =
  ObservableK.pure(
    Option.fromNullable(adressDB.get(addressId)).flatMap { it.country }
  ) //mock impl for simplicity

```

A naive implementation attempt to get to a `country.code` from a `person.id` might look something like this.

```kotlin
fun getCountryCode(personId: Int) =
  findPerson(personId).map { maybePerson ->
    maybePerson.map { person ->
      person.address.map { address ->
        findCountry(address.id).map { maybeCountry ->
          maybeCountry.map { country ->
            country.code
          }
        }
      }  
    }
  }

val lifted = { personId: Int -> getCountryCode(personId) }
lifted
// (kotlin.Int) -> arrow.effects.ObservableK<arrow.core.Option<arrow.core.Option<arrow.effects.ObservableK<arrow.core.Option<arrow.core.Option<kotlin.String>>>>>>
```


This isn't actually what we want since the inferred return type is `ObservableK<Option<Option<ObservableK<Option<Option<String>>>>>>`. We can't use flatMap in this case because the nested expression does not match the return type of the expression they're contained within. This is because we're not flatMapping properly over the nested types.

 Still not ideal. The levels of nesting are pyramidal with `flatMap` and `map` and are as deep as the number of operations that you have to perform.

Let's look at how a similar implementation would look like using monad comprehensions without transformers:

```kotlin
fun getCountryCode(personId: Int): ObservableK<Option<String>> =
      ObservableK.monad().binding {
        val maybePerson = findPerson(personId).bind()
        val person = maybePerson.fold(
          { ObservableK.raiseError<Person>(NoSuchElementException("...")) },
          { ObservableK.pure(it) }
        ).bind()
        val address = person.address.fold(
          { ObservableK.raiseError<Address>(NoSuchElementException("...")) },
          { ObservableK.pure(it) }
        ).bind()
        val maybeCountry = findCountry(address.id).bind()
        val country = maybeCountry.fold(
          { ObservableK.raiseError<Country>(NoSuchElementException("...")) },
          { ObservableK.pure(it) }
        ).bind()
        country.code
      }.fix()
```

While we've got the logic working now, we're in a situation where we're forced to deal with the `None cases`. We also have a ton of boilerplate type conversion with `fold`. The type conversion is necessary because in a monad comprehension you can only use a type of Monad. If we start with `ObservableK`, we have to stay in it’s monadic context by lifting anything we compute sequentially to a `ObservableK` whether or not it's async.

This is a commonly encountered problem, especially in the context of async services. So how can we reconcile the fact that we're mixing `Option` and `ObservableK`?

### Monad Transformers to the Rescue!

Monad Transformers enable you to combine two monads into a super monad. In this case, we're going to use `OptionT`
from Arrow to express the effect of potential absence inside our async computations.

`OptionT` has the form of `OptionT<F, A>`.

This means that for any monad `F` surrounding an `Option<A>` we can obtain an `OptionT<F, A>`.
So our specialization `OptionT<ForObservableK, A>` is the OptionT transformer around values that are of `ObservableK<Option<A>>`.

We can now lift any value to a `OptionT<F, A>` which looks like this:

```kotlin
import arrow.syntax.applicative.*
import arrow.data.*

val optTVal = 1.pure<OptionTPartialOf<ForObservableK>, Int>()
optTVal
// OptionT(value=ObservableK(observable=io.reactivex.internal.operators.observable.ObservableJust@7238bb9d))
```

or

```kotlin
val optTVal = OptionT.fromOption<ForObservableK, Int>(Some(1))
optTVal
// OptionT(value=ObservableK(observable=io.reactivex.internal.operators.observable.ObservableJust@1c88eb5a))
```

And back to the `ObservableK<Option<A>>` running the transformer

```kotlin
optTVal.value()
// ObservableK(observable=io.reactivex.internal.operators.observable.ObservableJust@1c88eb5a)
```

So how would our function look if we implemented it with the OptionT monad transformer?

```kotlin
fun getCountryCode(personId: Int): ObservableK<Option<String>> =
  OptionT.monad<ForObservableK>().binding {
    val person = OptionT(findPerson(personId)).bind()
    val address = OptionT(ObservableK.pure(person.address)).bind()
    val country = OptionT(findCountry(address.id)).bind()
    val code = OptionT(ObservableK.pure(country.code)).bind()
    code
  }.value().fix()
```

Here we no longer have to deal with the `None` cases, and the binding to the values on the left side are already the underlying values we want to focus on instead of the optional values. We have automatically `flatMapped` through the `ObservableK` and `Option` in a single expression reducing the boilerplate and encoding the effects concerns in the type signatures.

Available Instances:

```kotlin
import arrow.debug.*

showInstances<OptionTPartialOf<ForObservableK>, Unit>()
// [Applicative, Foldable, Functor, FunctorFilter, Monad, MonoidK, SemigroupK, Traverse, TraverseFilter]
```

Take a look at the [`EitherT` docs]({{ '/docs/datatypes/eithert' | relative_url }}) for an alternative version of this content with the `EitherT` monad transformer

## Credits

Contents partially adapted from [FP for the avg Joe at the 47 Degrees blog](https://www.47deg.com/blog/fp-for-the-average-joe-part-2-scalaz-monad-transformers/)
