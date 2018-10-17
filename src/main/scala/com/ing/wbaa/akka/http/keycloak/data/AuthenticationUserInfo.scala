package com.ing.wbaa.akka.http.keycloak.data

case class AuthenticationTokenId(value: String) extends AnyVal

case class UserGroup(value: String) extends AnyVal

case class UserName(value: String) extends AnyVal

case class UserAssumedGroup(value: String) extends AnyVal

case class AuthenticationUserInfo(
    userName: UserName,
    userGroups: Set[UserGroup],
    keycloakTokenId: AuthenticationTokenId)
