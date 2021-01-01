package de.lolhens.cadvisor.limiter

import cats.effect.ExitCode
import monix.eval.{Task, TaskApp}

object Server extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] = ???
}
