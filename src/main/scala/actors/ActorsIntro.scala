package actors

import actors.ActorsIntro.Person.LiveTheLife
import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import com.sun.javafx.scene.transform.TransformUtils

object ActorsIntro extends App {

  // part1 - actor systems
  val actorSystem = ActorSystem("firstActorSystem")

  // part2 - create actors
  class WordCountActor extends Actor {
    var totalWords = 0

    override def receive: PartialFunction[Any, Unit] = {
      case message: String => totalWords += message.split(" ").length
      case msg => println(s"[word counter] I cannot understant ${msg.toString}")
    }
  }

  // part3 - instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")

  // part4 - communicate
  wordCounter ! "I'm learning akka"

  /*object Person {
    def props(name: String) = Props(new Person(name))
  }
  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is $name")
      case _ =>
    }
  }

  val bob = actorSystem.actorOf(Props(new Person("Bob")))
  val alice = actorSystem.actorOf(Person.props("Alice"))*/


  object Counter {
    case object Increment
    case object Decrement
    case object Print

  }

  class Counter extends Actor {
    import Counter._

    var count = 0

    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"current counter: $count")
    }
  }

  val counter = actorSystem.actorOf(Props[Counter], "counter")

  counter ! Counter.Increment
  counter ! Counter.Increment
  counter ! Counter.Decrement
  counter ! Counter.Print


  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var balance = 0

    override def receive: Receive = {
      case Deposit(amount) =>
        if (amount < 0)
          sender() ! TransactionFailure("invalid deposit amount")
        else {
          balance += amount
          sender() ! TransactionSuccess(s"successfully deposited $amount")
        }
      case Withdraw(amount) =>
        if (amount < 0)
          sender() ! TransactionFailure("invalid withdraw amount")
        else if (amount > balance)
          sender() ! TransactionFailure("insufficient balance")
        else {
          balance -= amount
          sender() ! TransactionSuccess(s"successfully withdrew $amount")
        }
      case Statement => sender() ! s"Current balance: $balance"
    }
  }

  object Person {
    case class LiveTheLife(ref: ActorRef)
  }

  class Person extends Actor {
    import Person._
    import BankAccount._

    override def receive: Receive = {
      case LiveTheLife(account) =>
        account ! Deposit(10000)
        account ! Withdraw(90000)
        account ! Withdraw(500)
        account ! Statement
      case message => println(message.toString)
    }
  }

  val bankAccount = actorSystem.actorOf(Props[BankAccount])
  val person = actorSystem.actorOf(Props[Person])

  person ! LiveTheLife(bankAccount)

}