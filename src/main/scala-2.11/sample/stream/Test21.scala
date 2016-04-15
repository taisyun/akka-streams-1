package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{Fusing, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/14.
  */
object Test21 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val flow = Flow[Int].map(_ * 2).filter(_ > 500)
    val fused = Fusing.aggressive(flow)

    val source: Source[Int, NotUsed] = Source.fromIterator { () => Iterator from 0 }
      .via(fused)
      .take(1000)

    val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

    // connect the Source to the Sink, obtaining a RunnableGraph
    val runnable: RunnableGraph[Future[Done]] = source.toMat(sink)(Keep.right)

    // materialize the flow and get the value of the FoldSink
    val sum: Future[Done] = runnable.run()

    val s = Await.result(sum, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)
  }

}
