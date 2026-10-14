import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Closest Pair of Points: Brute-Force vs Divide-and-Conquer.
 *
 * Compile: javac ClosestPair.java
 * Run:     java ClosestPair            (then type N and press Enter)
 *          echo 1000 | java ClosestPair
 * Flags:   --nogui    do not open the windows
 *          --nobench  skip the benchmark (faster start)
 */
public class ClosestPair {

    // =====================================================================
    //  Basic types
    // =====================================================================
    static final class Point {
        final double x, y;
        Point(double x, double y) { this.x = x; this.y = y; }
        @Override public String toString() { return "(" + fmt(x) + ", " + fmt(y) + ")"; }
    }

    /** Holds the best (smallest) squared distance found so far and its two points. */
    static final class Result {
        double d2 = Double.POSITIVE_INFINITY;
        Point a, b;
        double dist() { return Math.sqrt(d2); }
        void update(Point p, Point q) {
            double dx = p.x - q.x, dy = p.y - q.y;
            double v = dx * dx + dy * dy;
            if (v < d2) { d2 = v; a = p; b = q; }
        }
    }

    static String fmt(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e12) return String.valueOf((long) v);
        return String.format("%.2f", v);
    }

    // =====================================================================
    //  Algorithm 1: Brute force  -  O(N^2)
    // =====================================================================
    static Result bruteForce(Point[] p) {
        Result r = new Result();
        for (int i = 0; i < p.length; i++)
            for (int j = i + 1; j < p.length; j++)
                r.update(p[i], p[j]);
        return r;
    }

    // =====================================================================
    //  Algorithm 2: Divide and conquer  -  O(N log N)
    // =====================================================================
    // Top-level dividing line and d (used only for drawing the strip).
    static double topMidX = Double.NaN, topD = Double.NaN;

    static Result divideAndConquer(Point[] pts) {
        Point[] px = pts.clone();
        Arrays.sort(px, Comparator.comparingDouble((Point q) -> q.x).thenComparingDouble(q -> q.y));
        Point[] tmp = new Point[px.length];
        topMidX = Double.NaN;
        topD = Double.NaN;
        return solve(px, tmp, 0, px.length, px.length);
    }

    /** Solves px[lo..hi) and leaves that range sorted by y (like merge sort). */
    private static Result solve(Point[] px, Point[] tmp, int lo, int hi, int total) {
        int n = hi - lo;
        if (n <= 3) {                                   // base case
            Result r = new Result();
            for (int i = lo; i < hi; i++)
                for (int j = i + 1; j < hi; j++) r.update(px[i], px[j]);
            Arrays.sort(px, lo, hi, Comparator.comparingDouble(q -> q.y));
            return r;
        }
        int mid = (lo + hi) >>> 1;
        double midX = px[mid].x;                        // dividing line (read BEFORE merging by y)

        Result left = solve(px, tmp, lo, mid, total);
        Result right = solve(px, tmp, mid, hi, total);
        Result best = left.d2 <= right.d2 ? left : right;

        // merge the two y-sorted halves
        int i = lo, j = mid, k = lo;
        while (i < mid && j < hi) tmp[k++] = (px[i].y <= px[j].y) ? px[i++] : px[j++];
        while (i < mid) tmp[k++] = px[i++];
        while (j < hi) tmp[k++] = px[j++];
        System.arraycopy(tmp, lo, px, lo, n);

        double d = best.dist();
        if (lo == 0 && hi == total) { topMidX = midX; topD = d; }

        // strip = points within distance d of the dividing line (already sorted by y)
        int s = 0;
        for (int t = lo; t < hi; t++)
            if (Math.abs(px[t].x - midX) < d) tmp[lo + s++] = px[t];

        for (int a = 0; a < s; a++) {
            for (int b = a + 1; b < s; b++) {
                double dy = tmp[lo + b].y - tmp[lo + a].y;
                if (dy * dy >= best.d2) break;          // too far in y: stop (at most ~7 checks)
                best.update(tmp[lo + a], tmp[lo + b]);
            }
        }
        return best;
    }

    // =====================================================================
    //  Input generation, timing, verification, benchmark
    // =====================================================================
    static Point[] randomPoints(int n, Random rnd, double range) {
        Point[] p = new Point[n];
        for (int i = 0; i < n; i++)
            p[i] = new Point(Math.round(rnd.nextDouble() * range * 100) / 100.0,
                             Math.round(rnd.nextDouble() * range * 100) / 100.0);
        return p;
    }

    static double timeMs(Runnable r) {
        long t0 = System.nanoTime();
        r.run();
        return (System.nanoTime() - t0) / 1e6;
    }

    static void cornerCases() {
        Object[][] cases = {
            {"N = 2",                 new Point[]{new Point(0, 0), new Point(3, 4)}},
            {"Duplicate points",      new Point[]{new Point(2, 2), new Point(7, 1), new Point(2, 2)}},
            {"Collinear (same y)",    new Point[]{new Point(1, 5), new Point(4, 5), new Point(9, 5)}},
            {"Collinear (same x)",    new Point[]{new Point(3, 1), new Point(3, 9), new Point(3, 4), new Point(3, 20)}},
            {"All points identical",  new Point[]{new Point(5, 5), new Point(5, 5), new Point(5, 5), new Point(5, 5), new Point(5, 5)}},
            {"Diagonal line",         new Point[]{new Point(0, 0), new Point(1, 1), new Point(2, 2), new Point(10, 10), new Point(11, 11)}},
        };
        System.out.println("Corner cases:");
        for (Object[] c : cases) {
            Point[] p = (Point[]) c[1];
            Result b = bruteForce(p), d = divideAndConquer(p);
            System.out.printf("  %-22s BF = %.6f   D&C = %.6f   %s%n", c[0], b.dist(), d.dist(),
                    Math.abs(b.dist() - d.dist()) < 1e-9 ? "OK" : "MISMATCH");
        }
    }

    static void verify() {
        Random rnd = new Random(42);
        int tests = 0;
        for (int t = 0; t < 3000; t++) {
            int n = 2 + rnd.nextInt(80);
            Point[] p = new Point[n];
            int kind = t % 5;
            for (int i = 0; i < n; i++) {
                switch (kind) {
                    case 0:  p[i] = new Point(rnd.nextDouble() * 100, rnd.nextDouble() * 100); break;
                    case 1:  p[i] = new Point(rnd.nextInt(5), rnd.nextInt(5)); break;            // many duplicates
                    case 2:  p[i] = new Point(rnd.nextInt(1000), 7); break;                     // same y
                    case 3:  p[i] = new Point(3, rnd.nextInt(1000)); break;                     // same x
                    default: { double v = rnd.nextInt(500); p[i] = new Point(v, 2 * v + 1); }   // diagonal line
                }
            }
            double b = bruteForce(p).dist(), d = divideAndConquer(p).dist();
            if (Math.abs(b - d) > 1e-9)
                throw new AssertionError("Mismatch! BF=" + b + " DC=" + d + " (kind " + kind + ", n=" + n + ")");
            tests++;
        }
        System.out.println("Verification: " + tests + " random tests (random, duplicates, collinear, same x/y): "
                + "both algorithms ALWAYS agree.");
    }

    static double[][] benchmark(int[] sizes) {
        Random rnd = new Random(1);
        double[][] out = new double[2][sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            Point[] p = randomPoints(sizes[i], rnd, 1_000_000);
            int reps = sizes[i] <= 1000 ? 20 : (sizes[i] <= 10000 ? 3 : 1);
            double bf = 0, dc = 0;
            for (int r = 0; r < reps; r++) {
                bf += timeMs(() -> bruteForce(p));
                dc += timeMs(() -> divideAndConquer(p));
            }
            out[0][i] = bf / reps;
            out[1][i] = dc / reps;
        }
        return out;
    }

    // =====================================================================
    //  main
    // =====================================================================
    public static void main(String[] args) {
        List<String> flags = Arrays.asList(args);
        boolean gui = !GraphicsEnvironment.isHeadless() && !flags.contains("--nogui");
        boolean bench = !flags.contains("--nobench");

        Scanner in = new Scanner(System.in);
        if (!in.hasNextInt()) { System.out.println("Please enter an integer N."); return; }
        int n = in.nextInt();
        if (n < 2) { System.out.println("Need at least 2 points (N = " + n + ")."); return; }

        Point[] pts = randomPoints(n, new Random(), 1000);

        if (n <= 20000) { bruteForce(pts); divideAndConquer(pts); }   // JIT warm-up

        Result[] res = new Result[2];
        double tBF = timeMs(() -> res[0] = bruteForce(pts));
        double tDC = timeMs(() -> res[1] = divideAndConquer(pts));
        final double midX = topMidX, topDist = topD;

        // ---- Required output (3 lines) ----
        System.out.printf("%.6f %s %s%n", res[0].dist(), res[0].a, res[0].b);
        System.out.printf("%.6f %s %s%n", res[1].dist(), res[1].a, res[1].b);
        System.out.printf("Brute-Force: %.3f ms | Divide-and-Conquer: %.3f ms%n", tBF, tDC);

        // ---- Extra analysis ----
        System.out.println();
        System.out.println("Distances agree: " + (Math.abs(res[0].dist() - res[1].dist()) < 1e-9));
        cornerCases();
        verify();

        int[] sizes = {100, 500, 1000, 2000, 5000, 10000, 20000, 40000};
        double[][] times = null;
        if (bench) {
            System.out.println("\nBenchmark (running...)");
            times = benchmark(sizes);
            System.out.printf("%8s %14s %14s%n", "N", "Brute (ms)", "D&C (ms)");
            for (int i = 0; i < sizes.length; i++)
                System.out.printf("%8d %14.3f %14.3f%n", sizes[i], times[0][i], times[1][i]);
        }

        if (gui) {
            final double[][] tt = times;
            SwingUtilities.invokeLater(() -> showWindow(pts, res, midX, topDist, sizes, tt));
        }
    }

    // =====================================================================
    //  Visualization
    // =====================================================================
    static void showWindow(Point[] pts, Result[] res, double midX, double d, int[] sizes, double[][] times) {
        JFrame f = new JFrame("Closest Pair of Points - Brute Force vs Divide and Conquer");
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("1. Points & closest pair", new PointsPanel(pts, res, midX, d));
        if (times != null) tabs.addTab("2. Running time vs N", new TimePanel(sizes, times));
        int m = Math.min(pts.length, 16);
        tabs.addTab("3. Animation (D&C)", new AnimPanel(Arrays.copyOf(pts, m)));
        f.add(tabs);
        f.setSize(950, 720);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    /** Maps data coordinates to screen coordinates. */
    static final class Mapper {
        final double minX, maxX, minY, maxY;
        final int L, T, W, H;
        Mapper(Point[] pts, int L, int T, int W, int H) {
            this.L = L; this.T = T; this.W = Math.max(W, 10); this.H = Math.max(H, 10);
            double a = Double.MAX_VALUE, b = -Double.MAX_VALUE, c = a, e = b;
            for (Point p : pts) {
                a = Math.min(a, p.x); b = Math.max(b, p.x);
                c = Math.min(c, p.y); e = Math.max(e, p.y);
            }
            if (b == a) b = a + 1;
            if (e == c) e = c + 1;
            double px = (b - a) * 0.04, py = (e - c) * 0.04;
            minX = a - px; maxX = b + px; minY = c - py; maxY = e + py;
        }
        double sx(double x) { return L + (x - minX) / (maxX - minX) * W; }
        double sy(double y) { return T + H - (y - minY) / (maxY - minY) * H; }
    }

    static void drawPair(Graphics2D g, Result r, Color c, float w, int ring, Mapper m) {
        double x1 = m.sx(r.a.x), y1 = m.sy(r.a.y), x2 = m.sx(r.b.x), y2 = m.sy(r.b.y);
        g.setColor(c);
        g.setStroke(new BasicStroke(w));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(new Ellipse2D.Double(x1 - ring / 2.0, y1 - ring / 2.0, ring, ring));
        g.draw(new Ellipse2D.Double(x2 - ring / 2.0, y2 - ring / 2.0, ring, ring));
        g.setStroke(new BasicStroke(1f));
    }

    static void legend(Graphics2D g, int x, int y, Color c, String text) {
        g.setColor(c);
        g.fillRect(x, y - 10, 14, 10);
        g.setColor(Color.BLACK);
        g.drawString(text, x + 20, y);
    }

    static final Color GREEN = new Color(0, 150, 0);
    static final Color ORANGE = new Color(220, 140, 0);
    static final Stroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
            10f, new float[]{8f, 6f}, 0f);

    // ---------- Tab 1: points, closest pairs, dividing line, strip ----------
    static class PointsPanel extends JPanel {
        final Point[] pts; final Result[] res; final double midX, d;
        PointsPanel(Point[] pts, Result[] res, double midX, double d) {
            this.pts = pts; this.res = res; this.midX = midX; this.d = d;
            setBackground(Color.WHITE);
        }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Mapper m = new Mapper(pts, 50, 40, getWidth() - 80, getHeight() - 100);

            g.setColor(Color.LIGHT_GRAY);
            g.drawRect(m.L, m.T, m.W, m.H);

            if (!Double.isNaN(midX)) {                       // strip of width 2d + dividing line
                double x1 = Math.max(m.L, m.sx(midX - d)), x2 = Math.min(m.L + m.W, m.sx(midX + d));
                g.setColor(new Color(255, 200, 0, 70));
                g.fill(new Rectangle2D.Double(x1, m.T, x2 - x1, m.H));
                g.setColor(ORANGE);
                g.setStroke(DASHED);
                g.draw(new Line2D.Double(m.sx(midX), m.T, m.sx(midX), m.T + m.H));
                g.setStroke(new BasicStroke(1f));
            }

            g.setColor(new Color(60, 90, 160));
            int rad = pts.length > 2000 ? 2 : (pts.length > 300 ? 3 : 5);
            for (Point p : pts)
                g.fill(new Ellipse2D.Double(m.sx(p.x) - rad / 2.0, m.sy(p.y) - rad / 2.0, rad, rad));

            drawPair(g, res[0], Color.RED, 6f, 14, m);       // brute force (thick, underneath)
            drawPair(g, res[1], GREEN, 2.5f, 9, m);          // divide & conquer (thin, on top)

            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            g.drawString(String.format("N = %d     min distance d = %.6f", pts.length, res[1].dist()), m.L, 25);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 12f));
            int ly = getHeight() - 22;
            legend(g, m.L, ly, Color.RED, "Brute-Force pair");
            legend(g, m.L + 150, ly, GREEN, "Divide & Conquer pair");
            legend(g, m.L + 330, ly, ORANGE, "Dividing line + strip (width 2d)");
        }
    }

    // ---------- Tab 2: running time vs N ----------
    static class TimePanel extends JPanel {
        final int[] sizes; final double[][] t;
        TimePanel(int[] sizes, double[][] t) { this.sizes = sizes; this.t = t; setBackground(Color.WHITE); }
        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int L = 70, R = 30, T = 50, B = 70;
            int W = getWidth() - L - R, H = getHeight() - T - B;
            double maxN = sizes[sizes.length - 1], maxT = 0;
            for (double[] row : t) for (double v : row) maxT = Math.max(maxT, v);
            maxT *= 1.1;

            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
            g.drawString("Running time vs N", L, 25);
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            for (int i = 0; i <= 5; i++) {
                int y = T + H - i * H / 5;
                g.setColor(new Color(230, 230, 230));
                g.drawLine(L, y, L + W, y);
                g.setColor(Color.BLACK);
                g.drawString(String.format("%.1f", maxT * i / 5), 15, y + 4);
                g.drawString(String.valueOf((int) (maxN * i / 5)), L + i * W / 5 - 14, T + H + 18);
            }
            g.drawLine(L, T, L, T + H);
            g.drawLine(L, T + H, L + W, T + H);
            g.drawString("N (number of points)", L + W / 2 - 50, T + H + 40);
            g.drawString("time (ms)", 15, T - 10);

            Color[] cols = {Color.RED, GREEN};
            String[] names = {"Brute-Force  O(N^2)", "Divide & Conquer  O(N log N)"};
            for (int s = 0; s < 2; s++) {
                g.setColor(cols[s]);
                g.setStroke(new BasicStroke(2.2f));
                Path2D.Double path = new Path2D.Double();
                for (int i = 0; i < sizes.length; i++) {
                    double x = L + sizes[i] / maxN * W, y = T + H - t[s][i] / maxT * H;
                    if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
                }
                g.draw(path);
                g.setStroke(new BasicStroke(1f));
                for (int i = 0; i < sizes.length; i++) {
                    double x = L + sizes[i] / maxN * W, y = T + H - t[s][i] / maxT * H;
                    g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
                }
                g.fillRect(L + 20 + s * 210, T - 28, 12, 8);
                g.setColor(Color.BLACK);
                g.drawString(names[s], L + 38 + s * 210, T - 20);
            }
        }
    }

    // ---------- Tab 3: step-by-step animation of the recursion ----------
    static final class Step {
        String text;
        int lo, hi;                                   // range of x-sorted points being worked on
        double midX = Double.NaN, d = Double.NaN;     // dividing line and current d
        List<Point> strip = Collections.emptyList();  // points inside the strip
        Point c1, c2;                                 // pair being compared right now
        Point bA, bB;                                 // best pair so far in this range
    }

    static Step mk(String text, int lo, int hi, double midX, double d, List<Point> strip,
                   Point c1, Point c2, Result best) {
        Step s = new Step();
        s.text = text; s.lo = lo; s.hi = hi; s.midX = midX; s.d = d;
        s.strip = strip; s.c1 = c1; s.c2 = c2;
        if (best != null) { s.bA = best.a; s.bB = best.b; }
        return s;
    }

    /** Same algorithm as solve(), but records a Step for every interesting moment. */
    static Result animSolve(Point[] px, int lo, int hi, List<Step> st) {
        int n = hi - lo;
        List<Point> none = Collections.emptyList();
        if (n <= 3) {
            Result r = new Result();
            for (int i = lo; i < hi; i++)
                for (int j = i + 1; j < hi; j++) r.update(px[i], px[j]);
            st.add(mk(String.format("CONQUER (base case): %d point(s) -> brute force, best = %.3f", n, r.dist()),
                    lo, hi, Double.NaN, Double.NaN, none, null, null, r));
            return r;
        }
        int mid = (lo + hi) >>> 1;
        double midX = px[mid].x;
        st.add(mk(String.format("DIVIDE: split %d points at x = %s", n, fmt(midX)),
                lo, hi, midX, Double.NaN, none, null, null, null));

        Result left = animSolve(px, lo, mid, st);
        Result right = animSolve(px, mid, hi, st);
        Result best = left.d2 <= right.d2 ? left : right;
        double d = best.dist();

        List<Point> strip = new ArrayList<>();
        for (int i = lo; i < hi; i++) if (Math.abs(px[i].x - midX) < d) strip.add(px[i]);
        strip.sort(Comparator.comparingDouble(q -> q.y));
        st.add(mk(String.format("MERGE: dLeft = %.3f, dRight = %.3f, d = %.3f. Strip (width 2d) holds %d point(s)",
                left.dist(), right.dist(), d, strip.size()), lo, hi, midX, d, strip, null, null, best));

        for (int a = 0; a < strip.size(); a++) {
            for (int b = a + 1; b < strip.size(); b++) {
                double dy = strip.get(b).y - strip.get(a).y;
                if (dy * dy >= best.d2) break;
                Point p = strip.get(a), q = strip.get(b);
                double before = best.d2;
                best.update(p, q);
                boolean improved = best.d2 < before;
                st.add(mk(String.format("COMPARE across the line: %s and %s, distance = %.3f%s",
                        p, q, Math.sqrt((p.x - q.x) * (p.x - q.x) + (p.y - q.y) * (p.y - q.y)),
                        improved ? "  -> NEW BEST" : ""),
                        lo, hi, midX, Math.sqrt(before), strip, p, q, best));
            }
        }
        st.add(mk(String.format("MERGED: closest pair in these %d points = %.3f", n, best.dist()),
                lo, hi, midX, d, none, null, null, best));
        return best;
    }

    static class AnimPanel extends JPanel {
        final Point[] px;
        final List<Step> steps = new ArrayList<>();
        int idx = 0;
        final JLabel caption = new JLabel(" ", SwingConstants.CENTER);
        final JLabel counter = new JLabel(" ");
        final JButton play = new JButton("Play");
        final javax.swing.Timer timer;
        final JPanel canvas;

        AnimPanel(Point[] pts) {
            px = pts.clone();
            Arrays.sort(px, Comparator.comparingDouble((Point q) -> q.x).thenComparingDouble(q -> q.y));
            List<Point> none = Collections.emptyList();
            steps.add(mk("START: " + px.length + " points sorted by x. Press Play or Next.",
                    0, px.length, Double.NaN, Double.NaN, none, null, null, null));
            Result r = animSolve(px, 0, px.length, steps);
            steps.add(mk(String.format("FINISHED: closest pair distance = %.6f", r.dist()),
                    0, px.length, Double.NaN, Double.NaN, none, null, null, r));

            setLayout(new BorderLayout());
            caption.setFont(caption.getFont().deriveFont(Font.BOLD, 14f));
            caption.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            add(caption, BorderLayout.NORTH);

            canvas = new JPanel() {
                @Override protected void paintComponent(Graphics g0) { super.paintComponent(g0); paintStep((Graphics2D) g0, this); }
            };
            canvas.setBackground(Color.WHITE);
            add(canvas, BorderLayout.CENTER);

            JButton reset = new JButton("Reset"), prev = new JButton("< Prev"), next = new JButton("Next >");
            JPanel bar = new JPanel();
            bar.add(reset); bar.add(prev); bar.add(play); bar.add(next); bar.add(counter);
            add(bar, BorderLayout.SOUTH);

            timer = new javax.swing.Timer(900, e -> {
                if (idx < steps.size() - 1) { idx++; refresh(); }
                else { ((javax.swing.Timer) e.getSource()).stop(); play.setText("Play"); }
            });
            reset.addActionListener(e -> { timer.stop(); play.setText("Play"); idx = 0; refresh(); });
            prev.addActionListener(e -> { timer.stop(); play.setText("Play"); if (idx > 0) idx--; refresh(); });
            next.addActionListener(e -> { timer.stop(); play.setText("Play"); if (idx < steps.size() - 1) idx++; refresh(); });
            play.addActionListener(e -> {
                if (timer.isRunning()) { timer.stop(); play.setText("Play"); }
                else { if (idx >= steps.size() - 1) idx = 0; timer.start(); play.setText("Pause"); refresh(); }
            });
            refresh();
        }

        void refresh() {
            caption.setText(steps.get(idx).text);
            counter.setText("   step " + (idx + 1) + " / " + steps.size());
            canvas.repaint();
        }

        void paintStep(Graphics2D g, JPanel c) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Mapper m = new Mapper(px, 50, 20, c.getWidth() - 170, c.getHeight() - 50);
            Step s = steps.get(idx);

            // active range
            if (s.hi > s.lo) {
                double x1 = m.sx(px[s.lo].x) - 10, x2 = m.sx(px[s.hi - 1].x) + 10;
                g.setColor(new Color(100, 150, 255, 35));
                g.fill(new Rectangle2D.Double(x1, m.T, x2 - x1, m.H));
            }
            // strip + dividing line
            if (!Double.isNaN(s.midX)) {
                if (!Double.isNaN(s.d)) {
                    double x1 = m.sx(s.midX - s.d), x2 = m.sx(s.midX + s.d);
                    g.setColor(new Color(255, 200, 0, 80));
                    g.fill(new Rectangle2D.Double(x1, m.T, x2 - x1, m.H));
                }
                g.setColor(ORANGE);
                g.setStroke(DASHED);
                g.draw(new Line2D.Double(m.sx(s.midX), m.T, m.sx(s.midX), m.T + m.H));
                g.setStroke(new BasicStroke(1f));
            }
            // best pair so far (green) and pair being compared (magenta)
            if (s.bA != null) line(g, m, s.bA, s.bB, GREEN, 3f);
            if (s.c1 != null) line(g, m, s.c1, s.c2, new Color(200, 0, 200), 2f);

            // points
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 10f));
            for (int i = 0; i < px.length; i++) {
                Point p = px[i];
                boolean in = i >= s.lo && i < s.hi;
                boolean inStrip = s.strip.contains(p);
                Color col = inStrip ? ORANGE : (in ? new Color(60, 90, 160) : new Color(190, 190, 190));
                double X = m.sx(p.x), Y = m.sy(p.y);
                g.setColor(col);
                g.fill(new Ellipse2D.Double(X - 5, Y - 5, 10, 10));
                g.setColor(Color.DARK_GRAY);
                g.drawString(p.toString(), (float) X + 7, (float) Y - 6);
            }
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
            int ly = c.getHeight() - 8;
            legend(g, 50, ly, new Color(60, 90, 160), "active points");
            legend(g, 170, ly, ORANGE, "inside strip");
            legend(g, 270, ly, GREEN, "best pair so far");
            legend(g, 400, ly, new Color(200, 0, 200), "pair being compared");
        }

        void line(Graphics2D g, Mapper m, Point a, Point b, Color col, float w) {
            g.setColor(col);
            g.setStroke(new BasicStroke(w));
            g.draw(new Line2D.Double(m.sx(a.x), m.sy(a.y), m.sx(b.x), m.sy(b.y)));
            g.setStroke(new BasicStroke(1f));
        }
    }
}
