import scala.language.implicitConversions

object PipeOps {
  implicit class Pipe[A](private val self: A) extends AnyVal {
    def pipe[B](f: A => B): B = f(self)
  }
}
