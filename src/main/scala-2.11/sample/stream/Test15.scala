package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.stage._
import akka.stream._
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/07.
  */

// each time an event is pushed through it will trigger a period of silence
class TimedGate[A](silencePeriod: FiniteDuration) extends GraphStage[FlowShape[A, A]] {

  val in = Inlet[A]("TimedGate.in")
  val out = Outlet[A]("TimedGate.out")

  val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) {

      var open = false

      setHandler(in, new InHandler {
        override def onPush(): Unit = {
          val elem = grab(in)
          if (open) pull(in)
          else {
            push(out, elem)
            open = true
            scheduleOnce(None, silencePeriod)
          }
        }
      })
      setHandler(out, new OutHandler {
        override def onPull(): Unit = { pull(in) }
      })

      override protected def onTimer(timerKey: Any): Unit = {
        open = false
      }
    }
}

object Test15 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100000000)

    val flow1Graph: Graph[FlowShape[Int, Int], NotUsed] = new TimedGate[Int](1 second)

    val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

    val g: RunnableGraph[Future[Done]] = source.via(flow1Graph).toMat(sink)(Keep.right)
    val result: Future[Done] = g.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
