package services

import helper.FutureHelper

import javax.inject.Inject
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.api.cache.SyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{JsNull, JsValue, Json}

import scala.concurrent.{ExecutionContext, Future}

case class QuestionAnswer(questionId: Int, answerId: Int, isAttempted: Boolean)

class QuestionManager @Inject()(db: DatabaseConfigProvider, memcached: SyncCacheApi)(implicit ec: ExecutionContext) {

  private[this] val questions = new Questions(db)
  private[this] val logger: Logger = Logger(this.getClass)

  def getAllCategories: Future[List[JsValue]] = {
    try {
      questions.findAllCategories().map { categorySeq =>
        categorySeq.toList.map { category =>
          Json.obj(
            "category" -> category.title,
            "id" -> category.id
          )
        }
      }
    } catch {
      case e: Exception =>
        logger.error(s"[QuestionManager] Failed to getAllCategories, " + e.getMessage + e.printStackTrace())
        Future.successful(Nil)
    }
  }

  def getQuestionsByCategoryId(categoryId: Int): Future[JsValue] = {
    try {
      questions.findCategoryById(categoryId).flatMap { categoryOpt =>
        categoryOpt.map { cat =>
          questions.findRandomQuestionByCategoryId(categoryId).flatMap { questionSeq =>
            val jsonList = questionSeq.toList.map { question =>
              questions.findOptionsByQuestionId(question.id.getOrElse(0)).map { optionSeq =>
                val optionJson = optionSeq.map { option =>
                  Json.obj(
                    "option" -> option.option,
                    "optionId" -> option.id,
                    "isCorrect" -> option.isCorrectOption
                  )
                }
                Json.obj(
                  "question" -> question.question,
                  "image" -> question.image,
                  "questionId" -> question.id,
                  "options" -> optionJson
                )
              }
            }
            FutureHelper.listFutureToFutureList(jsonList).map { result =>
              Json.obj(
                "category" -> cat.title,
                "questions" -> result
              )
            }
          }
        }.getOrElse(Future.successful(JsNull))
      }

    } catch {
      case e: Exception =>
        logger.error(s"[QuestionManager] Failed to getQuestionsByCategoryId, " + e.getMessage + e.printStackTrace())
        Future.successful(JsNull)
    }
  }

  def saveQuestionAnswer(userId: Int, questionAnswers: List[QuestionAnswer]): Future[(Boolean, String)] = {
    try {
      val paperId = userId.toString + "_" + DateTime.now().getMillis.toString
      questionAnswers.map { questionAnswer =>
        questions.findOptionsByQuestionId(questionAnswer.questionId).map { options =>
          val isCorrect = if(options.toList.exists(r => r.isCorrectOption && r.id.getOrElse(0) == questionAnswer.answerId)) true else false
          val userQuestionAnswer = UserQuestionAnswer(None, questionAnswer.questionId, userId, questionAnswer.answerId, isCorrect, paperId, DateTime.now(), DateTime.now(), questionAnswer.isAttempted)
          println(s"userQuestionAnswer :: $userQuestionAnswer")
          questions.createUserQuestionsByUser(userQuestionAnswer)
        }
      }
      Future.successful(true, "You have successfully completed the test")
    } catch {
      case e: Exception =>
        logger.error(s"[QuestionManager] Failed to saveQuestionAnswer, " + e.getMessage + e.printStackTrace())
        Future.successful(false, "Something Went Wrong!")
    }
  }

}
