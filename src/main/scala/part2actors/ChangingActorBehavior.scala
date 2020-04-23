package part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehavior extends App {

  val system = ActorSystem("system")

  /**
    * Exercises
    * 1 - recreate the Counter Actor with context.become and NO MUTABLE STATE
    */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    override def receive: Receive = counterReceive(0)

    def counterReceive(count: Int): Receive = {
      case Increment => context.become(counterReceive(count + 1))
      case Decrement => context.become(counterReceive(count - 1))
      case Print => println(s"[counter] My current count is $count")
    }
  }

  import Counter._
  val counter = system.actorOf(Props[Counter], "myCounter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
    * Exercise 2 - a simplified voting system
    */

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {
    override def receive: Receive = citizenReceive(None)

    def citizenReceive(candidate: Option[String]): Receive = {
      case Vote(candidate) =>
        if (candidate != "") context.become(citizenReceive(Some(candidate)))
      case VoteStatusRequest => sender() ! VoteStatusReply(candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = voteAggregatorReceive(Nil)

    def voteAggregatorReceive(ballotBox: List[String]): Receive = {
      case VoteStatusReply(candidate) => candidate match {
        case None =>
        case Some(name) => {
          context.become(voteAggregatorReceive(name :: ballotBox))
        }
      }
      case AggregateVotes(citizens) => {
        context.become(voteAggregatorReceive(Nil))
        citizens.foreach(_ ! VoteStatusRequest)
        self ! ShowAggregate
      }
      case ShowAggregate =>
        val result = ballotBox
          .groupBy(name => name)
          .map(xs => (xs._1, xs._2.length))
        println(result)
    }
    case object ShowAggregate
  }

  val alice = system.actorOf(Props[Citizen], "alice")
  val bob = system.actorOf(Props[Citizen], "bob")
  val charlie = system.actorOf(Props[Citizen], "charlie")
  val daniel = system.actorOf(Props[Citizen], "daniel")

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator], "voteAggregator")
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

  /*
    Print the status of the votes

    Martin -> 1
    Jonas -> 1
    Roland -> 2
   */
}
