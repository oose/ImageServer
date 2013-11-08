package backend

import model.Image

object CommonMsg {
  case class ExpiredImageEvaluation(id: Image)
  case class SuccessfulImageEvaluation(id: Image)
}
