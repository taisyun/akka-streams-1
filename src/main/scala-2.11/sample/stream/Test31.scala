package sample.stream

import akka.actor.{Inbox, Props, ActorSystem}
import akka.stream.actor.ActorPublisher
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by taishun.nakatani on 2016/03/15.
  */

object JobManager {
  def props: Props = Props[JobManager]

  final case class Job(payload: String)
  case object JobAccepted
  case object JobDenied
}

class JobManager extends ActorPublisher[JobManager.Job] {
  import akka.stream.actor.ActorPublisherMessage._
  import JobManager._

  val MaxBufferSize = 100
  var buf = Vector.empty[Job]

  def receive = {
    case job: Job if buf.size == MaxBufferSize =>
      sender() ! JobDenied
    case job: Job =>
      sender() ! JobAccepted
      if (buf.isEmpty && totalDemand > 0)
        onNext(job)
      else {
        buf :+= job
        deliverBuf()
      }
    case Request(_) =>
      deliverBuf()
    case Cancel =>
      context.stop(self)
  }

  @tailrec final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      /*
       * totalDemand is a Long and could be larger than
       * what buf.splitAt can accept
       */
      if (totalDemand <= Int.MaxValue) {
        val (use, keep) = buf.splitAt(totalDemand.toInt)
        buf = keep
        use foreach onNext
      } else {
        val (use, keep) = buf.splitAt(Int.MaxValue)
        buf = keep
        use foreach onNext
        deliverBuf()
      }
    }
}

object Test31 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val jobManagerSource = Source.actorPublisher[JobManager.Job](JobManager.props)
    val ref = Flow[JobManager.Job]
      .map(_.payload.toUpperCase)
      .map { elem => println(elem); elem }
      .to(Sink.ignore)
      .runWith(jobManagerSource)

    val inbox = Inbox.create(system)

    inbox.send( ref, JobManager.Job("a") )
    inbox.send( ref, JobManager.Job("b") )
    inbox.send( ref, JobManager.Job("c") )


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
