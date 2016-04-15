package sample.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object Test29 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    Source(1 to 3)
      .map { i => println(s"A: $i"); i }
      .map { i => println(s"B: $i"); i }
      .map { i => println(s"C: $i"); i }
      .runWith(Sink.ignore)


//    val s = Await.result(result, Duration.Inf)
//    Console.println("end of main " + s)

    Thread.sleep(1000)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
