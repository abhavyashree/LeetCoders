"""
EXAM TIMETABLE SCHEDULING
Graph Colouring Challenge
"""

import csv
import random
import time
from pathlib import Path

try:
    import matplotlib.pyplot as plt
    import networkx as nx
    PLOTTING_AVAILABLE = True
except ImportError:
    PLOTTING_AVAILABLE = False


# 1. INPUT FUNCTIONS

def read_positive_int(prompt, minimum=1):
    while True:
        try:
            value = int(input(prompt))
            if value >= minimum:
                return value
            print(f"Enter a number greater than or equal to {minimum}.")
        except ValueError:
            print("Please enter a valid whole number.")


def get_manual_graph():
    n = read_positive_int("Enter number of courses (N): ")

    max_edges = n * (n - 1) // 2
    m = read_positive_int(
        f"Enter number of conflicts (M, maximum {max_edges}): ", 0
    )

    while m > max_edges:
        print("Too many conflict edges.")
        m = read_positive_int("Enter M again: ", 0)

    graph = [set() for _ in range(n)]
    edges = set()

    print("\nEnter conflicting course pairs.")
    print("Example: 1 3 means Course 1 conflicts with Course 3.")

    while len(edges) < m:
        try:
            parts = input(
                f"Conflict {len(edges) + 1}/{m}: "
            ).replace(",", " ").split()

            if len(parts) != 2:
                print("Enter exactly two course numbers.")
                continue

            u, v = map(int, parts)

            if not (1 <= u <= n and 1 <= v <= n):
                print(f"Course numbers must be between 1 and {n}.")
                continue

            if u == v:
                print("A course cannot conflict with itself.")
                continue

            edge = tuple(sorted((u - 1, v - 1)))

            if edge in edges:
                print("This conflict already exists.")
                continue

            edges.add(edge)
            graph[u - 1].add(v - 1)
            graph[v - 1].add(u - 1)

        except ValueError:
            print("Enter two valid whole numbers.")

    return graph


def get_random_graph():
    n = read_positive_int("Enter number of courses (N): ")

    density = read_positive_int(
        "Enter conflict density percentage (1-100): "
    )

    while density > 100:
        density = read_positive_int("Enter density between 1 and 100: ")

    possible_edges = [
        (u, v)
        for u in range(n)
        for v in range(u + 1, n)
    ]

    edge_count = round(len(possible_edges) * density / 100)

    selected_edges = random.sample(
        possible_edges, edge_count
    ) if edge_count else []

    graph = [set() for _ in range(n)]

    for u, v in selected_edges:
        graph[u].add(v)
        graph[v].add(u)

    print(f"\nGenerated {n} courses and {len(selected_edges)} conflicts.")

    return graph


def print_graph(graph):
    print("\nCONFLICT GRAPH")

    for course, neighbours in enumerate(graph):
        conflicts = [str(v + 1) for v in sorted(neighbours)]

        print(
            f"Course {course + 1} conflicts with: "
            f"{', '.join(conflicts) if conflicts else 'None'}"
        )


# 2. WELSH-POWELL GREEDY ALGORITHM

def welsh_powell(graph, capacity):
    n = len(graph)

    # Sort courses by decreasing number of conflicts.
    order = sorted(
        range(n),
        key=lambda u: (-len(graph[u]), u)
    )

    colours = [-1] * n
    slot_sizes = []

    for course in order:
        placed = False

        # Try existing slots first.
        for slot in range(len(slot_sizes)):

            if slot_sizes[slot] >= capacity:
                continue

            conflict_found = any(
                colours[neighbour] == slot
                for neighbour in graph[course]
            )

            if not conflict_found:
                colours[course] = slot
                slot_sizes[slot] += 1
                placed = True
                break

        # Create a new slot if necessary.
        if not placed:
            colours[course] = len(slot_sizes)
            slot_sizes.append(1)

    return colours, len(slot_sizes)


# 3. EXACT BACKTRACKING ALGORITHM

