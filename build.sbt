lazy val gabbler = project
  .copy(id = "gabbler")
  .in(file("."))
  .enablePlugins(AutomateHeaderPlugin, GitVersioning)
  .aggregate(gabblerUser, gabblerChat)

lazy val gabblerUser = project
  .copy(id = "gabbler-user")
  .in(file("gabbler-user"))
  .enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)

lazy val gabblerChat = project
  .copy(id = "gabbler-chat")
  .in(file("gabbler-chat"))
  .enablePlugins(AutomateHeaderPlugin, JavaAppPackaging, DockerPlugin)

name := "gabbler"

unmanagedSourceDirectories.in(Compile) := Vector.empty
unmanagedSourceDirectories.in(Test)    := Vector.empty

publishArtifact := false
