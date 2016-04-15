package sample.stream

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/07.
  */
object Test9 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val in = Source(1 to 10)
    val out: Sink[Any, Future[Done]] = Sink.foreach(println)

    val rg = RunnableGraph.fromGraph(GraphDSL.create(out) { implicit builder => out_shape =>
      import GraphDSL.Implicits._

      val bcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))

      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

      in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out_shape
      bcast ~> f4 ~> merge

      ClosedShape
    })
    val result: Future[Done] = rg.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
