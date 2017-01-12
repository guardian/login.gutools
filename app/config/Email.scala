package config

import com.amazonaws.services.s3.model.{S3ObjectInputStream, S3Object, GetObjectRequest}
import com.amazonaws.util.StringInputStream
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.Play.current

import scala.io.Source

object Email {

  private val bucketOpt = play.api.Play.configuration.getString("s3.login.config")
  private val loginConfig = LoginConfig.loginConfig(AWS.eC2Client)
  private val fileName = s"${loginConfig.stage.toUpperCase}/email-settings.json"
  val emailSettings: Map[String, String] = getEmailSettings()

  private def getEmailSettings(): Map[String, String] = {
    bucketOpt.map { bucket =>
      try {
        val request = new GetObjectRequest(bucket, fileName)
        val result: S3Object = AWS.s3Client.getObject(request)
        val obj: S3ObjectInputStream = result.getObjectContent()
        val source = Json.parse(Source.fromInputStream(result.getObjectContent).mkString)
        val emailAddress: String = (source \ "email").get.toString
        val password: String = (source \ "password").get.toString
        Map(emailAddress -> emailAddress, password -> password)
      } catch {
        case e: Exception => {
          Logger.error(s"Unable to access email settings", e)
          Map[String, String]()
        }
      }
    }.getOrElse {
      Logger.error(s"S3 bucket login.gutools config not defined. Unable to access email settings")
      Map[String, String]()
    }
  }
}