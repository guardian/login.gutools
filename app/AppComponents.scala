import com.gu.pandomainauth.PublicSettings
import config.{AWS, LoginConfig, Switches}
import controllers._
import play.api.ApplicationLoader.Context
import utils.ElkLogging
import router.Routes

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context, new AWS()) {
  override def config = LoginConfig.forStage(asgTags.map(_.stage))

  val elkLogging = new ElkLogging(config.stage, aws.region, config.loggingStream, aws.composerAwsCredentialsProvider, applicationLifecycle)

  override val switches = new Switches(config, aws.s3Client)

  val loginPublicSettings = new PublicSettings(
    settingsFileKey = s"${config.domain}.settings.public",
    bucketName = config.pandaAuthBucket,
    s3Client = aws.s3Client
  )

  loginPublicSettings.start()

  switches.start()

  applicationLifecycle.addStopHook(() => {
    switches.stop()

    Future.successful(())
  })

  val app = new Application(this)
  val emergency = new Emergency(loginPublicSettings, this, aws.sesClient)
  val login = new Login(this)
  val switchesController = new SwitchesController(this)

  override lazy val router = new Routes(httpErrorHandler, app, login, emergency, switchesController, assets)
}
