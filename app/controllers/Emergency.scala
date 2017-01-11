package controllers

import javax.inject.Inject

import actions.EmergencySwitchIsOnAction
import cats.data.Xor
import com.amazonaws.services.dynamodbv2.model.PutItemResult
import com.github.nscala_time.time.Imports._
import com.gu.pandomainauth.PublicSettings
import com.gu.pandomainauth.model.{User, AuthenticatedUser, CookieParseException, CookieSignatureInvalidException}
import com.gu.pandomainauth.service.CookieUtils
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError
import com.gu.scanamo.syntax._
import config.{AWS, LoginPublicSettings}
import play.api.Logger
import play.api.mvc.{Result, Action, Controller}
import play.api.libs.mailer._
import scala.util.Random
import Xor.{Left, Right}

class Emergency @Inject() (val mailerClient: MailerClient) extends Controller with PanDomainAuthActions {

  val cookieLifetime = 1.day

  def reissueDisabled = Action {
    Ok(views.html.emergency.reissueDisabled())
  }

  def reissue = EmergencySwitchIsOnAction { req =>

    val reissueTopic = "Your login session has not been extended"

    (for {
      publicKey <- LoginPublicSettings.publicKey
      assymCookie <- req.cookies.find(_.name == PublicSettings.assymCookieName)
    } yield {
      try {
        val authenticatedUser: AuthenticatedUser = CookieUtils.parseCookieData(assymCookie.value, publicKey)
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
    Ok(views.html.emergency.newCookieIssued())
  }

  def sendCookieLink = EmergencySwitchIsOnAction { req =>

    val tokenIssuedAt = DateTime.now().getMillis
    val emailAddress= req.body.asFormUrlEncoded.get("email")(0)

    if (emailAddress.matches("^[a-z]+\\.[a-z]+@(guardian.co.uk|theguardian.com)$")) {

      val token = Random.alphanumeric.take(20).mkString

      val tableName = AWS.tokenDynamoTable

      val cookieIssue = NewCookieIssue(token, emailAddress,
        tokenIssuedAt, false)

      //val userOpt = Scanamo.put[NewCookieIssue](AWS.dynamoDbClient)(tableName)(cookieIssue)

      val email = Email(
        "Gutools cookie link",
        "reetta.vaahtoranta@guardian.co.uk",
        Seq(emailAddress),
        bodyText = Some(s"Your link to obtain a new cookie " +
          s"http://localhost:9000/emergency/new-cookie/$token")
      )

      Ok(views.html.emergency.emailSent())

    }
    else {
      BadRequest("Only guardian email addresses are supported")
    }
  }

  def issueNewCookie(userToken: String) = EmergencySwitchIsOnAction { req =>

    val newCookieTopic = "A new cookie has not been created"

    val issueNewCookieTopic = "New cookie has not been created"


    val tenMinutesInMilliSeconds = 600000

    val tableName = AWS.tokenDynamoTable
    val tokenOpt: Option[Xor[DynamoReadError, NewCookieIssue]] = Scanamo.get[NewCookieIssue](AWS.dynamoDbClient)(tableName)('id -> s"$userToken")

    tokenOpt.map {
      case Left(error) => {
        Logger.warn(s"Error when reading entry with $userToken from dynamo. A new cookie will not be issued")
        unauthorised("Checking your access token failed. You will not be issued with a new .", issueNewCookieTopic)
      }
      case Right(tokenEntry: NewCookieIssue) => {
        if (!tokenEntry.used) {
          val tokenAgeInMilliseconds = DateTime.now().getMillis - tokenEntry.requested
          if (tokenAgeInMilliseconds > tenMinutesInMilliSeconds) {
            Unauthorized(views.html.emergency.newCookieFailure("Your link has expired. Could not create a new cookie"))
          }
          else {
            val updatedTokenEntry: PutItemResult = Scanamo.put[NewCookieIssue](AWS.dynamoDbClient)(tableName)(tokenEntry.copy(used=true))
            val expires = (DateTime.now() + cookieLifetime).getMillis
            val names = tokenEntry.email.split("\\.")
            val firstName = names(0).capitalize
            val lastName = names(1).split("@")(0).capitalize
            val user = User(firstName, lastName, tokenEntry.email, None)
            val newAuthUser = AuthenticatedUser(user, "emergency-login", Set("emergency-login"), expires, true)
            val authCookies = generateCookies(newAuthUser)

            Ok(views.html.emergency.reissueSuccess())
              .withCookies(authCookies: _*)
          }
        } else {
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

