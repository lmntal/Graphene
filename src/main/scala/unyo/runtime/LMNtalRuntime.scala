package unyo.runtime

import unyo.entity.{Graph, Graphy}

import java.io.{BufferedReader,InputStreamReader,PrintWriter,OutputStreamWriter}
import java.io.{File,IOException}
import collection.mutable.Buffer
import collection.JavaConversions._

import scala.actors.Actor._

class LMNtalRuntime(file: File, options: java.util.List[String]) {

  val pb = new ProcessBuilder(Buffer("/Users/charlie/Documents/slim/slim/src/slim", "--json-dump") ++ options ++ Buffer(file.getAbsolutePath))
  pb.redirectErrorStream(true)
  val p = pb.start

  val reader = scala.actors.Actor.actor {
    val br = new BufferedReader(new InputStreamReader(p.getInputStream))
    loop {
      react {
        case "next" => reply(br.readLine)
        case "end" => exit
      }
    }
  }

  val writer = actor {
    val pw = new PrintWriter(new OutputStreamWriter(p.getOutputStream))
    loop {
      react {
        case "next" => pw.println("")
        case "end" => exit
      }
    }
  }

  var finished = false
  var _next: Option[String] = null
  def hasNext: Boolean = {
    if (_next == null) {
      _next = reader !? "next" match {
        case line: String => Some(line)
        case null => { finished = true; Option.empty[String] }
      }
      writer ! "next"
    }
    _next match {
      case Some(_) => true
      case None    => false
    }
  }

  def next: Graph = {
    val res = if (hasNext) { _next.get } else { throw new RuntimeException("no more element") }
    _next = null
    Graphy.fromString(res)
  }

}
