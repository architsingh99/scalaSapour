package helper

import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object FutureHelper {
  val logger: Logger = Logger(this.getClass)

  def optionFutureToFutureOption[A](x: Option[Future[A]]): Future[Option[A]] =
    x match {
      case Some(f) => f.map(Some(_))
      case None => Future.successful(None)
    }

  def optionFutureOptionToFutureOption[A](x: Option[Future[Option[A]]]): Future[Option[A]] =
    x match {
      case Some(f) => f
      case None => Future.successful(None)
    }

  def listFutureToFutureList[A](x: List[Future[A]]): Future[List[A]] = {
    val futureListOfTry = Future.sequence(x.map(_.transform(Success(_))))
    futureListOfTry.foreach(_.collect { case Failure(e) => logger.debug("conversion of List[Future[A]] to Future[List[A] fail " + e) })
    futureListOfTry.map(_.collect { case Success(value) => value })
  }

  def listFutureOptionToFutureList[A](x: List[Future[Option[A]]]): Future[List[A]] = {
    val futureListOfTry = Future.sequence(x.map(_.transform(Success(_))))
    futureListOfTry.foreach(_.collect { case Failure(e) => logger.debug("conversion of List[Future[Option[A]]] to Future[List[A] fail " + e) })
    futureListOfTry.map(_.collect { case Success(value) => value }).map(_.flatten)
  }


  def VectorFutureToFutureVector[A](x: Vector[Future[A]]): Future[Vector[A]] = {
    val futureVectorOfTry = Future.sequence(x.map(_.transform(Success(_))))
    futureVectorOfTry.foreach(_.collect { case Failure(e) => logger.debug("conversion of Vector[Future[A]] to Future[Vector[A] fail " + e) })
    futureVectorOfTry.map(_.collect { case Success(value) => value })
  }

  def seqFutureToFutureSeq[A](x: Seq[Future[A]]): Future[Seq[A]] = {
    val futureSeqOfTry = Future.sequence(x.map(_.transform(Success(_))))
    futureSeqOfTry.foreach(_.collect { case Failure(e) => logger.debug("conversion of Seq[Future[A]] to Future[Seq[A] fail " + e) })
    futureSeqOfTry.map(_.collect { case Success(value) => value })
  }


  def mapFutureToFutureMap[A,B](x : Map[Future[A], B]) : Future[Map[A, B]] = {
    try {
      Future.sequence(x.map(entry => entry._1.map(i => (i, entry._2)))).map(_.toMap)
    }catch {
      case exception: Exception => logger.error(s" FutureHelper : mapFutureToFutureMap -> ${exception.getMessage} ----> ${exception.printStackTrace()}")
        Future(Map())
    }
  }

  def listOfOptionsToOptionOfList[A](a:List[Option[A]]):Option[List[A]] =
    a.foldLeft(Option(List[A]())){ (os, oe) =>
      for {
        s <- os
        e <- oe
      } yield e :: s
    }.map{ _.reverse }

}

