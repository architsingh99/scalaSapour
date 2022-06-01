package models

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}
import slick.lifted.ProvenShape

import scala.concurrent.{ExecutionContext, Future}

object UserType {
  val USER: Int = 1
  val ADMIN: Int = 2

  def getUserTypeById(id: Int): String = {
    id match {
      case 1 => "USER"
      case 2 => "ADMIN"
      case _ => "OTHERS"
    }
  }
}

  object UserStatus {
    val ACTIVE: Int = 1
    val DEACTIVATED: Int = 2
    val LOCKED: Int = 3

    def getUserStatusById(id: Int): String = {
      id match {
        case 1 => "ACTIVE"
        case 2 => "DEACTIVATED"
        case 3 => "LOCKED"
        case _ => "OTHERS"
      }
    }
  }

case class User(id: Option[Int], name: String, mobile: Option[String], email: String, status: Int, userType: Int, createdAt: DateTime, updatedAt: DateTime, lastLoginAt: DateTime)
case class UserAuth(id: Option[Int], userId: Int, encryptedPassword: String, salt: String, rememberToken: String, rememberTokenExpiresAt: DateTime, secureCode: String, secureCodeExpiresAt: DateTime, appRememberToken: String, appRememberTokenExpiresAt: DateTime)
case class UserLoginHistory(id: Option[Long], userId: Int, loggedOn: DateTime, device: String)
case class CachedUser(userId: Int, mobile: Option[String], name: String, email: String, token: String, appToken: String, userType: Int)
case class RoleUser(id: Option[Int], userId: Int, roleId: Int)

