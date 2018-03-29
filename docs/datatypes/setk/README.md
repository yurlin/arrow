---
layout: docs
title: SetK
permalink: /docs/datatypes/setK/
---

## SetK

SetK(Kinded Wrapper) is a higher kinded wrapper around the the Set collection interface. 

It can be created from the Kotlin Set type with a convient `k()` function.

```kotlin
import arrow.*
import arrow.core.*
import arrow.data.*

setOf(1, 2, 5, 3, 2).k()
// SetK(set=[1, 2, 5, 3])
```

It can also be initialized with the following:

```kotlin
SetK(setOf(1, 2, 5, 3, 2))
// SetK(set=[1, 2, 5, 3])
```
or
```kotlin
SetK.pure(1)
// SetK(set=[1])
```

given the following:
```kotlin
val oldNumbers = setOf( -11, 1, 3, 5, 7, 9).k()
val evenNumbers = setOf(-2, 4, 6, 8, 10).k()
val integers = setOf(-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5).k()
```
SetK derives the following typeclasses:

[`Semigroup`](/docs/typeclasses/semigroup/) and [`SemigroupK`](/docs/typeclasses/semigroupk/):

```kotlin
val numbers = oldNumbers.combineK(evenNumbers.combineK(integers))
numbers
// SetK(set=[-11, 1, 3, 5, 7, 9, -2, 4, 6, 8, 10, -5, -4, -3, -1, 0, 2])
```
```kotlin
evenNumbers.combineK(integers).combineK(oldNumbers)
// SetK(set=[-2, 4, 6, 8, 10, -5, -4, -3, -1, 0, 1, 2, 3, 5, -11, 7, 9])
```

[`Monoid`](/docs/typeclasses/monoid/) and [`MonoidK`](/docs/typeclasses/monoidk/):
```kotlin
SetK.monoidK().combineK(numbers, SetK.empty())
// SetK(set=[-11, 1, 3, 5, 7, 9, -2, 4, 6, 8, 10, -5, -4, -3, -1, 0, 2])
```

[`Foldable`](/docs/typeclasses/foldable/):
```kotlin
numbers.foldLeft(0) {sum, number -> sum + (number * number)}
// 561
```

Available Instances:

```kotlin
import arrow.debug.*

showInstances<ForSetK, Unit>()
// [Foldable, Monoid, MonoidK, Semigroup, SemigroupK]
```
