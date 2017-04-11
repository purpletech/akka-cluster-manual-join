package nodes.worker
import scala.io.Source
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.ActorNotFound
import akka.util.Timeout
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.MemberStatus
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.TimeoutException
import scala.collection.JavaConverters._
import scala.util.control.Breaks._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.Config
import common.messages._

class WorkerNode(config: Config) extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  var tokenStore = List[PrintToken]()

  override def preStart(): Unit = {
    println("Worker created: " + self.path)
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent], classOf[UnreachableMember])
    joinCluster()
    context.system.scheduler.schedule(0 seconds, 10 seconds) {
      self ! CheckJob
    }
  }

  override def postStop(): Unit =
    cluster.unsubscribe(self)

  def joinCluster() = {
    var joined = false
    breakable {
      for (host <- config.getStringList("cluster.members").asScala) {
        val timout = new Timeout(5 seconds)
        val clusterName = config.getString("cluster.name")
        val path = "akka.tcp://" + clusterName + "@" + host + "/user/" + "worker"
        println("Trying to join cluster with member " + host)
        try {
          val member = Await.result(context.actorSelection(path).resolveOne()(timout), 5 seconds)
          println("Actor ref is " + member)
          cluster.join(member.path.address)
          joined = true
          break
        } catch {
          case e @ (_: TimeoutException | ActorNotFound(_)) => {
            //println("Exception joinning cluster " + e.getMessage)
          }
        }
      } //End try
    } //End breakable
    if (joined == false) {
      cluster.join(cluster.selfAddress)
      tokenStore = List(new PrintToken)
    }
  }

  def receive = {
    case MemberUp(member) =>
      println("Member is Up: " + member.address)
    case UnreachableMember(member) =>
      println("Member detected as unreachable: " + member)
    case MemberRemoved(member, previousStatus) =>
      println("Member is Removed: " + member.address + " after " + previousStatus)
    case LeaderChanged(member) => println("Leader changed: " + member)
    case TokenRequest =>
      println("Received message TokenRequest " + sender)
      if (!tokenStore.isEmpty) {
        println("Sending PrintToken to " + sender)
        sender ! tokenStore.head
        tokenStore = Nil
      }
    case CheckJob =>
      println("Received message CheckJob from " + sender)
      if (tokenStore.isEmpty) {
        for (member <- cluster.state.members.filter(_.status == MemberStatus.Up)) {
          context.actorSelection(member.address + "/user/worker").tell(TokenRequest, self)
        }
      } else {
        execute
      }
    case e: PrintToken =>
      {
        println("Received message PrintToken from" + sender)
        tokenStore = List(e)
        execute
      }
    case JobCompleted => {
      println("Received message JobCompleted from " + sender)
      context.become(idle)
    }
  }
  def execute = {
    if (isReadyToPrint) {
      println("=======We are started============")
      for (member <- cluster.state.members.filter(_.status == MemberStatus.Up)) {
    	  context.actorSelection(member.address + "/user/worker").tell(JobCompleted, self)
      }
    }
  }
  def idle: Receive = {
    case _ => //do nothing
  }

  def isReadyToPrint: Boolean = {
    Thread.sleep(5000)
    val homeDire = System.getProperty("user.home")
    val filename = homeDire + "/job.txt"
    try {
      !Source.fromFile(filename).getLines.isEmpty
    } catch {
      case _: Throwable => false
    }
  }
}
object WorkerNode {
  import common.Configurations._

  def props(config: Config) = Props(new WorkerNode(config))

  def main(args: Array[String]) = {
    val config = getConfig(args(0))
    val system = ActorSystem(config.getString("cluster.name"), config)
    val worker = system.actorOf(props(config), "worker")
  }
}