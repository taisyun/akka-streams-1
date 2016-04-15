import akka.actor.ActorSystem
import akka.stream.{OverflowStrategy, ActorMaterializer}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.testkit.{TestProbe, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import scala.concurrent.Await
import scala.concurrent.duration._
/**
  * Created by taishun.nakatani on 2016/03/15.
  */
class Test1Test(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with FlatSpecLike
    with BeforeAndAfterAll{

  implicit val materializer = ActorMaterializer()

  def this() = this(ActorSystem("Sys"))

  it should "return 1, 2 and 3, 4" in {

    import system.dispatcher
    import akka.pattern.pipe


    val sourceUnderTest = Source(1 to 4).grouped(2)

    val probe = TestProbe()
    sourceUnderTest.grouped(2).runWith(Sink.head).pipeTo(probe.ref)
    probe.expectMsg(100.millis, Seq(Seq(1, 2), Seq(3, 4)))
  }

  it should "tick" in {


    case object Tick
    val sourceUnderTest = Source.tick(0.seconds, 200.millis, Tick)

    val probe = TestProbe()
    val cancellable = sourceUnderTest.to(Sink.actorRef(probe.ref, "completed")).run()

    probe.expectMsg(1.second, Tick)
    probe.expectNoMsg(100.millis)
    probe.expectMsg(200.millis, Tick)
    cancellable.cancel()
    probe.expectMsg(200.millis, "completed")

  }

  it should "concatenate" in {


    val sinkUnderTest = Flow[Int].map(_.toString).toMat(Sink.fold("")(_ + _))(Keep.right)

    val (ref, future) = Source.actorRef(8, OverflowStrategy.fail)
      .toMat(sinkUnderTest)(Keep.both).run()

    ref ! 1
    ref ! 2
    ref ! 3
    ref ! akka.actor.Status.Success("done")

    val result = Await.result(future, 100.millis)
    assert(result == "123")
  }
}
