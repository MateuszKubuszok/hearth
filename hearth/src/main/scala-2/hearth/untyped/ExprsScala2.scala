package hearth
package untyped

import hearth.MacroCommonsScala2

trait ExprsScala2 extends Exprs { this: MacroCommonsScala2 =>

  import c.universe.*

  final override type UntypedExpr = Tree

  object UntypedExpr extends UntypedExprModule {

    override def fromTyped[A](expr: Expr[A]): UntypedExpr = expr.tree
    override def toTyped[A: Type](untyped: UntypedExpr): Expr[A] = c.Expr[A](untyped)
    override def as_??(untyped: UntypedExpr): Expr_?? = {
      val resultType: ?? =
        c.Expr(untyped).attemptPipe(_.actualType.finalResultType)(_.staticType.finalResultType).as_??
      import resultType.Underlying as Result
      toTyped[Result](untyped).as_??
    }

    override def defaultValue(instanceTpe: UntypedType)(param: UntypedParameter): Option[UntypedExpr] = ???
  }
}
