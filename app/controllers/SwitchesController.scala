package controllers

import services.switches._
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB


class SwitchesController(switchAccess: SwitchAccess, deps: LoginControllerComponents, dynamoDbClient: AmazonDynamoDB) extends LoginController(deps, dynamoDbClient) {

  private def errorMessage(state: SwitchState)  = s"Failed to update Emergency switch to ${state.name}. Contact editorial.tools.dev@theguardian.com for more help."
  private def success(state: SwitchState)  = s"Emergency switch updated to ${state.name}"

  def index = AuthAction { implicit req =>
    Ok(views.html.switches.switchValues(switches.allSwitches))
  }

  def setSwitch(switchName: String) = Action(parse.formUrlEncoded) { req =>
    (for {
        username <- req.body.get("username").flatMap(_.headOption)
        password <- req.body.get("password").flatMap(_.headOption)
        status <- req.body.get("status").flatMap(value => value.headOption.map(SwitchState.fromString))
        result = switchAccess.checkAccess(username, password).flatMap { _ =>
          switches.setSwitch(switchName, status)
        } match {
          case Right(_) => Redirect(routes.SwitchesController.index().url)
          case Left(switchError) => switchError.toResult(errorMessage(status))
        }
    }  yield result).getOrElse(BadRequest("Bad fields in form body"))
  }
}