def exact_colouring(graph, capacity, time_limit_seconds=30):
    start = time.perf_counter()
    n = len(graph)

    if n == 0:
        return [], 0, True, 0.0

    # Start with a valid greedy solution.
    best_colours, best_count = welsh_powell(graph, capacity)

    colours = [-1] * n
    slot_sizes = [0] * n

    deadline = start + time_limit_seconds
    timed_out = False

    def choose_vertex():
        uncoloured = [
            u for u in range(n)
            if colours[u] == -1
        ]

        def priority(u):
            neighbour_colours = {
                colours[v]
                for v in graph[u]
                if colours[v] != -1
            }

            return (
                len(neighbour_colours),
                len(graph[u]),
                -u
            )

        return max(uncoloured, key=priority)

    def search(used_slots):
        nonlocal best_colours, best_count, timed_out

        if time.perf_counter() >= deadline:
            timed_out = True
            return

        # All courses have been assigned.
        if all(colour != -1 for colour in colours):
            if used_slots < best_count:
                best_count = used_slots
                best_colours = colours.copy()
            return

        # Cannot improve the current best solution.
        if used_slots >= best_count:
            return

        course = choose_vertex()

        forbidden_slots = {
            colours[v]
            for v in graph[course]
            if colours[v] != -1
        }

        # Try placing the course in an existing slot.
        for slot in range(used_slots):

            if timed_out:
                return

            if slot in forbidden_slots:
                continue

            if slot_sizes[slot] >= capacity:
                continue

            colours[course] = slot
            slot_sizes[slot] += 1

            search(used_slots)

            slot_sizes[slot] -= 1
            colours[course] = -1

        # Try opening a new slot only if it can improve the best.
        if not timed_out and used_slots + 1 < best_count:

            slot = used_slots
            colours[course] = slot
            slot_sizes[slot] = 1

            search(used_slots + 1)

            slot_sizes[slot] = 0
            colours[course] = -1

    search(0)

    elapsed = time.perf_counter() - start
    proven_optimal = not timed_out

    return best_colours, best_count, proven_optimal, elapsed


# 4. TIMETABLE DISPLAY

def make_timetable(colours):
    timetable = {}

    for course, slot in enumerate(colours, start=1):
        timetable.setdefault(slot + 1, []).append(course)

    return dict(sorted(timetable.items()))


def print_timetable(title, colours, capacity):
    timetable = make_timetable(colours)

    print(f"\n{title}")
    print("-" * len(title))

    for slot, courses in timetable.items():
        course_names = ", ".join(
            f"Course {c}" for c in courses
        )

        print(
            f"Slot {slot}: {course_names} "
            f"({len(courses)}/{capacity} exams)"
        )

    print(f"Total slots: {len(timetable)}")


# 5. VALIDATE THE TIMETABLE

def validate_schedule(graph, colours, capacity):
    if len(colours) != len(graph):
        return False

    if any(colour < 0 for colour in colours):
        return False

    slot_sizes = {}

    for colour in colours:
        slot_sizes[colour] = slot_sizes.get(colour, 0) + 1

    if any(size > capacity for size in slot_sizes.values()):
        return False

    for course, neighbours in enumerate(graph):
        for neighbour in neighbours:
            if colours[course] == colours[neighbour]:
                return False

    return True


# 6. EXPORT TIMETABLE TO CSV

