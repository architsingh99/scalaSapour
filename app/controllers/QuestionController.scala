package controllers

import javax.inject.{Inject, Singleton}
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents}
import play.api.Logger
import services.{QuestionAnswer, QuestionManager}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class QuestionController @Inject()(cc: MessagesControllerComponents, questionManager: QuestionManager, auth: UserAuthentication)(implicit ec: ExecutionContext) extends MessagesAbstractController(cc) {
  import auth._

  private[this] val logger: Logger = Logger(this.getClass)

  def getAllCategories(): Action[AnyContent] = HasToken() { _ =>
    info =>
      cachedUser =>
        implicit request =>
          questionManager.getAllCategories.map { case result =>
            Ok(Json.obj("categories" -> result))
          }.recover {
            case e: Exception =>
              logger.error("[QuestionController] Error in getAllCategories  " + e.getMessage + e.printStackTrace())
              BadRequest(Json.obj("err" -> "Something went wrong."))
          }
  }

  def getQuestionsByCategory(categoryId: Int): Action[AnyContent] = HasToken() { _ =>
    info =>
      cachedUser =>
        implicit request =>
          questionManager.getQuestionsByCategoryId(categoryId).map { result =>
            Ok(Json.obj("data" -> result))
          }.recover {
            case e: Exception =>
              logger.error("[QuestionController] Error in getQuestionsByCategory  " + e.getMessage + e.printStackTrace())
              BadRequest(Json.obj("err" -> "Something went wrong."))
          }
  }

  implicit val questionAnswerReads: Reads[QuestionAnswer] = Json.reads[QuestionAnswer]

  def saveQuestionAnswers(): Action[AnyContent] = HasToken() { _ =>
    info =>
      cachedUser =>
        implicit request =>
          request.body.asJson.map { json =>
            (json \ "questionAnswers").asOpt[List[QuestionAnswer]].map { questionAnswer =>

              val result: Future[(Boolean, String)] = questionManager.saveQuestionAnswer(cachedUser.userId, questionAnswer)

              result.map { case (status, msg) =>
                if (status) Ok(Json.obj("status_message" -> msg)) else BadRequest(Json.obj("err" -> msg))
              }.recover {
                case e: Exception =>
                  logger.error("[QuestionController] Error in saveQuestionAnswers  " + e.getMessage + e.printStackTrace())
                  BadRequest(Json.obj("err" -> "Something went wrong."))
              }
            }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Answer Details not found."))))
          }.getOrElse(Future.successful(BadRequest(Json.obj("err" -> "Json Body not found."))))
  }

}
