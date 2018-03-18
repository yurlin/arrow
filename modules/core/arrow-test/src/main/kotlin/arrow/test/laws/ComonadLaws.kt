package arrow.test.laws

import arrow.Kind
import arrow.data.Cokleisli
import arrow.syntax.functor.map
import arrow.test.generators.genConstructor
import arrow.test.generators.genFunctionAToB
import arrow.typeclasses.Comonad
import arrow.typeclasses.Eq
import arrow.typeclasses.cobinding
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll

object ComonadLaws {

    inline fun <reified F> Comonad<F>.laws(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): List<Law> =
            FunctorLaws.laws(this, cf, EQ) + listOf(
                    Law("Comonad Laws: duplicate then extract is identity", { duplicateThenExtractIsId(cf, EQ) }),
                    Law("Comonad Laws: duplicate then map into extract is identity", { duplicateThenMapExtractIsId(cf, EQ) }),
                    Law("Comonad Laws: map and coflatmap are coherent", { mapAndCoflatmapCoherence(cf, EQ) }),
                    Law("Comonad Laws: left identity", { comonadLeftIdentity(cf, EQ) }),
                    Law("Comonad Laws: right identity", { comonadRightIdentity(cf, EQ) }),
                    Law("Comonad Laws: cokleisli left identity", { cokleisliLeftIdentity(cf, EQ) }),
                    Law("Comonad Laws: cokleisli right identity", { cokleisliRightIdentity(cf, EQ) }),
                    Law("Comonad Laws: cobinding", { cobinding(cf, EQ) })
            )

    inline fun <F> Comonad<F>.duplicateThenExtractIsId(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), { fa: Kind<F, Int> ->
                fa.duplicate().extract().equalUnderTheLaw(fa, EQ)
            })

    inline fun <reified F> Comonad<F>.duplicateThenMapExtractIsId(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), { fa: Kind<F, Int> ->
                fa.duplicate().map(this) { it.extract() }.equalUnderTheLaw(fa, EQ)
            })

    inline fun <reified F> Comonad<F>.mapAndCoflatmapCoherence(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), genFunctionAToB(Gen.int()), { fa: Kind<F, Int>, f: (Int) -> Int ->
                map(fa, f).equalUnderTheLaw(fa.coflatMap { f(it.extract()) }, EQ)
            })

    inline fun <reified F> Comonad<F>.comonadLeftIdentity(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), { fa: Kind<F, Int> ->
                fa.coflatMap { it.extract() }.equalUnderTheLaw(fa, EQ)
            })

    inline fun <reified F> Comonad<F>.comonadRightIdentity(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), genFunctionAToB(genConstructor(Gen.int(), cf)), { fa: Kind<F, Int>, f: (Kind<F, Int>) -> Kind<F, Int> ->
                fa.coflatMap(f).extract().equalUnderTheLaw(f(fa), EQ)
            })

    inline fun <reified F> Comonad<F>.cokleisliLeftIdentity(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), genFunctionAToB(genConstructor(Gen.int(), cf)), { fa: Kind<F, Int>, f: (Kind<F, Int>) -> Kind<F, Int> ->
                Cokleisli(this, { hk: Kind<F, Int> -> hk.extract() }).andThen(Cokleisli(this, f)).run(fa).equalUnderTheLaw(f(fa), EQ)
            })

    inline fun <reified F> Comonad<F>.cokleisliRightIdentity(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), genFunctionAToB(genConstructor(Gen.int(), cf)), { fa: Kind<F, Int>, f: (Kind<F, Int>) -> Kind<F, Int> ->
                Cokleisli(this, f).andThen(Cokleisli(this, { hk: Kind<F, Kind<F, Int>> -> hk.extract() })).run(fa).equalUnderTheLaw(f(fa), EQ)
            })

    inline fun <reified F> Comonad<F>.cobinding(crossinline cf: (Int) -> Kind<F, Int>, EQ: Eq<Kind<F, Int>>): Unit =
            forAll(genConstructor(Gen.int(), cf), { fa: Kind<F, Int> ->
                cobinding {
                    val x = fa.extract()
                    val y = extract { map(fa, { it + x }) }
                    map(fa, { x + y })
                }.equalUnderTheLaw(map(fa, { it * 3 }), EQ)
            })
}
