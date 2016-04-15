package sample.stream

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Tcp, Source}
import akka.stream.scaladsl.Tcp.{ServerBinding, IncomingConnection}
import akka.util.ByteString

import scala.concurrent.Future

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object Test35 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    import akka.stream.scaladsl.Framing

    val host = "127.0.0.1"
    val port = 8888

    val connections: Source[IncomingConnection, Future[ServerBinding]] =
      Tcp().bind(host, port)

    connections.runForeach { connection =>

      // server logic, parses incoming commands
      val commandParser = Flow[String].takeWhile(_ != "BYE").map(_ + "!")

      import connection._
      val welcomeMsg = s"Welcome to: $localAddress, you are: $remoteAddress!"
      val welcome = Source.single(welcomeMsg)

      val serverLogic = Flow[ByteString]
        .via(Framing.delimiter(
          ByteString("\r\n"),
          maximumFrameLength = 256,
          allowTruncation = true))
        .map(_.utf8String)
        .via(commandParser)
        // merge in the initial banner after parser
        .merge(welcome)
        .map(_ + "\n")
        .map(ByteString(_))

      connection.handleWith(serverLogic)
    }

    //    Thread.sleep(3000)
    //
    //    val whenTerminated = system.terminate()
    //    Await.result(whenTerminated, Duration.Inf)

  }
}
