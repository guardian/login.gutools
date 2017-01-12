package config

import java.net.URL

import com.amazonaws.services.ec2.AmazonEC2Client

import scala.util.control.NonFatal


case class LoginConfig(stage: String, domain: String, host: String, appName: String, emergencyAccessTableName: String,
                       tokensTableName: String, emailSettings: Map[String, String])

object LoginConfig {
  private[config] def createLoginConfig(stageOpt: Option[String]): LoginConfig = {
    val stage = stageOpt.getOrElse("DEV")
    val domain = stage match {
      case "PROD" => "gutools.co.uk"
      case "DEV" => "local.dev-gutools.co.uk"
      case x => x.toLowerCase() + ".dev-gutools.co.uk"
    }
    val host = "https://login." + domain
    val appName = "login.gutools"
    val tokensTableName = s"login.gutools-tokens-${stage.toUpperCase}"
    val emergencyAccessTableName = s"login.gutools-emergency-access-${stage.toUpperCase}"
    val emailSettings = Email.emailSettings

    LoginConfig(stage, domain, host, appName, emergencyAccessTableName, tokensTableName, emailSettings)
  }

  def loginConfig(implicit ec2Client: AmazonEC2Client): LoginConfig = {
    val instanceId = AWS.getInstanceId
    createLoginConfig(AWS.readTag("Stage", instanceId))
  }

  /**
    * returnUrl is a valid URL and host ends with a whitelisted domain
    */
  def isValidUrl(configuredDomainOpt: Option[String], returnUrl: String): Boolean = {
    configuredDomainOpt.exists { configuredDomain =>
      try {
        val url = new URL(returnUrl)
        // valid url, matches panda domain and is secure
        url.getHost.endsWith(configuredDomain) && url.getProtocol == "https"
      } catch {
        // invalid url
        case NonFatal(e) => false
      }
    }
  }
}
