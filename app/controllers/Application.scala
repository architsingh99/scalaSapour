package controllers

import models.{User, UserAuth}
import play.api.mvc._
import play.api.cache._
import play.api.libs.json._
import play.api.Logger
import models._
import org.joda.time.DateTime
import services.UserManager

import scala.concurrent.duration._
import scala.concurrent._
import ExecutionContext.Implicits.global

trait Security {
  self: MessagesAbstractController =>

  protected val logger: Logger = Logger(this.getClass)
  lazy val CacheExpiration: Duration = 43200.seconds

  val AuthTokenHeaderKey = "X-NP-TOKEN"
  val AuthTokenCookieKey = "XNP-TOKEN"
  val AuthTokenUrlKey = "auth"
  val deviceKey = "device"
  val appVersionKey = "appVersion"
  val sourceKey = "source"

  def cache: SyncCacheApi
  def userManager: UserManager
  def context: ExecutionContext

  case class Info(device: String, app: String, version: Option[String])

  implicit class ResultWithToken(result: Result) {
    def withToken(cachedUser: CachedUser): Result = {
      cache.set("authtoken-" + cachedUser.token, cachedUser, CacheExpiration)
      result.withCookies(Cookie(AuthTokenCookieKey, cachedUser.token, None, httpOnly = true, secure = true))
    }

    def discardingToken(token: String): Result = {
      cache.remove("authtoken-" + token)
      result.discardingCookies(DiscardingCookie(name = AuthTokenCookieKey, secure = true))
    }
  }

  def HasToken[A](p: BodyParser[A] = parse.anyContent)(f: String => Info => CachedUser => Request[A] => Future[Result]): Action[A] = Action.async(p) { implicit request =>
    val maybeToken: Option[String] = request.headers.get(AuthTokenHeaderKey).orElse(request.getQueryString(AuthTokenUrlKey))
    val device: Option[String] = request.headers.get(deviceKey).orElse(request.getQueryString(deviceKey))
    val version: Option[String] = request.headers.get(appVersionKey).orElse(request.getQueryString(appVersionKey))
    val source: String = request.headers.get(sourceKey).orElse(request.getQueryString(sourceKey)).getOrElse("user")
    val authFailed: play.api.mvc.Result = Unauthorized(Json.obj("err" -> "Authentication failed, Please sign out and sign in again."))

    maybeToken.flatMap { token =>
      getOrSetCachedUserFromToken(token, device, source, version).map { cachedUser =>
        f(token)(Info(device.getOrElse("web"), source, version))(cachedUser)(request)
      }
    } getOrElse { Future { authFailed } }
  }

  private def setCachedUser(cachedUser: CachedUser, token: String): CachedUser = {
    cache.set("authtoken-" + token, cachedUser, expiration = CacheExpiration)
    cachedUser
  }

  private def getOrSetCachedUserFromToken(token: String, device: Option[String], appName: String, version: Option[String])(implicit request: RequestHeader): Option[CachedUser] = {
    val optCachedUser: Option[CachedUser] = cache.get("authtoken-" + token).asInstanceOf[Option[CachedUser]]

    if (optCachedUser.isDefined) optCachedUser.map { cachedUser => cachedUser }
    else {
      Await.result(userManager.findUserAndUserAuthByToken(token) map { maybeUserOpt =>
        maybeUserOpt.map { maybeUser =>
          setCachedUser(getUserInfo(maybeUser, token, device, appName, version), token)
        }
      }, 20.seconds)
    }
  }

  private def getUserInfo(maybeUser: (User, UserAuth), token: String, device: Option[String], appName: String, appVersion: Option[String])(implicit request: RequestHeader): CachedUser = {
    val (user, userAuth) = maybeUser
    val userId: Int = user.id.get

    val userLoginHistory: UserLoginHistory = UserLoginHistory(None, userId, DateTime.now(), device.getOrElse("web"))

    Future{
      userManager.updateUserAuthData(userAuth.copy(rememberTokenExpiresAt = DateTime.now.plusYears(1), appRememberTokenExpiresAt = DateTime.now.plusYears(1)))
      userManager.saveUserLoginHistory(userLoginHistory)
    }

    CachedUser(userId, user.mobile, user.name, user.email, token, userAuth.appRememberToken, user.userType)
  }

  def invalidateUserCache(webToken: String, appToken: String): Unit = {
    cache.remove("authtoken-" + webToken)
    cache.remove("authtoken-" + appToken)
  }
}