package minesweeper

import cats.syntax.all._
import cats.effect.IO

import Engine._

case class UI(x: Int, y: Int, mines: Int) {
  def initial = Grid.random(x, y, mines)

  def mainLoop: IO[Unit] = initial.flatMap { grid =>
    def put[A](s: A): IO[Unit] = IO(print(s))

    def inputPrompt: IO[Input] = for {
      _ <- IO.println("\n\nEnter command:  ")
      s <- IO.readLine
      r <- Input.parse(s).fold(inputPrompt)(_.pure[IO])
    } yield r

    def continuePrompt: IO[Unit] = for {
      _ <- IO.println(
        "\n\n Enter 'q' to exit or any other key to play a new game: "
      )
      s <- IO.readLine
      r <- if (s.trim.toLowerCase == "q") IO.unit else mainLoop
    } yield r

    def inputLoop(grid: Grid): IO[Unit] =
      inputPrompt.flatMap {
        case Quit => IO.unit
        case Reset => mainLoop
        case Action(point, command) =>
          gameTurn(point, command, grid) match {
            case (newGrid, None) =>
              IO.println(show(newGrid)) >> inputLoop(newGrid)
            case (newGrid, outcome @ Some(_)) =>
              IO.println(show(newGrid, outcome)) >> continuePrompt
          }
      }

    IO.println(show(grid)) >> inputLoop(grid)
  }

  def show(grid: Grid, outcome: Option[Outcome] = None): String = {
    val xs = List.range(0, grid.x)
    val ys = List.range(0, grid.y).reverse

    def commands = s"""
      # ------------------------------------------------------------------------------
      #| 4 5 - Select cell at x = 4, y = 5 | f 4 5 - Flag/unflag cell at x = 4, y = 5 |
      #| r - Reset board and play          | q - Quit                                 |
      # ------------------------------------------------------------------------------
     """.stripMargin('#')

    def showCell(cell: Cell): Char = cell.state match {
      case Covered => '+'
      case Flagged => '!'
      case Uncovered =>
        cell.`type` match {
          case Mine => 'X'
          case Blank => ' '
          case Neighbour(n) => (n + '0').toChar
        }
    }

    def header = "\nY\n^"
    def footer = s"""  ${xs.mkString(" ")} > X"""

    def newGrid = ys.map { y =>
      val row =
        xs.map(x => showCell(grid.grid.at(Point(x, y)).get)).mkString(" ")
      s"$y $row"
    }

    def finalOutcome = outcome.foldMap {
      case Win => "\n\nGAME OVER: YOU WON!"
      case Loss => "\n\nGAME OVER: YOU LOST!"
    }

    commands ++ (header +: newGrid :+ footer).mkString("\n") ++ finalOutcome
  }

  sealed trait Input
  case object Reset extends Input
  case object Quit extends Input
  case class Action(point: Point, command: Command) extends Input
  object Input {
    def parse(s: String): Option[Input] = Either.catchNonFatal {
      s.trim.split("""\s+""").toList match {
        case List(x, y) => Action(Point(x.toInt, y.toInt), Uncover)
        case List("f", x, y) => Action(Point(x.toInt, y.toInt), Flag)
        case List("r") => Reset
        case List("q") => Quit
      }
    }.toOption
  }

}
