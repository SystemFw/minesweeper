package minesweeper

import cats.syntax.all._
import cats.effect.IO
import scala.util.Random

object Engine {
  type Point = Space.Point
  val Point = Space.Point

  sealed trait Command
  case object Flag extends Command
  case object Uncover extends Command

  sealed trait Outcome
  case object Win extends Outcome
  case object Loss extends Outcome

  sealed trait Type
  case object Mine extends Type
  case object Blank extends Type
  case class Neighbour(n: Int) extends Type

  sealed trait State
  case object Uncovered extends State
  case object Flagged extends State
  case object Covered extends State

  case class Cell(`type`: Type, state: State) {
    def isBlank = `type` == Blank
    def isMine = `type` == Mine
    def isNeighbour = `type` != Blank && `type` != Mine

    def flag = Cell(`type`, Flagged)
    def cover = Cell(`type`, Covered)
    def uncover = Cell(`type`, Uncovered)
  }
  object Cell {
    def mine = Cell(Mine, Covered)
    def neighbour(n: Int) = Cell(Neighbour(n), Covered)
    def blank = Cell(Blank, Covered)
  }

  case class Grid(x: Int, y: Int, mines: Int, grid: Space[Cell]) {
    def update(f: Space[Cell] => Space[Cell]) = Grid(
      x,
      y,
      mines,
      grid |+| f(grid)
    )

    def isComplete: Boolean = {
      def uncovered = grid.filter(_.state == Uncovered).size
      (x * y) - uncovered == mines
    }
  }
  object Grid {
    def grid(x: Int, y: Int): List[Point] = for {
      x <- List.range(0, x)
      y <- List.range(0, y)
    } yield Point(x, y)

    def build(x: Int, y: Int, mines: List[Point]): Grid = {
      val mineField = mines.foldMap(Space.point(_, Cell.mine))

      val otherCells = {
        def status(point: Point) = Space
          .neighbours(point)
          .toList
          .foldMap { mineField.at(_).as(1) }
          .fold(Cell.blank)(Cell.neighbour)

        grid(x, y).foldMap(point => Space.point(point, status(point)))
      }

      Grid(x, y, mines.size, otherCells |+| mineField)
    }

    def random(x: Int, y: Int, mines: Int): IO[Grid] = IO {
      build(x, y, Random.shuffle(grid(x, y)).take(mines))
    }
  }

  def gameTurn(
      point: Point,
      command: Command,
      grid: Grid
  ): (Grid, Option[Outcome]) = {
    val input = (command, grid.grid.at(point).map(_.`type`))

    val newGrid = grid.update { grid =>
      input match {
        case (Flag, _) =>
          grid.zoom(point).map { cell =>
            cell.state match {
              case Uncovered => cell
              case Covered => cell.flag
              case Flagged => cell.cover
            }
          }
        case (Uncover, Some(Blank)) =>
          val (blankRegion, edgePoints) =
            Space.region(point, grid.at(_).filter(_.isBlank))
          val edge = edgePoints.toList.foldMap(grid.zoom).filter(_.isNeighbour)
          (blankRegion |+| edge).map(_.uncover)
        case (Uncover, Some(Mine)) =>
          grid.filter(_.isMine).map(_.uncover)
        case (Uncover, _) =>
          grid.zoom(point).map(_.uncover)
      }
    }

    val outcome = input match {
      case (Uncover, Some(Mine)) => Loss.some
      case _ if newGrid.isComplete => Win.some
      case _ => None
    }

    newGrid -> outcome
  }
}
