package config

import com.gu.pandomainauth.PublicSettings
import okhttp3.OkHttpClient
import play.api.Logger

import scala.concurrent.ExecutionContext

class LoginPublicSettings(config: LoginConfig)(implicit ec: ExecutionContext) {
  private val agent = new PublicSettings(config.domain)(new OkHttpClient(), ec)

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
