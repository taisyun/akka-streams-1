package sample.stream

import akka.Done
import akka.actor.ActorSystem
import akka.stream.{Inlet, _}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source, Zip}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/25.
  */

class ZipInt extends GraphStage[FanInShape2[Int, Int, (Int, Int)]] {
  override def initialAttributes = Attributes.name("ZipInt")

  override val shape: FanInShape2[Int, Int, (Int, Int)] = new FanInShape2[Int, Int, (Int, Int)]("ZipInt")

  def out: Outlet[(Int, Int)] = shape.out

  val in0: Inlet[Int] = shape.in0
  val in1: Inlet[Int] = shape.in1

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
    var pending = 0
    // Without this field the completion signalling would take one extra pull
    var willShutDown = false

    private def pushAll(): Unit = {
      push(out, (grab(in0), grab(in1)))
      if (willShutDown) completeStage()
      else {
        pull(in0)
        pull(in1)
      }
    }

    override def preStart(): Unit = {
      pull(in0)
      pull(in1)
    }

    setHandler(in0, new InHandler {
      override def onPush(): Unit = {
        pending -= 1
        if (pending == 0) pushAll()
      }

      override def onUpstreamFinish(): Unit = {
        if (!isAvailable(in0)) completeStage()
        willShutDown = true
      }

    })
    setHandler(in1, new InHandler {
      override def onPush(): Unit = {
        pending -= 1
        if (pending == 0) pushAll()
      }

      override def onUpstreamFinish(): Unit = {
        if (!isAvailable(in1)) completeStage()
        willShutDown = true
      }

    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        pending += shape.inlets.size
        if (pending == 0) pushAll()
      }
    })
  }
}
object ZipInt{
  def apply(): ZipInt = new ZipInt()
}

object Test37 {
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
      val C: FanInShape2[Int, Int, (Int, Int)] = builder.add(ZipInt())
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
