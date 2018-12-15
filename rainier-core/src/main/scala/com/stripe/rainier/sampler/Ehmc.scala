package com.stripe.rainier.sampler

import scala.collection.mutable.ListBuffer
import com.stripe.rainier.compute._
import com.stripe.rainier.core._

/**
  * Empirical HMC - automated tuning of step size and number of leapfrog steps
  * @param l0 the initial number of leapfrog steps to use during the dual averaging phase
  * @param k the number of iterations to use when determining the
  * empirical distribution of the total number of leapfrog steps until a u-turn
  */
final case class EHMC(l0: Int, k: Int = 2000) extends Sampler {
  def sample(density: DensityFunction,
             warmupIterations: Int,
             iterations: Int,
             keepEvery: Int)(implicit rng: RNG): List[Array[Double]] = {

    val lf = LeapFrog(density)
    val params = lf.initialize
    val stepSize =
      DualAvg.findStepSize(lf, params, 0.65, l0, warmupIterations)

    val empiricalL: Vector[Int] = lf.empiricalLongestSteps(l0, k, stepSize)

    def discreteUniform(min: Int, max: Int)(implicit rng: RNG) = {
      math.floor(rng.standardUniform * (max - min) + min).toInt
    }

    if (stepSize == 0.0)
      List(lf.variables(params))
    else {
      val buf = new ListBuffer[Array[Double]]
      var i = 0

      while (i < iterations) {

        // val j: Int = Categorical.list(Seq.fill(k)(1.0 / k)).generator.get
        val j = discreteUniform(0, k)
        val nSteps = empiricalL(j)

        lf.step(params, nSteps, stepSize)
        if (i % keepEvery == 0)
          buf += lf.variables(params)
        i += 1
      }
      buf.toList
    }
  }
}
