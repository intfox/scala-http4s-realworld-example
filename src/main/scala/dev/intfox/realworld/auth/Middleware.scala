package dev.intfox.realworld.auth

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.Sync
import dev.intfox.realworld.users.{User, UserRepo}
import org.http4s.Request
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import pdi.jwt.{JwtAlgorithm, JwtCirce}
import io.circe.generic.semiauto._

case class JwtPayload(username: String)

object JwtMiddleware {
  private val logger = org.log4s.getLogger
  val jwtAlgorithm = JwtAlgorithm.HS256
  val secretKey = "abcdef"

  implicit val jwtPayloadDecoder = deriveDecoder[JwtPayload]

  def apply[F[_] : Monad : Sync](userRepo: UserRepo[F]): AuthMiddleware[F, User] = {
    val authUser: Kleisli[OptionT[F, ?], Request[F], User] =
      Kleisli(request => for {
        authHead <- OptionT.fromOption(request.headers.get(Authorization))
        token <- OptionT.fromOption(authHead.value.split(" ").lift(1))
        json <- OptionT.fromOption(JwtCirce.decodeJson(token, secretKey, Seq(jwtAlgorithm)).toOption)
        payload <- OptionT.fromOption(json.as[JwtPayload].toOption)
        user <- OptionT.liftF(userRepo.findByName(payload.username))
      } yield user)

    AuthMiddleware(authUser)
  }
}
