package models

import javax.inject.{Inject, Singleton}
import org.joda.time.DateTime
import play.api.db.slick.DatabaseConfigProvider
import slick.ast.BaseTypedType
import slick.jdbc.{JdbcProfile, JdbcType}

import scala.concurrent.{ExecutionContext, Future}

case class Category(id: Option[Int], title: String, status: Int, createdAt: DateTime, updatedAt: DateTime)
case class Question(id: Option[Int], categoryId: Int, question: String, image: Option[String], status: Int, createdAt: DateTime, updatedAt: DateTime)
case class QuestionOption(id: Option[Int], questionId: Int, option: String, isCorrectOption: Boolean, status: Int, createdAt: DateTime, updatedAt: DateTime)
case class UserQuestionAnswer(id: Option[Int], questionId: Int, userId: Int, selectedOption: Int, isCorrect: Boolean, paperId: String, createdAt: DateTime, updatedAt: DateTime, isAttempted: Boolean)

@Singleton
class Questions @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
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
  private class QuestionCategoriesTable(tag: Tag) extends Table[Category](tag, "question_categories") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def status = column[Int]("status")
    def createdAt = column[DateTime]("created_at", O.SqlType("DATETIME"))
    def updatedAt = column[DateTime]("updated_at", O.SqlType("DATETIME"))

    def * = {
      val shaped = (id.?, title, status, createdAt, updatedAt).shaped
      shaped.<>((Category.apply _).tupled, Category.unapply)
    }
  }

  private class QuestionsTable(tag: Tag) extends Table[Question](tag, "questions") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def categoryId = column[Int]("category_id")
    def question = column[String]("question")
    def image = column[Option[String]]("image")
    def status = column[Int]("status")
    def createdAt = column[DateTime]("created_at", O.SqlType("DATETIME"))
    def updatedAt = column[DateTime]("updated_at", O.SqlType("DATETIME"))

    def * = {
      val shaped = (id.?, categoryId, question, image, status, createdAt, updatedAt).shaped
      shaped.<>((Question.apply _).tupled, Question.unapply)
    }
  }

  private class QuestionOptionsTable(tag: Tag) extends Table[QuestionOption](tag, "question_options") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def questionId = column[Int]("question_id")
    def option = column[String]("option")
    def isCorrectOption = column[Boolean]("is_correct_option")
    def status = column[Int]("status")
    def createdAt = column[DateTime]("created_at", O.SqlType("DATETIME"))
    def updatedAt = column[DateTime]("updated_at", O.SqlType("DATETIME"))

    def * = {
      val shaped = (id.?, questionId, option, isCorrectOption, status, createdAt, updatedAt).shaped
      shaped.<>((QuestionOption.apply _).tupled, QuestionOption.unapply)
    }
  }

  private class UserQuestionAnswerTable(tag: Tag) extends Table[UserQuestionAnswer](tag, "user_question_answers") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def questionId = column[Int]("question_id")
    def userId = column[Int]("user_id")
    def selectedOption = column[Int]("selected_option")
    def isCorrect = column[Boolean]("is_correct")
    def paperId = column[String]("paper_id")
    def createdAt = column[DateTime]("created_at", O.SqlType("DATETIME"))
    def updatedAt = column[DateTime]("updated_at", O.SqlType("DATETIME"))
    def isAttempted = column[Boolean]("is_attempted")

    def * = {
      val shaped = (id.?, questionId, userId, selectedOption, isCorrect, paperId, createdAt, updatedAt, isAttempted).shaped
      shaped.<>((UserQuestionAnswer.apply _).tupled, UserQuestionAnswer.unapply)
    }
  }

  private val categories = TableQuery[QuestionCategoriesTable]
  private val questions = TableQuery[QuestionsTable]
  private val options = TableQuery[QuestionOptionsTable]
  private val userQuestionAnswers = TableQuery[UserQuestionAnswerTable]

  /** Queries **/
  def findCategoryById(categorId: Int): Future[Option[Category]] = db.run {
    categories.filter(_.id === categorId).result.headOption
  }

  def findAllCategories(): Future[Seq[Category]] = db.run {
    categories.result
  }

  def createCategory(category: Category): Future[Int] = db.run {
    (categories returning categories.map(_.id)) += category
  }

  def updateCategory(category: Category): Future[Int] = db.run {
    categories.insertOrUpdate(category)
  }

  def findQuestionById(questionId: Int): Future[Option[Question]] = db.run {
    questions.filter(_.id === questionId).result.headOption
  }

  def findRandomQuestionByCategoryId(categoryId: Int): Future[Seq[Question]] = db.run {
    val rand = SimpleFunction.nullary[Double]("rand")
    questions.filter(_.categoryId === categoryId).sortBy(x => rand).take(10).result
  }

  def createQuestion(question: Question): Future[Int] = db.run {
    (questions returning questions.map(_.id)) += question
  }

  def updateQuestion(question: Question): Future[Int] = db.run {
    questions.insertOrUpdate(question)
  }

  def findOptionsById(optionId: Int): Future[Option[QuestionOption]] = db.run {
    options.filter(_.id === optionId).result.headOption
  }

  def findOptionsByQuestionId(questionId: Int): Future[Seq[QuestionOption]] = db.run {
    options.filter(_.questionId === questionId).result
  }

  def createOption(option: QuestionOption): Future[Int] = db.run {
    (options returning options.map(_.id)) += option
  }

  def updateOption(option: QuestionOption): Future[Int] = db.run {
    options.insertOrUpdate(option)
  }

  def findUserQuestionById(userQuestionAnswerId: Int): Future[Option[UserQuestionAnswer]] = db.run {
    userQuestionAnswers.filter(_.id === userQuestionAnswerId).result.headOption
  }

  def findUserQuestionsByUserId(userId: Int): Future[Seq[UserQuestionAnswer]] = db.run {
    userQuestionAnswers.filter(_.userId === userId).result
  }

  def createUserQuestionsByUser(userQuestionAnswer: UserQuestionAnswer): Future[Int] = db.run {
    (userQuestionAnswers returning userQuestionAnswers.map(_.id)) += userQuestionAnswer
  }

  def updateUserQuestionsByUser(userQuestionAnswer: UserQuestionAnswer): Future[Int] = db.run {
    userQuestionAnswers.insertOrUpdate(userQuestionAnswer)
  }

}
