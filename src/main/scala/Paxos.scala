import com.twitter.finagle.Service
import com.twitter.finagle.thrift.{ThriftClientRequest, ThriftClientFramedCodec, ThriftServerFramedCodec}
import com.twitter.finagle.builder.{ClientBuilder, ServerBuilder, Server}
import com.twitter.util.{Await, Future, Duration, Stopwatch}
import com.twitter.conversions.time._
import org.apache.thrift.protocol.TBinaryProtocol
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import thrift.PaxosIPC
import thrift.PrepareResponse

class Proposer(val ports : Seq[Int]) {
  val threshold : Int = (ports.size / 2)
  var next = 0

  val clients = ports.map(port => {
    println("Proposer connecting to port " + port)
    val service : Service[ThriftClientRequest, Array[Byte]] = ClientBuilder()
      .hosts(new InetSocketAddress(port))
      .codec(ThriftClientFramedCodec())
      .hostConnectionLimit(32)
      .build()
    new PaxosIPC.FinagledClient(service, new TBinaryProtocol.Factory())
  })

  var np = 0
  var a  = new AtomicInteger(0)
  var p  = new AtomicInteger(0)
  var no = 0
  var vo : Option[Int] = None

  def propose(): Unit = {
    np = np + 1
    a.set(0)
    p.set(0)
    no = 0
    vo = None

    Await.ready(Future.join(clients.map(c => c.prepare(np) flatMap prepared)), 1.second)
  }

  def prepared(pr : PrepareResponse): Future[Unit] = {
    val n = pr.round
    val v = pr.value
    if (n > no) {
      no = n
      vo = v
    }
    if (p.getAndIncrement() == threshold) {
       if (vo == None) {
          vo = Some(next)
          next = next + 1
       }
       np = if (np > no) np else no
       Future.join(clients.map(c => c.accept(np,vo.get) flatMap accepted))
    } else Future()
  }

  def accepted(n : Int): Future[Unit] = {
    if (n == np && a.getAndIncrement() == threshold)
      Future.join(clients.map(c => c.decided(vo.get)))
    else Future()
  }
}

class Acceptor(val port : Int) extends PaxosIPC.FutureIface {
  var nl = 0
  var na = 0
  var va : Option[Int] = None

  def listen() = {
    val service = new PaxosIPC.FinagledService(this, new TBinaryProtocol.Factory())
    val server : Server = ServerBuilder()
      .name("PaxosService")
      .bindTo(new InetSocketAddress(port))
      .codec(ThriftServerFramedCodec())
      .build(service)
    println("Started Acceptor on port " + port)
  }

  def prepare(n : Int): Future[PrepareResponse] = {
    nl = if (nl > n) nl else n
    Future.value(PrepareResponse(na,va))
  }

  def accept(n : Int, v : Int): Future[Int] = {
    if (n >= nl) {
      na = n
      nl = n
      va = Some(v)
    }
    Future.value(na)
  }

  def decided(value : Int): Future[Unit] = {
    // println(port + ": decided(" + value + ")")
    Future()
  }
}

object Paxos extends App {
  if (args.size < 2) {
     println("Usage: <# failures> <# rounds> [delay in s]")
     System.exit(-1)
  }
  val f = args(0).toInt
  val rounds = args(1).toInt
  val waittime = if (args.size > 2) args(2).toLong else 1L
  val ports = 12000 to 12000 + (2 * f)
  ports.map(p => new Acceptor(p).listen())
  val proposer = new Proposer(ports)
  Thread.sleep(1000L * waittime)
  val elapsed: () => Duration = Stopwatch.start()
  1 to rounds foreach { _ => proposer.propose() }
  val duration: Duration = elapsed()
  println(rounds + " rounds of paxos completed in " + duration)
  println("Throughput: " + (rounds / duration.inSeconds) + " rounds per second")
  System.exit(0)
}
