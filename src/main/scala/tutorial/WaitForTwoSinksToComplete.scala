package tutorial


import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._
import akka.util.ByteString
import akka.{Done, NotUsed}

import scala.concurrent._

object WaitForTwoSinksToComplete {

  def main(args: Array[String]) = {

    implicit val system = ActorSystem("WaitForTwoSinksToComplete")
    implicit val ec = system.dispatcher
    implicit val materializer = ActorMaterializer()

    val source: Source[Int, NotUsed] = Source(1 to 100)

    val f1Fut: Future[Done] = source.runForeach(i => println(i))(materializer)

    //a description of what happens when we scan (= transform) the source
    val factorials = source.scan(BigInt(1))((acc, next) => acc * next)
    val f2fut: Future[IOResult] =
      factorials
        .map(num => ByteString(s"$num\n"))
        .runWith(FileIO.toPath(Paths.get("factorials.txt")))

    val aggFut = for {
      f1Result <- f1Fut
      f2Result <- f2fut
    } yield (f1Result, f2Result)

    aggFut.onComplete{  results =>
      println("Futures completed with results: " + results + " - about to terminate")
      system.terminate()
    }}
}
