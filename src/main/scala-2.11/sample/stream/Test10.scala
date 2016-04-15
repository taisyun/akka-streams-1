package sample.stream

import akka.stream.stage.{PushPullStage, SyncDirective, Context, TerminationDirective}
import akka.{NotUsed, Done}
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, ActorMaterializer}
import akka.stream.scaladsl._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

/**
  * Created by taishun.nakatani on 2016/03/07.
  */
object Test10 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()


    Source(1 to 9)
      .transform(() => new PushPullStage[Int, Int] {
        var cache: Option[Int] = None
        override def onPush(elem: Int, ctx: Context[Int]): SyncDirective = cache match {
          case Some(n) =>
            cache = None
            ctx.push(n + elem) // 次(Downstream)に送出！
          case None =>
            cache = Some(elem)
            ctx.pull() // 次の値をくれと(Upstreamに)要求！
        }

        override def onPull(ctx: Context[Int]): SyncDirective = {
          if (ctx.isFinishing && cache.isDefined) ctx.pushAndFinish(cache.get)  // 最後の要素を送出
          else ctx.pull()
        }

        override def onUpstreamFinish(ctx: Context[Int]): TerminationDirective = {
          // If the stream is finished, we need to emit the last element in the onPull block.
          // It is not allowed to directly emit elements from a termination block
          // (onUpstreamFinish or onUpstreamFailure)
          ctx.absorbTermination()
        }
      })
      .runForeach(println)

//    val s = Await.result(result, Duration.Inf)
//    Console.println("end of main " + s)
//
//    val whenTerminated = system.terminate()
//    Await.result(whenTerminated, Duration.Inf)

    Thread.sleep(1000)

    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)

  }

}
