ThisBuild / organization := "com.xmlcalabash"
ThisBuild / organizationName := "XML Calabash"
ThisBuild / organizationHomepage := Some(url("https://xmlcalabash.com/"))

//  case PathList("javax", "servlet", xs @ _*)         => MergeStrategy.first
//  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
//  case PathList("com", "fasterxml", "jackson", xs @ _*)  =>
//     MergeStrategy.first

ThisBuild / assemblyMergeStrategy := {
  case "application.conf"                            => MergeStrategy.concat
  case "module-info.class"                           => MergeStrategy.discard
  case x =>
     val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
     oldStrategy(x)
}

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ndw/xmlcalabash3"),
    "scm:git@github.com:ndw/xmlcalabash3.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id    = "ndw",
    name  = "Norman Walsh",
    email = "ndw@nwalsh.com",
    url   = url("https://nwalsh.com/")
  )
)

ThisBuild / description := "XML Calabash."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://xmlcalabash.com"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
