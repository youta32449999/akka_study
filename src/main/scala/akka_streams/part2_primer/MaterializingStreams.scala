package akka_streams.part2_primer

import java.nio.file.FileAlreadyExistsException

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system = ActorSystem("system")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  /**
    * - return the last element out of a source (use Sink.last)
    * - compute the total word count out of stream of sentences
    *   - map, fold, reduce
    */

  val elementsSource = Source(1 to 10)
  val lastElementSink = Sink.last[Int]
  val lastElementGraph = elementsSource.toMat(lastElementSink)(Keep.right)
  lastElementGraph.run().onComplete {
    case Success(n) => println(s"success: last element is $n")
    case Failure(exception) => println(s"failed: $exception")
  }

  val sentences = List("I love Scala and Akka", "Functional programming is very beautiful")
  val sentenceSource = Source(sentences)
  val sentenceToWordCountFlow = Flow[String].map(sentence => sentence.split(" ").length)
  // val wordCountSumSink = Sink.reduce[Int](_ + _)
  sentenceSource.viaMat(sentenceToWordCountFlow)(Keep.right).runReduce(_ + _).onComplete {
    case Success(n) => println(s"success: word count is $n")
    case Failure(exception) => println(s"failed: $exception")
  }

  // model answer
  val f1 = Source(1 to 10).toMat(Sink.last)(Keep.right).run()
  val f2 = Source(1 to 10).runWith(Sink.last)

  val wordCountSink = Sink.fold[Int, String](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g1 = sentenceSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentenceSource.runWith(wordCountSink)
  val g3 = sentenceSource.runFold(0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)

  val wordCountFlow = Flow[String].fold[Int](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g4 = sentenceSource.via(wordCountFlow).toMat(Sink.head)(Keep.right).run()
  val g5 = sentenceSource.viaMat(wordCountFlow)(Keep.left).toMat(Sink.head)(Keep.right).run()
  val g6 = sentenceSource.via(wordCountFlow).runWith(Sink.head)
  val g7 = wordCountFlow.runWith(sentenceSource, Sink.head)._2
}
