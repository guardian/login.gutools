package config

import com.gu.pandomainauth.PublicSettings
import dispatch.Http
import play.api.Logger
import scala.concurrent.ExecutionContext.Implicits.global

object LoginPublicSettings {

  private val loginConfig = LoginConfig.loginConfig(AWS.eC2Client)
  private implicit val dispatchClient = Http

  private val agent = new PublicSettings(loginConfig.domain)

  def start = {
    Logger.info("Starting LoginPublicSettings agent")
    agent.start()
  }

  def stop = {
    Logger.info("Stopping LoginPublicSettings agent")
    agent.stop()
  }

  def publicKey = {
    val key = agent.publicKey
    if(key.isEmpty) Logger.error("Public key not available")
    key
  }

}
