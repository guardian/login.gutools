package actions

import config.{Off, On}
import play.api.mvc.Results._
import play.api.mvc.{ActionBuilder, Request, Result}

import scala.concurrent.Future

object EmergencySwitchIsOnAction extends ActionBuilder[Request]{
  override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
    config.Switches.allSwitches.get("emergency") match {
      case Some(On) => block(request)
      case Some(Off) => Future.successful(SeeOther("/emergency/reissue-disabled"))
      case _ => Future.successful(BadRequest("Emergency reissue config switch is not configured correctly, value must be 'on' or 'off'."))
    }
  }
}