package controllers

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB
import com.gu.pandomainauth.model.Authenticated
import com.nimbusds.jose.{EncryptionMethod, JWEAlgorithm}
import org.pac4j.jwt.config.encryption.RSAEncryptionConfiguration
import org.pac4j.jwt.config.signature.RSASignatureConfiguration
import org.pac4j.jwt.profile.JwtGenerator
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import java.security.KeyPairGenerator
import java.util

class OIDC(deps: LoginControllerComponents, dynamoDbClient: AmazonDynamoDB) extends LoginController(deps, dynamoDbClient) {

  // TODO surely these will need to come from config, so they're the same every time
  val keyGen = KeyPairGenerator.getInstance("RSA")
  val rsaKeyPair = keyGen.generateKeyPair()

  val jwtSignatureConfig = new RSASignatureConfiguration(rsaKeyPair);

  val jwtGenerator = new JwtGenerator(
    jwtSignatureConfig/*,
    new RSAEncryptionConfiguration(
      rsaKeyPair,
      JWEAlgorithm.RSA_OAEP_256,
      EncryptionMethod.A256GCM
    )*/
  );

  // bare minimum : all REQUIRED fields from https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderMetadata
  case class DiscoveryResponse(
    issuer: String,
    authorization_endpoint: String,
    token_endpoint: String,
    userinfo_endpoint: String,
    jwks_uri: String,
    response_types_supported: List[String],
    subject_types_supported: List[String],
    id_token_signing_alg_values_supported: List[String]
  )
  implicit val writesDiscoveryResponse = Json.writes[DiscoveryResponse]

  private def issuerURL(implicit request: RequestHeader) = s"https://${request.host}/oidc"

  def discovery = Action { implicit request =>
    Ok(Json.toJson(DiscoveryResponse(
      issuer = issuerURL,
      authorization_endpoint = routes.OIDC.auth().absoluteURL(secure = true),
      token_endpoint = routes.OIDC.token().absoluteURL(secure = true),
      userinfo_endpoint = routes.OIDC.userInfo().absoluteURL(secure = true),
      jwks_uri = routes.OIDC.jwks().absoluteURL(secure = true),
      response_types_supported = List("token"),
      subject_types_supported = List("public"),
      id_token_signing_alg_values_supported = List(jwtSignatureConfig.getAlgorithm.getName)
    ))).as("application/json")
  }

  def auth = AuthAction { implicit request =>
    Ok("authenticated")
  }
  def token = Action { implicit request =>

    extractAuth(request) match {
      case Authenticated(authedUser) => {
        // TODO header missing 'kid' which should correspond to what's served on jwks endpoint
        val claims = new util.HashMap[String, Object]()
        claims.put("iss", issuerURL)
        claims.put("sub", authedUser.user.email) // TODO is this a safe value given spec 'A locally unique and never reassigned identifier within the Issuer for the End-User'
        claims.put("email", authedUser.user.email)
        claims.put("aud", authedUser.authenticatedIn)
        claims.put("exp", new java.lang.Long(authedUser.expires / 1000)) // TODO do we re-use the cookie expiry or set a new expiry relative to token generation (e.g. plus one hour)
        claims.put("iat", new java.lang.Long(System.currentTimeMillis() / 1000))
//        claims.put("auth_time", authedUser.) //TODO figure out a way to get this from the original auth
        Ok(
          jwtGenerator.generate(
            claims
          )
        )
      }
      case otherStatus => Unauthorized(otherStatus.getClass.getSimpleName)
    }


  }
  //TODO consider extending AuthAction to make use of token in Authentication header
  def userInfo = AuthAction { implicit request =>
    Ok(request.user.toJson).as("application/json")
  }
  def jwks = Action { implicit request =>
    Ok("") //TODO expose keyset with id matching what will be on the 'kid' in token header
  }

}
