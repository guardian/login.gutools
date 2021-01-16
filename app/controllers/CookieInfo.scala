package controllers

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.model.{Authenticated, AuthenticatedUser, AuthenticationStatus, Expired, GracePeriod, InvalidCookie, NotAuthenticated, NotAuthorized, User}
import com.gu.pandomainauth.service.CookieUtils
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.filters.csrf.CSRF
import utils.Loggable

import scala.util.{Success, Try}

trait DecodedCookie
case object NoCookie extends DecodedCookie
case class BadCookie(reason: String, jsonOrRawCookieData: Either[String, AuthenticatedUser], e: Exception) extends DecodedCookie
case class ValidCookie(validity: String, user: AuthenticatedUser) extends DecodedCookie

class CookieInfo(deps: LoginControllerComponents, dynamoDbClient: AmazonDynamoDB)
  extends LoginController(deps, dynamoDbClient) with Loggable {

  def decodeForm = AuthAction { request =>
    Ok(views.html.cookieInfo.submitCookie(request))
  }

  def decodeCookie = AuthAction(parse.formUrlEncoded) { request =>
    val maybeCookie = request.body.get("cookie").flatMap(_.headOption)
    maybeCookie
      .map(trimCookie)
      .fold[Result](BadRequest){ cookie =>
        val authStatus = decodePandaCookie(cookie)
        Ok(views.html.cookieInfo.cookieResult(authStatus))
      }
  }

  private def decodePandaCookie(cookie: String): DecodedCookie = {
    PanDomain.authStatus(cookie, panDomainSettings.settings.publicKey) match {
      case InvalidCookie(exception) =>
        cookie match {
          case CookieUtils.CookieRegEx(b64data, b64sig) =>
            val data = new String(Base64.decodeBase64(b64data.getBytes("UTF-8")))
            log.info(data)
            Try(deserializeAuthenticatedUser(data)) match {
              case Success(authedUser) =>
                BadCookie(
                  "Signature invalid",
                  Right(authedUser),
                  exception
                )
              case _ =>
                BadCookie(
                  "User data not formatted correctly",
                  Left(data),
                  exception
                )
            }
          case other => BadCookie("Not in <data>.<signature> format", Left(other), exception)
        }
      case NotAuthenticated => NoCookie
      case Authenticated(authedUser) => ValidCookie("Authenticated", authedUser)
      case Expired(authedUser) => ValidCookie("Expired", authedUser)
      case GracePeriod(authedUser) => ValidCookie("Grace period", authedUser)
      case NotAuthorized(authedUser) => ValidCookie("Not authorized", authedUser)
    }
  }

  /** this is copied from CookieUtils as it is private there so we can deserialise
    * cookies even when they have bad signatures */
  private def deserializeAuthenticatedUser(serializedForm: String): AuthenticatedUser = {
    val data = serializedForm
      .split("&")
      .map(_.split("=", 2))
      .map{p => p(0) -> p(1)}
      .toMap

    AuthenticatedUser(
      user = User(data("firstName"), data("lastName"), data("email"), data.get("avatarUrl")),
      authenticatingSystem = data("system"),
      authenticatedIn = Set(data("authedIn").split(",") :_*),
      expires = data("expires").toLong,
      multiFactor = data("multifactor").toBoolean
    )
  }

  private def trimCookie(cookie: String): String = {
    val initialTrim = cookie.trim
    if (initialTrim.contains(" ")) {
      initialTrim.split(" ").last.trim
    } else initialTrim
  }
}
