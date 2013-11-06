package util

import play.api.libs.functional.syntax._
import play.api.libs.json._

import backend.DirectoryActor.StatusResponse
import model.Evaluated
import model.EvaluationState
import model.Image
import model.InEvaluation
import model.UnEvaluated

class LowLevelImplicits {
  
  import backend.DirectoryActor._
  
  implicit val evaluationStateJson = new Writes[EvaluationState] {
    def writes(state: EvaluationState) = {
      state match {
        case UnEvaluated => JsString("not evaluated")
        case InEvaluation => JsString("in evaluation")
        case Evaluated => JsString("is evaluated")
      }
    }
  }
  implicit val imageJson : Writes[Image] = Json.writes[Image]
  implicit val statusReponseJson : Writes[StatusResponse] = Json.writes[backend.DirectoryActor.StatusResponse]
  implicit val evaluationJson = Json.writes[Evaluation]
}

object Implicits extends LowLevelImplicits