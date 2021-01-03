package de.lolhens.cadvisor.limiter

import cats.effect.concurrent.MVar
import cats.effect.{ExitCode, Resource}
import fs2.{Chunk, Stream}
import monix.eval.{Task, TaskApp}
import org.http4s._
import org.http4s.client.Client
import org.http4s.client.jdkhttpclient.JdkHttpClient
import org.http4s.dsl.task._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder

import java.net.http.HttpClient
import scala.concurrent.duration._

object Server extends TaskApp {
  private val connectTimeout: FiniteDuration = 5.minutes

  private val targetUri = Uri.unsafeFromString("http://localhost:8081")

  override def run(args: List[String]): Task[ExitCode] = Task.deferAction { scheduler =>
    BlazeServerBuilder[Task](scheduler)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(service.orNotFound)
      .resource
      .use(_ => Task.never)
  }

  lazy val clientResource: Resource[Task, Client[Task]] =
    Resource.liftF(Task(JdkHttpClient[Task](
      HttpClient.newBuilder()
        .sslParameters {
          val ssl = javax.net.ssl.SSLContext.getDefault
          val params = ssl.getDefaultSSLParameters
          params.setProtocols(Array("TLSv1.2"))
          params
        }
        .connectTimeout(java.time.Duration.ofMillis(connectTimeout.toMillis))
        .build()
    )).memoizeOnSuccess)

  private lazy val activeRequestVar = MVar[Task].of[Option[Response[Task]]](None).memoizeOnSuccess

  lazy val service: HttpRoutes[Task] = HttpRoutes.of {
    case request@GET -> Root / "metrics" =>
      for {
        activeRequest <- activeRequestVar
        response <- activeRequest.tryTake.bracket[Response[Task]] {
          case Some(_) =>
            for {
              response <- clientResource.flatMap(_.run(Request[Task](Method.GET, targetUri.withPath(request.uri.path))))
                .use(response =>
                  for {
                    chunk <- response.body.compile.to(Chunk)
                  } yield
                    response.withBodyStream(Stream.chunk(chunk))
                )
              _ <- activeRequest.tryPut(Some(response))
            } yield
              response

          case None =>
            for {
              responseOption <- activeRequest.read
              response <- responseOption.map(Task.now).getOrElse(InternalServerError())
            } yield
              response
        } {
          case Some(_) =>
            activeRequest.tryPut(None).map(_ => ())

          case None =>
            Task.unit
        }
      } yield
        response

    case request =>
      (for {
        client <- clientResource
        response <- client.run(request.withUri(targetUri.withPath(request.uri.path)))
      } yield
        response)
        .allocated.map(_._1)
  }
}
