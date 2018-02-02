package graphene

import graphene.core.Env
import graphene.core.gui.MainFrame
import com.typesafe.scalalogging.slf4j._

object Graphene extends App with Logging {

  import scala.collection.JavaConversions._

  System.setProperty("apple.laf.useScreenMenuBar", "true");

  logger.info("graphene version {} started", Env.version.map(_.toString).getOrElse("unknown"))

  val runtime = Runtime.getRuntime
  logger.info(s"${runtime.availableProcessors} processors")
  logger.info(s"${runtime.freeMemory / 1024 / 1024}MB free memory")
  logger.info(s"${runtime.totalMemory / 1024 / 1024}MB total memory")
  logger.info(s"${runtime.maxMemory / 1024 / 1024}MB max memory")
  runtime.addShutdownHook(new Thread {
    override def run(): Unit = logger.info("graphene will shutdown...\n\n")
  })

  val frame = MainFrame.instance
  frame.setVisible(true)
  frame.pack

  import java.io.PrintWriter
  import scala.io.Source

  if(args.size > 0 && args(0) == "--lmntal.file"){
    val file = args(1)
    frame.runWithFile(file)
  }


  graphene.core.Updater.runAsync

}
