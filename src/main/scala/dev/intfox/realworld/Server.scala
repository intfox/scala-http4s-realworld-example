import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import cats.implicits._
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.implicits._
import cats.effect.{ExitCode, IO, IOApp}
import cats.implicits._
import dev.intfox.realworld.auth.JwtMiddleware
import dev.intfox.realworld.http.HandleErrorMiddleware
import dev.intfox.realworld.users.{UserRepo, UserRepoTest, UserRoute}

object Server extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    (for {
      userRepo <- Resource.pure[IO, UserRepo[IO]](new UserRepoTest[IO])
      userRouter <- Resource.pure[IO, HttpRoutes[IO]]( UserRoute[IO](userRepo, JwtMiddleware[IO](userRepo)) )
      httpApp = HandleErrorMiddleware(Router( "/api" -> userRouter ))
      testApp = HttpRoutes.of[IO] { case GET -> Root / "api" => Ok("ok") }
      server <- BlazeServerBuilder[IO]
        .bindHttp(9000, "0.0.0.0")
        .withHttpApp( httpApp.orNotFound )
        .resource
    } yield ()).use( _ => IO.never ).as(ExitCode.Success)

}