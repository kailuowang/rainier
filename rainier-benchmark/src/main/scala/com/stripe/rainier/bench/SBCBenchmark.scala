package com.stripe.rainier.bench

import org.openjdk.jmh.annotations._
import java.util.concurrent.TimeUnit

import com.stripe.rainier.core._
import com.stripe.rainier.sampler._

@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(3)
@Threads(4)
@State(Scope.Benchmark)
abstract class SBCBenchmark {
  implicit val rng: RNG = RNG.default

  protected def sbc: SBC[_, _]

  @Param(Array("1000", "10000", "100000"))
  var syntheticSamples: Int = _
  @Param(Array("1", "100", "1000"))
  var batches: Int = _

  lazy val s = sbc
  lazy val context = build
  lazy val vars = context.variables
  lazy val cf = compile
  lazy val inlined = inline
  lazy val inlinecf = compileInlined

  @Benchmark
  def synthesize = s.synthesize(syntheticSamples)

  @Benchmark
  def build = s.model(syntheticSamples).context(batches)

  @Benchmark
  def compile =
    context.compileDensity

  @Benchmark
  def inline =
    context.base + context.batched.inlined

  @Benchmark
  def compileInlined =
    context.compiler.compile(vars, inlined)

  @Benchmark
  def run =
    cf(vars.map { _ =>
      rng.standardUniform
    }.toArray)

  @Benchmark
  def runInlined =
    inlinecf(vars.map { _ =>
      rng.standardUniform
    }.toArray)
}

class SBCNormalBenchmark extends SBCBenchmark {
  def sbc =
    SBC[Double, Continuous](Uniform(0, 1)) { n =>
      Normal(n, 1)
    }
}

class SBCLaplaceBenchmark extends SBCBenchmark {
  def sbc = SBC[Double, Continuous](LogNormal(0, 1)) { x =>
    Laplace(x, x)
  }
}
