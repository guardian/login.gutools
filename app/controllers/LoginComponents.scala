package controllers

import java.time.Duration
import java.util.function.Supplier

import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import com.gu.pandomainauth.{PanDomain, PanDomainAuthSettingsRefresher}
import com.gu.play.secretrotation.aws.ParameterStore
import com.gu.play.secretrotation.{RotatingSecretComponents, SecretState, TransitionTiming}
import config._
import services.switches._
import play.api.ApplicationLoader.Context
import play.api.libs.ws.WSClient
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.mvc._
import play.api.{BuiltInComponentsFromContext, Logger}
import play.filters.HttpFiltersComponents
import services.{AWS, InstanceTags}

import scala.concurrent.{ExecutionContext, Future}

abstract class LoginControllerComponents(context: Context) extends BuiltInComponentsFromContext(context)
  with AhcWSComponents with AssetsComponents with HttpFiltersComponents with RotatingSecretComponents {

  def config: LoginConfig
  def switches: SwitchStatus

  lazy val instanceTags: Option[InstanceTags] = AWS.readTags()

  lazy val panDomainSettings: PanDomainAuthSettingsRefresher =
    new PanDomainAuthSettingsRefresher(config.domain, "login", actorSystem, AWS.workflowAwsCredentialsProvider)

  override lazy val secretStateSupplier: Supplier[SecretState] = {
    val stack = instanceTags.map(_.stack).getOrElse("flexible")
    val app = instanceTags.map(_.app).getOrElse("login")
    val stage = instanceTags.map(_.stack).getOrElse("DEV")

    new ParameterStore.SecretSupplier(
      TransitionTiming(usageDelay = Duration.ofMinutes(3), overlapDuration = Duration.ofHours(2)),
      parameterName = s"/$stack/$app/$stage/play.http.secret.key",
      AWS.ssmClient
    )
  }
}

abstract class LoginController(deps: LoginControllerComponents) extends BaseController with AuthActions {
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

