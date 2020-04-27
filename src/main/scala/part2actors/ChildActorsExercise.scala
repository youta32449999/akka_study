package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChildActorsExercise extends App {

  // Distributed Word counting

  object WordCounterMaster {
    case class Initialize(nChildren: Int)
    case class WordCountTask(text: String)
    case class WordCountReply(count: Int)
  }
  class WordCounterMaster extends Actor {

    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        if(nChildren > 0){
          val children = (1 to nChildren).map(_ => context.actorOf(Props[WordCounterWorker])).toList
          context.become(running(children, Nil))
        }
    }

    def running(children: List[ActorRef], executed: List[ActorRef]): Receive = {
      case WordCountTask(text) =>
        children match {
          case Nil => println(0)
          case x :: xs => {
            x ! WordCountTask(text)
            xs match {
              case Nil => context.become(running(executed, Nil))
              case _ => context.become(running(xs, executed :+ x))
            }
          }
        }
      case WordCountReply(count) => println(count)
    }
  }
  class WordCounterWorker extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case WordCountTask(text) =>
        println(s"${context.self.path}")
        val count = text.split(" ").length
        sender() ! WordCountReply(count)
    }
  }

  val system = ActorSystem("system")
  val wordCounterMaster = system.actorOf(Props[WordCounterMaster], "wordCountMaster")
  import WordCounterMaster._

  wordCounterMaster ! Initialize(5)
  (1 to 7).foreach(_ => wordCounterMaster ! WordCountTask("Akka is awesome"))


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
