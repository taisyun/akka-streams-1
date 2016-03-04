package sample.stream

import akka.NotUsed
import akka.actor.{Props, ActorRef, ActorSystem}
import akka.stream.ActorMaterializer
import akka.stream.actor.ActorPublisher
import akka.stream.scaladsl.{RunnableGraph, Source, Sink}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
  * Created by taishun.nakatani on 2016/03/04.
  */
case class Message(n: Int)
case object END

class SampleActor extends ActorPublisher[Int] {
  def receive: PartialFunction[Any, Unit] = {
    case Message(n) => onNext(n)
    case END => onComplete() // ※ちゃんと終了シグナルを送らないとfoldのような処理がDownstreamにいた場合、どこがデータの終了かわからなくなってfoldが終わらなくなる
  }
}

object Test2 {
  def main(args: Array[String]): Unit = {
    // actor system and implicit materializer
    implicit val system = ActorSystem("Sys")
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, ActorRef] = Source.actorPublisher[Int](Props[SampleActor])
    val rg: RunnableGraph[ActorRef] = source.to(Sink.foreach(println))
    val actor = rg.run()

//    Thread.sleep(1000) // 調整のためのsleep

    actor ! Message(1)
    actor ! Message(2)
    actor ! Message(3)
    actor ! END


  }

}
