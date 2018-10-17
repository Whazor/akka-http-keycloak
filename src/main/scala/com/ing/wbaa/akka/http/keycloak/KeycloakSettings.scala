package com.ing.wbaa.akka.http.keycloak

import akka.actor.{ ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider }
import com.typesafe.config.Config

class KeycloakSettings(config: Config) extends Extension {

  val realmPublicKeyId: String = config.getString("realmPublicKeyId")
  val realm: String = config.getString("realm")
  val resource: String = config.getString("resource")
  val url: String = config.getString("url")
  val checkRealmUrl: Boolean = config.getBoolean("verifyToken.checkRealmUrl")
}

object KeycloakSettings extends ExtensionId[KeycloakSettings] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): KeycloakSettings = new KeycloakSettings(system.settings.config)

  override def lookup(): ExtensionId[KeycloakSettings] = KeycloakSettings
}
