package controllers

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import config.LoginConfig
import login.BuildInfo


class Application(deps: LoginControllerComponents, dynamoDbClient: AmazonDynamoDB)
  extends LoginController(deps, dynamoDbClient) {

  def login(returnUrl: String) = AuthAction { implicit request =>
    if (LoginConfig.isValidUrl(config.domain, returnUrl)) {
      Redirect(returnUrl)
    } else {
      Ok("Please redirect to a valid, secure url on the relevant stage")
    }
  }

  def healthCheck() = Action { implicit request =>
    Ok(BuildInfo.gitCommitId)
  }

  def index() = Action { implicit request => Ok("A small application to login a user via pan-domain-auth and redirect them.")}
}
