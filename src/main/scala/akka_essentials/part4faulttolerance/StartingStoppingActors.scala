package akka_essentials.part4faulttolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Kill, PoisonPill, Props, Terminated}

object StartingStoppingActors extends App {

  val system = ActorSystem("StoppingActorDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = withChildren(Map())

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Starting child $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child with the name $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
        log.info(s"Stopped child $name")
      case Stop =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(message.toString)
    }
  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  /**
    * method #1 - using context.stop
    */
  import Parent._
  // val parent = system.actorOf(Props[Parent], "parent")
  // parent ! StartChild("child1")
  // val child1 = system.actorSelection("/user/parent/child1")
  // child1 ! "hi kid"

  /*
    context.stop(childRef)が実行されてからはchild1はメッセージを処理しない
    "Stopped child child1"以降のメッセージはDead Letterになっている
   */
  // parent ! StopChild("child1")
  // for (_ <- 1 to 50) child1 ! "are you still there?"

  // parent ! StartChild("child2")
  // // Thread.sleepを入れないとDead Letterになる
  // Thread.sleep(1)
  // val child2 = system.actorSelection("/user/parent/child2")
  // child2 ! "hi, second child"

  /*
    parentはStopメッセージでcontext.stop(self)しているので、"parent, are you still there?"は処理されない
    child2はcontext.stop(self)により、parentがchild2を停止させるまでメッセージを処理する
   */
  // parent ! Stop
  // for (_ <- 1 to 10) parent ! "parent, are you still there?" // should not be received
  // for (i <- 1 to 100) child2 ! s"[$i] second kid, are you still alive?"

  /**
    * method #2 - using special messages
    * ActorCellのautoReceiveMessageメソッドで以下の処理が呼ばれてActorが停止する
    * PoisonPill: self.stop()
    * Kill: throw ActorKilledException("Kill")
    */
  // val looseActor = system.actorOf(Props[Child])
  // looseActor ! "hello, loose actor"
  // looseActor ! PoisonPill
  // looseActor ! "loose actor, are you still there?"

  // val abruptlyTerminatedActor = system.actorOf(Props[Child])
  // abruptlyTerminatedActor ! "you are about to be terminated"
  // abruptlyTerminatedActor ! Kill
  // abruptlyTerminatedActor ! "you have been terminated"


  /**
    * Death watch
    * context.watch(actorRef)を実行したとき、
    * watchしたactorRefが停止した時にTerminated(ActorRef)メッセージを受け取る
    */
  class Watcher extends Actor with ActorLogging {
    import Parent._

    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child], name)
        log.info(s"Started and watching child $name")
        context.watch(child)
      case Terminated(ref) =>
        log.info(s"the reference that I'm watching $ref has benn stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher], "watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)

  watchedChild ! PoisonPill

}
