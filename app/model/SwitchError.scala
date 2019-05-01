package model

import play.api.mvc.Result
import play.api.mvc.Results._

sealed trait SwitchError {
  def toResult(message: String): Result = this match {
    case SwitchError.UsersTableError  => InternalServerError(message)
    case SwitchError.UserNotFoundError  => Unauthorized(message)
    case SwitchError.AccessBadPasswordError => Unauthorized(message)
    case SwitchError.PutSwitchStatusError => InternalServerError(message)
    case SwitchError.NonWhitelistedSwitchError(switch) => BadRequest(s"Non-whitelisted switch: $switch")
  }
}

object SwitchError {
  case object UsersTableError extends SwitchError
  case object UserNotFoundError extends SwitchError
  case object AccessBadPasswordError extends SwitchError
  case object PutSwitchStatusError extends SwitchError
  case class NonWhitelistedSwitchError(switch: String) extends SwitchError
}
