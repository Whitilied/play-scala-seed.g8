import ReleaseTransformations._
import com.typesafe.sbt.packager.docker.DockerPermissionStrategy

name := """$name$"""
organization := "$organization$"

version := "1.0-SNAPSHOT"

git.useGitDescribe := true


lazy val root = (project in file("."))
  .enablePlugins(JavaAppPackaging, PlayScala, JavaAgent, sbtdocker.DockerPlugin, GitBranchPrompt, BuildInfoPlugin)
  .settings(
    dockerBaseImage := "java:8",
    dockerPermissionStrategy := DockerPermissionStrategy.CopyChown,
    packageName in Docker := organization.value + "/" + name.value,
    version in Docker := git.gitDescribedVersion.value.getOrElse("Unknown-git-version") ,
    dockerUpdateLatest := true,
    dockerExposedPorts in Docker := Seq(9000),
  )

scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  guice,
  // For kamon module
  "io.kamon" %% "kamon-bundle" % "2.0.2",
  "io.kamon" %% "kamon-prometheus" % "2.0.0",
  "io.kamon" %% "kamon-zipkin" % "2.0.0",

  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
)

// release step
releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,              // : ReleaseStep
  inquireVersions,                        // : ReleaseStep
  runTest,                                // : ReleaseStep
  setReleaseVersion,                      // : ReleaseStep
  commitReleaseVersion,                   // : ReleaseStep, performs the initial git checks
  tagRelease,                             // : ReleaseStep
  releaseStepCommand("docker:publish"),
  setNextVersion,                         // : ReleaseStep
  commitNextVersion,                      // : ReleaseStep
  pushChanges                             // : ReleaseStep, also checks that an upstream branch is properly configured
)

packageOptions in (Compile, packageBin) +=  {
  import java.util.jar.Manifest
  import java.util.jar.Attributes.Name
  val manifest = new Manifest
  val mainAttributes = manifest.getMainAttributes
  mainAttributes.put(new Name("Git-Version"), git.gitDescribedVersion.value.getOrElse("Unknown-git-version"))
  mainAttributes.put(new Name("Git-Uncommitted-Changes"), git.gitUncommittedChanges.value.toString)
  Package.JarManifest( manifest )
}

buildInfoKeys ++= Seq[BuildInfoKey](
  "applicationOwner" -> organization.value,
  BuildInfoKey.action("buildTime") { System.currentTimeMillis },
  BuildInfoKey.action("gitVersion") { git.gitDescribedVersion.value.getOrElse("Unknown-git-version") },
  BuildInfoKey.action("releasedVersion") { git.gitUncommittedChanges.value.toString }
)

buildInfoOptions += BuildInfoOption.ToJson

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "$organization$.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "$organization$.binders._"
