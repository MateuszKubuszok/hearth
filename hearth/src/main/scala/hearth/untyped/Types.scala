package hearth.untyped

import hearth.MacroCommons
import scala.collection.compat.*
import scala.collection.immutable.ListMap

trait Types { this: MacroCommons =>

  /** Platform-specific untyped type representation (`c.Type` in 2, `quotes.TypeRepr` in 3) */
  type UntypedType

  val UntypedType: UntypedTypeModule
  trait UntypedTypeModule { this: UntypedType.type =>

    def fromTyped[A: Type]: UntypedType
    def toTyped[A](untyped: UntypedType): Type[A]
    final def as_??(untyped: UntypedType): ?? = toTyped[Any](untyped).as_??

    final def isPrimitive(instanceTpe: UntypedType): Boolean =
      Type.primitiveTypes.exists(tpe => instanceTpe <:< fromTyped(using tpe.Underlying))
    final def isBuiltIn(instanceTpe: UntypedType): Boolean =
      Type.builtInTypes.exists(tpe => instanceTpe <:< fromTyped(using tpe.Underlying))

    def isAbstract(instanceTpe: UntypedType): Boolean
    def isFinal(instanceTpe: UntypedType): Boolean

    // TODO: rename class to something more unambiguous
    def isClass(instanceTpe: UntypedType): Boolean

    def isSealed(instanceTpe: UntypedType): Boolean
    def isJavaEnum(instanceTpe: UntypedType): Boolean
    def isJavaEnumValue(instanceTpe: UntypedType): Boolean

    def isCase(instanceTpe: UntypedType): Boolean
    def isObject(instanceTpe: UntypedType): Boolean
    def isVal(instanceTpe: UntypedType): Boolean

    final def isCaseClass(instanceTpe: UntypedType): Boolean = isClass(instanceTpe) && isCase(instanceTpe)
    final def isCaseObject(instanceTpe: UntypedType): Boolean = isObject(instanceTpe) && isCase(instanceTpe)
    final def isCaseVal(instanceTpe: UntypedType): Boolean = isVal(instanceTpe) && isCase(instanceTpe)

    def isPublic(instanceTpe: UntypedType): Boolean
    def isAvailableHere(instanceTpe: UntypedType): Boolean

    def isSubtypeOf(subtype: UntypedType, supertype: UntypedType): Boolean
    def isSameAs(a: UntypedType, b: UntypedType): Boolean

    def primaryConstructor(instanceTpe: UntypedType): Option[UntypedMethod]
    def constructors(instanceTpe: UntypedType): List[UntypedMethod]

    def directChildren(instanceTpe: UntypedType): Option[ListMap[String, UntypedType]]
    final def exhaustiveChildren(instanceTpe: UntypedType): Option[ListMap[String, UntypedType]] =
      directChildren(instanceTpe)
        .flatMap(_.foldLeft[Option[Vector[(String, UntypedType)]]](Some(Vector.empty)) {
          case (None, _)                                      => None
          case (Some(list), (_, subtype)) if subtype.isSealed => exhaustiveChildren(subtype).map(list ++ _)
          case (_, (_, subtype)) if subtype.isAbstract        => None
          case (Some(list), nameSubtype)                      => Some(list :+ nameSubtype)
        })
        .map(_.filter(_._2 <:< instanceTpe)) // TODO: handle it somehow for GADT in abstract type context
        .map(ListMap.from(_))

    def parameterAt(instanceTpe: UntypedType)(param: UntypedParameter): UntypedType

    def parametersAt(instanceTpe: UntypedType)(method: UntypedMethod): UntypedParameters
    def unsafeApplyAt(instanceTpe: UntypedType)(method: UntypedMethod): UntypedArguments => UntypedExpr
    def returnTypeAt(instanceTpe: UntypedType)(method: UntypedMethod): UntypedType
  }

  implicit final class UntypedTypeMethods(private val untyped: UntypedType) {

    def asTyped[A]: Type[A] = UntypedType.toTyped(untyped)
    def as_?? : ?? = UntypedType.as_??(untyped)

    def isPrimitive: Boolean = UntypedType.isPrimitive(untyped)
    def isBuiltIn: Boolean = UntypedType.isBuiltIn(untyped)

    def isAbstract: Boolean = UntypedType.isAbstract(untyped)
    def isFinal: Boolean = UntypedType.isFinal(untyped)

    def isClass: Boolean = UntypedType.isClass(untyped)

    def isSealed: Boolean = UntypedType.isSealed(untyped)
    def isJavaEnum: Boolean = UntypedType.isJavaEnum(untyped)
    def isJavaEnumValue: Boolean = UntypedType.isJavaEnumValue(untyped)

    def isCase: Boolean = UntypedType.isCase(untyped)
    def isObject: Boolean = UntypedType.isObject(untyped)
    def isVal: Boolean = UntypedType.isVal(untyped)

    def isCaseClass: Boolean = UntypedType.isCaseClass(untyped)
    def isCaseObject: Boolean = UntypedType.isCaseObject(untyped)
    def isCaseVal: Boolean = UntypedType.isCaseVal(untyped)

    def isPublic: Boolean = UntypedType.isPublic(untyped)
    def isAvailableHere: Boolean = UntypedType.isAvailableHere(untyped)

    def <:<(other: UntypedType): Boolean = UntypedType.isSubtypeOf(untyped, other)
    def =:=(other: UntypedType): Boolean = UntypedType.isSameAs(untyped, other)

    def primaryConstructor: Option[UntypedMethod] = UntypedType.primaryConstructor(untyped)
    def constructors: List[UntypedMethod] = UntypedType.constructors(untyped)

    def directChildren: Option[ListMap[String, UntypedType]] = UntypedType.directChildren(untyped)
    def exhaustiveChildren: Option[ListMap[String, UntypedType]] = UntypedType.exhaustiveChildren(untyped)
    def parameter(param: UntypedParameter): UntypedType = UntypedType.parameterAt(untyped)(param)
    def defaultValue(param: UntypedParameter): Option[UntypedExpr] = UntypedExpr.defaultValue(untyped)(param)

    def parametersAt(method: UntypedMethod): UntypedParameters =
      UntypedType.parametersAt(untyped)(method)
    def unsafeApplyAt(method: UntypedMethod): UntypedArguments => UntypedExpr =
      UntypedType.unsafeApplyAt(untyped)(method)
    def returnTypeAt(method: UntypedMethod): UntypedType =
      UntypedType.returnTypeAt(untyped)(method)
  }
}
