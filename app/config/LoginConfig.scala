package config

import java.net.URL

import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient

import scala.util.control.NonFatal


case class LoginConfig(stage: String, domain: String, host: String, appName: String, emergencyAccessTableName: String,
                       tokensTableName: String, tokenReissueUri: String, emailSettings: Map[String, String], switchBucket: String, notifyOnSwitchChange: List[String])

object LoginConfig {
 def forStage(stageOpt: Option[String]): LoginConfig = {
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
    val tokenReissueUri = host + "/emergency/new-cookie/"
    val emailSettings = Map(
      "from" -> "editorial.tools.dev@theguardian.com",
      "replyTo" -> "core.central.production@guardian.co.uk"
    )

   val switchBucket = "login-gutools-config"

   val notifyOnSwitchChange = List(
     "editorial.tools.dev@theguardian.com",
     "core.central.production@guardian.co.uk"
   )

    LoginConfig(stage, domain, host, appName, emergencyAccessTableName,
      tokensTableName, tokenReissueUri, emailSettings, switchBucket, notifyOnSwitchChange)
  }

  /**
    * returnUrl is a valid URL and host ends with a whitelisted domain
    */
  def isValidUrl(domain: String, returnUrl: String): Boolean = {
    try {
      val url = new URL(returnUrl)
      // valid url, matches panda domain and is secure
      url.getHost.endsWith(domain) && url.getProtocol == "https"
    } catch {
      // invalid url
      case NonFatal(e) => false
    }
  }
}
