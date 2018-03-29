---
layout: docs
title: Functor
permalink: /docs/typeclasses/functor/
---

## Functor

The `Functor` typeclass abstracts the ability to `map` over the computational context of a type constructor.
Examples of type constructors that can implement instances of the Functor typeclass include `Option`, `NonEmptyList`,
`List` and many other datatypes that include a `map` function with the shape `fun F<B>.map(f: (A) -> B): F<B>` where `F`
refers to `Option`, `List` or any other type constructor whose contents can be transformed.

### Example

Often times we find ourselves in situations where we need to transform the contents of some datatype. `Functor#map` allows
us to safely compute over values under the assumption that they'll be there returning the transformation encapsulated in the same context.

Consider both `Option` and `Try`:

`Option<A>` allows us to model absence and has two possible states, `Some(a: A)` if the value is not absent and `None` to represent an empty case.

In a similar fashion `Try<A>` may have two possible cases `Success(a: A)` for computations that succeed and `Failure(e: Throwable)` if they fail with an exception.

Both `Try` and `Option` are example datatypes that can be computed over transforming their inner results.

```kotlin
import arrow.*
import arrow.core.*
import arrow.data.*
import arrow.syntax.function.*

Try { "1".toInt() }.map { it * 2 }
Option(1).map { it * 2 }
// Some(2)
```

Both `Try` and `Option` include ready to use `Functor` instances:

```kotlin
val optionFunctor = Option.functor()
```

```kotlin
val tryFunctor = Try.functor()
```

Mapping over the empty/failed cases is always safe since the `map` operation in both Try and Option operate under the bias of those containing success values

```kotlin
import arrow.syntax.option.*

Try { "x".toInt() }.map { it * 2 }
none<Int>().map { it * 2 }
// None
```

Arrow allows abstract polymorphic code that operates over the evidence of having an instance of a typeclass available.
This enables programs that are not coupled to specific datatype implementations.
The technique demonstrated below to write polymorphic code is available for all other `Typeclasses` beside `Functor`.

```kotlin
import arrow.typeclasses.*

inline fun <reified F> multiplyBy2(fa: Kind<F, Int>, FT: Functor<F> = functor()): Kind<F, Int> =
    FT.map(fa, { it * 2 })

multiplyBy2<ForOption>(Option(1))
```

```kotlin
multiplyBy2<ForTry>(Try.pure(1))
```

In the example above we've defined a function that can operate over any data type for which a `Functor` instance is available.
And then we applied `multiplyBy2` to two different datatypes for which Functor instances exist.
This technique applied to other Typeclasses allows users to describe entire programs in terms of behaviors typeclasses removing
dependencies to concrete data types and how they operate.

This technique does not enforce inheritance or any kind of subtyping relationship and is frequently known as [`ad-hoc polymorphism`](https://en.wikipedia.org/wiki/Ad_hoc_polymorphism)
and frequently used in programming languages that support [Typeclasses](https://en.wikipedia.org/wiki/Type_class) and [Higher Kinded Types](https://en.wikipedia.org/wiki/Kind_(type_theory)).

Entire libraries and applications can be written without enforcing consumers to use the lib author provided datatypes but letting
users provide their own provided there is typeclass instances for their datatypes.

### Main Combinators

#### map

Transforms the inner contents

`fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B>`

```kotlin
optionFunctor.map(Option(1), { it + 1 })
// Some(2)
```

#### lift

Lift a function to the Functor context so it can be applied over values of the implementing datatype

`fun <A, B> lift(f: (A) -> B): (Kind<F, A>) -> Kind<F, B>`

```kotlin
val lifted = optionFunctor.lift({ n: Int -> n + 1 })
lifted(Option(1))
// Some(2)
```

#### Other combinators

For a full list of other useful combinators available in `Functor` see the [Source][functor_source]{:target="_blank"}

### Syntax

#### Kind<F, A>#map

Maps over any higher kinded type constructor for which a functor instance is found

```kotlin
Try { 1 }.map({ it + 1 })
// Success(value=2)
```

#### ((A) -> B)#lift

Lift a function into the functor context

```kotlin
import arrow.syntax.functor.*

val f = { n: Int -> n + 1 }.lift<ForOption, Int, Int>()
f(Option(1))
// Some(2)
```


### Laws

Arrow provides [`FunctorLaws`][functor_laws_source]{:target="_blank"} in the form of test cases for internal verification of lawful instances and third party apps creating their own Functor instances.

#### Creating your own `Functor` instances

Arrow already provides Functor instances for most common datatypes both in Arrow and the Kotlin stdlib.
Often times you may find the need to provide your own for unsupported datatypes.

You may create or automatically derive instances of functor for your own datatypes which you will be able to use in the context of abstract polymorfic code
as demonstrated in the [example](#example) above.

See [Deriving and creating custom typeclass]({{ '/docs/patterns/glossary' | relative_url }})

### Data types

The following datatypes in Arrow provide instances that adhere to the `Functor` typeclass.

- [Cofree]({{ '/docs/datatypes/cofree' | relative_url }})
- [Coproduct]({{ '/docs/datatypes/coproduct' | relative_url }})  
- [Coyoneda]({{ '/docs/datatypes/coyoneda' | relative_url }})
- [Either]({{ '/docs/datatypes/either' | relative_url }})
- [EitherT]({{ '/docs/datatypes/eithert' | relative_url }})
- [FreeApplicative]({{ '/docs/datatypes/freeapplicative' | relative_url }})
- [Function1]({{ '/docs/datatypes/function1' | relative_url }})
- [Ior]({{ '/docs/datatypes/ior' | relative_url }})
- [Kleisli]({{ '/docs/datatypes/kleisli' | relative_url }})
- [OptionT]({{ '/docs/datatypes/optiont' | relative_url }})
- [StateT]({{ '/docs/datatypes/statet' | relative_url }})
- [Validated]({{ '/docs/datatypes/validated' | relative_url }})
- [WriterT]({{ '/docs/datatypes/writert' | relative_url }})
- [Yoneda]({{ '/docs/datatypes/yoneda' | relative_url }})
- [Const]({{ '/docs/datatypes/const' | relative_url }})
- [Try]({{ '/docs/datatypes/try' | relative_url }})
- [Eval]({{ '/docs/datatypes/eval' | relative_url }})
- [IO]({{ '/docs/effects/io' | relative_url }})
- [NonEmptyList]({{ '/docs/datatypes/nonemptylist' | relative_url }})
- [Id]({{ '/docs/datatypes/id' | relative_url }})
- [Function0]({{ '/docs/datatypes/function0' | relative_url }})

Additionally all instances of [`Applicative`]({{ '/docs/typeclasses/applicative' | relative_url }}), [`Monad`]({{ '/docs/typeclasses/monad' | relative_url }}) and their MTL variants implement the `Functor` typeclass directly
since they are all subtypes of `Functor`

[functor_source]: https://github.com/arrow-kt/arrow/blob/master/arrow-data/src/main/kotlin/arrow/typeclasses/Functor.kt
[functor_laws_source]: https://github.com/arrow-kt/arrow/blob/master/arrow-test/src/main/kotlin/arrow/laws/FunctorLaws.kt
