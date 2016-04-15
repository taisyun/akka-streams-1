package sample.stream

import akka.actor._
import akka.event.Logging
import akka.routing.{ActorRefRoutee, Router, RoundRobinRoutingLogic}
import akka.stream.{Attributes, ActorMaterializer}
import akka.stream.actor.ActorSubscriberMessage.OnNext
import akka.stream.actor.{ActorSubscriber, ActorSubscriberMessage, MaxInFlightRequestStrategy}
import akka.stream.scaladsl.{Sink, Flow, Source}

/**
  * Created by taishun.nakatani on 2016/03/15.
  */
object WorkerPool {
  case class Msg(id: Int, replyTo: ActorRef)
  case class Work(id: Int)
  case class Reply(id: Int)
  case class Done(id: Int)

  def props: Props = Props(new WorkerPool)
}

class WorkerPool extends ActorSubscriber {
  import WorkerPool._
  import ActorSubscriberMessage._

  val MaxQueueSize = 10
  var queue = Map.empty[Int, ActorRef]

  val router = {
    val routees = Vector.fill(3) {
      ActorRefRoutee(context.actorOf(Props[Worker]))
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override val requestStrategy = new MaxInFlightRequestStrategy(max = MaxQueueSize) {
    override def inFlightInternally: Int = queue.size
  }

  def receive = {
    case OnNext(Msg(id, replyTo)) =>
      queue += (id -> replyTo)
      assert(queue.size <= MaxQueueSize, s"queued too many: ${queue.size}")
      router.route(Work(id), self)
    case Reply(id) =>
      queue(id) ! Done(id)
      queue -= id
  }
}

class Worker extends Actor {
  import WorkerPool._
  def receive = {
    case Work(id) =>
      println(id)
      sender() ! Reply(id)
  }
}

object Test32 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    val dummy = system.actorOf(Props[Worker])

    val N = 117
    Source(1 to N)
      .log("before-map").withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel))
      .map(WorkerPool.Msg(_, dummy))
      .runWith(Sink.actorSubscriber(WorkerPool.props))

    //    val result = rg.run()
    //
    //    val s = Await.result(result, Duration.Inf)
    //    Console.println("end of main " + s)

    //    Thread.sleep(3000)
    //
    //    val whenTerminated = system.terminate()
    //    Await.result(whenTerminated, Duration.Inf)

  }

}
