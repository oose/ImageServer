package design

import org.specs2.mutable._
import org.specs2.specification.Analysis
import org.specs2.analysis.CompilerDependencyFinder

class DependencySpec extends SpecificationWithJUnit with Analysis  {

  val design = layers(
    "view",
    "controllers",
    "backend",
    "model util"
    ).inTargetDir("target/scala-2.10/classes").inSourceDir("app")

  "Program design" should {
    "adhere to layer structure" in {
    	design must beRespected
    }
  }
}