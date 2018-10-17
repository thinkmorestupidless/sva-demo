addSbtPlugin("com.lightbend.rp" % "sbt-reactive-app" % "1.3.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.4")

addSbtPlugin("com.lightbend.cinnamon" % "sbt-cinnamon" % "2.10.0")

credentials += Credentials("Bintray", "dl.bintray.com", "9483eb5a-4e7a-4c34-9986-476b59620449@lightbend", "ebc01bd6df4125fa6e470303b88b09ede59bd244")

resolvers += Resolver.url("lightbend-commercial", url("https://repo.lightbend.com/commercial-releases"))(Resolver.ivyStylePatterns)
