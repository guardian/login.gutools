package controllers

import com.github.nscala_time.time.Imports._
import com.gu.pandomainauth.model.{AuthenticatedUser, CookieParseException, CookieSignatureInvalidException, User}
import com.gu.pandomainauth.service.CookieUtils
import com.gu.pandomainauth.{PublicKey, PublicSettings}
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import config.{AWS, LoginPublicSettings}
import mailer._
import play.api.Logger
import play.api.mvc._

import scala.util.Random
import scala.util.control.NonFatal


class Emergency(loginPublicSettings: LoginPublicSettings, deps: LoginControllerComponents) extends LoginController(deps) {

  val cookieLifetime = 1.day

  def reissueDisabled = Action {
    Ok(views.html.emergency.reissueDisabled())
  }

  def reissue = EmergencySwitchIsOnAction { req =>

    val reissueTopic = "Your login session has not been extended"

    (for {
      publicKey <- loginPublicSettings.publicKey
      assymCookie <- req.cookies.find(_.name == PublicSettings.assymCookieName)
    } yield {
      try {
        val authenticatedUser = CookieUtils.parseCookieData(assymCookie.value, PublicKey(publicKey))
        if (validateUser(authenticatedUser)) {
          val expires = (DateTime.now() + cookieLifetime).getMillis
          val newAuthUser = authenticatedUser.copy(expires = expires)
          val authCookies = generateCookies(newAuthUser)
          Ok(views.html.emergency.reissueSuccess())
            .withCookies(authCookies: _*)
        } else {
          unauthorised("Only Guardian email addresses with two-factor auth are supported.", reissueTopic)
        }
      }
      catch {
        case e: CookieSignatureInvalidException =>
          unauthorised("Invalid existing session, could not log you in.", reissueTopic)
        case e: CookieParseException =>
          unauthorised("Could not refresh existing session due to a corrupted cookie.", reissueTopic)
      }
    }).getOrElse {
      unauthorised("No existing login session found, unable to log you in.", reissueTopic)
    }
  }

  def requestCookieLink = EmergencySwitchIsOnAction { req =>
    Ok(views.html.emergency.requestNewCookie())
  }

  def sendCookieLink = EmergencySwitchIsOnAction { req =>

    val tokenIssuedAt = DateTime.now().getMillis

    try {
      val emailPrefix = req.body.asFormUrlEncoded.get("email")(0)

      val emailAddress = s"$emailPrefix@guardian.co.uk"

      val token = Random.alphanumeric.take(20).mkString

      val cookieIssue = NewCookieIssue(token, emailAddress,
        tokenIssuedAt, false)

      try {
        val userOpt = Scanamo.put[NewCookieIssue](AWS.dynamoDbClient)(config.tokensTableName)(cookieIssue)
        val ses = new SES(AWS.sesClient, config)
        ses.sendCookieEmail(token, emailAddress)

        Ok(views.html.emergency.emailSent())
      }
      catch {
        case NonFatal(e) => InternalServerError(e.toString)
      }
    }
    catch {
      case NonFatal(e) => BadRequest("both first and last names must be submitted")
    }
  }

  def issueNewCookie(userToken: String) = EmergencySwitchIsOnAction { req =>

    def issueNewCookie(tokenEntry: NewCookieIssue, tableName: String) = {
      val updatedTokenEntry = Scanamo.put[NewCookieIssue](AWS.dynamoDbClient)(tableName)(tokenEntry.copy(used = true))
      val expires = (DateTime.now() + cookieLifetime).getMillis
      val names = tokenEntry.email.split("\\.")
      val firstName = names(0).capitalize
      val lastName = names(1).split("@")(0).capitalize
      val user = User(firstName, lastName, tokenEntry.email, None)
      val newAuthUser = AuthenticatedUser(user, config.appName, Set(config.appName), expires, true)
      val authCookies = generateCookies(newAuthUser)

      Ok(views.html.emergency.reissueSuccess())
        .withCookies(authCookies: _*)
    }

    val newCookieTopic = "A new cookie has not been created"
    val issueNewCookieTopic = "New cookie has not been created"
    val tenMinutesInMilliSeconds = 600000

    val tableName = config.tokensTableName
    val tokenOpt: Option[Either[DynamoReadError, NewCookieIssue]] =
      Scanamo.get[NewCookieIssue](AWS.dynamoDbClient)(tableName)('id -> s"$userToken")

    tokenOpt.map {
      case Left(error) => {
        Logger.warn(s"Error when reading entry with $userToken from dynamo. A new cookie will not be issued: $error")
        unauthorised("Checking your access token failed. You will not be issued with a new ", issueNewCookieTopic)
      }
      case Right(tokenEntry: NewCookieIssue) => {
        if (!tokenEntry.used) {
          val tokenAgeInMilliseconds = DateTime.now().getMillis - tokenEntry.requested
          if (tokenAgeInMilliseconds > tenMinutesInMilliSeconds) {
            Logger.warn(s"Attempted to use expired token: ${tokenEntry.id}")
            Unauthorized(views.html.emergency.newCookieFailure("Your link has expired. Could not create a new cookie"))
          }
          else {
            issueNewCookie(tokenEntry, tableName)
          }
        } else {
          Logger.warn(s"Attempted to use a used token: ${tokenEntry.id}")
          Unauthorized(views.html.emergency.newCookieFailure("Your link has already been been used"))
        }

      }
    }.getOrElse(Unauthorized("Token not found"))
  }

  private def unauthorised(message: String, topic: String): Result = {
    Logger.warn(message)
    Unauthorized(views.html.emergency.reissueFailure(message, topic))
  }

}

case class NewCookieIssue(id: String, email: String, requested: Long, used: Boolean)

