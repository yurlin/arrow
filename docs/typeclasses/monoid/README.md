---
layout: docs
title: Monoid
permalink: /docs/typeclasses/monoid/
---

## Monoid

`Monoid` extends the `Semigroup` type class, adding an `empty` method to semigroup's `combine`. The empty method must return a value that when combined with any other instance of that type returns the other instance, i.e.

```kotlin
(combine(x, empty) == combine(empty, x) == x)
```

For example, if we have a `Monoid<String>` with `combine` defined as string concatenation, then `empty = ""`.

Having an empty defined allows us to combine all the elements of some potentially empty collection of `T` for which a `Monoid<T>` is defined and return a `T`, rather than an `Option<T>` as we have a sensible default to fall back to.

```kotlin
import arrow.*
import arrow.typeclasses.*
```

And let's see the instance of Monoid<String> in action.

```kotlin
val StringMonoid = monoid<String>()
StringMonoid.empty()
// 
```

```kotlin
import arrow.syntax.monoid.*

listOf("Λ", "R", "R", "O", "W").combineAll()
// ΛRROW
```

```kotlin
import arrow.core.*

listOf<Option<Int>>(Some(1), Some(1)).combineAll()
// Some(2)
```

The advantage of using these type class provided methods, rather than the specific ones for each type, is that we can compose monoids to allow us to operate on more complex types, e.g.

This is also true if we define our own instances. As an example, let's use `Foldable`'s `foldMap`, which maps over values accumulating the results, using the available `Monoid` for the type mapped onto.

```kotlin
import arrow.data.*

ListK.foldable().foldMap(
  monoid<Int>(),
  listOf(1, 2, 3, 4, 5).k(),
  ::identity
)
// 15
```

```kotlin
ListK.foldable().foldMap(
  monoid<String>(),
  listOf(1, 2, 3, 4, 5).k(),
  { it.toString() }
)
// 12345
```

To use this with a function that produces a tuple, we can define a Monoid for a tuple that will be valid for any tuple where the types it contains also have a Monoid available. 

```kotlin
fun <A, B> monoidTuple(MA: Monoid<A>, MB: Monoid<B>): Monoid<Tuple2<A, B>> =
  object: Monoid<Tuple2<A, B>> {
    override fun combine(x: Tuple2<A, B>, y: Tuple2<A, B>): Tuple2<A, B> {
      val (xa, xb) = x
      val (ya, yb) = y
      return Tuple2(MA.combine(xa, ya), MB.combine(xb, yb))
    }
    override fun empty(): Tuple2<A, B> = Tuple2(MA.empty(), MB.empty())
  }
```

This way we are able to combine both values in one pass, hurrah!

```kotlin
import arrow.instances.*

val M = monoidTuple(IntMonoid, StringMonoid)
val list = listOf(1, 1).k()
ListK.foldable().foldMap(M, list, { n: Int -> 
   Tuple2(n, n.toString()) 
})
// Tuple2(a=2, b=11)
```
