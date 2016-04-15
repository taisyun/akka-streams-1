package sample.stream

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, UniformFanInShape, _}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/25.
  */
object Test36 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val out: Sink[(Int, Int), Future[Done]] = Sink.foreach({ i =>
      println(i)
    })

    val rg = RunnableGraph.fromGraph(GraphDSL.create(out) { implicit builder => out_shape =>
      import GraphDSL.Implicits._

      val A: Outlet[Int]                  = builder.add(Source(0 to 2)).out
      val B: Outlet[Int]                  = builder.add(Source(5 to 7)).out
      val C: FanInShape2[Int, Int, (Int, Int)] = builder.add(Zip[Int,Int])
      val G: Sink[(Int, Int), Future[Done]]#Shape = out_shape

      A  ~>  C.in0
      B  ~>  C.in1
      C.out ~>  G

      ClosedShape
    })
    val result: Future[Done] = rg.run()



    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
