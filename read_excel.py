import openpyxl
import json

# Открыть файл
wb = openpyxl.load_workbook(r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\Електронна таблиця без назви.xlsx')

# Получить все листы
sheets = wb.sheetnames
print(f"=== ЛИСТЫ: {sheets} ===\n")

for sheet_name in sheets:
    ws = wb[sheet_name]
    print(f"\n{'='*60}")
    print(f"ЛИСТ: {sheet_name}")
    print(f"{'='*60}")
    print(f"Максимальна рядків: {ws.max_row}")
    print(f"Максимальна колонок: {ws.max_column}")
    print()
    
    # Вивести заголовок
    header = []
    for cell in ws[1]:
        header.append(cell.value)
    print(f"Заголовок: {header}\n")
    
    # Вивести всі рядки з даними
    for row_idx in range(2, min(ws.max_row + 1, 50)):
        row_data = []
        for col_idx in range(1, ws.max_column + 1):
            cell = ws.cell(row=row_idx, column=col_idx)
            row_data.append(cell.value)
        if any(row_data):  # Вивести, якщо рядок не пустий
            print(f"Рядок {row_idx}: {row_data}")

print("\n\n=== ВСЬОГО РЯДКІВ ПО ЛИСТАХ ===")
for sheet_name in sheets:
    ws = wb[sheet_name]
    print(f"{sheet_name}: {ws.max_row - 1} рядків (без заголовка)")
