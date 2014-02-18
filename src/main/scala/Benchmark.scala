import com.twitter.finagle.Service
import com.twitter.finagle.thrift.{ThriftClientRequest, ThriftClientFramedCodec, ThriftServerFramedCodec}
import com.twitter.finagle.builder.{ClientBuilder, ServerBuilder, Server}
import com.twitter.util.{Await, Future, Duration, Stopwatch}
import com.twitter.conversions.time._
import org.apache.thrift.protocol.TBinaryProtocol
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import thrift.BenchmarkIPC

class BenchmarkClient(val ports : Seq[Int]) {
  val clients = ports.map(port => {
    println("Client connecting to port " + port)
    val service : Service[ThriftClientRequest, Array[Byte]] = ClientBuilder()
      .hosts(new InetSocketAddress(port))
      .codec(ThriftClientFramedCodec())
      .hostConnectionLimit(1)
      .build()
    new BenchmarkIPC.FinagledClient(service, new TBinaryProtocol.Factory())
  })

  def request(): Unit = {
    Await.ready(Future.join(clients.map(c => c.foo())))
  }
}

class BenchmarkServer(val port : Int) extends BenchmarkIPC.FutureIface {
  def listen() = {
    val service = new BenchmarkIPC.FinagledService(this, new TBinaryProtocol.Factory())
    val server : Server = ServerBuilder()
      .name("BenchmarkService")
      .bindTo(new InetSocketAddress(port))
      .codec(ThriftServerFramedCodec())
      .build(service)
    println("Started server on port " + port)
  }

  def foo(): Future[Unit] = {
    Future.value()
  }
}

object Benchmark extends App {
  if (args.size < 2) {
     println("Usage: <# clients> <# messages> [delay in s]")
     System.exit(-1)
  }
  val clients = args(0).toInt
  val messages = args(1).toInt
  val waittime = if (args.size > 2) args(2).toLong else 1L
  val ports = 12000 to 12000 + (clients - 1)
  ports.map(p => new BenchmarkServer(p).listen())
  val client = new BenchmarkClient(ports)
  Thread.sleep(1000L * waittime)
  val elapsed: () => Duration = Stopwatch.start()
  1 to messages foreach { _ => client.request() }
  val duration: Duration = elapsed()
  println(messages + " messages processed in " + duration)
  println("Throughput: " + (messages / duration.inSeconds) + " messages per second")
  System.exit(0)
}