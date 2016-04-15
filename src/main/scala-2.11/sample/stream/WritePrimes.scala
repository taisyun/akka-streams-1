package sample.stream

import java.io.File

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{Graph, IOResult, ActorMaterializer, ClosedShape}
import akka.stream.scaladsl._
import akka.util.ByteString

import scala.concurrent.Future
import scala.concurrent.forkjoin.ThreadLocalRandom
import scala.util.{ Failure, Success }

object WritePrimes {

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    // generate random numbers
    val maxRandomNumberSize = 1000000
    val primeSource: Source[Int, NotUsed] =
      Source.fromIterator(() => Iterator.continually(ThreadLocalRandom.current().nextInt(maxRandomNumberSize))).
        // filter prime numbers
        filter(rnd => isPrime(rnd)).
        // and neighbor +2 is also prime
        filter(prime => isPrime(prime + 2))

    // write to file sink
    val fileSink = FileIO.toFile(new File("target/primes.txt"))
    val slowSink: Sink[Int, Future[IOResult]] = Flow[Int]
      // act as if processing is really slow
      .map(i => { Thread.sleep(1000); ByteString(i.toString + System.lineSeparator) })
      .toMat(fileSink)((_, bytesWritten) => bytesWritten)

    // console output sink
    val consoleSink: Sink[Int, Future[Done]] = Sink.foreach[Int](println)

    // send primes to both slow file sink and console sink using graph API
    val graph: Graph[ClosedShape.type, Future[IOResult]] = GraphDSL.create(slowSink, consoleSink)((slow, _) => slow) { implicit builder =>
      (slow, console) =>
        import GraphDSL.Implicits._
        val broadcast = builder.add(Broadcast[Int](2)) // the splitter - like a Unix tee
        primeSource ~> broadcast ~> slow // connect primes to splitter, and one side to file
        broadcast ~> console // connect other side of splitter to console
        ClosedShape
    }
    val materialized: Future[IOResult] = RunnableGraph.fromGraph(graph).run()

    // ensure the output file is closed and the system shutdown upon completion
    materialized.onComplete {
      case Success(_) =>
        system.terminate()
      case Failure(e) =>
        println(s"Failure: ${e.getMessage}")
        system.terminate()
    }

  }

  def isPrime(n: Int): Boolean = {
    if (n <= 1) false
    else if (n == 2) true
    else !(2 to (n - 1)).exists(x => n % x == 0)
  }
}
