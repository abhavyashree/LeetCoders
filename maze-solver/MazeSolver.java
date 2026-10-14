import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

public class MazeSolver {

    static final int MAX_KEYS = 6;
    static final int TOTAL_MASKS = 1 << MAX_KEYS;

    // Symbols. The exit is 'X' so that door 'E' (key 'e') can exist and all
    // 6 key/door pairs (a-f / A-F) are usable. If you set EXIT back to 'E',
    // key 'e' and door 'E' are rejected and T is limited to 4.
    static final char START = 'S';
    static final char EXIT = 'X';

    // Chance that a cell off the route becomes a wall
    // (raise it, e.g. to 0.45, for tighter mazes where doors matter more)
    static final double WALL_PROB = 0.35;

    static char[][] grid;
    static int N;
    static int T = MAX_KEYS; // number of key types actually in use

    static int[] dr = {-1, 1, 0, 0};
    static int[] dc = {0, 0, -1, 1};

    static class State {
        int r, c, mask, dist;
        State parent;

        State(int r, int c, int mask, int dist, State parent) {
            this.r = r;
            this.c = c;
            this.mask = mask;
            this.dist = dist;
            this.parent = parent;
        }
    }

    static int[] findCell(char symbol) {
        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                if (grid[r][c] == symbol) {
                    return new int[]{r, c};
                }
            }
        }
        return null;
    }

    // A door is A-F, except the letter used as the exit symbol.
    static boolean isDoor(char ch) {
        return ch >= 'A' && ch <= 'F' && ch != EXIT;
    }

    // A key is a-f, except the key whose door letter is the exit symbol.
    static boolean isKey(char ch) {
        return ch >= 'a' && ch <= 'f' && Character.toUpperCase(ch) != EXIT;
    }

    static int collectKey(char ch, int mask) {
        if (isKey(ch)) {
            mask |= (1 << (ch - 'a'));
        }
        return mask;
    }

    // BFS over (row, col, keyMask) finds the shortest valid path
    static List<State> bfs() {
        int[] start = findCell(START);
        int[] end = findCell(EXIT);

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                "Maze must contain " + START + " and " + EXIT + ".");
        }

        boolean[][][] visited = new boolean[N][N][TOTAL_MASKS];
        Queue<State> queue = new ArrayDeque<>();

        queue.offer(new State(start[0], start[1], 0, 0, null));
        visited[start[0]][start[1]][0] = true;

        while (!queue.isEmpty()) {
            State current = queue.poll();

            if (current.r == end[0] && current.c == end[1]) {
                List<State> path = new ArrayList<>();
                for (State s = current; s != null; s = s.parent) {
                    path.add(s);
                }
                Collections.reverse(path);
                return path;
            }

            for (int i = 0; i < 4; i++) {
                int nr = current.r + dr[i];
                int nc = current.c + dc[i];

                if (nr < 0 || nr >= N || nc < 0 || nc >= N) continue;

                char ch = grid[nr][nc];

                if (ch == '#') continue;

                if (isDoor(ch) && (current.mask & (1 << (ch - 'A'))) == 0) {
                    continue;
                }

                int newMask = collectKey(ch, current.mask);

                if (visited[nr][nc][newMask]) continue;
                visited[nr][nc][newMask] = true;

                queue.offer(new State(nr, nc, newMask, current.dist + 1, current));
            }
        }

        return Collections.emptyList();
    }

    // Read a square maze from a CSV file
    static char[][] readCSV(String filename) throws IOException {
        List<char[]> rows = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(Path.of(filename))) {
            String line;
            boolean first = true;

            while ((line = br.readLine()) != null) {
                if (first) {
                    // strip a possible UTF-8 BOM
                    if (!line.isEmpty() && line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                    }
                    first = false;
                }

                if (line.trim().isEmpty()) continue;

                String[] cells = line.split(",", -1);
                char[] row = new char[cells.length];

                for (int i = 0; i < cells.length; i++) {
                    String cell = cells[i].trim();
                    if (cell.length() != 1) {
                        throw new IllegalArgumentException(
                            "Each CSV cell must contain exactly one character."
                        );
                    }
                    row[i] = cell.charAt(0);
                }
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }

        int size = rows.size();
        for (char[] row : rows) {
            if (row.length != size) {
                throw new IllegalArgumentException(
                    "Maze must be an N x N square grid."
                );
            }
        }

        return rows.toArray(new char[0][]);
    }

    // ---------------------------------------------------------------------
    // Random maze generator
    // ---------------------------------------------------------------------

    // Random winding route from (0,0) to (n-1,n-1) using iterative randomized DFS
    static List<int[]> randomPath(int n, Random rnd) {
        boolean[][] seen = new boolean[n][n];
        List<int[]> stack = new ArrayList<>();

        stack.add(newNode(0, 0, rnd));
        seen[0][0] = true;

        while (!stack.isEmpty()) {
            int[] top = stack.get(stack.size() - 1);

            if (top[0] == n - 1 && top[1] == n - 1) break;

            if (top[2] == 4) {               // all directions tried: backtrack
                stack.remove(stack.size() - 1);
                continue;
            }

            int d = top[3 + top[2]++];
            int nr = top[0] + dr[d];
            int nc = top[1] + dc[d];

            if (nr < 0 || nr >= n || nc < 0 || nc >= n || seen[nr][nc]) continue;

            seen[nr][nc] = true;
            stack.add(newNode(nr, nc, rnd));
        }

        return stack;   // the stack now holds the route from S to the exit
    }

    // Node = {row, col, nextDirectionIndex, dir0, dir1, dir2, dir3}
    static int[] newNode(int r, int c, Random rnd) {
        List<Integer> toward = new ArrayList<>();
        List<Integer> other = new ArrayList<>();
        for (int d = 0; d < 4; d++) {
            if (d == 1 || d == 3) toward.add(d);   // down / right
            else other.add(d);
        }
        Collections.shuffle(toward, rnd);
        Collections.shuffle(other, rnd);

        // half the time try "toward the exit" first, otherwise fully random
        List<Integer> order = new ArrayList<>();
        order.addAll(toward);
        order.addAll(other);
        if (!rnd.nextBoolean()) {
            Collections.shuffle(order, rnd);
        }

        return new int[]{r, c, 0,
            order.get(0), order.get(1), order.get(2), order.get(3)};
    }

    // Generate a random solvable maze: keys and doors are scattered along a
    // random route, and each key always comes before its own door on that route.
    static char[][] generateMaze(int size, int keyTypes) {
        if (keyTypes < 1 || keyTypes > MAX_KEYS) {
            throw new IllegalArgumentException(
                "T must be between 1 and " + MAX_KEYS + ".");
        }
        if (size < keyTypes + 2) {
            throw new IllegalArgumentException(
                "Choose N >= T + 2 (for T = " + keyTypes + ", N >= "
                + (keyTypes + 2) + ").");
        }

        Random rnd = new Random();
        List<int[]> path = randomPath(size, rnd);

        // random walls everywhere, then carve the route open
        char[][] maze = new char[size][size];
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                maze[r][c] = rnd.nextDouble() < WALL_PROB ? '#' : '.';
            }
        }
        for (int[] p : path) {
            maze[p[0]][p[1]] = '.';
        }

        // pick 2T distinct positions on the route (not S or the exit)
        List<Integer> idx = new ArrayList<>();
        for (int i = 1; i < path.size() - 1; i++) idx.add(i);
        Collections.shuffle(idx, rnd);

        List<Integer> chosen = new ArrayList<>(idx.subList(0, 2 * keyTypes));
        Collections.sort(chosen);

        // walk along the route: place a key, or a door for a key already placed
        List<Integer> toPlace = new ArrayList<>();
        for (int k = 0; k < keyTypes; k++) toPlace.add(k);
        Collections.shuffle(toPlace, rnd);

        List<Integer> pending = new ArrayList<>();   // keys placed, door not yet

        for (int i = 0; i < chosen.size(); i++) {
            int remaining = chosen.size() - i;
            boolean placeDoor;

            if (toPlace.isEmpty()) placeDoor = true;
            else if (pending.size() == remaining) placeDoor = true;
            else if (pending.isEmpty()) placeDoor = false;
            else placeDoor = rnd.nextBoolean();

            int[] cell = path.get(chosen.get(i));

            if (placeDoor) {
                int k = pending.remove(rnd.nextInt(pending.size()));
                maze[cell[0]][cell[1]] = (char) ('A' + k);
            } else {
                int k = toPlace.remove(toPlace.size() - 1);
                maze[cell[0]][cell[1]] = (char) ('a' + k);
                pending.add(k);
            }
        }

        maze[0][0] = START;
        maze[size - 1][size - 1] = EXIT;

        return maze;
    }

    // Export the shortest path to CSV
    static void writeSolutionCSV(String filename, List<State> path)
            throws IOException {

        char[][] output = new char[N][];
        for (int r = 0; r < N; r++) {
            output[r] = grid[r].clone();
        }

        for (State state : path) {
            if (output[state.r][state.c] == '.') {
                output[state.r][state.c] = 'x';
            }
        }

        writeGridCSV(filename, output);
    }

    // Validate symbols, key/door range and the start/exit cells
    static void validateMaze() {
        int starts = 0;
        int exits = 0;

        for (int r = 0; r < N; r++) {
            for (int c = 0; c < N; c++) {
                char ch = grid[r][c];

                if (ch == START) {
                    starts++;
                } else if (ch == EXIT) {
                    exits++;
                } else if (ch == '.' || ch == '#') {
                    // valid
                } else if (isKey(ch) || isDoor(ch)) {
                    int index = isKey(ch) ? ch - 'a' : ch - 'A';
                    if (index >= T) {
                        throw new IllegalArgumentException(
                            "Symbol '" + ch + "' exceeds T = " + T + " key types."
                        );
                    }
                } else if ((ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
                    throw new IllegalArgumentException(
                        "Symbol '" + ch + "' conflicts with the exit symbol '"
                        + EXIT + "'."
                    );
                } else {
                    throw new IllegalArgumentException(
                        "Invalid grid symbol: " + ch
                    );
                }
            }
        }

        if (starts != 1 || exits != 1) {
            throw new IllegalArgumentException(
                "Maze must contain exactly one " + START
                + " and one " + EXIT + "."
            );
        }
    }

    static void printGrid(char[][] maze) {
        for (char[] row : maze) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < row.length; c++) {
                if (c > 0) sb.append(',');
                sb.append(row[c]);
            }
            System.out.println(sb);
        }
    }

    // =====================================================================
    // Swing visualization with key pick-up and door-opening animations
    // =====================================================================
    static class MazePanel extends JPanel {

        // ---- layout ----
        static final int PANEL_HEIGHT = 90;  // inventory area under the grid
        static final int SLOT = 44;          // inventory box size (px)
        static final int SLOT_GAP = 12;
        static final int SLOT_X0 = 10;

        // ---- animation timings (ms) ----
        static final int START_DELAY_MS = 700;
        static final int KEY_FLIGHT_MS = 700; // key flies into the inventory
        static final int DOOR_MS = 600;       // door fades to white + unlocks
        static final int PULSE_MS = 500;      // inventory box pulse

        static final Color TRAIL = new Color(190, 235, 190);
        static final Color START_COLOR = new Color(46, 160, 67);
        static final Color EXIT_COLOR = new Color(229, 57, 53);

        final List<State> path;
        final char[][] original;

        // per-cell animation state
        final boolean[][] visited;
        final boolean[][] keyTaken;
        final long[][] keyStart;   // when the key started flying (-1 = never)
        final long[][] doorStart;  // when the door started opening (-1 = never)
        final long[] slotLand;     // when each key type landed in the inventory
        final int[] slotIndex = new int[MAX_KEYS]; // position of each key's box
        int keysPicked = 0;                        // different keys collected so far

        final int stepMs;
        int step = 0;
        long stepStartTime;        // when the current move begins
        long finishTime = -1;
        final Timer timer;

        final Color[] pairColors = {
            new Color(255, 193, 7),
            new Color(156, 100, 200),
            new Color(0, 188, 212),
            new Color(255, 112, 67),
            new Color(102, 187, 106),
            new Color(236, 64, 122)
        };

        MazePanel(char[][] maze, List<State> path) {
            this.original = maze;
            this.path = path;

            visited = new boolean[N][N];
            keyTaken = new boolean[N][N];
            keyStart = new long[N][N];
            doorStart = new long[N][N];
            for (int r = 0; r < N; r++) {
                Arrays.fill(keyStart[r], -1L);
                Arrays.fill(doorStart[r], -1L);
            }
            slotLand = new long[MAX_KEYS];
            Arrays.fill(slotLand, -1L);

            // keep very long paths from taking forever (max ~20 s of walking)
            stepMs = Math.max(120, Math.min(500, 20000 / Math.max(1, path.size())));

            setPreferredSize(new Dimension(650, 650 + PANEL_HEIGHT));
            setBackground(Color.WHITE);

            onArrive(0, System.currentTimeMillis() + START_DELAY_MS);

            timer = new Timer(16, e -> tick());
            timer.start();
        }

        // Called every ~16 ms: advance the walk when the current move is done.
        private void tick() {
            long now = System.currentTimeMillis();
            int last = path.size() - 1;

            if (step < last) {
                if (now - stepStartTime >= stepMs) {
                    step++;
                    onArrive(step, now);
                    if (step == last) finishTime = now;
                }
            } else {
                if (finishTime < 0) finishTime = now;
                // let the last animations finish, then stop the timer
                if (now - finishTime > KEY_FLIGHT_MS + DOOR_MS + PULSE_MS) {
                    timer.stop();
                }
            }
            repaint();
        }

        // Runs once when the player arrives at path cell i.
        private void onArrive(int i, long now) {
            State s = path.get(i);
            visited[s.r][s.c] = true;
            char ch = original[s.r][s.c];

            long hold = 0; // how long the player waits before the next move

            // Key pick-up: the key leaves the cell and flies to the inventory
            if (isKey(ch) && !keyTaken[s.r][s.c]) {
                int k = ch - 'a';
                keyTaken[s.r][s.c] = true;
                keyStart[s.r][s.c] = now;
                if (slotLand[k] < 0) {
                    slotLand[k] = now + KEY_FLIGHT_MS;
                    slotIndex[k] = keysPicked++;   // next free box
                }
                hold = KEY_FLIGHT_MS;
            }

            // Door opening: if the next cell is a closed door, unlock it first
            // (after any key has landed), then let the player walk in.
            if (i < path.size() - 1) {
                State nx = path.get(i + 1);
                char nch = original[nx.r][nx.c];
                if (isDoor(nch) && doorStart[nx.r][nx.c] < 0) {
                    doorStart[nx.r][nx.c] = now + hold;
                    hold = hold + DOOR_MS;
                }
            }

            stepStartTime = now + hold;
        }

        // ---------------------------------------------------------------
        // Painting
        // ---------------------------------------------------------------
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            long now = System.currentTimeMillis();
            int gridPx = Math.min(getWidth(), getHeight() - PANEL_HEIGHT);
            int cell = Math.max(1, gridPx / N);

            drawCells(g2, cell, now);
            drawInventory(g2, cell, now);
            drawFlyingKeys(g2, cell, now);
            drawPlayer(g2, cell, now);

            g2.dispose();
        }

        private void drawCells(Graphics2D g2, int cell, long now) {
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(6, cell / 2)));

            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    char ch = original[r][c];
                    int x = c * cell;
                    int y = r * cell;

                    Color fill = Color.WHITE;
                    Color labelColor = Color.BLACK;
                    String label = "";
                    double doorP = 0;

                    if (ch == '#') {
                        fill = Color.BLACK;
                    } else if (ch == START) {
                        fill = START_COLOR;
                        label = String.valueOf(START);
                        labelColor = Color.WHITE;
                    } else if (ch == EXIT) {
                        fill = EXIT_COLOR;
                        label = String.valueOf(EXIT);
                        labelColor = Color.WHITE;
                    } else if (isKey(ch)) {
                        if (keyTaken[r][c]) {
                            fill = TRAIL;            // key is gone from the cell
                        } else {
                            fill = pairColors[ch - 'a'];
                            label = String.valueOf(ch);
                        }
                    } else if (isDoor(ch)) {
                        doorP = doorProgress(r, c, now);
                        fill = blend(pairColors[ch - 'A'], Color.WHITE, ease(doorP));
                        label = String.valueOf(ch);
                    } else if (visited[r][c]) {
                        fill = TRAIL;
                    }

                    g2.setColor(fill);
                    g2.fillRect(x, y, cell, cell);
                    g2.setColor(Color.GRAY);
                    g2.drawRect(x, y, cell, cell);

                    double cx = x + cell / 2.0;
                    double cy = y + cell / 2.0;

                    if (isDoor(ch)) {
                        // letter fades out while the unlock mark fades in
                        double letterAlpha = 1.0 - Math.min(1.0, doorP * 3);
                        if (letterAlpha > 0) {
                            g2.setColor(new Color(0, 0, 0, (int) (letterAlpha * 255)));
                            drawCentered(g2, label, cx, cy);
                        }
                        if (doorP > 0) {
                            drawLock(g2, cx, cy, cell * 0.62,
                                     ease(doorP), Math.min(1.0, doorP * 3));
                        }
                    } else if (!label.isEmpty()) {
                        g2.setColor(labelColor);
                        drawCentered(g2, label, cx, cy);
                    }
                }
            }
        }

        // Inventory panel: a box appears only for keys that were really collected.
        private void drawInventory(Graphics2D g2, int cell, long now) {
            int panelY = N * cell + 8;
            int last = path.size() - 1;

            // names of keys already landed in the inventory
            StringBuilder held = new StringBuilder();
            for (int k = 0; k < T; k++) {
                if (slotLand[k] >= 0 && now >= slotLand[k]) {
                    if (held.length() > 0) held.append(", ");
                    held.append((char) ('a' + k));
                }
            }

            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.setColor(Color.BLACK);
            g2.drawString("Key inventory: "
                    + (held.length() == 0 ? "(empty)" : held.toString()),
                    SLOT_X0, panelY + 16);

            String status = (step == last)
                    ? "Exit reached!  Distance: " + last
                    : "Step " + step + " / " + last;
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(step == last ? new Color(30, 130, 60) : Color.DARK_GRAY);
            g2.drawString(status, getWidth() - fm.stringWidth(status) - 10, panelY + 16);

            int y0 = panelY + 26;

            for (int k = 0; k < T; k++) {
                // no box at all until the key has landed
                if (slotLand[k] < 0 || now < slotLand[k]) continue;

                int x = SLOT_X0 + slotIndex[k] * (SLOT + SLOT_GAP);

                long since = now - slotLand[k];
                boolean pulsing = since < PULSE_MS;
                double prog = pulsing ? since / (double) PULSE_MS : 1.0;
                double grow = pulsing ? SLOT * 0.14 * Math.sin(Math.PI * prog) : 0;

                Color base = pairColors[k];
                RoundRectangle2D box = new RoundRectangle2D.Double(
                        x - grow, y0 - grow, SLOT + 2 * grow, SLOT + 2 * grow, 10, 10);

                g2.setColor(base);
                g2.fill(box);
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(base.darker());
                g2.draw(box);
                g2.setStroke(new BasicStroke(1f));

                g2.setFont(new Font("SansSerif", Font.BOLD, 20));
                g2.setColor(Color.BLACK);
                drawCentered(g2, String.valueOf((char) ('a' + k)),
                        x + SLOT / 2.0, y0 + SLOT / 2.0);

                // expanding ring when the key lands
                if (pulsing) {
                    double expand = SLOT * 0.45 * prog;
                    g2.setColor(new Color(base.getRed(), base.getGreen(),
                            base.getBlue(), (int) (200 * (1 - prog))));
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(new RoundRectangle2D.Double(
                            x - expand, y0 - expand,
                            SLOT + 2 * expand, SLOT + 2 * expand, 14, 14));
                    g2.setStroke(new BasicStroke(1f));
                }
            }
        }

        // Keys currently flying from their cell to their inventory box.
        private void drawFlyingKeys(Graphics2D g2, int cell, long now) {
            int panelY = N * cell + 8;

            for (int r = 0; r < N; r++) {
                for (int c = 0; c < N; c++) {
                    long s = keyStart[r][c];
                    if (s < 0) continue;

                    long elapsed = now - s;
                    if (elapsed < 0 || elapsed >= KEY_FLIGHT_MS) continue;

                    double t = elapsed / (double) KEY_FLIGHT_MS;
                    double e = ease(t);
                    int k = original[r][c] - 'a';

                    double sx = (c + 0.5) * cell;
                    double sy = (r + 0.5) * cell;
                    double tx = SLOT_X0 + slotIndex[k] * (SLOT + SLOT_GAP) + SLOT / 2.0;
                    double ty = panelY + 26 + SLOT / 2.0;

                    // straight line to the box plus a small upward arc
                    double x = sx + (tx - sx) * e;
                    double y = sy + (ty - sy) * e - Math.sin(Math.PI * t) * cell * 0.8;
                    double size = cell * 0.7 * (1 - e) + SLOT * 0.6 * e;

                    Color base = pairColors[k];
                    RoundRectangle2D box = new RoundRectangle2D.Double(
                            x - size / 2, y - size / 2, size, size, 8, 8);
                    g2.setColor(base);
                    g2.fill(box);
                    g2.setColor(base.darker());
                    g2.setStroke(new BasicStroke(2f));
                    g2.draw(box);
                    g2.setStroke(new BasicStroke(1f));

                    g2.setFont(new Font("SansSerif", Font.BOLD,
                            Math.max(8, (int) (size * 0.55))));
                    g2.setColor(Color.BLACK);
                    drawCentered(g2, String.valueOf((char) ('a' + k)), x, y);
                }
            }
        }

        // The player glides smoothly from one cell to the next.
        private void drawPlayer(Graphics2D g2, int cell, long now) {
            State cur = path.get(step);
            double px = cur.c;
            double py = cur.r;

            if (step < path.size() - 1) {
                double t = (now - stepStartTime) / (double) stepMs;
                t = ease(Math.max(0, Math.min(1, t)));
                State nx = path.get(step + 1);
                px += (nx.c - cur.c) * t;
                py += (nx.r - cur.r) * t;
            }

            double size = Math.max(3, cell / 2.0);
            double cx = (px + 0.5) * cell;
            double cy = (py + 0.5) * cell;

            g2.setColor(Color.WHITE);
            g2.fillOval((int) (cx - size / 2 - 2), (int) (cy - size / 2 - 2),
                    (int) size + 4, (int) size + 4);
            g2.setColor(Color.BLUE);
            g2.fillOval((int) (cx - size / 2), (int) (cy - size / 2),
                    (int) size, (int) size);
        }

        // ---------------------------------------------------------------
        // Helpers
        // ---------------------------------------------------------------
        private double doorProgress(int r, int c, long now) {
            long s = doorStart[r][c];
            if (s < 0 || now < s) return 0;
            return Math.min(1.0, (now - s) / (double) DOOR_MS);
        }

        private static double ease(double t) {
            return t * t * (3 - 2 * t);
        }

        private static Color blend(Color a, Color b, double t) {
            int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }

        private static void drawCentered(Graphics2D g2, String s, double cx, double cy) {
            FontMetrics fm = g2.getFontMetrics();
            int x = (int) (cx - fm.stringWidth(s) / 2.0);
            int y = (int) (cy - fm.getHeight() / 2.0 + fm.getAscent());
            g2.drawString(s, x, y);
        }

        // Padlock drawn with shapes (no font needed).
        // open = 0 -> shackle closed, open = 1 -> shackle lifted and open.
        private static void drawLock(Graphics2D g2, double cx, double cy,
                                     double size, double open, double alpha) {
            int a = (int) Math.max(0, Math.min(255, alpha * 255));
            Color ink = new Color(50, 50, 50, a);

            double bw = size * 0.56;
            double bh = size * 0.40;
            double bx = cx - bw / 2;
            double by = cy - bh / 2 + size * 0.14;

            double sw = bw * 0.64;          // shackle width
            double sx = cx - sw / 2;
            double leg = sw * 0.30;         // straight part of the shackle
            double lift = open * size * 0.16;
            double arcTop = by - leg - sw / 2 - lift;
            double midY = arcTop + sw / 2;  // where the arc meets the legs

            g2.setColor(ink);
            g2.setStroke(new BasicStroke((float) Math.max(1.5, size * 0.09),
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.draw(new Arc2D.Double(sx, arcTop, sw, sw, 0, 180, Arc2D.OPEN));
            // right leg always stays in the body
            g2.draw(new Line2D.Double(sx + sw, midY, sx + sw, by));
            // left leg leaves the body as the lock opens
            double leftEnd = by - open * size * 0.14;
            g2.draw(new Line2D.Double(sx, midY, sx, leftEnd));

            g2.setStroke(new BasicStroke(1f));

            // lock body and keyhole
            g2.setColor(ink);
            g2.fill(new RoundRectangle2D.Double(bx, by, bw, bh, size * 0.1, size * 0.1));
            g2.setColor(new Color(255, 255, 255, a));
            g2.fillOval((int) (cx - size * 0.04), (int) (by + bh * 0.35),
                    (int) Math.max(2, size * 0.08), (int) Math.max(2, size * 0.08));
        }
    }

    static void showAnimation(char[][] maze, List<State> path) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Maze with Keys and Doors");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MazePanel(maze, path));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    static void writeGridCSV(String filename, char[][] maze)
            throws IOException {

        try (BufferedWriter bw = Files.newBufferedWriter(Path.of(filename))) {
            for (char[] row : maze) {
                for (int c = 0; c < row.length; c++) {
                    if (c > 0) bw.write(",");
                    bw.write(row[c]);
                }
                bw.newLine();
            }
        }
    }

    static int readInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        String line = sc.nextLine().trim();
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected an integer, got: " + line);
        }
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        try {
            System.out.println("=== Maze with Keys and Doors ===");
            System.out.println("Symbols: " + START + " = start, " + EXIT
                    + " = exit, # = wall, . = empty, a-f = keys, A-F = doors");
            System.out.println("1. Read maze from CSV");
            System.out.println("2. Generate random maze");

            int option = readInt(sc, "Choose option: ");

            if (option == 1) {
                T = readInt(sc, "Enter number of key types T: ");
                if (T < 1 || T > MAX_KEYS) {
                    throw new IllegalArgumentException("T must be between 1 and 6.");
                }

                System.out.print("Enter CSV filename: ");
                String filename = sc.nextLine().trim();

                grid = readCSV(filename);
                N = grid.length;

            } else if (option == 2) {
                int size = readInt(sc, "Enter grid size N: ");
                T = readInt(sc, "Enter number of key types T (1-6): ");

                grid = generateMaze(size, T);
                N = grid.length;

                writeGridCSV("generated_maze.csv", grid);
                System.out.println("Generated maze saved to generated_maze.csv");

            } else {
                throw new IllegalArgumentException("Choose option 1 or 2.");
            }

            validateMaze();

            System.out.println("\nOriginal maze:");
            printGrid(grid);

            List<State> path = bfs();

            if (path.isEmpty()) {
                System.out.println("\nShortest path: -1");
                return;
            }

            int distance = path.get(path.size() - 1).dist;
            System.out.println("\nShortest path length: " + distance);

            writeSolutionCSV("solution.csv", path);
            System.out.println("Solution saved to solution.csv");

            System.out.println("\nPath-marked maze:");
            printGrid(readCSV("solution.csv"));

            showAnimation(grid, path);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            sc.close();
        }
    }
}