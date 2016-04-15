package sample.stream

import akka.{NotUsed, Done}
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/14.
  */
object Test25 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val out: Sink[Int, Future[Done]] = Sink.foreach({ i =>
      println(i)
    })

    val rg = RunnableGraph.fromGraph(GraphDSL.create(out) { implicit builder => out_shape =>
      import GraphDSL.Implicits._

      val A: Outlet[Int]                  = builder.add(Source(0 to 1)).out
      val B: UniformFanOutShape[Int, Int] = builder.add(Broadcast[Int](2))
      val C: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
      val D: FlowShape[Int, Int]          = builder.add(Flow[Int].map({ i =>
        i + 1
      }))
      val E: UniformFanOutShape[Int, Int] = builder.add(Balance[Int](2))
      val F: UniformFanInShape[Int, Int]  = builder.add(Merge[Int](2))
      val G: Sink[Int, Future[Done]]#Shape = out_shape

      val log1: FlowShape[Int, Int] = builder.add(Flow[Int].map{ i => {
        println("log1 " + i)
        i
      }})
      val log2: FlowShape[Int, Int] = builder.add(Flow[Int].map{ i => {
        println("log2 " + i)
        i
      }})
      val log3: FlowShape[Int, Int] = builder.add(Flow[Int].map{ i => {
        println("log3 " + i)
        i
      }})
      val log4: FlowShape[Int, Int] = builder.add(Flow[Int].map{ i => {
        println("log4 " + i)
        i
      }})

                            C             <~      F
      A  ~>  B  ~>          C             ~>      F
             B  ~> log1 ~>  D  ~> log2 ~>  E  ~>  log4 ~>  F
                                           E  ~>  log3 ~>  G
      /*
      val A: Outlet[Int]                  = builder.add(Source.single(0)).out
      val D: FlowShape[Int, Int]          = builder.add(Flow[Int].map({ i =>
        i + 1
      }))
      val G: Sink[Int, Future[Done]]#Shape = out_shape
      A ~> D ~> G
*/
      ClosedShape
    })
    val result: Future[Done] = rg.run()

    Thread.sleep(1000)

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
