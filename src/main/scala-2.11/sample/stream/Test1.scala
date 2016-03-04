package sample.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * Created by taishun.nakatani on 2016/03/04.
  */
object Test1 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    import system.dispatcher

    implicit val materializer = ActorMaterializer()

    val foldSink: Sink[Int, Future[String]] = Sink.fold(""){ _ + _.toString }
    val source: Source[Int, NotUsed] = Source(1 to 10)
    val result: Future[String] = source.runWith(foldSink)
    result.onComplete((r) => {
        r match {
          case Success(s) => Console.println("onComplete " + s)
          case Failure(e) => Console.println("Error! " + e.getMessage)
        }

        val whenTerminated = system.terminate()
        Await.result(whenTerminated, Duration.Inf)
      }
    )
    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)
  }
}
