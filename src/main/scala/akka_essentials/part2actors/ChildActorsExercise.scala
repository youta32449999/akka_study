package akka_essentials.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.BackoffSupervisor.CurrentChild

object ChildActorsExercise extends App {

  // Distributed Word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(id: Int, text: String)
    case class WordCountReply(id: Int, count: Int)
  }
  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        if(nChildren > 0){
          val children = (1 to nChildren).map(_ => context.actorOf(Props[WordCounterWorker])).toArray
          context.become(running(children, 0, 0, Map()))
        }
    }

    def running(children: Seq[ActorRef], currentChildId: Int, currentTaskId: Int, senderMap: Map[Int, (ActorRef,String)]): Receive = {
      case text: String =>
        children(currentChildId) ! WordCountTask(currentTaskId, text)
        val nextChildId = (currentChildId + 1) % children.length
        val newSenderMap = senderMap + (currentTaskId -> (sender(), text))
        context.become(running(children, nextChildId, currentTaskId + 1, newSenderMap))
      case WordCountReply(taskId, count) =>
        val (sender, text) = senderMap(taskId)
        sender ! (text, count)
    }
  }
  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(taskId, text) =>
        println(s"${context.self.path}")
        val count = text.split(" ").length
        sender() ! WordCountReply(taskId, count)
    }
  }

  class TestActor extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case "go" =>
        val master = context.actorOf(Props[WordCounterMaster], "wordCounterMaster")
        master ! Initialize(3)
        val texts = List("I love Akka", "Scala is super dope", "yes", "me too")
        texts.foreach(text => master ! text)
      case (text, count) =>
        println(s"'$text' is $count words")
    }
  }

  val system = ActorSystem("system")
  val testActor = system.actorOf(Props[TestActor], "testActor")
  testActor ! "go"


  /*
    create WordCounterMaster
    send Initialize(10) to wordCounterMaster
    send "Akka is awesome" to wordCounterMaster
      wcm will send a WordCountTask("...") to one of its children
        child replies with a WordCountReply(3) to the master
      master replies with 3 to the sender.

    request -> wcm -> wcw
          r <- wcm <-
   */
}
