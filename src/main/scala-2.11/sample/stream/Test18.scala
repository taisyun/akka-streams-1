package sample.stream

import java.io.File

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{IOResult, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/09.
  */
object Test18 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100)
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)

//    val result: Future[IOResult] =
//      factorials
//        .map(num => ByteString(s"$num\n"))
//        .runWith(FileIO.toFile(new File("factorials.txt")))

    def lineSink(filename: String): Sink[String, Future[IOResult]] =
      Flow[String]
        .map(s => ByteString(s + "\n"))
        .toMat(FileIO.toFile(new File(filename)))(Keep.right)

    val result: Future[IOResult] =
      factorials.map(_.toString).runWith(lineSink("factorial2.txt"))



    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
