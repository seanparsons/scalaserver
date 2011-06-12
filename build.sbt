scalaVersion := "2.9.0-1"

ideaProjectGroup := "ScalaServer"

resolvers += "Akka Repo" at "http://akka.io/repository"

resolvers += "JBoss Repo" at "https://repository.jboss.org/nexus/content/repositories/releases"

libraryDependencies += "se.scalablesolutions.akka" % "akka-actor" % "1.1.2"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "6.0.1"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "0.9.28"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.6.1"