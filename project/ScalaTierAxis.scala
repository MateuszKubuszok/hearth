import sbt.*

case class ScalaTierAxis(idSuffix: String, directorySuffix: String) extends VirtualAxis.WeakAxis {
  override val suffixOrder: Int = VirtualAxis.scalaABIVersion("x").suffixOrder + 1
}
