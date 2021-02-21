rootProject.name = "Discord Bot"

include("buildSrc")
findProject(":buildSrc")?.name = "gradle-plugin"

include("core")
include("echo")
include("base")
include("bot")
include("firebase")
include("cinco")
include("game")
include("gameevent")
include("time")
include("maps")
include("random")
include("oust")
include("oust-core")
include("qotd")
