package controllers

import java.time.Duration

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.github.t3hnar.bcrypt._
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import com.gu.play.secretrotation.aws.parameterstore.{AwsSdkV1, SecretSupplier}
import com.gu.play.secretrotation.{RotatingSecretComponents, SnapshotProvider, TransitionTiming}
import config._
import services.switches._
import play.api.ApplicationLoader.Context
import play.api.BuiltInComponentsFromContext
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import play.filters.csrf.CSRFComponents
import play.filters.headers.SecurityHeadersComponents
import utils.Loggable
import services.{AWS, InstanceTags}

import scala.concurrent.{ExecutionContext, Future}

abstract class LoginControllerComponents(context: Context, val aws: AWS) extends BuiltInComponentsFromContext(context)
  with AhcWSComponents with AssetsComponents with CSRFComponents
  with SecurityHeadersComponents with RotatingSecretComponents {

  def httpFilters: Seq[EssentialFilter] = Seq(csrfFilter, securityHeadersFilter)

  def config: LoginConfig
  def switches: SwitchStatus

  lazy val asgTags: Option[InstanceTags] = aws.readTags()

  lazy val panDomainSettings: PanDomainAuthSettingsRefresher =
    new PanDomainAuthSettingsRefresher(config.domain, "login", actorSystem, aws.workflowAwsCredentialsProvider)

  override lazy val secretStateSupplier: SnapshotProvider = {
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

abstract class LoginController(deps: LoginControllerComponents, dynamoDbClient: AmazonDynamoDB) extends BaseController with AuthActions with Loggable {
  final override def wsClient: WSClient = deps.wsClient
  final override def controllerComponents: ControllerComponents = deps.controllerComponents

  final def config: LoginConfig = deps.config
  final def switches: SwitchStatus = deps.switches

  final override lazy val panDomainSettings: PanDomainAuthSettingsRefresher = deps.panDomainSettings

  final override lazy val cacheValidation = true
  final override lazy val authCallbackUrl = config.host + "/oauthCallback"

  final override def validateUser(authedUser: AuthenticatedUser) = PanDomain.guardianValidation(authedUser)

  object EmergencySwitchIsOnAction extends ActionBuilder[Request, AnyContent] {
    final override def parser: BodyParser[AnyContent] = deps.controllerComponents.parsers.default
    final override def executionContext: ExecutionContext = deps.executionContext

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      switches.allSwitches.get("emergency") match {
        case Some(On) => block(request)
        case Some(Off) => Future.successful(SeeOther("/emergency/reissue-disabled"))
        case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
      }
    }
  }
}

