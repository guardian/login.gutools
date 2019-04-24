package utils

import com.gu.anghammarad.Anghammarad
import com.gu.anghammarad.models._
import config.{LoginConfig, SwitchState}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

class Notifier(config: LoginConfig)(implicit ec: ExecutionContext) extends Loggable {
  def sendStateChangeNotification(switchName: String, state: SwitchState)= sendNotification(s"$switchName switch is now $state", All)

  def sendStillActiveNotification(switchName: String) = sendNotification(s"$switchName switch is still ON", HangoutsChat)

  private def sendNotification(message: String, channel: RequestedChannel): String = {
    log.info("talking to Anghammarad")

    Await.result(Anghammarad.notify(
      subject = "login.gutools switches monitor",
      message = message,
      actions = List(
        // yeah, this is gross, we need better runbooks!
        Action("Open runbook", "https://github.com/guardian/login.gutools#emergency-access-when-google-auth-is-down")
      ),
      target = List(
        Stack("flexible"),
        Stage(config.stage),
        App(config.appName)
      ),
      channel = channel,
      sourceSystem = config.appName,
      topicArn = config.anghammaradSnsArn
    ), Duration.Inf)
  }
}