package unyo.util

object Tapper {
  implicit class TapperImpl[T](v: T) {
    def tap(f: T => Unit): T = { f(v); v }
  }
}