def export_csv(colours, filename="exam_timetable.csv"):
    timetable = make_timetable(colours)

    with open(filename, "w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)

        writer.writerow(["Slot", "Course"])

        for slot, courses in timetable.items():
            for course in courses:
                writer.writerow([slot, f"Course {course}"])

    print(f"Timetable saved to: {Path(filename).resolve()}")


# 7. VISUALIZE THE CONFLICT GRAPH

def plot_graph(graph, colours):
    if not PLOTTING_AVAILABLE:
        print("Install matplotlib and networkx to view graphs.")
        return

    G = nx.Graph()
    G.add_nodes_from(range(1, len(graph) + 1))

    for u, neighbours in enumerate(graph):
        for v in neighbours:
            if u < v:
                G.add_edge(u + 1, v + 1)

    node_colours = [
        colours[u] for u in range(len(graph))
    ]

    pos = nx.spring_layout(G, seed=42)

    nx.draw_networkx(
        G,
        pos,
        node_color=node_colours,
        cmap=plt.cm.Set3,
        with_labels=True,
        node_size=800,
        edge_color="gray"
    )

    plt.title("Conflict Graph (Same Colour = Same Slot)")
    plt.axis("off")
    plt.tight_layout()
    plt.show()


# 8. VISUALIZE THE TIMETABLE GRID

def plot_timetable_grid(greedy_colours, exact_colours):
    if not PLOTTING_AVAILABLE:
        return

    schedules = [
        ("Welsh-Powell", greedy_colours),
        ("Backtracking", exact_colours)
    ]

    max_slots = max(
        max(colours) + 1
        for _, colours in schedules
        if colours
    )

    fig, axes = plt.subplots(2, 1, figsize=(10, 6))

    for ax, (name, colours) in zip(axes, schedules):
        for course, slot in enumerate(colours, start=1):
            ax.scatter(slot + 1, course, s=260)
            ax.text(
                slot + 1, course, str(course),
                ha="center", va="center"
            )

        ax.set_title(name)
        ax.set_xlabel("Time Slot")
        ax.set_ylabel("Course")
        ax.set_xticks(range(1, max_slots + 1))
        ax.set_yticks(range(1, len(colours) + 1))
        ax.grid(True, alpha=0.3)

    plt.tight_layout()
    plt.show()


# 9. COMPARE ALGORITHMS

def plot_comparison(
    greedy_slots, exact_slots,
    greedy_time, exact_time
):
    if not PLOTTING_AVAILABLE:
        return

    labels = ["Welsh-Powell", "Backtracking"]

    fig, axes = plt.subplots(1, 2, figsize=(10, 4))

    axes[0].bar(labels, [greedy_slots, exact_slots])
    axes[0].set_title("Number of Time Slots")
    axes[0].set_ylabel("Slots")

    axes[1].bar(labels, [greedy_time, exact_time])
    axes[1].set_title("Runtime")
    axes[1].set_ylabel("Seconds")

    plt.tight_layout()
    plt.show()


# 10. MAIN PROGRAM

def main():
    print("=" * 55)
    print("EXAM TIMETABLE SCHEDULING")
    print("GRAPH COLOURING CHALLENGE")
    print("=" * 55)

    print("\nChoose graph input:")
    print("1. Enter conflicts manually")
    print("2. Generate a random graph")

    while True:
        choice = input("Enter choice (1 or 2): ").strip()

        if choice in ("1", "2"):
            break

        print("Please enter 1 or 2.")

    if choice == "1":
        graph = get_manual_graph()
    else:
        graph = get_random_graph()

    print_graph(graph)

    capacity = read_positive_int(
        "\nEnter maximum exams allowed per slot (R): "
    )

    # Run Welsh-Powell.
    greedy_start = time.perf_counter()

    greedy_colours, greedy_slots = welsh_powell(
        graph, capacity
    )

    greedy_time = time.perf_counter() - greedy_start

    # Run exact backtracking.
    exact_colours, exact_slots, optimal, exact_time = (
        exact_colouring(
            graph,
            capacity,
            time_limit_seconds=30
        )
    )

    # Compare the algorithms.
    print("\n" + "=" * 55)
    print("ALGORITHM COMPARISON")
    print("=" * 55)

    print(
        f"Welsh-Powell: {greedy_slots} slots, "
        f"{greedy_time:.6f} seconds"
    )

    print(
        f"Backtracking: {exact_slots} slots, "
        f"{exact_time:.6f} seconds"
    )

    if optimal:
        print("Optimality: PROVED")
    else:
        print("Optimality: NOT PROVED (time limit reached)")

    print(
        "Greedy schedule valid:",
        validate_schedule(graph, greedy_colours, capacity)
    )

    print(
        "Backtracking schedule valid:",
        validate_schedule(graph, exact_colours, capacity)
    )

    # Print timetables.
    print_timetable(
        "WELSH-POWELL TIMETABLE",
        greedy_colours,
        capacity
    )

    print_timetable(
        "BACKTRACKING TIMETABLE",
        exact_colours,
        capacity
    )

    # Export CSV.
    export_choice = input(
        "\nExport timetable to CSV? (y/n): "
    ).strip().lower()

    if export_choice == "y":
        filename = input(
            "Filename [exam_timetable.csv]: "
        ).strip()

        if not filename:
            filename = "exam_timetable.csv"

        if not filename.lower().endswith(".csv"):
            filename += ".csv"

        export_csv(exact_colours, filename)

    # Display visualizations.
    if PLOTTING_AVAILABLE:
        plot_choice = input(
            "\nShow visualizations? (y/n): "
        ).strip().lower()

        if plot_choice == "y":
            plot_graph(graph, exact_colours)

            plot_timetable_grid(
                greedy_colours,
                exact_colours
            )

            plot_comparison(
                greedy_slots,
                exact_slots,
                greedy_time,
                exact_time
            )
    else:
        print(
            "\nTo enable visualizations, run:\n"
            "python -m pip install matplotlib networkx"
        )

    print("\nProject finished successfully!")


if __name__ == "__main__":
    main()
