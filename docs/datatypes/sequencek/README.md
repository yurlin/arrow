---
layout: docs
title: SequenceK
permalink: /docs/datatypes/sequenceK/
---

## SequenceK

SequenceK implements lazy lists representing lazily-evaluated ordered sequence of homogenous values.

It can be created from Kotlin Sequence type with a convenient `k()` function.

```kotlin
import arrow.*
import arrow.data.*

sequenceOf(1, 2, 3).k()
// SequenceK(sequence=kotlin.collections.ArraysKt___ArraysKt$asSequence$$inlined$Sequence$1@2fc1b097)
```

SequenceK derives many useful typeclasses. For instance, it has a [`SemigroupK`](/docs/typeclasses/semigroupk/) instance.

```kotlin
val hello = sequenceOf('h', 'e', 'l', 'l', 'o').k()
val commaSpace = sequenceOf(',', ' ').k()
val world = sequenceOf('w', 'o', 'r', 'l', 'd').k()

hello.combineK(commaSpace.combineK(world)).toList() == hello.combineK(commaSpace).combineK(world).toList()
// true
```

[`Functor`](/docs/typeclasses/functor/)

Transforming a sequence:
```kotlin
val fibonacci = generateSequence(0 to 1) { it.second to it.first + it.second }.map { it.first }.k()
fibonacci.map { it * 2 }.takeWhile { it < 10 }.toList()
// [0, 2, 2, 4, 6]
```

[`Applicative`](/docs/typeclasses/applicative/)

Applying a sequence of functions to a sequence:
```kotlin
SequenceK.applicative().ap(sequenceOf(1, 2, 3).k(), sequenceOf({ x: Int -> x + 1}, { x: Int -> x * 2}).k()).toList()
// [2, 3, 4, 2, 4, 6]
```

SequenceK is a [`Monad`](/docs/_docs/typeclasses/monad/) too. For example, it can be used to model non-deterministic computations. (In a sense that the computations return an arbitrary number of results.)

```kotlin
import arrow.typeclasses.*

val positive = generateSequence(1) { it + 1 }.k() // sequence of positive numbers
val positiveEven = positive.filter { it % 2 == 0 }.k()

SequenceK.monad().binding {
   val p = positive.bind()
   val pe = positiveEven.bind()
   p + pe
}.fix().take(5).toList()
// [3, 5, 7, 9, 11]
```

Folding a sequence,

```kotlin
sequenceOf('a', 'b', 'c', 'd', 'e').k().foldLeft("") { x, y -> x + y }
// abcde
```

Available Instances:

```kotlin
import arrow.debug.*

showInstances<ForSequenceK, Unit>()
// [Applicative, Foldable, Functor, Monad, Monoid, MonoidK, Semigroup, SemigroupK, Traverse, TraverseFilter]
```
