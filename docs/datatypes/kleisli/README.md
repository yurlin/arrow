---
layout: docs
title: Kleisli
permalink: /docs/datatypes/kleisli/
---

## Kleisli 
Kleisli enables composition of functions that return a monadic value, for instance, an `Option<Int>` or an `Either<String, Int>`, without having functions take an `Option` or `Either` as a parameter.

For example, we have the function `String.toInt()` which can throw a `NumberFormatException` and we want to do a safe conversion like this:

```kotlin
import arrow.core.*
import arrow.data.Kleisli

val optionIntKleisli = Kleisli { str: String ->
  if (str.toCharArray().all { it.isDigit() }) Some(str.toInt()) else None
}

fun String.safeToInt(): Option<Int> {
  return optionIntKleisli.run(this).fix()
}
```

Then when we use the function we have an `Option<Int>`

```kotlin
"a".safeToInt()
// None
```
```kotlin
"1".safeToInt()
// Some(1)
```

## Functions

#### Local
This function allows doing a conversion inside the Kleisli to the original input value before the Kleisli will be executed, creating a Kleisli with the input type of the conversion

```kotlin
optionIntKleisli.local { optStr :Option<String> -> optStr.getOrElse { "0" } }.run(None)
// Some(0)
```

#### Ap
The `ap` function transform the `Kleisli` into another `Kleisli` with a function as a output value.

```kotlin
import arrow.data.fix

val intToDouble = {number:Int -> number.toDouble()}

val optionIntDoubleKleisli = Kleisli { str: String ->
  if (str.toCharArray().all { it.isDigit() }) Some(intToDouble) else None
}
  
optionIntKleisli.ap(optionIntDoubleKleisli,Option.applicative()).fix().run("1")
// Some(1.0)
```

#### Map
The `map` function transform the `Kleisli` output value.

```kotlin
import arrow.syntax.functor.map

optionIntKleisli.map { output -> output + 1 }.fix().run("1")
// Some(2)
```

#### FlatMap
`flatMap` is useful to map the `Kleisli` output into another kleisli

```kotlin
import arrow.data.fix

val optionDoubleKleisli = Kleisli { str: String ->
  if (str.toCharArray().all { it.isDigit() }) Some(str.toDouble()) else None
}
  
optionIntKleisli.flatMap({optionDoubleKleisli},Option.monad()).fix().run("1")
// Some(1.0)
```


#### AndThen
You can use `andThen` to compose with another kleisli

```kotlin
import arrow.data.fix

val optionFromOptionKleisli = Kleisli { number: Int ->
   Some(number+1)
}
  
optionIntKleisli.andThen(optionFromOptionKleisli,Option.monad()).fix().run("1")
// Some(2)
```

with another function

```kotlin
optionIntKleisli.andThen({number: Int -> Some(number+1)}, Option.monad()).fix().run("1")
// Some(2)
```

or to replace the `Kleisli` result

```kotlin
optionIntKleisli.andThen(Some(0), Option.monad()).fix().run("1")
// Some(0)
```


