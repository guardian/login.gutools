import config.{AWS, LoginConfig, LoginPublicSettings, Switches}
import controllers._
import play.api.ApplicationLoader.Context
import router.Routes
import utils.ElkLogging

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context, new AWS()) {
  override val config = LoginConfig.forStage(asgTags.map(_.stage))

  val elkLogging = new ElkLogging(config.stage, aws.region, config.loggingStream, aws.composerAwsCredentialsProvider, applicationLifecycle)

  override val switches = new Switches(config, aws.s3Client)

  val loginPublicSettings = new LoginPublicSettings(config)

  loginPublicSettings.start
  switches.start()

  applicationLifecycle.addStopHook(() => {
    loginPublicSettings.stop
    switches.stop()

    Future.successful(())
  })

  val app = new Application(this, aws.dynamoDbClient)
  val emergency = new Emergency(loginPublicSettings, this, aws.dynamoDbClient, aws.sesClient)
  val login = new Login(this, aws.dynamoDbClient)
  val switchesController = new SwitchesController(this, aws.dynamoDbClient)
  val oidcController = new OIDC(this, aws.dynamoDbClient)

  override lazy val router = new Routes(
    httpErrorHandler,
    app,
    login,
    oidcController,
    emergency,
    switchesController,
    assets
  )
}
