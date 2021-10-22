package minesweeper

import cats._, syntax.all._
import scala.annotation.tailrec
import Space.Point

case class Space[A](v: Map[Point, A]) {
  def at(p: Point): Option[A] = v.get(p)
  def zoom(p: Point): Space[A] = Space(at(p).tupleLeft(p).toMap)
  def map[B](f: A => B) = Space(v.fmap(f))
  def filter(f: A => Boolean): Space[A] = Space(v.filter(f))
  def merge(that: Space[A]): Space[A] = Space(v ++ that.v)
  def points: Set[Point] = v.keys.toSet
  def size: Int = v.size
}
object Space {
  case class Point(x: Int, y: Int) {
    def add(that: Point) = Point(x + that.x, y + that.y)
  }

  def point[A](p: Point, a: A): Space[A] = Space(Map(p -> a))
  def empty[A]: Space[A] = Space(Map.empty)
  def origin: Point = Point(0, 0)

  def neighbours(p: Point): Set[Point] = {
    for {
      x <- List(-1, 0, 1)
      y <- List(-1, 0, 1)
      if (x, y) != (0, 0)
    } yield p |+| Point(x, y)
  }.toSet

  /** Searches `p`, then its neighbourhood, then the neighbourhood of
    * the neighbourhood and so on, stopping in any direction when
    * `get` returns `None`, and continuing in the others to build a
    * region.
    *
    * Returns a `Space` representing the region and a `Set[Point]`
    * representing the edge of points surrounding but not belonging
    * to it.
    */
  def region[A](p: Point, get: Point => Option[A]): (Space[A], Set[Point]) = {
    def include(p: Point): (Space[A], Set[Point]) = get(p) match {
      case Some(v) => Space.point(p, v) -> Set(p)
      case None => Space.empty[A] -> Set.empty[Point]
    }

    @tailrec
    def loop(
        values: Space[A],
        seen: Set[Point],
        iteration: Set[Point]
    ): (Space[A], Set[Point]) =
      if (iteration.isEmpty) {
        val edge = values.points.flatMap(neighbours) -- seen
        values -> edge
      } else {
        val seenNow = seen |+| iteration
        val candidates = iteration.flatMap(neighbours) -- seenNow
        val (newValues, newIteration) = candidates.toList.foldMap(include)
        loop(values |+| newValues, seenNow, newIteration)
      }

    val (initialSpace, iteration0) = include(p)
    loop(initialSpace, Set.empty, iteration0)
  }

  implicit def spaceMonoid[A]: Monoid[Space[A]] =
    Monoid.instance(Space.empty, _ merge _)

  implicit val pointMonoid: Monoid[Point] =
    Monoid.instance(origin, _ add _)
}
