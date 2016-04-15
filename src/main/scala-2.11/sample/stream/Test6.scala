package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/07.
  */
object Test6 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    val source: Source[Int, NotUsed] = Source(1 to 10)
    val flow1: Flow[Int, Int, NotUsed] = Flow[Int].filter(_ % 3 == 0)
    val flow2: Flow[Int, Int, NotUsed] = Flow[Int].map(_ * 2)
    val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

    val g = RunnableGraph.fromGraph(GraphDSL.create(sink) { implicit builder => sink_shape =>
      import GraphDSL.Implicits._
      source ~> flow1 ~> flow2 ~> sink_shape
      ClosedShape
    })
    val result = g.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
