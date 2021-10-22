package minesweeper

import cats.effect.IOApp

object Main extends IOApp.Simple {
  val run = UI(x = 10, y = 10, mines = 10).mainLoop // beginner level
}
