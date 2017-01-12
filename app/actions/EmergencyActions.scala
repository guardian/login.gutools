package actions

import cats.data.Xor
import config.{AWS, LoginConfig, Off, On}
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc.{ActionBuilder, Headers, Request, Result}
import com.github.t3hnar.bcrypt._
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import Xor.{Left, Right}
import scala.concurrent.Future

object EmergencySwitchIsOnAction extends ActionBuilder[Request] {
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    config.Switches.allSwitches.get("emergency") match {
      case Some(On) => block(request)
      case Some(Off) => Future.successful(SeeOther("/emergency/reissue-disabled"))
      case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
    }
  }
}

object EmergencySwitchChangeAccess extends ActionBuilder[Request] {

  lazy val loginConfig = LoginConfig.loginConfig(AWS.eC2Client)

  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {

    def checkPassword(user: EmergencyUser, username: String, password: String): Future[Result] = {
      if (password.isBcrypted(user.passwordHash)) {
        Logger.info(s"$username is authorised to change the Emergency switch.")
        block(request)
      } else {
        refuseSwitchChange(s"The password provided by $username is incorrect. User will be refused access to change emergency switch.")
      }
    }

    def refuseSwitchChange(logErrorMsg: String): Future[Result] = {
      Logger.warn(logErrorMsg)
      Future.successful {
        Unauthorized(
          views.html.switches.switchChange(
            "Authorisation checks failed, the Emergency switch will not be changed. Contact digitalcms.dev@theguardian.com for more help."
          ))
      }
    }

    try {
      val authHeaderUser = getBasicAuthDetails(request.headers)
      val userId = authHeaderUser.id
      val tableName = loginConfig.emergencyAccessTableName
      val userOpt = Scanamo.get[EmergencyUser](AWS.dynamoDbClient)(tableName)('userId -> s"$userId")
      userOpt.map {
        case Left(error) => refuseSwitchChange(s"Error with reading $userId from Dynamo. User will be refused access to change emergency switch.")
        case Right(user) => checkPassword(user, userId, authHeaderUser.password)
      }.getOrElse(refuseSwitchChange(s"User $userId not found. User will be refused access to change emergency switch."))
    } catch {
      case e: EmergencyActionsException =>
        refuseSwitchChange(e.getMessage)
    }
  }

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

case class EmergencyUser(userId: String, passwordHash: String)

case class AuthorizationHeaderUser(id: String, password: String)

class EmergencyActionsException(message: String) extends Exception(message: String)