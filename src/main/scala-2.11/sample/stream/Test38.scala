package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanInShape2, Outlet}
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, RunnableGraph, Sink, Source, SubFlow}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/25.
  */
object Test38 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100)

    val flow1: Flow[Int, Int, NotUsed] = Flow[Int]
    val flow2
          = flow1.groupBy( 2, _ % 2)
    val flow3
          = flow2.map( _ + 3)

    val flow31 = Flow[Int].groupBy( 2, _ % 2).map( _ + 3)

    val flow: Flow[Int,Int, NotUsed] = Flow[Int].groupBy( 2, _ % 2).map( _ + 3).concatSubstreams

    val out: Sink[Int, Future[Done]] = Sink.foreach({ i =>
      println(i)
    })

    val rg = source.via(flow.async).toMat(out)(Keep.right)
    val result: Future[Done] = rg.run()



    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
