package unyo

package object util {

  import scala.language.reflectiveCalls

  type Closable = { def close() }

  /** Loan patternの実装。
   *  リソースの初期化時に例外が出る場合にも対応するため、resourceは名前渡しで渡している。*/
  def using[A <: Closable,B](resource: => A)(f: A => B): Either[Exception,B] =
    try {
      val r = resource
      try {
        Right(f(r))
      } catch {
        case e: Exception => Left(e)
      } finally {
        if (r != null) r.close
      }
    } catch {
      case e: Exception => Left(e)
    }
}
