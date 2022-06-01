package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import play.api.Logger
import services.{UserManager, UserRegistrationInfo}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UserController @Inject()(cc: MessagesControllerComponents, userManager: UserManager)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {


  protected val logger: Logger = Logger(this.getClass)

  def signUp(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.map { json =>
      (json \ "email").asOpt[String].map { email =>
        (json \ "password").asOpt[String].map { password =>
          (json \ "name").asOpt[String].map { name =>

            val device: String = request.headers.get("device").orElse(request.getQueryString("device")).getOrElse("web")
            val userType: Int = (json \ "userType").asOpt[Int].getOrElse(1)
            val phone: Option[String] = (json \ "phone").asOpt[String]

            val registrationInfo: UserRegistrationInfo = UserRegistrationInfo(name = name, email = email, userType = userType, password, mobileNo = phone, device = device)
            userManager.registerUser(registrationInfo).map { case (status, message) =>
              if (status) Ok(Json.obj("status_message" -> message)) else BadRequest(Json.obj("err" -> message))
            }.recover {
              case e: Exception => BadRequest(Json.obj("err" -> "Something went wrong."))
            }

          }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Please enter name."))))
        }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Please enter Password."))))
      }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Please enter Phone No."))))
    }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Json Body not found."))))
  }

  def login(): Action[AnyContent] = Action.async { implicit request =>
    request.body.asJson.map { json =>
      (json \ "email").asOpt[String].map { email =>
        (json \ "password").asOpt[String].map { password =>

          val userType: Int = (json \ "userType").asOpt[Int].getOrElse(1)
          userManager.loginUser(email, password, userType).map { case (status, response) =>
            if (status) Ok(response) else BadRequest(response)
          }.recover {
            case e: Exception => BadRequest(Json.obj("err" -> "Something went wrong."))
          }

        }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Please enter Password."))))
      }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Please enter Phone No."))))
    }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Json Body not found."))))
  }

}
