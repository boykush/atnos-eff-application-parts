package io.github.boykush.eff.addon.skunk

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.concurrent.duration.DurationInt
import scala.concurrent.Await
import scala.concurrent.Future

trait AbstractFreeSpec extends AnyFreeSpec with Matchers {
  def await[A](f: Future[A]): A = Await.result(f, 1.minutes)
}
