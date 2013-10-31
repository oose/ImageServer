package util

import play.api.libs.functional.syntax._
import play.api.libs.json.JsString
import play.api.libs.json.Writes
import backend.Evaluated
import backend.EvaluationState
import backend.InEvaluation
import backend.UnEvaluated
import backend.DirectoryActor
import backend.Image
import play.api.libs.json.Json

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
  import backend.DirectoryActor._
  implicit val imageJson = Json.writes[backend.Image]
  implicit val statusReponseJson = Json.writes[StatusResponse]
  implicit val evaluationJson = Json.writes[Evaluation]
}

object Implicits extends LowLevelImplicits