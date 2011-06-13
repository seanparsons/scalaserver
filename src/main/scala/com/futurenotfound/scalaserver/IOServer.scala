package com.futurenotfound.scalaserver

import akka.actor.Actor
import akka.routing._
import akka.actor.Actor._
import java.util.concurrent.atomic.AtomicBoolean
import java.lang.Thread
import java.net.{Socket, ServerSocket}
import java.io.{BufferedReader, InputStreamReader}

/*
object HttpTestServer extends App {
  val testHttpHandler = new HttpRequestHandler {
    def handle(socket: Socket, request: HttpRequest) = {
      val content = "<html><body><h1>GIGANTOR!</h1></body></html>"
      "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\nContent-Length: %s\r\nServer: Gigantor!\r\n\r\n%s".format(content.length(), content).getBytes
    }
  }
  val pool = actorOf(new IOServerActorPool(testHttpHandler)).start()
  val serverSocket = new ServerSocket(8080, 1000)
  val keepRunning = new AtomicBoolean(true)
  def createWorkerThread = new Thread(new Runnable {
    def run() = {
      while (keepRunning.get()) {
        try {
          val clientSocket = serverSocket.accept()
          pool ! clientSocket
        } catch {
          case throwable => println(throwable)
        }
      }
    }
  }, "Accept Thread")
  val workerThread1 = createWorkerThread
  workerThread1.start()

  while(readChar() != 'q') {
    Thread.sleep(100)
  }

  println("Attempting to stop")
  registry.shutdownAll()
  keepRunning.set(false)
  serverSocket.close()
  println("Stopped")
}
*/

case class IOServerActorPool(handler: SocketHandler) extends Actor
                                                            with DefaultActorPool
                                                            with BoundedCapacityStrategy
                                                            with ActiveFuturesPressureCapacitor
                                                            with SmallestMailboxSelector
                                                            with BasicNoBackoffFilter
{
   def receive = _route
   def lowerBound = 2
   def upperBound = 100
   def rampupRate = 0.1
   def partialFill = true
   def selectionCount = 1
   def instance = actorOf(new IOServerActor(handler))
}

case class IOServerActor(handler: SocketHandler) extends Actor {
  def receive = {
    case socket: Socket => {
      socket.setSoTimeout(30000)
      handler.handle(socket)
      socket.close()
    }
  }
}

trait SocketHandler {
  def handle(socket: Socket): Unit
}

trait HttpRequestHandler extends SocketHandler {
  override def handle(socket: Socket): Unit = {
    val in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
    val request = HttpRequest.parse(in)
    val out = socket.getOutputStream()
    out.write(handle(socket, request))
    socket.close()
  }
  def handle(socket: Socket, request: HttpRequest): Array[Byte]
}

case class Header(name: String, value: String)
object Header {
  def parse(line: String) = {
    val (name, value) = line.splitAt(line.indexOf(":"))
    Header(name, value)
  }
}
case class HttpRequest(method: String, path: String, headers: List[Header])
object HttpRequest {
  def parse(reader: BufferedReader) = {
    val lines = Stream.continually(reader.readLine()).takeWhile(line => line.length() > 0).toList
    lines.head.split(' ') match {
      case Array(method, path, version) => HttpRequest(method, path, lines.tail.map(line => Header.parse(line)))
    }
  }
}