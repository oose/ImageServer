package util

import play.api.libs.json.Json
import backend.DirectoryActor._

class LowLevelImplicits {
   implicit val statusReponseJson = Json.format[StatusResponse]
}

object Implicits extends LowLevelImplicits