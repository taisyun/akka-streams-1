package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{FlowShape, Graph, ActorMaterializer}
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Created by taishun.nakatani on 2016/03/09.
  */
object Test17 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100)

    val result = source.runForeach(i => println(i))(materializer)

//    Thread.sleep(1000)

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
