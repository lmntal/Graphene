package unyo

package object util {

  import scala.language.reflectiveCalls

  type Closable = { def close() }

  def using[A <: Closable,B](resource: A)(f: A => B): Either[Exception,B] =
    try {
      Right(f(resource))
    } catch {
      case e: Exception => Left(e)
    } finally {
      if (resource != null) resource.close
    }
}
