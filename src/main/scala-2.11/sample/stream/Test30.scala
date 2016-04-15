package sample.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object Test30 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    import scala.concurrent.duration._
    case class Tick()

    val rg = RunnableGraph.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val zipper = b.add(ZipWith[Tick, Int, Int]((tick, count) => count))

      Source.tick(initialDelay = 3.second, interval = 3.second, Tick()) ~> zipper.in0

      Source.tick(initialDelay = 1.second, interval = 1.second, "message!")
        .conflateWithSeed(seed = (_) => 1)((count, _) => count + 1) ~> zipper.in1

      zipper.out ~> Sink.foreach(println)
      ClosedShape
    })

    val result = rg.run()

//    val s = Await.result(result, Duration.Inf)
//    Console.println("end of main " + s)

//    Thread.sleep(10000)
//
//    val whenTerminated = system.terminate()
//    Await.result(whenTerminated, Duration.Inf)

  }

}
