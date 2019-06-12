package config

import com.gu.pandomainauth.PublicSettings
import okhttp3.OkHttpClient
import utils.Loggable

import scala.concurrent.ExecutionContext

class LoginPublicSettings(config: LoginConfig)(implicit ec: ExecutionContext) extends Loggable {
  private val agent = new PublicSettings(config.domain)(new OkHttpClient(), ec)

  def start = {
    log.info("Starting LoginPublicSettings agent")
    agent.start()
  }

  def stop = {
    log.info("Stopping LoginPublicSettings agent")
    agent.stop()
  }

  def publicKey = {
    val key = agent.publicKey
    if(key.isEmpty) log.error("Public key not available")
    key
  }

}
