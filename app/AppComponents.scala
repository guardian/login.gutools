import com.gu.pandomainauth.{PanDomainAuthSettingsRefresher, PublicSettings, S3BucketLoader, Settings}
import config.{AWS, LoginConfig, Switches}
import controllers._
import play.api.ApplicationLoader.Context
import router.Routes

import scala.concurrent.Future

class AppComponents(context: Context) extends LoginControllerComponents(context, new AWS()) {
  override def config = LoginConfig.forStage(asgTags.map(_.stage))

  override val switches = new Switches(config, aws.s3AsyncClient)

  private val s3BucketLoader: S3BucketLoader = aws.PandaHelpers.forAwsSdkV2(aws.s3SyncClient, "pan-domain-auth-settings")

  private lazy val panDomainSettings: PanDomainAuthSettingsRefresher = PanDomainAuthSettingsRefresher(
    domain = config.domain,
    system = "login",
    s3BucketLoader
  )

  private lazy val desktopPanDomainSettings: PanDomainAuthSettingsRefresher = PanDomainAuthSettingsRefresher(
    domain = config.desktopDomain,
    system = "login-desktop",
    s3BucketLoader
  )

  val loginPublicSettings: PublicSettings = PublicSettings(
    new Settings.Loader(s3BucketLoader, s"${config.domain}.settings.public")
  )

  loginPublicSettings.start()

  switches.start()

  applicationLifecycle.addStopHook(() => {
    switches.stop()

    Future.successful(())
  })

  private val app = new Application(this, panDomainSettings)
  private val emergency = new Emergency(loginPublicSettings, this, aws.sesClient, panDomainSettings)
  private val login = new Login(this, panDomainSettings)
  private val desktopLogin = new DesktopLogin(this, desktopPanDomainSettings)
  private val switchesController = new SwitchesController(this, panDomainSettings)

  override lazy val router = new Routes(httpErrorHandler, app, desktopLogin, login, emergency, switchesController, assets)
}
