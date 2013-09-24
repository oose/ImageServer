package backend

case class Expired(id: Option[String] = None)
case class Evaluation(id: String, tags: List[String])

trait EvaluationStatus
case object EvaluationAccepted extends EvaluationStatus
case class EvaluationRejected(reason: String) extends EvaluationStatus

case object RequestId

case object DirectoryContent

case object StatusRequest
case class StatusResponse(total: Int, inEvaluation: Int) 