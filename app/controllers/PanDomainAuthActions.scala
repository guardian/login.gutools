package controllers

import com.gu.pandomainauth.PanDomain
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import config.{LoginConfig, AWS}


trait PanDomainAuthActions extends AuthActions {
  val loginConfig = LoginConfig.loginConfig(AWS.eC2Client)

  override lazy val awsCredentialsProvider = AWS.awsCredentialsProvider

  override lazy val domain: String = loginConfig.domain
  override lazy val system = "login"
  override lazy val cacheValidation = true
  override lazy val authCallbackUrl = loginConfig.host + "/oauthCallback"

  override def validateUser(authedUser: AuthenticatedUser) = PanDomain.guardianValidation(authedUser)
}
