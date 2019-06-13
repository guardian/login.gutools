package config

import java.util.concurrent.{Executors, TimeUnit}

import _root_.utils.Notifier
import akka.agent.Agent
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.StringInputStream
import utils.Loggable
import org.quartz._
import play.api.libs.json.{Format, JsString, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class Switches(config: LoginConfig, s3Client: AmazonS3) extends Loggable {
  def allSwitches: Map[String, SwitchState] = agent.get()
  private val agent = Agent[Map[String, SwitchState]](Map.empty)
  private val scheduler = Executors.newScheduledThreadPool(2)

  private val notifier = new Notifier(config)

  val fileName = s"${config.stage.toUpperCase}/switches.json"

  class SwitchJob extends Job {
    override def execute(context: JobExecutionContext): Unit = refresh()
  }

  private val job = JobBuilder.newJob(classOf[SwitchJob])
    .withIdentity("refresh-switches-gu-login-tools")
    .build()

  def setEmergencySwitch(state: SwitchState): Option[Unit] = {
    val name = "emergency"
    val newStates = allSwitches + (name -> state)
    val json = Json.toJson(newStates)
    val jsonString = Json.stringify(json)
    val metaData = new ObjectMetadata()
    metaData.setContentLength(jsonString.getBytes("UTF-8").length)

    try {
      val request = new PutObjectRequest(config.switchBucket, fileName, new StringInputStream(jsonString), metaData)
      s3Client.putObject(request)
      log.info(s"$name has been updated to ${state.name}")
      agent.send(newStates)
      notifier.sendStateChangeNotification(name, state)
      Some(())
    } catch {
      case e: Exception => {
        log.error(s"Unable to update switch $name ${state.name}", e)
        None
      }
    }
  }

  def start() {
    log.info("Starting switches scheduled task")

    scheduler.scheduleAtFixedRate(() => refresh(), 0, 60, TimeUnit.SECONDS)
    scheduler.scheduleAtFixedRate(() => notifyIfSwitchStillActive(), 0, 60, TimeUnit.MINUTES)
  }

  def stop()  {
    log.info("Stopping switches scheduled task")
    scheduler.shutdown()
  }

  def refresh() {
    log.debug("Refreshing switches agent")

    try {
      val request = new GetObjectRequest(config.switchBucket, fileName)
      val result = s3Client.getObject(request)
      val source = Source.fromInputStream(result.getObjectContent).mkString
      val statesInS3 = Json.parse(source).as[Map[String, SwitchState]]

      agent.send(statesInS3)
      result.close()
    }
    catch {
      case e: Exception =>
        log.error(s"Unable to get an updated version of switches.json from S3 ${config.switchBucket} $fileName. The switches map is likely to be stale. ", e)
    }
  }

  def notifyIfSwitchStillActive(): Unit = {
    agent.get.filter(_._2 == On).keys.foreach(notifier.sendStillActiveNotification)
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