package com.ing.wbaa.akka.http.keycloak

import akka.http.scaladsl.model.headers.{ Authorization, OAuth2BearerToken }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import com.ing.wbaa.akka.http.keycloak.data._
import com.typesafe.scalalogging.LazyLogging
import org.keycloak.RSATokenVerifier
import org.keycloak.adapters.KeycloakDeploymentBuilder
import org.keycloak.common.VerificationException
import org.keycloak.representations.adapters.config.AdapterConfig

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

trait KeycloakTokenVerifier extends LazyLogging {

  protected[this] def keycloakSettings: KeycloakSettings

  implicit protected[this] def executionContext: ExecutionContext

  import scala.collection.JavaConverters._

  /**
    * get the token from http reguest and verify by the provided tokenVerifier
    *
    * @return the verifiedToken or rejection
    */
  def authorizeToken(): Directive1[AuthenticationUserInfo] = {
    bearerToken.flatMap {
      case Some(token) =>
        logger.debug("received oauth token={}", token)
        verifyAuthenticationToken(token) match {
          case Some(t) => provide(t)
          case None =>
            logger.error("Authorization Token could not be verified")
            reject(AuthorizationFailedRejection).toDirective[Tuple1[AuthenticationUserInfo]]
        }
      case None =>
        logger.debug("no credential token")
        reject(AuthorizationFailedRejection)
    }
  }

  /**
    * because the token can be in many places we have to check:
    * - header - OAuth2BearerToken
    * - cookie - X-Authorization-Token
    * - parameters - WebIdentityToken or TokenCode
    * - body - WebIdentityToken or TokenCode
    *
    * @return the directive with authorization token
    */
  private val bearerToken: Directive1[Option[BearerToken]] =
    for {
      tokenFromAuthBearerHeader <- optionalTokenFromAuthBearerHeader
      tokenFromAuthCookie <- optionalTokenFromCookie
      tokenFromWebIdentityToken <- optionalTokenFromWebIdentityToken
      tokenFromTokenCode <- optionalTokenFromTokenCode
    } yield tokenFromAuthBearerHeader
      .orElse(tokenFromAuthCookie)
      .orElse(tokenFromWebIdentityToken)
      .orElse(tokenFromTokenCode)

  private def optionalTokenFromTokenCode = {
    val tokenCodeString = "TokenCode" ? ""
    for {
      tokenFromParam <- parameter(tokenCodeString).map(stringToBearerTokenOption)
      tokenFromField <- formField(tokenCodeString).map(stringToBearerTokenOption)
    } yield tokenFromParam.orElse(tokenFromField)
  }

  private def optionalTokenFromWebIdentityToken = {
    val webIdentityTokenString = "WebIdentityToken" ? ""
    for {
      tokenFromParam <- parameter(webIdentityTokenString).map(stringToBearerTokenOption)
      tokenFromField <- formField(webIdentityTokenString).map(stringToBearerTokenOption)
    } yield tokenFromParam.orElse(tokenFromField)
  }

  private def optionalTokenFromCookie = {
    optionalCookie("X-Authorization-Token").map(_.map(c => BearerToken(c.value)))
  }

  private def optionalTokenFromAuthBearerHeader = {
    optionalHeaderValueByType(classOf[Authorization]).map(extractBearerToken)
  }

  private def extractBearerToken(authHeader: Option[Authorization]): Option[BearerToken] =
    authHeader.collect {
      case Authorization(OAuth2BearerToken(token)) => BearerToken(token)
    }

  private val stringToBearerTokenOption: String => Option[BearerToken] = t => if (t.isEmpty) None else Some(BearerToken(t))


  protected[this] def verifyAuthenticationToken(token: BearerToken): Option[AuthenticationUserInfo] = Try {
    RSATokenVerifier
      .create(token.value)
      .publicKey(keycloakDeployment.getPublicKeyLocator.getPublicKey(
        keycloakSettings.realmPublicKeyId, keycloakDeployment
      ))
      .realmUrl(keycloakDeployment.getRealmInfoUrl)
      .checkRealmUrl(keycloakSettings.checkRealmUrl)
      .verify
      .getToken
  } match {
    case Success(keycloakToken) =>
      logger.debug("Token successfully validated with Keycloak")
      Some(
        AuthenticationUserInfo(
          UserName(keycloakToken.getPreferredUsername),
          keycloakToken.getRealmAccess.getRoles.asScala.toSet.map(UserGroup),
          AuthenticationTokenId(keycloakToken.getId)
        ))
    case Failure(exc: VerificationException) =>
      logger.info("Token verification failed", exc)
      None
    case Failure(exc) =>
      logger.error("Unexpected exception during token verification", exc)
      None
  }

  private[this] lazy val keycloakDeployment = {
    val config = new AdapterConfig()
    config.setRealm(keycloakSettings.realm)
    config.setAuthServerUrl(s"${keycloakSettings.url}/auth")
    config.setSslRequired("external")
    config.setResource(keycloakSettings.resource)
    config.setPublicClient(true)
    config.setConfidentialPort(0)
    KeycloakDeploymentBuilder.build(config)
  }
}