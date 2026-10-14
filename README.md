# Budget Traveller with Coupons

## About
A Python-based flight cost optimization project that finds the cheapest route between a starting city and a destination while considering a maximum number of stops and available discount coupons.

## Features
- Finds the minimum flight cost using Dynamic Programming.
- Reconstructs the cheapest route.
- Supports CSV input and output.
- Visualizes cities and weighted flight routes.
- Automatically animates the selected route.
- Highlights coupon usage and running travel cost.

## Technologies Used
- Python
- Dynamic Programming
- CSV
- NetworkX
- Matplotlib

## Requirements
Install the required libraries:

```bash
python -m pip install networkx matplotlib
```

## How to Run
1. Keep `main.py` and `flights.csv` in the same folder.
2. Open a terminal in that folder.
3. Run:

```bash
python main.py
```

4. Enter the number of cities, maximum stops, and coupons when prompted.
5. View the minimum cost, selected route, generated `result.csv`, and graph animation.

## Input Format
The `flights.csv` file must contain these columns:

```csv
from,to,price
1,2,100
2,5,100
```

## Algorithm
Dynamic Programming is used to find the minimum cost while tracking the number of flights taken and coupons used.
