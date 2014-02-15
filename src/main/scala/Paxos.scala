import com.twitter.finagle.Service
import com.twitter.finagle.thrift.{ThriftClientRequest, ThriftClientFramedCodec, ThriftServerFramedCodec}
import com.twitter.finagle.builder.{ClientBuilder, ServerBuilder, Server}
import com.twitter.util.{Await, Future}
import org.apache.thrift.protocol.TBinaryProtocol
import java.net.InetSocketAddress
import thrift.PaxosIPC
import thrift.PrepareResponse

class General extends PaxosIPC.FutureIface {
  var np = 0
  var nl = 0
  var na = 0
  var va : Option[Int] = None

  def propose() : Int = {
    println("Proposing!")

    val service : Service[ThriftClientRequest, Array[Byte]] = ClientBuilder()
       .hosts(new InetSocketAddress(8080))
       .codec(ThriftClientFramedCodec())
       .hostConnectionLimit(1)
       .build()
    val client = new PaxosIPC.FinagledClient(service, new TBinaryProtocol.Factory())

    client.accept(1,2) onSuccess { value => client.decided(value) } ensure { service.release() }

    var a = 0
    var no = 0

    np = np + 1

    np
  }

  def listen() = {
    val service = new PaxosIPC.FinagledService(this, new TBinaryProtocol.Factory())
    val server : Server = ServerBuilder()
      .name("PaxosService")
      .bindTo(new InetSocketAddress(8080))
      .codec(ThriftServerFramedCodec())
      .build(service)
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
    println("Decided on " + value)
    Future()
  }
}

object Main extends App {
  val g = new General
  g.listen()
  if (!args.find(a => a == "-p").isEmpty) g.propose() else g.listen()
}