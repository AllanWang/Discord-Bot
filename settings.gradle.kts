rootProject.name = "Discord Bot"

include("buildSrc")
findProject(":buildSrc")?.name = "gradle-plugin"

include("core")
include("echo")
include("bot")
include("firebase")
include("game")
