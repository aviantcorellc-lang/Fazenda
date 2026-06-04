import openpyxl
wb = openpyxl.load_workbook(r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\Електронна таблиця без назви.xlsx')

# Check Лист1
ws1 = wb['Лист1']
print(f"=== ЛІД 1 ===")
print(f"Max row: {ws1.max_row}, Max col: {ws1.max_column}")
print(f"\nКатегорії (рядки 4-30):")
for row_idx in range(4, min(ws1.max_row + 1, 30)):
    category = ws1.cell(row=row_idx, column=1).value
    varieties = ws1.cell(row=row_idx, column=2).value
    if category:
        print(f"  Рядок {row_idx}: {category} | {varieties}")

# Check Лист2
ws2 = wb['Виноград (копия)']
print(f"\n=== ЛІД 2: Виноград (копия) ===")
print(f"Max row: {ws2.max_row}, Max col: {ws2.max_column}")

# Count non-empty rows
count = 0
for row_idx in range(2, ws2.max_row + 1):
    name = ws2.cell(row=row_idx, column=3).value
    if name:
        count += 1
    if count <= 5 or row_idx >= ws2.max_row - 5:
        row_data = [ws2.cell(row=row_idx, column=i).value for i in range(1, 7)]
        if name:
            print(f"Рядок {row_idx}: {row_data}")

print(f"\nВсього рядків з даними у Лист2: {count}")