@Singleton
class Users @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  implicit def dateTimeMapper: JdbcType[DateTime] with BaseTypedType[DateTime] = MappedColumnType.base[org.joda.time.DateTime, String](
    tmap = { dateTimeObject: DateTime => dateTimeObject.toString("YYYY-MM-dd HH:mm:ss") },
    tcomap = { dateTimeDb: String =>
      val dateTimeFormatter = org.joda.time.format.DateTimeFormat.forPattern("YYYY-MM-dd HH:mm:ss")
      DateTime.parse(dateTimeDb, dateTimeFormatter)
    }
  )

  /** Table Schema **/
  private class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def mobile = column[Option[String]]("mobile")
    def email = column[String]("email")
    def status = column[Int]("status")
    def userType = column[Int]("user_type")
    def createdAt = column[DateTime]("created_at", O.SqlType("DATETIME"))
    def updatedAt = column[DateTime]("updated_at", O.SqlType("DATETIME"))
    def lastLoginAt = column[DateTime]("last_login_at", O.SqlType("DATETIME"))

    def * = {
      val shaped = (id.?, name, mobile, email, status, userType, createdAt, updatedAt, lastLoginAt).shaped
      shaped.<>((User.apply _).tupled, User.unapply)
    }
  }

  private class UserAuthTable(tag: Tag) extends Table[UserAuth](tag, "user_auth_data") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def encryptedPassword = column[String]("encrypted_password")
    def salt = column[String]("salt")
    def rememberToken = column[String]("remember_token")
    def rememberTokenExpiresAt = column[DateTime]("remember_token_expires_at", O.SqlType("DATETIME"))
    def secureCode = column[String]("secure_code")
    def secureCodeExpiresAt = column[DateTime]("secure_code_expires_at", O.SqlType("DATETIME"))
    def appRememberToken = column[String]("app_remember_token")
    def appRememberTokenExpiresAt = column[DateTime]("app_remember_token_expires_at", O.SqlType("DATETIME"))

    def * : ProvenShape[UserAuth] = {
      val shaped = (id.?, userId, encryptedPassword, salt, rememberToken, rememberTokenExpiresAt, secureCode, secureCodeExpiresAt, appRememberToken, appRememberTokenExpiresAt).shaped
      shaped.<>((UserAuth.apply _).tupled, UserAuth.unapply)
    }
  }

  private class UserLoginHistoryTable(tag: Tag) extends Table[UserLoginHistory](tag, "user_login_history") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def loggedOn = column[DateTime]("logged_on", O.SqlType("DATETIME"))
    def device = column[String]("device")

    def * : ProvenShape[UserLoginHistory] = {
      val shaped = (id.?, userId, loggedOn, device).shaped
      shaped.<>((UserLoginHistory.apply _).tupled, UserLoginHistory.unapply)
    }
  }

  private val users = TableQuery[UserTable]
  private val userAuths = TableQuery[UserAuthTable]
  private val userLoginHistories = TableQuery[UserLoginHistoryTable]

  /** Queries **/
  def findById(userId: Int): Future[Option[User]] = db.run {
    users.filter(_.id === userId).result.headOption
  }

  def findAll(userType: Int = UserType.USER): Future[Seq[User]] = db.run {
    users.filter(_.userType === userType).result
  }

  def findByEmail(email: String, userType: Int = UserType.USER): Future[Option[User]] = db.run {
    users.filter(_.email === email).filter(_.userType === userType).result.headOption
  }

  def createUser(user: User): Future[Int] = db.run {
    (users returning users.map(_.id)) += user
  }

  def updateUser(user: User): Future[Int] = db.run {
    users.insertOrUpdate(user)
  }

  def createUserAuth(userAuth: UserAuth): Future[Int] = db.run {
    (userAuths returning userAuths.map(_.id)) += userAuth
  }

  def updateUserAuthData(userAuth: UserAuth): Future[Int] = db.run {
    userAuths.insertOrUpdate(userAuth)
  }

  def createUserLoginHistory(userLoginHistory: UserLoginHistory): Future[Long] = db.run {
    (userLoginHistories returning userLoginHistories.map(_.id)) += userLoginHistory
  }

  def findUserAndUserAuthById(userId: Option[Int]): Future[Option[(User, UserAuth)]] = db.run {
    val q = for {
      u <- users if u.status === UserStatus.ACTIVE && u.id === userId
      ua <- userAuths if ua.userId === u.id
    } yield {
      (u, ua)
    }
    q.result.headOption
  }

  def findUserAuthDataByUserId(userId: Option[Int]): Future[Option[UserAuth]] = db.run {
    userAuths.filter(_.userId === userId).result.headOption
  }

  def findUserAndUserAuthByToken(token: String): Future[Option[(User, UserAuth)]] = db.run {
    val today = DateTime.now()
    val q = for {
      u <- users if u.status === UserStatus.ACTIVE
      ua <- userAuths if ua.userId === u.id && ((ua.rememberToken === token && ua.rememberTokenExpiresAt > today) || (ua.appRememberToken === token && ua.appRememberTokenExpiresAt > today))
    } yield {
      (u, ua)
    }
    val result = q.result
    result.headOption
  }

  def findByTokenAndDevice(token: String, device: String): Future[Option[UserAuth]] = db.run {
    val today = DateTime.now()
    userAuths.filter { ua =>
      device match {
        case "ios" | "android" => ua.appRememberToken === token && ua.appRememberTokenExpiresAt > today
        case _ => ua.rememberToken === token && ua.rememberTokenExpiresAt > today
      }
    }.result.headOption
  }

  def findUserAndUserAuthByPhoneNo(phoneNo: String, userType: Int): Future[Option[(User, UserAuth)]] = db.run {
    val q = for {
      u <- users if u.mobile === phoneNo && u.userType === userType
      ua <- userAuths if ua.userId === u.id
    } yield {
      (u, ua)
    }
    q.result.headOption
  }

  def findUserAndUserAuthByEmail(email: Option[String], userType: Int): Future[Option[(User, UserAuth)]] = db.run {
    val q = for {
      u <- users if u.email === email && u.userType === userType
      ua <- userAuths if ua.userId === u.id
    } yield {
      (u, ua)
    }
    q.result.headOption
  }
}