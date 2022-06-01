package controllers

import javax.inject.Inject
import play.api.cache.SyncCacheApi
import play.api.mvc.{MessagesAbstractController, MessagesControllerComponents}
import services.UserManager

import scala.concurrent.ExecutionContext

/**
 * Created by Kinshuk on 04/10/20.
 */

//@Singleton
class UserAuthentication @Inject()(cc: MessagesControllerComponents, memcached: SyncCacheApi, users: UserManager)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) with Security {

  override def userManager: UserManager = users
  override def context: ExecutionContext = ec
  override def cache: SyncCacheApi = memcached

}