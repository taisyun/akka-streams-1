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
object Test22 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val flow = Flow[Int].map(_ * 2).filter(_ > 500)
    val fused = Fusing.aggressive(flow)

    val runnable = Source(List(1, 2, 3))
      .map(_ + 1).async
      .map(_ * 2)
      .toMat(Sink.foreach(println))(Keep.right)



    // materialize the flow and get the value of the FoldSink
    val sum: Future[Done] = runnable.run()

    val s = Await.result(sum, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)
  }

}
