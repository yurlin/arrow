package arrow.free.instances

import arrow.Kind
import arrow.TC
import arrow.free.Cofree
import arrow.free.CofreeOf
import arrow.free.CofreePartialOf
import arrow.free.fix
import arrow.instance
import arrow.typeclasses.Comonad
import arrow.typeclasses.Functor

@instance(Cofree::class)
interface CofreeFunctorInstance<S> : Functor<CofreePartialOf<S>>, TC {
    override fun <A, B> map(fa: CofreeOf<S, A>, f: (A) -> B): Cofree<S, B> = fa.fix().map(f)
}

@instance(Cofree::class)
interface CofreeComonadInstance<S> : CofreeFunctorInstance<S>, Comonad<CofreePartialOf<S>>, TC {
    override fun <A, B> Kind<CofreePartialOf<S>, A>.coflatMap(f: (Kind<CofreePartialOf<S>, A>) -> B): Cofree<S, B> = fix().coflatMap(f)

    override fun <A> Kind<CofreePartialOf<S>, A>.extract(): A = fix().extract()

    override fun <A> Kind<CofreePartialOf<S>, A>.duplicate(): Kind<CofreePartialOf<S>, Cofree<S, A>> = fix().duplicate()
}
