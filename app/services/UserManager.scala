package services

import helper.UserUtils

import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{BadRequest, NotFound}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


case class UserRegistrationInfo(name: String, email: String, userType: Int, password: String, mobileNo: Option[String], device: String)

class UserManager @Inject()(db: DatabaseConfigProvider, memcached: SyncCacheApi)(implicit ec: ExecutionContext) {

  private[this] val users = new Users(db)
  private[this] val logger: Logger = Logger(this.getClass)
  private[this] val cache: SyncCacheApi = memcached

  def registerUser(info: UserRegistrationInfo): Future[(Boolean, String)] = {
    try {
      users.findByEmail(info.email, info.userType).flatMap { userOpt =>
        userOpt.map { _ =>
          Future.successful(false, "Error, You are already registered with us. Please login.")
        }.getOrElse {
          val user: User = User(None, info.name, info.mobileNo, info.email, UserStatus.ACTIVE, info.userType, DateTime.now, DateTime.now, DateTime.now)
          users.createUser(user).map { userId =>
            val userAuth: UserAuth = createAuthData(userId, info.email, info.password)
            users.createUserAuth(userAuth)
            logger.info(s"[UserService.registerUser] Registered user with Phone number => ${info.email}")
            (true, "Registration Successfull")
          }
        }
      }
    } catch {
      case e: Exception =>
        logger.error(s"[UserService.registerUser] Failed to register user with email ${info.email}, " + e.getMessage + e.printStackTrace())
        Future.successful(false, "Something Went Wrong!!")
    }
  }

  def loginUser(email: String, password: String, userType: Int): Future[(Boolean, JsValue)] = {
    try {
      users.findByEmail(email, userType).flatMap { maybeUser =>
        maybeUser.map { user =>
          user.status match {
            case UserStatus.ACTIVE =>
              users.findUserAuthDataByUserId(user.id).map { userAuthOpt =>
                userAuthOpt.map { userAuth =>
                  if (userAuth.encryptedPassword == UserUtils.secureHash(password, userAuth.salt)) {
                    val newToken: String = java.util.UUID.randomUUID().toString
                    val webUa = userAuth.copy(rememberToken = newToken, rememberTokenExpiresAt = DateTime.now.plusYears(1))
                    users.updateUserAuthData(webUa)

                      cache.remove(s"login_attempts_$email")
                      (true, Json.obj(
                        "name" -> user.name,
                        "email" -> user.email,
                        "userType" -> UserType.getUserTypeById(user.userType),
                        "token" -> webUa.rememberToken
                      ))

                  } else if (user.status != 3) {
                    val loginAttemptsCacheKey: String = s"login_attempts_$email"

                    val alreadyAttemptCount: Int = cache.getOrElseUpdate(loginAttemptsCacheKey, 900.seconds)(0)
                    val currAttemptCount: Int = alreadyAttemptCount + 1

                    if (currAttemptCount == 5) {
                      logger.info(s"[Login] - [$email] >>> Updating [users] for user status.")
                      users.updateUser(user.copy(status = 3))
                      cache.remove(loginAttemptsCacheKey)
                      logger.info(s"[Login] - [$email] >>> Account locked [attempt-count] : $currAttemptCount.")
                      (false, Json.obj("err" -> "Error, Your account is locked due to multiple failures in giving the correct password. Please contact support."))
                    } else {
                      cache.set(loginAttemptsCacheKey, currAttemptCount)
                      if (currAttemptCount > 2) {
                        logger.info(s"[Login] - [$email] >>> Invalid password [attempt-count] : $currAttemptCount.")
                        (false, Json.obj("err" -> s"Error, Invalid Password. ${5 - currAttemptCount} attempts left. After this your account will be blocked."))
                      } else {
                        logger.info(s"[Login] - [$email] >>> Invalid password.")
                        (false, Json.obj("err" -> "Error, You have entered invalid email or password."))
                      }
                    }
                  } else {
                    logger.info(s"[Login] - [$email] >>> Account Locked.")
                    (false, Json.obj("err" -> "Error, Your account is locked due to multiple failures in giving the correct password. Please contact support."))
                  }
                }.getOrElse {
                  (false, Json.obj("err" -> "Something went wrong, Please try again later"))
                }
              }
            case UserStatus.DEACTIVATED =>
              Future.successful(false, Json.obj("err" -> "Sorry your account is no longer active!"))
            case UserStatus.LOCKED =>
              Future.successful(false, Json.obj("err" -> "Sorry your account currently locked!"))
            case _ =>
              Future.successful(false, Json.obj("err" -> "Something went wrong, Please try again later"))
          }
        }.getOrElse {
          Future.successful(false, Json.obj("err" -> "User Details Not Found"))
        }
      }
    } catch {
      case e: Exception =>
        logger.error("Failed to fetch user info -> " + e.getMessage)
        Future.successful(false, Json.obj("err" -> "Something went wrong, Please try again later"))
    }
  }

  private[this] def createAuthData(userId: Int, email: String, password: String): UserAuth = {
    val token = java.util.UUID.randomUUID().toString
    val (secureCode, salt, encryptedPassword): (String, String, String) = getSecureCodeSaltAndEncryptedPassword(email, password)
    UserAuth(None, userId, encryptedPassword, salt, token, DateTime.now.plusDays(365), secureCode, DateTime.now.plusYears(3), token, DateTime.now.plusDays(365))
  }

  private def getSecureCodeSaltAndEncryptedPassword(mobile: String, password: String): (String, String, String) = {
    val secureCode = UserUtils.randomAlphaNumericString(19)
    val salt = UserUtils.md5(mobile)
    val encryptedPassword = UserUtils.secureHash(password, salt)
    (secureCode, salt, encryptedPassword)
  }

  def updateUserAuthData(userAuth: UserAuth): Future[Int] = {
    users.updateUserAuthData(userAuth)
  }

  def findUserAndUserAuthByToken(token: String): Future[Option[(User, UserAuth)]] = {
    users.findUserAndUserAuthByToken(token)
  }

  def saveUserLoginHistory(userLoginHistory: UserLoginHistory): Future[Long] = {
    users.createUserLoginHistory(userLoginHistory)
  }
}