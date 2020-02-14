package dev.intfox.realworld.http

import cats.MonadError
import cats.data.{Kleisli, OptionT}
import cats.implicits._
import io.circe.Encoder
import io.circe.generic.semiauto._
import io.circe.syntax._

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.syntax._
import org.http4s.circe._

case class ErrorResponse(errors: BodyErrorResponse)
case class BodyErrorResponse(body: Seq[String])

object ErrorResponse {
  def apply(str: String): ErrorResponse = ErrorResponse(BodyErrorResponse(Seq(str)))
}

object HandleErrorMiddleware {
  implicit val bodyErrorResponseEncoder: Encoder[BodyErrorResponse] = deriveEncoder[BodyErrorResponse]
  implicit val errorResponseEncoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]

  def apply[F[_] : MonadError[*[_], Throwable]](httpRoutes: HttpRoutes[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    Kleisli { req =>
      OptionT(httpRoutes.run(req).value.handleErrorWith( err => Ok(ErrorResponse(err.getMessage).asJson).map(Option(_))))
    }
  }
}
