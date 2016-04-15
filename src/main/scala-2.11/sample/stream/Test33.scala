package sample.stream

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Props, ActorSystem}
import akka.stream.{ActorMaterializerSettings, ActorMaterializer}
import akka.stream.scaladsl.{Sink, Source}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
class SometimesSlowService(implicit ec: ExecutionContext) {

  private val runningCount = new AtomicInteger

  def convert(s: String): Future[String] = {
    println(s"running: $s (${runningCount.incrementAndGet()})")
    Future {
      if (s.nonEmpty && s.head.isLower)
        Thread.sleep(500)
      else
        Thread.sleep(20)
      println(s"completed: $s (${runningCount.decrementAndGet()})")
      s.toUpperCase
    }
  }
}

object Test33 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer(
      ActorMaterializerSettings(system).withInputBuffer(initialSize = 4, maxSize = 4))

    implicit val blockingExecutionContext = system.dispatchers.lookup("blocking-dispatcher")
    val service = new SometimesSlowService


    Source(List("a", "B", "C", "D", "e", "F", "g", "H", "i", "J"))
      .map(elem => { println(s"before: $elem"); elem })
      .mapAsync(4)(service.convert)
//      .mapAsyncUnordered(4)(service.convert)
      .runForeach(elem => println(s"after: $elem"))

    //    val result = rg.run()
    //
    //    val s = Await.result(result, Duration.Inf)
    //    Console.println("end of main " + s)

        Thread.sleep(3000)

        val whenTerminated = system.terminate()
        Await.result(whenTerminated, Duration.Inf)

  }

}
