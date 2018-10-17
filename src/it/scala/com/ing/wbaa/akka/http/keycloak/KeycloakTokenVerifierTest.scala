package com.ing.wbaa.akka.http.keycloak

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ing.wbaa.akka.http.keycloak.data.{BearerToken, UserGroup, UserName}
import com.ing.wbaa.akka.http.keycloak.helper.{KeycloakToken, OAuth2TokenRequest}
import org.scalatest.{Assertion, AsyncWordSpec, DiagrammedAssertions}

import scala.concurrent.{ExecutionContextExecutor, Future}

class KeycloakTokenVerifierTest extends AsyncWordSpec with DiagrammedAssertions with OAuth2TokenRequest with KeycloakTokenVerifier {

  implicit val testSystem: ActorSystem = ActorSystem.create("test-system")
  implicit val materializer: ActorMaterializer = ActorMaterializer()(testSystem)
  implicit val exContext: ExecutionContextExecutor = testSystem.dispatcher

  override val keycloakSettings: KeycloakSettings = new KeycloakSettings(testSystem.settings.config) {
    override val realmPublicKeyId: String = "FJ86GcF3jTbNLOco4NvZkUCIUmfYCqoqtOQeMfbhNlE"
  }

  private def withOAuth2TokenRequest(formData: Map[String, String])(testCode: KeycloakToken => Assertion): Future[Assertion] = {
    keycloakToken(formData).map(testCode)
  }

  private val validCredentials = Map("grant_type" -> "password", "username" -> "userone", "password" -> "password", "client_id" -> "sts-airlock")

  "Keycloak verifier" should {
    "return verified token" in withOAuth2TokenRequest(validCredentials) { keycloakToken =>
      val token = verifyAuthenticationToken(BearerToken(keycloakToken.access_token))
      assert(token.map(_.userName).contains(UserName("userone")))
      assert(token.exists(_.userGroups.contains(UserGroup("user"))))
    }
  }

  "return None when an invalid token is provided" in {
    val result = verifyAuthenticationToken(BearerToken("invalid"))
    assert(result.isEmpty)
  }
}
