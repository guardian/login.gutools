package controllers

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth._
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser
import config.LoginConfig

trait PanDomainAuthActions extends AuthActions {

  import play.api.Play.current
  lazy val config = play.api.Play.configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
  }

  override def cacheValidation = true

  override def authCallbackUrl: String = LoginConfig.host + "/oauthCallback"

  override lazy val domain: String = LoginConfig.domain

  def credentialsProviderChain = new AWSCredentialsProviderChain(
    new EnvironmentVariableCredentialsProvider(),
    new SystemPropertiesCredentialsProvider(),
    new InstanceProfileCredentialsProvider(),
    new ProfileCredentialsProvider("workflow")
  )

  override lazy val awsCredentials = Some(credentialsProviderChain.getCredentials)

  override lazy val system: String = "login"
}
