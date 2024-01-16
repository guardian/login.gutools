package config

import java.net.URL

import scala.util.control.NonFatal


case class LoginConfig(
  stage: String,
  domain: String,
  desktopDomain: String,
  host: String,
  appName: String,
  emergencyAccessTableName: String,
  tokensTableName: String,
  tokenReissueUri: String,
  emailSettings: Map[String, String],
  switchBucket: String,
  pandaAuthBucket: String,
  anghammaradSnsArn: String
)

object LoginConfig {
  def forStage(stageOpt: Option[String]): LoginConfig = {
    val stage = stageOpt.getOrElse("DEV")
    val domain = stage match {
      case "DEV" => "local.dev-gutools.co.uk"
      case "CODE" => "code.dev-gutools.co.uk"
      case "PROD" => "gutools.co.uk"
    }
    val desktopDomain = stage match {
      case "DEV" => "local.integration.flexible.gnm"
      case "CODE" => "code.integration.flexible.gnm"
      case "PROD" => "prod.integration.flexible.gnm"
    }
    val host = "https://login." + domain
    val appName = "login.gutools"
    val tokensTableName = s"login.gutools-tokens-${stage.toUpperCase}"
    val emergencyAccessTableName = s"login.gutools-emergency-access-${stage.toUpperCase}"
    val tokenReissueUri = host + "/emergency/new-cookie/"
    val emailSettings = Map(
      "from" -> "editorial.tools.dev@theguardian.com",
      "replyTo" -> "core.central.production@guardian.co.uk "
    )

    val switchBucket = "login-gutools-config"
    val pandaAuthBucket = "pan-domain-auth-settings"

    val anghammaradSnsArn = "arn:aws:sns:eu-west-1:095768028460:anghammarad-PROD-NotificationTopic-HDJHBGZT0FFD"

    LoginConfig(
      stage = stage,
      domain = domain,
      desktopDomain = desktopDomain,
      host = host,
      appName = appName,
      emergencyAccessTableName = emergencyAccessTableName,
      tokensTableName = tokensTableName,
      tokenReissueUri = tokenReissueUri,
      emailSettings = emailSettings,
      switchBucket = switchBucket,
      pandaAuthBucket = pandaAuthBucket,
      anghammaradSnsArn = anghammaradSnsArn
    )
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
