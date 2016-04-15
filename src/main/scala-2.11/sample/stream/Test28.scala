package sample.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Tcp.OutgoingConnection
import akka.stream.{SourceShape, FlowShape, ActorMaterializer}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.{Future, Promise, Await}
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object Test28 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    // Materializes to Promise[Option[Int]]                                   (red)
    val source: Source[Int, Promise[Option[Int]]] = Source.maybe[Int]

    // Materializes to Unit                                                   (black)
    val flow1: Flow[Int, Int, NotUsed] = Flow[Int].take(100)

    // Materializes to Promise[Int]                                          (red)
    val nestedSource: Source[Int, Promise[Option[Int]]] =
      source.viaMat(flow1)(Keep.left).named("nestedSource")

    // Materializes to Unit                                                   (orange)
    val flow2: Flow[Int, ByteString, NotUsed] = Flow[Int].map { i => ByteString(i.toString) }

    // Materializes to Future[OutgoingConnection]                             (yellow)
    val flow3: Flow[ByteString, ByteString, Future[OutgoingConnection]] =
      Tcp().outgoingConnection("172.16.19.231", 80)

    // Materializes to Future[OutgoingConnection]                             (yellow)
    val nestedFlow: Flow[Int, ByteString, Future[OutgoingConnection]] =
      flow2.viaMat(flow3)(Keep.right).named("nestedFlow")

    // Materializes to Future[String]                                         (green)
    val sink: Sink[ByteString, Future[String]] = Sink.fold("")(_ + _.utf8String)

    // Materializes to (Future[OutgoingConnection], Future[String])           (blue)
    val nestedSink: Sink[Int, (Future[OutgoingConnection], Future[String])] =
      nestedFlow.toMat(sink)(Keep.both)

    case class MyClass(private val p: Promise[Option[Int]], conn: OutgoingConnection) {
      def close() = p.trySuccess(None)
    }

    def f(p: Promise[Option[Int]],
          rest: (Future[OutgoingConnection], Future[String])): Future[MyClass] = {

      val connFuture = rest._1
      connFuture.map(MyClass(p, _))
    }

    // Materializes to Future[MyClass]                                        (purple)
    val runnableGraph: RunnableGraph[Future[MyClass]] =
      nestedSource.toMat(nestedSink)(f)

    val result: Future[MyClass] = runnableGraph.run()

    val s = Await.result(result, Duration.Inf)
    Console.println("end of main " + s)

//    Thread.sleep(1000)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
