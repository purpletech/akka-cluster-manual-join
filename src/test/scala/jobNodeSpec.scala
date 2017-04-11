import akka.actor.ActorSystem
import akka.actor.Actor
import akka.testkit.{ TestKit, TestActorRef, ImplicitSender }
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.BeforeAndAfterAll
import nodes.job.JobNode
import common.messages._

class JobNodeSpec extends TestKit(ActorSystem("testSystem"))
    with ImplicitSender
    with WordSpecLike
    with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A job node" must {
    "send back a job at first worker started" in {
      val actorRef = TestActorRef[JobNode]
      actorRef ! WorkerStarted
      expectMsg(Job("We are started"))
    }
  }
  "A job node" must {
    "send back a Ping after the job was sent" in {
      val actorRef = TestActorRef[JobNode]
      actorRef ! WorkerStarted
      expectMsg(Job("We are started"))
      actorRef ! WorkerStarted
      expectMsg("Ping")
    }
  }
}