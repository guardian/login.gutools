package model

import play.api.mvc.Result
import play.api.mvc.Results._

sealed trait SwitchError {
  def toResult(message: String): Result = this match {
    case SwitchError.UsersTableError  => InternalServerError(message)
    case SwitchError.UserNotFoundError  => Unauthorized(message)
    case SwitchError.AccessBadPasswordError => Unauthorized(message)
    case SwitchError.PutSwitchStatusError => InternalServerError(message)
    case SwitchError.UnrecognisedSwitchError(switch) => BadRequest(s"Unrecognised switch: $switch")
  }
}

object SwitchError {
  case object UsersTableError extends SwitchError
  case object UserNotFoundError extends SwitchError
  case object AccessBadPasswordError extends SwitchError
  case object PutSwitchStatusError extends SwitchError
  case class UnrecognisedSwitchError(switch: String) extends SwitchError
}
