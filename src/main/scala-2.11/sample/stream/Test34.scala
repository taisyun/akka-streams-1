package sample.stream

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Tcp, Source}
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import akka.util.ByteString

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object Test34 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    import akka.stream.scaladsl.Framing

    val host = "127.0.0.1"
    val port = 8888

    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      Tcp().bind(host, port)
    connections runForeach { connection =>
      println(s"New connection from: ${connection.remoteAddress}")

      val echo = Flow[ByteString]
        .via(Framing.delimiter(
          ByteString("\r\n"),
          maximumFrameLength = 256,
          allowTruncation = true))
        .map(_.utf8String)
        .map(_ + "!!!\n")
        .map(ByteString(_))

      connection.handleWith(echo)
    }
//    Thread.sleep(3000)
//
//    val whenTerminated = system.terminate()
//    Await.result(whenTerminated, Duration.Inf)

  }

}
