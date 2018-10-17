val keycloakVersion = "4.2.1.Final"
lazy val akkaHttpVersion = "10.1.4"
lazy val akkaVersion    = "2.5.16"
lazy val scalatestVersion  = "3.0.5"

name := "akka-http-keycloak"
ThisBuild / organization := "com.ing.wbaa"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version      := "0.1.0-SNAPSHOT"


lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    Defaults.itSettings,
    inThisBuild(List(
      organization := "com.ing.wbaa",
      scalaVersion := "2.12.7",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Akka HTTP Keycloak",
    test in assembly := {},
    libraryDependencies ++= Seq(
      "com.typesafe.scala-logging"   %% "scala-logging"          % "3.9.0",

      "org.keycloak" % "keycloak-core" % keycloakVersion,
      "org.keycloak" % "keycloak-adapter-core" % keycloakVersion,
//      "org.apache.httpcomponents" % "httpclient" % "4.5.6"
      "com.typesafe.akka"            %% "akka-http"              % akkaHttpVersion,
      "com.typesafe.akka"            %% "akka-stream"            % akkaVersion,
      "org.scalatest"     %% "scalatest"            % scalatestVersion  % "it,test",

      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5" % "it,test",

      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.jboss.logging" % "jboss-logging" % "3.3.2.Final",
    ),
    assemblyJarName in assembly := "akka-http-keycloak.jar",
    assemblyOutputPath in assembly := file("./lib/") / (assemblyJarName in assembly).value,
  )
