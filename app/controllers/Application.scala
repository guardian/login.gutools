package controllers

import com.netaporter.uri.Uri
import controllers.Login._
import play.api.mvc._
import com.netaporter.uri.dsl._
import play.api.Play.current

object Application extends Controller {

  lazy val domainWhiteList = play.api.Play.configuration.getStringSeq("redirect.whitelist").map(_.toList).getOrElse(Nil)

  /**
    * returnUrl is a valid domain and host of returnUrl ends with a whitelisted domain
    * @param returnUrl the url to return to
    * @return
    */
  def isGuToolsDomain(returnUrl: String): Boolean = {

    val uri: Uri = returnUrl

    uri.host match {
      case Some(host) => domainWhiteList.exists(d => host.endsWith(d))
      case None => false
    }
  }

  def login(returnUrl: String) = AuthAction { request =>
    if (isGuToolsDomain(returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Not a whitelisted gutools.co.uk domain")
    }

  }

  def healthcheck() = {
    Ok("Ok")
  }

}
