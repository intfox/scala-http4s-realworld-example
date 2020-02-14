package dev.intfox.realworld.users

import cats.effect.Sync
import cats.implicits._
import dev.intfox.realworld.auth.JwtPayload
import io.circe.generic.JsonCodec
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import io.circe.generic.semiauto._
import org.http4s.server.{AuthMiddleware, Router}
import pdi.jwt.{JwtCirce, JwtAlgorithm, JwtClaim}

case class LoginRequest( email: String, password: String )
case class UserResponse(user: UserBody)
case class UserBody(email: String, token: String, username: String, bio: String, image: Option[String] )
object UserResponse {
  def apply(user: User)(token: String): UserResponse = UserResponse(UserBody(user.email, token, user.username, user.bio, None))
}

case class RegistrationRequest( username: String, email: String, password: String )


object UserRoute{
  def apply[F[_] : Sync](userRepo: UserRepo[F], authMiddleware: AuthMiddleware[F, User]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._
    implicit val loginRequestDecoder: Decoder[LoginRequest] = deriveDecoder[LoginRequest].at("user")
    implicit val loginRequestEntityDecoder = jsonOf[F, LoginRequest]

    implicit val userEncoder = deriveEncoder[UserBody]
    implicit val loginResponseEncoder = deriveEncoder[UserResponse]

    implicit val jwtPayloadEncoder = deriveEncoder[JwtPayload]

    val jwtAlgorithm = JwtAlgorithm.HS256
    val secretKey = "abcdef"

    val userResponseWithJwt = (user: User) => UserResponse(user)(JwtCirce.encode(JwtPayload(user.username).asJson, secretKey, jwtAlgorithm))
    val login = HttpRoutes.of[F]{
      case req @ POST -> Root / "login" => for {
        loginReq <- req.as[LoginRequest]
        user <- userRepo.findByEmail(loginReq.email)
        _ <- if(user.password == loginReq.password) ().pure[F] else new Throwable("Wrong password.").raiseError[F, Unit]
        resp <- Ok(userResponseWithJwt(user).asJson)
      } yield resp
    }

    implicit val registrationRequestDecoder = deriveDecoder[RegistrationRequest].at("user")
    implicit val registrationRequestEntityDecoder = jsonOf[F, RegistrationRequest]

    val register = HttpRoutes.of[F] {
      case req @ POST -> Root => for {
        registerReq <- req.as[RegistrationRequest]
        user <- userRepo.create(User(registerReq.email, registerReq.username, registerReq.password, ""))
        resp <- Ok(userResponseWithJwt(user).asJson)
      } yield resp
    }

    val user = AuthedRoutes.of[User, F]{
      case req @ PUT -> Root as user => ???
      case GET -> Root as user => Ok(userResponseWithJwt(user).asJson)
    }


    Router(
      "/user" -> authMiddleware(user),
      "/users" -> login
    )
  }
}