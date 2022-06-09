package graphene

import graphene.core.Env
import graphene.core.gui.MainFrame
import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object Graphene extends App {

  import scala.collection.JavaConversions._

  val logger = Logger(LoggerFactory.getLogger("Graphene"))

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
  frame.pack
  frame.setLocationRelativeTo(null)
  frame.setVisible(true)

  if(args.size > 0){
    if(args(0) == "--lmntal.file"){
      if(args.size == 1){
        System.err.println("File name is required.")
        sys.exit(1)
      }
    }
    if(args(0) == "--help"){
      System.out.println("Usage: java -jar Graphene-assembly-x.x.x.jar [OPTION-OR-FILENAME]")
      sys.exit(0)
    }
    if(!args(args.size-1).startsWith("-")){
      var file = args(args.size-1)
      frame.runWithFile(file)
    }
  }

  graphene.core.Updater.runAsync

}
