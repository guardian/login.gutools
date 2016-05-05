package actions

import play.api.mvc.Results._
import play.api.mvc.{ActionBuilder, Request, Result}
import play.api.Play.current
import scala.concurrent.Future

object EmergencySwitchIsOnAction extends ActionBuilder[Request]{
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    val emergencyReissueSwitchOption = play.api.Play.configuration.getString("emergency.reissue")
    emergencyReissueSwitchOption match {
      case Some("on") => block(request)
      case Some("off") => Future.successful(SeeOther("/emergency/reissue-disabled"))
      case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
    }
  }
}