package util

import play.api.libs.json.Json
import backend.DirectoryActor._
import backend.Image
import backend.EvaluationState
import play.api.libs.json.Writes
import backend.UnEvaluated
import play.api.libs.json.JsString
import backend.InEvaluation
import backend.Evaluated

class LowLevelImplicits {

  implicit val evaluationStateJson = new Writes[EvaluationState] {
    def writes(state: EvaluationState) = {
      state match {
        case UnEvaluated => JsString("not evaluated")
        case InEvaluation => JsString("in evaluation")
        case Evaluated => JsString("is evaluated")
      }
    }
  }
  implicit val imageJson = Json.writes[Image]
  implicit val statusReponseJson = Json.writes[StatusResponse]
}

object Implicits extends LowLevelImplicits