package config

import java.util.concurrent.{Executors, TimeUnit}
import _root_.utils.Notifier
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.StringInputStream
import utils.Loggable
import play.api.libs.json.{Format, JsString, JsValue, Json}

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class Switches(config: LoginConfig, s3Client: AmazonS3) extends Loggable {

  type SwitchMap = Map[String, SwitchState]
  private val atomicSwitchMap: AtomicReference[SwitchMap] = new AtomicReference[SwitchMap](Map.empty)

  private val scheduler = Executors.newScheduledThreadPool(2)

  private val notifier = new Notifier(config)

  val fileName = s"${config.stage.toUpperCase}/switches.json"

  def allSwitches: Map[String, SwitchState] = atomicSwitchMap.get()

  def start(): Unit = {
    log.info("Starting switches scheduled task")

    scheduler.scheduleAtFixedRate(() => refresh(), 0, 1, TimeUnit.MINUTES)
    scheduler.scheduleAtFixedRate(() => notifyIfSwitchStillActive(), 0, 1, TimeUnit.HOURS)
  }

  def stop(): Unit = {
    log.info("Stopping switches scheduled task")
    scheduler.shutdown()
  }

  def refresh(): Unit = {
    log.debug("Refreshing switches agent")

    try {
      val request = new GetObjectRequest(config.switchBucket, fileName)
      val result = s3Client.getObject(request)
      val source = Source.fromInputStream(result.getObjectContent).mkString
      val statesInS3 = Json.parse(source).as[Map[String, SwitchState]]
      
      val currentSwitches = atomicSwitchMap.get()

      atomicSwitchMap.set(statesInS3)
      result.close()

      // Check for any state changes and notify
      statesInS3.foreach { case (switchName, newState) =>
        currentSwitches.get(switchName) match {
          case Some(oldState) if oldState != newState =>
            notifier.sendStateChangeNotification(switchName, newState)
            log.info(s"$switchName has been changed to ${newState.name}")
          case None if newState == On => 
            notifier.sendStillActiveNotification(switchName)
          case _  => 
        }
      }
    }
    catch {
      case e: Exception =>
        log.error(s"Unable to get an updated version of switches.json from S3 ${config.switchBucket} $fileName. The switches map is likely to be stale. ", e)
    }
  }

  def notifyIfSwitchStillActive(): Unit = {
    atomicSwitchMap.get.filter(_._2 == On).keys.foreach(notifier.sendStillActiveNotification)
  }
}

sealed trait SwitchState {
  val name: String
}
object On extends SwitchState {
  val name = "ON"
}
object Off extends SwitchState {
  val name = "OFF"
}

object SwitchState {
  // Used to use play-json-extras which has not been compiled for Scala 2.12
  // This code does not attempt to maintain the original format
  implicit val format: Format[SwitchState] = Format(
    (v: JsValue) => {
      v.validate[String].map {
        case "on" => On
        case _ => Off
      }
    },
    {
      case On => JsString("on")
      case Off => JsString("off")
    }
  )
}
