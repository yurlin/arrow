---
layout: docs
title: Order
permalink: /docs/typeclasses/order/
---

## Order

The `Order` typeclass abstracts the ability to compare two instances of any object and determine their total order.
Depending on your needs this comparison can be structural -the content of the object-, referential -the memory address of the object-, based on an identity -like an Id field-, or any combination of the above.

It can be considered the typeclass equivalent of Java's `Comparable`.

### Main Combinators

#### compare

`fun compare(a: F, b: F): Int`

Compare [a] with [b]. Returns an Int whose sign is:
  * negative if `x < y`
  * zero     if `x = y`
  * positive if `x > y`

```kotlin
import arrow.*
import arrow.typeclasses.*

order<Int>().compare(1, 2)
// -1
```

#### lte / lt

Lesser than or equal to defines total order in a set, it compares two elements and returns true if they're equal or the first is lesser than the second.
It is the opposite of `gte`.

```kotlin
order<Int>().lte(1, 2)
// true
```

#### gte / gt

Greater than or equal compares two elements and returns true if they're equal or the first is lesser than the second.
It is the opposite of `lte`.

```kotlin
order<Int>().gte(1, 2)
// false
```

#### max / min

Compares two elements and respectively returns the maximum or minimum in respect to their order.

```kotlin
order<Int>().min(1, 2)
// 1
```
```kotlin
order<Int>().max(1, 2)
// 2
```

### Laws

Arrow provides [`OrderLaws`]({{ '/docs/typeclasses/laws#orderlaws' | relative_url }}) in the form of test cases for internal verification of lawful instances and third party apps creating their own `Order` instances.

#### Creating your own `Order` instances

Order has a constructor to create an `Order` instance from a compare function `(F, F) -> Int`.

```kotlin
import arrow.syntax.order.*

val reverseOrder = Order { a: Int, b: Int -> b - a }
1.lt(reverseOrder, 2)
// false
```

Since `Order` can be constructed from the same function as defined by `Comparable` you can retrieve an `Order` instance form any type that implements `Comparable`.

```kotlin
toOrder<Char>().max('A', 'B')
// B
```

See [Deriving and creating custom typeclass]({{ '/docs/patterns/glossary' | relative_url }}) to provide your own `Order` instances for custom datatypes.
