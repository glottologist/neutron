/*
 * Copyright 2020 Chatroulette
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cr.pulsar

import cats._
import cats.effect._
import cats.implicits._
import cr.pulsar.Config._
import munit.FunSuite
import scala.concurrent.ExecutionContext
import scala.util.Try

abstract class PulsarSuite extends FunSuite {

  implicit val `⏳` = IO.contextShift(ExecutionContext.global)
  implicit val `⏰` = IO.timer(ExecutionContext.global)

  override def munitValueTransforms: List[ValueTransform] =
    super.munitValueTransforms :+ new ValueTransform("IO", {
          case ioa: IO[_] => IO.suspend(ioa).unsafeToFuture
        })

  case class Event(id: Long, value: String) {
    def key(nrOfConsumers: Int): Publisher.MessageKey =
      Publisher.MessageKey.Of(s"shard-${id % nrOfConsumers}")
  }

  object Event {
    implicit val eq: Eq[Event] = Eq.fromUniversalEquals

    implicit val inject: Inject[Event, Array[Byte]] =
      new Inject[Event, Array[Byte]] {
        def inj: Event => Array[Byte] = e => s"${e.value}-${e.id}".getBytes("UTF-8")
        def prj: Array[Byte] => Option[Event] =
          bs =>
            new String(bs, "UTF-8").split("-").toList match {
              case (v :: id :: Nil) =>
                Try(id.toLong).toOption.map { i =>
                  Event(i, v)
                }
              case _ => None
            }
      }
  }

  val cfg = Config(
    PulsarTenant("public"),
    PulsarNamespace("default"),
    PulsarURL("pulsar://localhost:6650")
  )

}
