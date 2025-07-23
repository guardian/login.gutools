package controllers

import java.time.Duration
import com.github.t3hnar.bcrypt._
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import com.gu.play.secretrotation.aws.parameterstore.{AwsSdkV1, SecretSupplier}
import com.gu.play.secretrotation.{RotatingSecretComponents, SnapshotProvider, TransitionTiming}
import config._
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import services.{TokenDBService}
import utils.Loggable

import scala.concurrent.{ExecutionContext, Future}

abstract class LoginControllerComponents(
  context: Context,
  val aws: AWS
) extends BuiltInComponentsFromContext(context)
    with AhcWSComponents
    with AssetsComponents
    with CSRFComponents
    with SecurityHeadersComponents
    with RotatingSecretComponents {

  def httpFilters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter)

  lazy val tokenDBService = new TokenDBService(aws.dynamoDbClient, config.tokensTableName)

  def config: LoginConfig
  def switches: Switches

  lazy val asgTags: Option[InstanceTags] = aws.readTags()

  val secretStateSupplier: SnapshotProvider = {
    val stack = asgTags.map(_.stack).getOrElse("flexible")
    val app = asgTags.map(_.app).getOrElse("login")
    val stage = asgTags.map(_.stage).getOrElse("DEV")

    new SecretSupplier(
      TransitionTiming(usageDelay = Duration.ofMinutes(3), overlapDuration = Duration.ofHours(2)),
      parameterName = s"/$stack/$app/$stage/play.http.secret.key",
      AwsSdkV1(aws.ssmClient)
    )
  }
}

abstract class LoginController(
  deps: LoginControllerComponents,
  final override val panDomainSettings: PanDomainAuthSettingsRefresher
)(implicit val ec: ExecutionContext) extends BaseController with AuthActions with Loggable {

  final override def wsClient: WSClient = deps.wsClient
  final override def controllerComponents: ControllerComponents = deps.controllerComponents

  final def config: LoginConfig = deps.config
  final def switches: Switches = deps.switches

  final override lazy val cacheValidation = true
  override lazy val authCallbackUrl: String = config.host + "/oauthCallback"

  final override def validateUser(authedUser: AuthenticatedUser): Boolean = PanDomain.guardianValidation(authedUser)

  object EmergencySwitchIsOnAction extends ActionBuilder[Request, AnyContent] {
    final override def parser: BodyParser[AnyContent] = deps.controllerComponents.parsers.default
    final override def executionContext: ExecutionContext = deps.executionContext

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = for {
      allSwitches <- switches.allSwitches
      result <- allSwitches.get("emergency") match {
        case Some(On) => block(request)
        case Some(Off) => Future.successful(SeeOther("/emergency/reissue-disabled"))
        case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
      }
    } yield result
  }
}
