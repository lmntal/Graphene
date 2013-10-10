package unyo.plugin.lmntal

import java.io.{BufferedReader,InputStreamReader,PrintWriter,OutputStreamWriter}
import java.io.{File,IOException}
import collection.mutable.Buffer
import collection.JavaConversions._

import scala.actors.Actor._

class LMNtalRuntime extends LMNtalPlugin.Runtime {
  var runner: SlimRunner = null
  var visualGraph: VisualGraph = null
  def exec(options: Seq[String]): VisualGraph = {
    runner = new SlimRunner(options)
    visualGraph = new VisualGraph
    visualGraph.rewrite(runner.next)
    visualGraph
  }
  def next = {
    visualGraph.rewrite(runner.next)
    visualGraph
  }
  def hasNext = runner.hasNext
}

class SlimRunner(options: Seq[String]) {

  val reader = scala.actors.Actor.actor {
    val pb = new ProcessBuilder(Buffer("/Users/charlie/Documents/slim/slim/src/slim", "--json-dump") ++ options)
    pb.redirectErrorStream(true)
    val p = pb.start
    val br = new BufferedReader(new InputStreamReader(p.getInputStream))
    val pw = new PrintWriter(new OutputStreamWriter(p.getOutputStream))
    loop {
      react {
        case "next" => {
          reply(br.readLine)
          pw.println("")
          pw.flush()
        }
        case "exit" => {
          p.destroy
          exit
        }
      }
    }
  }

  var finished = false
  var _next: Option[String] = null
  def hasNext: Boolean = {
    if (finished) return false
    if (_next == null) {
      _next = reader !? "next" match {
        case line: String => Some(line)
        case null => { finished = true; Option.empty[String] }
      }
    }
    _next match {
      case Some(_) => true
      case None    => false
    }
  }

  def next: Graph = {
    val res = if (hasNext) { _next.get } else { throw new RuntimeException("no more element") }
    _next = null
    Membrane.fromString(res)
  }
}