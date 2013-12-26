package unyo

import unyo.core.gui.MainFrame
import com.typesafe.scalalogging.slf4j._

object UNYO extends App with Logging {

  System.setProperty("apple.laf.useScreenMenuBar", "true");

  logger.info("unyo unyo started")

  val runtime = Runtime.getRuntime
  logger.info(s"${runtime.availableProcessors} processors")
  logger.info(s"${runtime.freeMemory / 1024 / 1024}MB free memory")
  logger.info(s"${runtime.totalMemory / 1024 / 1024}MB total memory")
  logger.info(s"${runtime.maxMemory / 1024 / 1024}MB max memory")
  runtime.addShutdownHook(new Thread {
    override def run(): Unit = logger.info("unyo unyo will shutdown...\n\n")
  })

  val frame = MainFrame.instance
  frame.setVisible(true)
  frame.pack

}
