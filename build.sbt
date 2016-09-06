lazy val gabbler =
  project
    .in(file("."))
    .aggregate(`gabbler-chat`, `gabbler-user`)
    .enablePlugins(GitVersioning)

lazy val `gabbler-user` =
  project.enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)

lazy val `gabbler-chat` =
  project.enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)

name := "gabbler"

unmanagedSourceDirectories.in(Compile) := Vector.empty
unmanagedSourceDirectories.in(Test)    := Vector.empty

publishArtifact := false
