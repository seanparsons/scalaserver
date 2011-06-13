package com.futurenotfound.scalaserver

import java.util.concurrent.TimeUnit
import org.glassfish.grizzly.utils.IdleTimeoutFilter
import org.glassfish.grizzly.nio.transport.TCPNIOTransportBuilder
import org.glassfish.grizzly.filterchain.{FilterChainContext, BaseFilter, FilterChainBuilder, TransportFilter}
import org.glassfish.grizzly.{Context, Processor}
import org.glassfish.grizzly.memory.Buffers
import akka.actor.Actor
import akka.actor.Actor._
import org.glassfish.grizzly.http._

object GrizzlyServer extends App {
  val HOST = "localhost"
  val PORT = 8080

  val serverFilterChainBuilder = FilterChainBuilder.stateless()
  serverFilterChainBuilder.add(new TransportFilter())
  //serverFilterChainBuilder.add(new IdleTimeoutFilter(10L, TimeUnit.SECONDS))
  serverFilterChainBuilder.add(new HttpServerFilter())
  serverFilterChainBuilder.add(new WebFilter())
  val transport = TCPNIOTransportBuilder.newInstance().build()
  transport.setProcessor(serverFilterChainBuilder.build().asInstanceOf[Processor[Context]])
  transport.bind(HOST, PORT);
  transport.start()
  System.in.read()
  transport.stop()
}

case class WebFilter() extends BaseFilter {
  override def handleRead(ctx: FilterChainContext) = {
    println("handleRead")
    val message = ctx.getMessage[Object]()
    println(message)
    val httpContent = ctx.getMessage[Object]().asInstanceOf[HttpContent]
    val request = httpContent.getHttpHeader().asInstanceOf[HttpRequestPacket]
    if (!httpContent.isLast()) {
      println(request)
      ctx.getStopAction()
    } else {
      ctx.getInvokeAction()
    }
  }
}

case class WebFilterActor() extends Actor {
  type HeaderBuilderType = HttpHeader.Builder[HttpHeader]
  def receive = {
    case (context: FilterChainContext, request: HttpRequestPacket) => {
      val responseHeader = HttpResponsePacket.builder(request)
                                             .protocol(request.getProtocol())
                                             .status(404)
                                             .reasonPhrase("Not Found")
                                             .build()

      val header = responseHeader.httpContentBuilder().asInstanceOf[HeaderBuilderType].content(Buffers.wrap(null, "Hi!")).build()
      context.write(header)
    }
  }
}