package model

/**
 * Enumeration of states an image can obtain.
 */
sealed trait EvaluationState
case object UnEvaluated extends EvaluationState
case object InEvaluation extends EvaluationState
case object Evaluated extends EvaluationState

/**
 * 
 */
case class Image(id: String, state: EvaluationState = UnEvaluated, tags: Option[List[String]] = None) {
  override def equals(arg: Any) = arg match {
    case Image(id, _, _) => id == this.id
    case _ => false
  }
  override def hashCode() = id.hashCode
}

