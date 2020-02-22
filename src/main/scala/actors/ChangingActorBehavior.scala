package actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ChangingActorBehavior extends App {

  /**
    * Exercises: create the Counter Actor with context.become and no mutable state
    *
    * */

  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  class Counter extends Actor {
    import Counter._

    override def receive: Receive = countReceiver(0)

    def countReceiver(currentCount: Int): Receive = {
      case Increment =>
        println(s"[countReceiver($currentCount)] incrementing")
        context.become(countReceiver(currentCount + 1))
      case Decrement =>
        println(s"[countReceiver($currentCount)] decrementing")
        context.become(countReceiver(currentCount - 1))
      case Print => println(s"[countReceive($currentCount)] my current count is $currentCount")
    }
  }

  import Counter._
  val system = ActorSystem("actorSystem")
  val counter = system.actorOf(Props[Counter], "myCounter")

  (1 to 5).foreach(_ => counter ! Increment)
  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  /**
    * Exercise: a simplified voting system
    * */

  case class Vote(candidate: String)
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])
  class Citizen extends Actor {

    var candidate: Option[String] = None

    override def receive: Receive = {
      case Vote(candidate) => this.candidate = Some(candidate)
      case VoteStatusRequest => sender() ! VoteStatusReply(this.candidate)
    }
  }

  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    var stillWaiting: Set[ActorRef] = Set()
    var currentStats: Map[String, Int] = Map()

    override def receive: Receive = {
      case AggregateVotes(citizens) =>
        stillWaiting = citizens
        citizens.foreach(citizenRef => citizenRef ! VoteStatusRequest)
      case VoteStatusReply(None) => sender() ! VoteStatusRequest
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVoteOfCandidate = currentStats.getOrElse(candidate, 0)
        currentStats = currentStats + (candidate -> (currentVoteOfCandidate + 1))
        if (newStillWaiting.isEmpty) {
          println(s"[aggregator] poll stats: $currentStats")
        } else{
          stillWaiting = newStillWaiting
        }

    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice, bob, charlie, daniel))

}
