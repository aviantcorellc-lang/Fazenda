import csv
import os

csv_path = r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\app\src\main\res\raw\plants.csv'
with open(csv_path, 'r', encoding='utf-8') as f:
    reader = csv.reader(f)
    header = next(reader)
    rows = list(reader)
    print(f"Total rows (excluding header): {len(rows)}")
    categories = {}
    for row in rows:
        if len(row) >= 2:
            cat = row[1]
            categories[cat] = categories.get(cat, 0) + 1
    print("\nCategory counts:")
    for cat, count in sorted(categories.items()):
        print(f"{cat}: {count}")
    print(f"\nUnique categories: {len(categories)}")