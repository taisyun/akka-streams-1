package sample.stream

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.stage.{OutHandler, InHandler, GraphStageLogic, GraphStage}
import akka.stream._
import akka.stream.scaladsl.{Keep, RunnableGraph, Sink, Source}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/07.
  */
object Test12 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(11 to 19)

    val flow1Graph: Graph[FlowShape[Int, Int], NotUsed] = new GraphStage[FlowShape[Int, Int]] {
      val in = Inlet[Int]("flow1Graph_in")
      val out = Outlet[Int]("flow1Graph_out")
      override val shape = FlowShape.of(in, out)

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
        new GraphStageLogic(shape) {
          // All state MUST be inside the GraphStageLogic,
          // never inside the enclosing GraphStage.
          // This state is safe to access and modify from all the
          // callbacks that are provided by GraphStageLogic and the
          // registered handlers.

          def filter(elem: Int) : Boolean = {
            elem match  {
              case e if( e % 2 == 0 ) => true
              case _ => false
            }
          }
          setHandler(in, handler = new InHandler {
            override def onPush(): Unit = {
              val elem = grab(in)
              if (filter(elem)) push(out, elem)
              else pull(in)
            }
          })
          setHandler(out, new OutHandler {
            override def onPull(): Unit = {
              pull(in)
            }
          })
        }
    }

    val sink: Sink[Int, Future[Done]] = Sink.foreach(println)

    val g: RunnableGraph[Future[Done]] = source.via(flow1Graph).toMat(sink)(Keep.right)
    val result: Future[Done] = g.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
