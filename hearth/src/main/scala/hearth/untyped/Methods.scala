package hearth
package untyped

import hearth.compat.*
import scala.collection.immutable.ListMap
import scala.collection.compat.*

trait Methods { this: MacroCommons =>

  type UntypedParameter

  val UntypedParameter: UntypedParameterModule
  trait UntypedParameterModule { this: UntypedParameter.type =>

    def fromTyped(param: Parameter): UntypedParameter
    def toTyped(instanceTpe: UntypedType)(untyped: UntypedParameter): Parameter

    def name(param: UntypedParameter): String

    def annotations(param: UntypedParameter): List[UntypedExpr]
  }

  implicit final class UntypedParameterMethods(private val param: UntypedParameter) {

    def paramName: String = UntypedParameter.name(param)
    def annotations: List[UntypedExpr] = UntypedParameter.annotations(param)

    def asTyped[Instance: Type]: Parameter = UntypedParameter.toTyped(UntypedType.fromTyped[Instance])(param)
  }

  type UntypedParameters = List[ListMap[String, UntypedParameter]]
  object UntypedParameters {

    def fromTyped(typed: Parameters): UntypedParameters = typed.map { inner =>
      ListMap.from(inner.view.mapValues(_.asUntyped))
    }
    def toTyped[Instance: Type](untyped: UntypedParameters): Parameters = untyped.map { inner =>
      ListMap.from(inner.view.mapValues(_.asTyped[Instance]))
    }
  }

  type UntypedArguments = Map[String, UntypedExpr]
  object UntypedArguments {

    def fromTyped(typed: Arguments): UntypedArguments = typed.view.mapValues(_.value.asUntyped).toMap // wtf?
    def toTyped(untyped: UntypedArguments): Arguments = untyped.view.mapValues(_.as_??).toMap
  }

  /** Platform-specific method representation (`c.Symbol` in 2, `quotes.reflect.Symbol` in 3) */
  type UntypedMethod

  val UntypedMethod: UntypedMethodModule
  trait UntypedMethodModule { this: UntypedMethod.type =>

    final def fromTyped[Out](method: Method[Out]): UntypedMethod = method.untyped
    final def toTyped[In: Type, Out: Type](untyped: UntypedMethod): Method[Out] =
      new Method[Out](untyped, UntypedType.fromTyped[In])
    final def as_??[In: Type](untyped: UntypedMethod): Existential[Method] = {
      val returned = UntypedType.fromTyped[In].returnTypeAt(untyped).as_??
      import returned.Underlying as Returned
      Existential[Method, Returned](toTyped[In, Returned](untyped))
    }

    def name(method: UntypedMethod): String
    def position(method: UntypedMethod): Position
    def annotations(method: UntypedMethod): List[UntypedExpr]

    // TODO: isConstructorArgument?
    def isVal(method: UntypedMethod): Boolean
    def isVar(method: UntypedMethod): Boolean
    def isLazy(method: UntypedMethod): Boolean
    def isDef(method: UntypedMethod): Boolean
    def isInherited(method: UntypedMethod): Boolean
    def isImplicit(method: UntypedMethod): Boolean

    def isPublic(method: UntypedMethod): Boolean
    def isAccessibleHere(method: UntypedMethod): Boolean
  }

  implicit final class UntypedMethodMethods(private val method: UntypedMethod) {

    def methodName: String = UntypedMethod.name(method)
    def methodPosition: Position = UntypedMethod.position(method)

    def annotations: List[UntypedExpr] = UntypedMethod.annotations(method)

    def isVal: Boolean = UntypedMethod.isVal(method)
    def isVar: Boolean = UntypedMethod.isVar(method)
    def isLazy: Boolean = UntypedMethod.isLazy(method)
    def isDef: Boolean = UntypedMethod.isDef(method)
    def isInherited: Boolean = UntypedMethod.isInherited(method)
    def isImplicitMethod: Boolean = UntypedMethod.isImplicit(method)

    def isPublic: Boolean = UntypedMethod.isPublic(method)
    def isAccessibleHere: Boolean = UntypedMethod.isAccessibleHere(method)
  }

  implicit lazy val UntypedMethodOrdering: Ordering[UntypedMethod] =
    // Stabilize order in case of https://github.com/scala/scala3/issues/21672 (does not solve the warnings!)
    Ordering[Position].on[UntypedMethod](_.methodPosition).orElseBy(_.methodName)
}
