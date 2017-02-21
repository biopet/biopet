package nl.lumc.sasc.biopet.utils.implicits

/**
  * Created by pjvanthof on 20/02/2017.
  */
object Either {
  implicit def optionEitherLeft[T](x: Option[Either[T,_]]): Option[Option[T]] = {
    x match {
      case Some(i:Either[T, _]) if i.isLeft => Some(i.left.toOption)
      case _ => None
    }
  }

  implicit def optionEitherRight[T](x: Option[Either[_,T]]): Option[Option[T]] = {
    x match {
      case Some(i:Either[_,T]) if i.isRight => Some(i.right.toOption)
      case _ => None
    }
  }

  implicit def eitherLeft[T](x: Either[T,_]): Option[T] = {
    x match {
      case Left(x) => Some(x)
      case _ => None
    }
  }

  implicit def eitherRight[T](x: Either[_,T]): Option[T] = {
    x match {
      case Right(x) => Some(x)
      case _ => None
    }
  }

  implicit def left[T](x:T): Left[T, _] = Left(x)
  implicit def right[T](x:T): Right[_, T] = Right(x)
  implicit def left[T](x:Option[T]): Option[Left[T, _]] = x.map(Left(_))
  implicit def right[T](x:Option[T]): Option[Right[_, T]] = x.map(Right(_))

}
