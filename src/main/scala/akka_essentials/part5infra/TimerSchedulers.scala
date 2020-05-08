package akka_essentials.part5infra

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._

object TimerSchedulers extends App {

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimerDemo")
  val simpleActor = system.actorOf(Props[SimpleActor])

  system.log.info("Scheduling reminder for simpleActor")

  import system.dispatcher

  /**
    * scheduleOnceは第一引数に指定した秒数後に一度だけ
    * 第二引数に記述した処理を行う
    */
//  system.scheduler.scheduleOnce(1 second){
//    simpleActor ! "reminder"
//  }

  /**
    * scheduleは第一引数に指定した秒数後に{}に記述した処理を実行
    * 以降は第二引数で指定した周期で処理を実行する
    * 返り値はCancellableとなり、.cancel()メソッドでスケジューラを停止できる
    */
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds){
//    simpleActor ! "heartbeat"
//  }
//
//  system.scheduler.scheduleOnce(5 seconds){
//    routine.cancel()
//  }

  /**
    * Exercise: implement a self-closing actor
    *
    * - if the actor receives a message (anything), you have 1 second to send it another message
    * - if the time window expires, the actor will stop itself
    * - if you send another message, the time window is reset
    */
//  class SelfClosingActor extends Actor with ActorLogging {
//    override def receive: Receive = {
//      case message =>
//        log.info(message.toString)
//        context.become(waitingClose(createCloseTime))
//    }
//
//    def waitingClose(timer: Cancellable): Receive = {
//      case message => {
//        log.info(message.toString)
//        timer.cancel()
//        context.become(waitingClose(createCloseTime))
//      }
//    }
//
//    private def createCloseTime(): Cancellable = {
//      system.scheduler.scheduleOnce(1 second) {
//        log.info("closing actor")
//        context.stop(self)
//      }
//    }
//  }
//
//  val selfClosingActor = system.actorOf(Props[SelfClosingActor],"selfClosingActor")
//
//  selfClosingActor ! "start"
//  selfClosingActor ! "next message"
//  Thread.sleep(1020)
//  selfClosingActor ! "maybe dead letter"

  /**
    * Model answer
    */
  class SelfClosingActor extends Actor with ActorLogging {

    override def receive: Receive = {
      case message =>
        log.info(s"Received $message, staying alive")
        context.become(withScheduler(createTimeoutWindow()))
    }

    def withScheduler(scheduler: Cancellable): Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message, staying alive")
        scheduler.cancel()
        context.become(withScheduler(createTimeoutWindow()))
    }

    def createTimeoutWindow(): Cancellable ={
      context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }
    }
  }

  /*
    250ms後にpingを送信,2s後にpongを送信
    250ms後の1s後にselfClosingActorはstopするのでpongメッセージはDead Letterになる
   */
//  val selfClosingActor = system.actorOf(Props[SelfClosingActor], "selfClosingActor")
//  system.scheduler.scheduleOnce(250 millis) {
//    selfClosingActor ! "ping"
//  }
//
//  system.scheduler.scheduleOnce(2 seconds) {
//    system.log.info("sending pong to the self-closing actor")
//    selfClosingActor ! "pong"
//  }

  /**
    * Timer
    */
  case object TimerKey
  case object Start
  case object Reminder
  case object Stop
  class TimerBaseHeartbeatActor extends Actor with ActorLogging with Timers {
    /*
      timers.startSingleTimer(key, msg, timeout): timeout後に1度だけselfへmsgを送信する
      (key: timerに付与するkey。同一keyが振られたtimerはcancelメソッドなどで同時に停止できる

      timers.startTimerWithFixedDelay(key, msg, delay): delay毎にselfへmsgを送信する
     */
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        timers.startTimerWithFixedDelay(TimerKey, Reminder, 1 second)
      case Reminder =>
        log.info("I am alive")
      case Stop =>
        log.warning("Stopping!")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

//  val timerHeartbeatActor = system.actorOf(Props[TimerBaseHeartbeatActor], "timerActor")
//  system.scheduler.scheduleOnce(5 seconds) {
//    timerHeartbeatActor ! Stop
//  }
}
