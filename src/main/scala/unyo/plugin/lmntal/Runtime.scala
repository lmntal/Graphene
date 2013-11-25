package unyo.plugin.lmntal

import java.io.{BufferedReader,InputStreamReader,PrintWriter,OutputStreamWriter}
import java.io.{File,IOException}
import collection.mutable.Buffer
import collection.JavaConversions._

import scala.actors.Actor._

import unyo.utility.model._

class LMNtalRuntime extends LMNtalPlugin.Runtime {
  var runner: SlimRunner = null
  var viewContext: ViewContext = null
  def exec(options: Seq[String]): ViewContext = {
    runner = new SlimRunner(LMNtalPlugin.config.slimPath, options)
    viewContext = new ViewContext
    viewContext.rewrite(runner.next)
    viewContext
  }
  def current = viewContext
  def next = {
    viewContext.rewrite(runner.next)
    viewContext
  }
  def hasNext = runner.hasNext
}

class SlimRunner(slimPath: String, options: Seq[String]) {

  val pb = new ProcessBuilder(Buffer(slimPath, "-t", "--dump-json", "--hl") ++ options)
  pb.redirectErrorStream(true)
  val p = pb.start
  val br = new BufferedReader(new InputStreamReader(p.getInputStream))

  var _next: Option[String] = null
  def hasNext: Boolean = {
    if (_next == null) {
      _next = Option[String](br.readLine)
    }
    _next match {
      case Some(_) => true
      case None    => false
    }
  }

  def next: Graph = {
    val res = if (hasNext) { _next.get } else { throw new RuntimeException("no more element") }
    _next = null
    LMN.fromString(res)
  }
}
