package sample.stream

import akka.{Done, NotUsed}
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/04.
  */
object Test4 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    val source: Source[Int, NotUsed] = Source(1 to 10)
    val flow1: Flow[Int, Int, NotUsed] = Flow[Int].filter(_ % 3 == 0)
    val flow2: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)
    val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

    val g: RunnableGraph[Future[Done]] = source.via(flow1).via(flow2).toMat(sink)(Keep.right)

    var result = g.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
