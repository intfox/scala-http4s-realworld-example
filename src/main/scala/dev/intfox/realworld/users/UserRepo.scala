package dev.intfox.realworld.users

import cats.MonadError
import cats.implicits._

trait UserRepo[F[_]] {
  def findByEmail(email: String): F[User]

  def findByName(name: String): F[User]

  def create(user: User): F[User]
}


class UserRepoTest[F[_] : MonadError[*[_], Throwable]] extends UserRepo[F] {
  override def findByEmail(email: String): F[User] = if (email == "jake@jake.jake") User("jake@jake.jake", "jake", "jakejake", "I work at statefarm").pure[F] else UserRepo.UserNotFound().raiseError[F, User]

  override def findByName(name: String): F[User] = if (name == "jake") User("jake@jake.jake", "jake", "jakejake", "I work at statefarm").pure[F] else UserRepo.UserNotFound().raiseError[F, User]

  override def create(user: User): F[User] = ???
}

case class User(email: String, username: String, password: String, bio: String)

object UserRepo {

  sealed trait Error extends Throwable

  case class UserNotFound() extends Throwable("User not found") with Error

  case class UserAlreadyExist() extends Throwable("User already exist") with Error

}