package graphene.core

object Version {
  def fromString(s: String): Option[Version] = try {
    Some(Version(s.split('.').map(_.toInt)))
  } catch {
    case _: Exception => Option.empty[Version]
  }

  def apply(vs: Seq[Int]) = new Version(vs(0), vs(1), vs(2))
}

case class Version(major: Int, minor: Int, patch: Int) {
  def <(other: Version) =
    (major < other.major) ||
    (major == other.major && minor < other.minor) ||
    (major == other.major && minor == other.minor && patch < other.patch)

  def <=(other: Version) = this < other || this == other
  def >(other: Version)  = !(this <= other)
  def >=(other: Version) = !(this < other)

  override def toString = s"$major.$minor.$patch"
}

