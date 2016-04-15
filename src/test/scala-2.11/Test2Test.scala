import akka.actor.ActorSystem
import akka.pattern
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.testkit.scaladsl.{TestSource, TestSink}
import akka.testkit.{TestProbe, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Future, Await}
import scala.util.Failure

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
class Test2Test(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll {

  implicit val materializer = ActorMaterializer()

  def this() = this(ActorSystem("Sys"))

  it should "do it" in {

    val sourceUnderTest = Source(1 to 4).filter(_ % 2 == 0).map(_ * 2)

    sourceUnderTest
      .runWith(TestSink.probe[Int])
      .request(2)
      .expectNext(4, 8)
      .expectComplete()
  }

  it should "be cancelled" in {

    val sinkUnderTest = Sink.cancelled

    TestSource.probe[Int]
      .toMat(sinkUnderTest)(Keep.left)
      .run()
      .expectCancellation()
  }

  it should "catch" in {

    val sinkUnderTest = Sink.head[Int]

    val (probe, future) = TestSource.probe[Int]
      .toMat(sinkUnderTest)(Keep.both)
      .run()
    probe.sendError(new Exception("boom"))

    Await.ready(future, 100.millis)
    val Failure(exception) = future.value.get
    assert(exception.getMessage == "boom")
  }

  it should "combinate" in {

    val flowUnderTest = Flow[Int].mapAsyncUnordered(2) { sleep =>
      pattern.after(10.millis * sleep, using = system.scheduler)(Future.successful(sleep))
    }

    val (pub, sub) = TestSource.probe[Int]
      .via(flowUnderTest)
      .toMat(TestSink.probe[Int])(Keep.both)
      .run()

    sub.request(n = 3)
    pub.sendNext(3)
    pub.sendNext(2)
    pub.sendNext(1)
    sub.expectNextUnordered(1, 2, 3)

    pub.sendError(new Exception("Power surge in the linear subroutine C-47!"))
    val ex = sub.expectError()
    assert(ex.getMessage.contains("C-47"))
  }

}
