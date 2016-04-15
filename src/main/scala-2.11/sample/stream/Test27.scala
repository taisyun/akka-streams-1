package sample.stream

import akka.actor.ActorSystem
import akka.stream.{SourceShape, FlowShape, ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/14.
  */
object Test27 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    import GraphDSL.Implicits._
    val partial = GraphDSL.create() { implicit builder =>
      val B = builder.add(Broadcast[Int](2))
      val C = builder.add(Merge[Int](2))
      val E = builder.add(Balance[Int](2))
      val F = builder.add(Merge[Int](2))

      C  <~  F
      B  ~>                            C  ~>  F
      B  ~>  Flow[Int].map(_ + 1)  ~>  E  ~>  F
      FlowShape(B.in, E.out(1))
    }.named("partial")
    // Convert the partial graph of FlowShape to a Flow to get
    // access to the fluid DSL (for example to be able to call .filter())
    val flow = Flow.fromGraph(partial)

    // Simple way to create a graph backed Source
    val source = Source.fromGraph( GraphDSL.create() { implicit builder =>
      val merge = builder.add(Merge[Int](2))
      Source.single(0)      ~> merge
      Source(List(2, 3, 4)) ~> merge

      // Exposing exactly one output port
      SourceShape(merge.out)
    })

    // Building a Sink with a nested Flow, using the fluid DSL
    val sink = {
      val nestedFlow = Flow[Int].map(_ * 2).drop(10).named("nestedFlow")
      nestedFlow.to(Sink.foreach(println))
    }

    // Putting all together
    val closed = source.via(flow.filter(_ > 1)).to(sink)

    val result = closed.run()

    //    val s = Await.result(result, Duration.Inf)
    //    Console.println("end of main " + s)
    Thread.sleep(1000)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
