package config

import _root_.aws.AwsInstanceTags
import com.amazonaws.auth.BasicAWSCredentials
import play.api.Play.current
import play.api._

object LoginConfig extends AwsInstanceTags {

  lazy val stage: String = readTag("Stage") match {
    case Some(value) => value
    case None => "DEV" // default to dev stage
  }

  val domain: String = stage match {
    case "PROD" => "gutools.co.uk"
    case "DEV" => "local.dev-gutools.co.uk"
    case x => x.toLowerCase() + ".dev-gutools.co.uk"
  }

  val host = "https://login." + domain

  lazy val config = play.api.Play.configuration

}