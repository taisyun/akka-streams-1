package sample.stream

import java.io.File

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{ThrottleMode, IOResult, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/09.
  */
object Test19 {
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

    val done: Future[Done] =
      factorials
        .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
        .throttle(1, 1.second, 1, ThrottleMode.shaping)
        .runForeach(println)


    val s = Await.result(done, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
