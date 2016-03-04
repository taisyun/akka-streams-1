package sample.stream

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.concurrent.{Future, Promise, Await}
import scala.concurrent.duration.Duration

object BasicTransformation {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher

    implicit val materializer = ActorMaterializer()

//    val text =
//      """|Lorem Ipsum is simply dummy text of the printing and typesetting industry.
//         |Lorem Ipsum has been the industry's standard dummy text ever since the 1500s,
//         |when an unknown printer took a galley of type and scrambled it to make a type
//         |specimen book.""".stripMargin
    val text =
      """|Lorem Ipsum is simply dummy text of the printing and typesetting industry.
        |""".stripMargin

//    Source.fromIterator(() => text.split("\\s").iterator).
//      map(_.toUpperCase).
//      runForeach(println).
//      onComplete(_ => system.terminate())

    val ret1 = Source.fromIterator(() => {
        text.split("\\s").iterator
      }
    )
    val ret2 = ret1.map( s => {
        s.toUpperCase
      }
    )
    val ret3: Future[Done] = ret2.runForeach(s =>
      println(s)
    )
    val finishedComplete = Promise[akka.Done]
    val whenFinishedComplete = finishedComplete.future
    ret3.onComplete(_ => {
        Console.println("before system.terminate()")
        val whenTerminated = system.terminate()
        Console.println("after system.terminate()")
        Console.println("before Await.result(whenTerminated)")
        Await.result(whenTerminated, Duration.Inf)
        Console.println("after Await.result(whenTerminated)")
        finishedComplete.success(akka.Done)
      }
    )
    Console.println("before Await.result(ret3)")
    val result = Await.result(ret3, Duration.Inf) // This will not wait for onConmplete finished
    Console.println("after Await.result(ret3)")

    Console.println("before Await.result(whenFinishedComplete)")
    val result2 = Await.result(whenFinishedComplete, Duration.Inf) // Wait until onConmplete finished
    Console.println("after Await.result(whenFinishedComplete)")

    val a = 1
    // could also use .runWith(Sink.foreach(println)) instead of .runForeach(println) above
    // as it is a shorthand for the same thing. Sinks may be constructed elsewhere and plugged
    // in like this. Note also that foreach returns a future (in either form) which may be
    // used to attach lifecycle events to, like here with the onComplete.
  }
}
