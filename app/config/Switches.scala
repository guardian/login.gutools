package config

import _root_.utils.{Loggable, Notifier}
import com.gu.etagcaching.ETagCache
import com.gu.etagcaching.FreshnessPolicy.TolerateOldValueWhileRefreshing
import com.gu.etagcaching.aws.s3.ObjectId
import com.gu.etagcaching.aws.sdkv2.s3.S3ObjectFetching
import play.api.libs.json.{Format, JsString, JsValue, Json}
import software.amazon.awssdk.services.s3.S3AsyncClient

import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class Switches(config: LoginConfig, s3AsyncClient: S3AsyncClient) extends Loggable {

  type SwitchMap = Map[String, SwitchState]
  val switchMapCache: ETagCache[ObjectId, SwitchMap] = new ETagCache(
    S3ObjectFetching.byteArraysWith(s3AsyncClient)
      .thenParsing(Json.parse(_).as[SwitchMap])
      .onUpdate { update => logSwitchDiff(update.oldV,update.newV) },
    TolerateOldValueWhileRefreshing,
    _.maximumSize(1).refreshAfterWrite(1.minute)
  )

  private val scheduler = Executors.newScheduledThreadPool(2)

  private val notifier = new Notifier(config)

  val fileName = s"${config.stage.toUpperCase}/switches.json"

  def allSwitches: Future[Map[String, SwitchState]] =
    switchMapCache.get(ObjectId(config.switchBucket, fileName)).map(_.getOrElse(Map.empty))

  def start(): Unit = {
    log.info("Starting switches scheduled task")
    scheduler.scheduleAtFixedRate(() => notifyIfSwitchStillActive(), 0, 1, TimeUnit.HOURS)
  }

  def stop(): Unit = {
    log.info("Stopping switches scheduled task")
    scheduler.shutdown()
  }

  private def logSwitchDiff(oldSwitchesOpt: Option[SwitchMap], newSwitchesOpt: Option[SwitchMap]): Unit = {
    newSwitchesOpt.fold {
      log.error(s"Unable to get an updated version of switches.json from S3 ${config.switchBucket} $fileName. The switches map is likely to be stale.")
    } { _.foreach { case (switchName, newState) =>
        oldSwitchesOpt.flatMap(_.get(switchName)) match {
          case Some(oldState) if oldState != newState =>
            notifier.sendStateChangeNotification(switchName, newState)
            log.info(s"$switchName has been changed to ${newState.name}")
          case None if newState == On =>
            notifier.sendStillActiveNotification(switchName)
          case _  =>
        }
      }
    }
  }

  def notifyIfSwitchStillActive(): Unit =
    allSwitches.foreach(_.filter(_._2 == On).keys.foreach(notifier.sendStillActiveNotification))
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
