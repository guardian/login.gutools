package config

import akka.agent.Agent
import com.amazonaws.services.s3.model.{GetObjectRequest, ObjectMetadata, PutObjectRequest}
import com.amazonaws.util.StringInputStream
import org.quartz._
import org.quartz.impl.StdSchedulerFactory
import play.api.Logger
import play.api.libs.json.{Format, Json}
import play.json.extra.Variants
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

object Switches {
  lazy val allSwitches: Map[String, SwitchState] = agent.get()
  private val agent = Agent[Map[String, SwitchState]](Map.empty)
  private val bucket = "login-gutools-config"
  private val scheduler = StdSchedulerFactory.getDefaultScheduler

  class SwitchJob extends Job() {
    override def execute(context: JobExecutionContext): Unit = refresh()
  }
  private val job = JobBuilder.newJob(classOf[SwitchJob])
    .withIdentity("refresh-switches-gu-login-tools")
    .build()

  def set(name: String, state: SwitchState) = {
    val newStates = allSwitches + (name -> state)

    val json = Json.toJson(newStates)
    val jsonString = Json.stringify(json)
    val metaData = new ObjectMetadata()
    metaData.setContentLength(jsonString.getBytes("UTF-8").length)
    val request = new PutObjectRequest(bucket, "DEV/switches.json", new StringInputStream(jsonString), metaData) //TODO fix stage

    try {
      AWS.s3Client.putObject(request)
    } catch {
      case e: Exception => Logger.error(s"Unable to update switch $name ${state}", e)
    }
    agent.send(newStates)
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
    scheduler.deleteJob(job.getKey)
  }

  def refresh() = {
    Logger.debug("Refreshing switches agent")
    try {
      val request = new GetObjectRequest(bucket, "DEV/switches.json") //TODO fix stage
      val result = AWS.s3Client.getObject(request)
      val source = Source.fromInputStream(result.getObjectContent).mkString
      val statesInS3 = Json.parse(source).as[Map[String, SwitchState]]

      agent.send(statesInS3)
      result.close
    }
    catch {
      case e: Exception => Logger.error("Unable to get an update from config agent ", e) //TODO fix
    }
  }
}

sealed trait SwitchState {
  val name: String
}
object On extends SwitchState {
  val name = "On"
}
object Off extends SwitchState {
  val name = "Off"
}

object SwitchState {
  implicit val format: Format[SwitchState] = Variants.format[SwitchState]
}