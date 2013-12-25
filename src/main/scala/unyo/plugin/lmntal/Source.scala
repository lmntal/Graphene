package unyo.plugin.lmntal

import java.io.{BufferedReader,InputStreamReader,PrintWriter,OutputStreamWriter}
import java.io.{File,IOException}
import collection.mutable.Buffer
import collection.JavaConversions._

import scala.actors.Actor._

import unyo.model._

class LMNtalSource extends LMNtal.Source {

  private var iter: Iterator[Graph] = _
  private var graph: Graph = null

  def run(options: Seq[String]): Graph = {
    val pb = new ProcessBuilder(Buffer("env", s"LMNTAL_HOME=${LMNtal.config.lmntalHome}", LMNtal.config.slimPath, "-t", "--dump-json", "--hl") ++ options)
    println(pb.command.mkString(" "))
    pb.redirectErrorStream(true)
    val p = pb.start
    val br = new BufferedReader(new InputStreamReader(p.getInputStream))

    iter = Iterator.continually(br.readLine).takeWhile(_ != null).map(LMN.fromString(_))
    graph = iter.next
    graph
  }
  def current = graph
  def next = { graph = iter.next.inheritViews(graph); graph }
  def hasNext = iter.hasNext

}
