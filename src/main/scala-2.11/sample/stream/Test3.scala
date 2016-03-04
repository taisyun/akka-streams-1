package sample.stream

import akka.actor.{Props, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, RunnableGraph, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/04.
  */
object Test3 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, ActorRef] = Source.actorPublisher[Int](Props[SampleActor])
    val sink: Sink[Int, Future[String]] = Sink.fold[String,Int](""){ _ + _.toString }

    val rg: RunnableGraph[(ActorRef, Future[String])] = source.toMat(sink)(Keep.both)

    val (actor: ActorRef, result: Future[String]) = rg.run()

    Thread.sleep(1000) // 調整のためのsleep

    actor ! Message(1)
    actor ! Message(2)
    actor ! Message(3)
    actor ! END

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

  }

}
