Maze with Keys and Doors — Shortest Path Solver

A Java-based maze-solving application that uses Breadth-First Search (BFS) to find the shortest valid path from the start ("S") to the exit ("E") in an (N \times N) grid containing walls, keys, and locked doors. The project also provides CSV input/output, random maze generation, and a Java Swing visualization with key pickup and door-unlocking animations.

Features

- Shortest Path using BFS: Finds the shortest valid path while considering the keys collected.
- Keys and Doors: Keys ("a"–"f") are collected automatically. A door ("A"–"F") can be entered only after collecting its matching key.
- CSV Input: Loads a maze from a CSV file.
- Random Maze Generation: Generates a maze containing a guaranteed solvable route.
- CSV Output: Saves the shortest path in "solution.csv", marking ordinary path cells with "x" while preserving the original key and door symbols.
- Graphical Visualization: Displays the maze using Java Swing.
- Animation: Highlights the path, animates key pickups, updates the key inventory, and shows doors unlocking.
- Input Validation: Checks maze dimensions, symbols, and the presence of exactly one start and one exit.

Technologies Used

- Language: Java
- Algorithm: Breadth-First Search (BFS)
- Data Structures: Queue, arrays, and bitmask for key tracking
- GUI: Java Swing and AWT
- File Handling: Java I/O and NIO for CSV files

Project Structure

DSA HACKATHON/
├── MazeSolver.java
├── solution.csv
├── generated_maze.csv   (created when using option 2)
└── README.md

- "MazeSolver.java" — Contains the BFS algorithm, CSV handling, maze generation, validation, and visualization.
- "maze.csv" — Sample maze input.
- "solution.csv" — Generated output containing the shortest path.
- "generated_maze.csv" — Randomly generated maze, created when option 2 is selected.
- "README.md" — Project documentation.

Problem Statement

Given an (N \times N) grid, find the length of the shortest path from "S" to "E".

Each cell can contain:

Symbol| Meaning
"S"| Starting position
"E"| Exit
"."| Open cell
"#"| Wall
"a"–"f"| Keys
"A"–"F"| Doors requiring matching keys

Movement is allowed only up, down, left, or right, one cell per step. Keys are collected automatically and are never consumed. A door can be entered only if its matching key has already been collected.

The program outputs the shortest path length or "-1" if no valid path exists.

Algorithm

The project uses Breadth-First Search (BFS) over an expanded state space.

A state consists of:

(row, column, keyMask, distance)

- "row", "column" — Current position in the maze.
- "keyMask" — Bitmask representing the collected keys.
- "distance" — Number of steps taken from the start.

How It Works

1. Read the maze from a CSV file or generate a random maze.
2. Validate the grid and identify the start and exit.
3. Initialize the BFS queue with the starting position.
4. Explore the four neighboring cells.
5. Skip cells outside the grid, walls, and doors whose matching keys have not been collected.
6. When entering a key cell, update the key bitmask.
7. Track visited states using the position and key bitmask to avoid redundant exploration.
8. Stop when the exit is reached.
9. Reconstruct the shortest path using parent pointers.
10. Print the distance, save the path to "solution.csv", and display the animated visualization.

Why BFS?

BFS explores states in increasing order of distance. Since every valid move has the same cost of one step, BFS guarantees the shortest valid path.

Input Format

The program first asks for the number of key types, "T", where:

1 ≤ T ≤ 6

Then choose one of the following options:

Option 1 — Read from CSV

Provide the filename of an existing maze, such as "maze.csv".

Option 2 — Generate a Random Maze

Provide the grid size "N" and the number of key types "T". The program generates a maze with a guaranteed route and saves it as "generated_maze.csv".

«Note: In the current implementation, random maze generation supports "T" from 1 to 4 because the door symbol "E" conflicts with the exit symbol "E". The generated grid must also satisfy "N ≥ 2T + 2".»

CSV Input Example

Create a file named "maze.csv":

S,.,#,.
.,a,A,.
#,#,.,E
.,.,.,.

Each CSV cell must contain exactly one character, and the grid must be square.

For this example, the shortest valid path length is:

5

The path is:

S → . → a → A → . → E

The distance is the number of moves, not the number of cells in the path.

How to Run

Prerequisites

- Java Development Kit (JDK) installed.
- A terminal or an IDE such as Visual Studio Code.
- Java configured in your system's PATH.

Verify the installation:

java -version
javac -version

Step 1: Open the Project Folder

Open the "DSA HACKATHON" folder in VS Code or navigate to it using your terminal.

Step 2: Compile the Program

javac MazeSolver.java

Step 3: Run the Program

java MazeSolver

Step 4: Select an Option

To load the sample maze, enter:

=== Maze with Keys and Doors ===
1. Read maze from CSV
2. Generate random maze
Choose option: 1
Enter number of key types T: 1
Enter CSV filename: maze.csv

Run the command from the directory containing "maze.csv", or provide the correct file path.

Output

The program displays the original maze, the shortest path length, and the path-marked maze in the terminal. It also writes the solution to "solution.csv" and opens a Swing window for visualization.

Example terminal output:

Original maze:
S,.,#,.
.,a,A,.
#,#,.,E
.,.,.,.

Shortest path length: 5
Solution saved to solution.csv

Solution CSV Example

The path is marked with "x" in ordinary open cells. The start, exit, keys, and doors retain their original symbols.

S,x,#,.
.,a,A,x
#,#,.,E
.,.,.,.

The exact marked cells depend on the shortest path found by BFS. If multiple shortest paths exist, the program may return one of them.

Visualization

The Java Swing visualization includes:

Element| Appearance
Open cell| White
Wall| Black
Start| Green
Exit| Red
Key and matching door| Corresponding pair colors
Traversed path| Light green
Collected key| Animates toward the inventory panel
Unlocked door| Transitions to white and displays an unlock animation

The visualization animates movement along the computed shortest path, key collection, and door unlocking. The key inventory updates as keys are collected.

Complexity Analysis

Let (N) be the grid dimension and (K) the number of supported key types.

There are at most (N^2 \times 2^K) possible states because each grid position can be associated with a different combination of collected keys.

- Time Complexity: (O(N^2 \times 2^K))
- Space Complexity: (O(N^2 \times 2^K))

Since (K \leq 6), the number of key combinations is bounded by (2^6 = 64).

Limitations

- The maze must be a square (N \times N) grid.
- Exactly one "S" and one "E" must be present.
- Each CSV cell must contain exactly one character.
- Key and door types must be within the configured range "T".
- The current implementation reserves "E" for the exit, so key "e" and door "E" are not supported as a matching pair.
- Random maze generation supports up to four key types in the current implementation.

Future Enhancements

- Support all six key types by using a separate exit symbol or an explicit exit coordinate.
- Add step-by-step controls, pause, resume, and restart functionality.
- Display the key bitmask and visited-state count for educational purposes.
- Add more maze-generation algorithms and difficulty levels.
- Allow users to create and edit mazes through the GUI.

Learning Outcomes

This project demonstrates the practical application of:

- Graph traversal and shortest-path algorithms.
- BFS with state tracking.
- Bitmasking for representing collected keys.
- Queue-based search and path reconstruction.
- CSV file processing and validation.
- Java Swing-based graphical visualization and animation.

Author

DSA Hackathon Project — Maze with Keys and Doors

Built using Java, BFS, and Java Swing.
