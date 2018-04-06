package config

import akka.agent.Agent
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.StringInputStream
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import play.api.Logger
import play.api.libs.json.{Format, JsString, JsValue, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

class Switches(config: LoginConfig) {
  def allSwitches: Map[String, SwitchState] = agent.get()
  private val agent = Agent[Map[String, SwitchState]](Map.empty)
  private val scheduler = StdSchedulerFactory.getDefaultScheduler
  val fileName = s"${config.stage.toUpperCase}/switches.json"

  class SwitchJob extends Job() {
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
      AWS.s3Client.putObject(request)
      Logger.info(s"$name has been updated to ${state.name}")
      agent.send(newStates)
      Some(())
    } catch {
      case e: Exception => {
        Logger.error(s"Unable to update switch $name ${state.name}", e)
        None
      }
    }
  }

  def start() {
    Logger.info("Starting switches scheduled task")
    val schedule = SimpleScheduleBuilder.simpleSchedule()
      .withIntervalInSeconds(60)
      .repeatForever()

    val trigger = TriggerBuilder.newTrigger()
      .withSchedule(schedule)
      .build()

    if(scheduler.checkExists(job.getKey)) {
      scheduler.deleteJob(job.getKey)
    }
    scheduler.scheduleJob(job, trigger)
    scheduler.start()
  }

  def stop()  {
    Logger.info("Stopping switches scheduled task")
    scheduler.deleteJob(job.getKey)
  }

  def refresh() {
    Logger.debug("Refreshing switches agent")

    try {
      val request = new GetObjectRequest(config.switchBucket, fileName)
      val result = AWS.s3Client.getObject(request)
      val source = Source.fromInputStream(result.getObjectContent).mkString
      val statesInS3 = Json.parse(source).as[Map[String, SwitchState]]

      agent.send(statesInS3)
      result.close()
    }
    catch {
      case e: Exception =>
        Logger.error(s"Unable to get an updated version of switches.json from S3 ${config.switchBucket} $fileName. The switches map is likely to be stale. ", e)
    }
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