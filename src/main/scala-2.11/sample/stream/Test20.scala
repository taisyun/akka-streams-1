package sample.stream

import java.io.File

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ThrottleMode, IOResult, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/14.
  */
object Test20 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source = Source(1 to 10)
    val sink = Sink.fold[Int, Int](0)(_ + _)

    // connect the Source to the Sink, obtaining a RunnableGraph
    val runnable: RunnableGraph[Future[Int]] = source.toMat(sink)(Keep.right)

    // materialize the flow and get the value of the FoldSink
    val sum: Future[Int] = runnable.run()

    val s = Await.result(sum, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)
  }

}
