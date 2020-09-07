rootProject.name = "Discord Bot"
include("core")
include("buildSrc")
findProject(":buildSrc")?.name = "gradle-plugin"
include("echo")
