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
import services.{EmergencyUser, EmergencyUserDBService, TokenDBService}
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

  lazy val emergencyUserDBService = new EmergencyUserDBService(aws.dynamoDbClient, config.emergencyAccessTableName)
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
) extends BaseController with AuthActions with Loggable {

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

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      switches.allSwitches.get("emergency") match {
        case Some(On) => block(request)
        case Some(Off) => Future.successful(SeeOther("/emergency/reissue-disabled"))
        case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
      }
    }
  }

  object EmergencySwitchChangeAccess extends ActionBuilder[Request, AnyContent] {
    final override def parser: BodyParser[AnyContent] = deps.controllerComponents.parsers.default
    final override def executionContext: ExecutionContext = deps.executionContext

    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {

      def checkPassword(user: EmergencyUser, username: String, password: String): Future[Result] = {
        if (password.isBcryptedBounded(user.passwordHash)) {
          log.info(s"$username is authorised to change the Emergency switch.")
          block(request)
        } else {
          refuseSwitchChange(s"The password provided by $username is incorrect. User will be refused access to change emergency switch.")
        }
      }

      def refuseSwitchChange(logErrorMsg: String): Future[Result] = {
        log.warn(logErrorMsg)
        Future.successful {
          Unauthorized(
            views.html.switches.switchChange(
              "Authorisation checks failed, the Emergency switch will not be changed. Contact digitalcms.dev@theguardian.com for more help."
            ))
        }
      }

      try {
        val authHeaderUser = EmergencyActions.getBasicAuthDetails(request.headers)
        val userId = authHeaderUser.id
        val userOpt = deps.emergencyUserDBService.getUser(userId)
        userOpt.map {
          case Left(error) => refuseSwitchChange(s"Error with reading $userId from Dynamo: ${error.toString}. User will be refused access to change emergency switch.")
          case Right(user) => checkPassword(user, userId, authHeaderUser.password)
        }.getOrElse(refuseSwitchChange(s"User $userId not found. User will be refused access to change emergency switch."))
      } catch {
        case e: EmergencyActionsException =>
          refuseSwitchChange(e.getMessage)
      }
    }
  }
}

object EmergencyActions {
  def getBasicAuthDetails(headers: Headers): AuthorizationHeaderUser = {
    val authUserOpt = for {
      authHeaders <- headers.toMap.get("Authorization")
      basicAuthHead <- authHeaders.find(_.startsWith("Basic"))
    } yield {
      val basicAuthHeaderValue = basicAuthHead.split("Basic")(1).trim
      if (!basicAuthHeaderValue.contains(":")) {
        throw new EmergencyActionsException("Authorization header value is not the correct format.")
      }
      val usernameAndPassword = basicAuthHeaderValue.split(":")
      if (usernameAndPassword.length != 2 || !usernameAndPassword(0).endsWith("@guardian.co.uk")) {
        throw new EmergencyActionsException("Authorization header value is not the correct format.")
      }
      AuthorizationHeaderUser(usernameAndPassword(0).split("@guardian.co.uk")(0), usernameAndPassword(1))
    }
    authUserOpt.getOrElse(throw new EmergencyActionsException("Basic authorization header is missing"))
  }
}

case class AuthorizationHeaderUser(id: String, password: String)

class EmergencyActionsException(message: String) extends Exception(message: String)
