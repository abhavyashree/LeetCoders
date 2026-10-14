# Problem 2: Closest Pair of Points – Comparative Analysis

## 📌 Problem Statement

Given `N` points on a 2D plane, implement and compare two algorithms to find the closest pair of points:

1. **Brute-Force Algorithm**
2. **Divide-and-Conquer Algorithm**

Both algorithms must find the minimum Euclidean distance between any two points and display the corresponding pair of points.

The project also compares the execution time of both algorithms for increasing input sizes and visualizes their results.

## 🎯 Objectives

- Implement the Brute-Force and Divide-and-Conquer algorithms.
- Find the minimum distance and closest pair of points using both methods.
- Handle corner cases such as duplicate points, collinear points, shared x or y coordinates, and small input sizes.
- Compare the execution time of both algorithms.
- Verify that both algorithms produce the same minimum distance.
- Visualize the points, closest pairs, dividing line, strip region, and running-time comparison graph.

## ⚙️ Input Format

- Read an integer `N` representing the number of points.
- Generate `N` random points with integer or floating-point coordinates `(x, y)`.

## 📤 Output Format

The program displays:

1. The minimum distance and closest pair found by the Brute-Force algorithm.
2. The minimum distance and closest pair found by the Divide-and-Conquer algorithm.
3. The execution time of both algorithms in milliseconds.

The minimum distance is displayed to **6 decimal places**.

If `N < 2`, the program should handle the input gracefully and report that at least two points are required.

## 🧠 Algorithms Used

### 1. Brute-Force Algorithm

The Brute-Force method calculates the distance between every possible pair of points and selects the pair with the smallest distance.

**Steps:**
1. Consider every possible pair of points.
2. Calculate the Euclidean distance between each pair.
3. Keep track of the minimum distance and the corresponding pair.
4. Return the result.

**Time Complexity:** `O(N²)`

**Space Complexity:** `O(1)` auxiliary space, excluding input storage.

### 2. Divide-and-Conquer Algorithm

The Divide-and-Conquer method divides the points into smaller subsets, solves the problem recursively, and combines the results to find the closest pair.

**Steps:**
1. Sort the points by their x-coordinates.
2. Divide the points into two halves.
3. Recursively find the closest pair in each half.
4. Select the smaller of the two minimum distances.
5. Examine the strip around the dividing line for a closer pair crossing the two halves.
6. Return the overall minimum distance and closest pair.

**Time Complexity:** `O(N log N)` with an efficient strip-processing implementation and suitable sorting strategy.

**Space Complexity:** Typically `O(N)` auxiliary space for an implementation using additional arrays or temporary storage.

## 📏 Distance Formula

The Euclidean distance between two points `(x1, y1)` and `(x2, y2)` is:

\[
d=\sqrt{(x_2-x_1)^2+(y_2-y_1)^2}
\]

The pair with the smallest Euclidean distance is the closest pair.

## 🧪 Corner Cases Handled

- `N < 2`: Report that at least two points are required.
- `N = 2`: Calculate the distance between the two points.
- Duplicate points: Return a minimum distance of `0`.
- Collinear points: Correctly identify the closest pair on the same line.
- Same x-coordinate: Handle vertically aligned points.
- Same y-coordinate: Handle horizontally aligned points.
- Random integer or floating-point coordinates.
- Multiple pairs with the same minimum distance: Return any valid closest pair.
- Both algorithms must agree on the minimum distance within an appropriate floating-point tolerance.

## 📊 Visualization

The project includes the following visualizations:

### 1. Closest Pair Visualization
- Plot all generated points on a 2D plane.
- Highlight the closest pair found by the Brute-Force algorithm in one colour.
- Highlight the closest pair found by the Divide-and-Conquer algorithm in another colour.
- Draw lines connecting the points in each closest pair.

### 2. Divide-and-Conquer Visualization
- Display the vertical dividing line used to split the points.
- Highlight the strip region around the dividing line.
- Show how the algorithm searches for a closer pair across the two halves.

### 3. Running-Time Comparison
- Run both algorithms with increasing values of `N`.
- Measure their execution times.
- Plot a graph with input size `N` on the x-axis and execution time in milliseconds on the y-axis.
- Compare the performance of both algorithms.

### 4. Optional Animation
Animate the recursive splitting and merging steps of the Divide-and-Conquer algorithm.

## 📈 Performance Analysis

The two algorithms are compared based on their execution time for increasing input sizes.

| Feature | Brute-Force | Divide-and-Conquer |
|---|---|---|
| Approach | Checks every pair | Divides points into subsets |
| Time Complexity | `O(N²)` | `O(N log N)` |
| Best suited
