package arrow.data

import arrow.Kind3
import arrow.core.ForId
import arrow.core.Id
import arrow.core.Right
import arrow.core.comonad
import arrow.test.UnitSpec
import arrow.test.laws.ComonadLaws.laws
import arrow.test.laws.TraverseLaws
import arrow.typeclasses.*
import io.kotlintest.KTestJUnitRunner
import io.kotlintest.matchers.shouldNotBe
import org.junit.runner.RunWith

@RunWith(KTestJUnitRunner::class)
class CoproductTest : UnitSpec() {
    val EQ: Eq<Kind3<ForCoproduct, ForId, ForId, Int>> = Eq { a, b ->
        a.fix().fix() == b.fix().fix()
    }

    init {

        "instances can be resolved implicitly" {
            functor<CoproductPartialOf<ForId, ForNonEmptyList>>() shouldNotBe null
            comonad<CoproductPartialOf<ForId, ForNonEmptyList>>()  shouldNotBe null
            foldable<CoproductPartialOf<ForId, ForNonEmptyList>>() shouldNotBe null
            traverse<CoproductPartialOf<ForId, ForNonEmptyList>>() shouldNotBe null
        }

        testLaws(
            TraverseLaws.laws(traverse(), functor(), { Coproduct(Right(Id(it))) }, EQ),
            Coproduct.comonad(Id.comonad(), Id.comonad()).laws ({ Coproduct(Right(Id(it))) }, EQ)
        )

    }
}
