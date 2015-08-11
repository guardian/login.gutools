package controllers

import com.amazonaws.auth.BasicAWSCredentials
import com.gu.pandomainauth.action.AuthActions
import com.gu.pandomainauth.model.AuthenticatedUser

trait PanDomainAuthActions extends AuthActions {

  import play.api.Play.current
  lazy val config = play.api.Play.configuration

  override def validateUser(authedUser: AuthenticatedUser): Boolean = {
    (authedUser.user.email endsWith ("@guardian.co.uk")) && authedUser.multiFactor
  }

  override def cacheValidation = true

  override def authCallbackUrl: String = config.getString("host").get + "/oauthCallback"

  override lazy val domain: String = config.getString("pandomain.domain").get
  
  lazy val awsSecretAccessKey: Option[String] = config.getString("pandomain.aws.secret")
  lazy val awsKeyId: Option[String] = config.getString("pandomain.aws.keyId")
  override lazy val awsCredentials = for (key <- awsKeyId; secret <- awsSecretAccessKey) yield new BasicAWSCredentials(key, secret)

  override lazy val system: String = "login"
}