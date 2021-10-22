You are faced with a 5 x 5 grid which is known to contain exactly 3 mines.

Initially all cells in the grid are marked with a `?`, meaning you do not know what they contain.

Choosing a cell will change its status to one of the following:

* `👍`  - the cell is empty and is not adjacent to any mines
* `1` - the cell is empty and is adjacent to exactly one mine
* `2` - the cell is empty and is adjacent to exactly two mines
* `3` - the cell is empty and is adjacent to exactly three mines
* `☠️` - you hit a mine! Game over.

Note that "adjacent" means there is a mine in one or more of the 8 cells surrounding the given cell (north, north-east, east, south-east, south, south-west, west, north-west).

Write a program to play Minesweeper.

The programs initial input will be a randomly generated 5 x 5 grid of `?` cells.

At every step, the program should choose a cell. It will be given an updated grid as its next input.

### Clarifying questions
* Do we need to support an n x n grid. "If you can make it more generic then you should"
* How do you interact with the grid in the game. "For the interviewer to decide!"
* How do you win. "By revealing all non mines"

### Extensions
* If a blank cell is clicked, all adjacent cells are displayed, if they are blank or contain a number. This rule is repeated for every blank cell that has just been revealed, until no more cells match the criteria.
* Add a concurrent interface for this game, the candidate would be expected to describe the needed components to achieve this from architectural design to data structures.
