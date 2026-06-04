import openpyxl
import csv
import os

# Шляхи до файлів
excel_path = r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\Електронна таблиця без назви.xlsx'
output_plants_path = r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\app\src\main\res\raw\plants_new.csv'
output_grapes_path = r'c:\Users\lexus\Documents\Projects\Fazenda_app_android\app\src\main\res\raw\grapes.csv'

# Открыть Excel
wb = openpyxl.load_workbook(excel_path)

# === ЛИД 1: Список всех культур ===
ws1 = wb['Лист1']

print("Обработка Лист1...")
plants_by_category = {}

# Пройти по рядам и собрать категории с сортами
for row_idx in range(1, ws1.max_row + 1):
    category = ws1.cell(row=row_idx, column=1).value
    varieties_str = ws1.cell(row=row_idx, column=2).value
    
    # Пропустить пусті рядки
    if not varieties_str:
        continue
    
    # Якщо немає категорії, виконувати цей рядок як продовження попередньої категорії
    if not category:
        # Знайти попередню категорію
        for prev_row_idx in range(row_idx - 1, 0, -1):
            prev_category = ws1.cell(row=prev_row_idx, column=1).value
            if prev_category:
                category = prev_category
                break
    
    if category and isinstance(category, str) and category.strip():
        # Цей рядок має категорію або продовження
        if category not in plants_by_category:
            plants_by_category[category] = []
        
        # Разбить сорти по запятым
        if isinstance(varieties_str, str):
            varieties = [v.strip() for v in varieties_str.split(',')]
            plants_by_category[category].extend(varieties)

print(f"\nНайдено категорий: {len(plants_by_category)}")
for cat, vars in sorted(plants_by_category.items()):
    print(f"  {cat}: {len(vars)} сортів")
    for var in vars[:2]:
        print(f"    - {var}")

# === ЛИСТ 2: Детальна таблиця Винограду ===
ws2 = wb['Виноград (копия)']

print("\n\nОбработка Винограду...")
grapes_data = []

for row_idx in range(2, ws2.max_row + 1):
    name = ws2.cell(row=row_idx, column=3).value  # Назва
    
    # Пропустити пусті рядки
    if not name:
        continue
    
    row_num = ws2.cell(row=row_idx, column=1).value  # Ряд
    position_num = ws2.cell(row=row_idx, column=2).value  # Номер
    photo_id = ws2.cell(row=row_idx, column=4).value  # Фото (як число або текст)
    comment = ws2.cell(row=row_idx, column=5).value  # Коментар
    rating = ws2.cell(row=row_idx, column=6).value  # Оцінка
    
    # Формировать запись
    plant_id = len(grapes_data) + 1  # ID від 1
    category = "Виноград"
    location = f"Ряд {int(row_num) if row_num is not None else 0}, Номер {int(position_num) if position_num is not None else 0}"
    
    # CSV: id,category,name,location,row,position,rating,comment,photo_id
    grapes_data.append({
        'id': plant_id,
        'category': category,
        'name': str(name),
        'location': location,
        'row': int(row_num) if row_num is not None else 0,
        'position': int(position_num) if position_num is not None else 0,
        'rating': float(rating) if rating is not None else 0,
        'comment': str(comment) if comment else "",
        'photo_id': str(photo_id) if photo_id is not None else ""
    })

print(f"Обработано записей Винограду: {len(grapes_data)}")

# === ЗАПИСЬ CSV ФАЙЛОВ ===

# Запис plants.csv з усіма категоріями (крім Винограду, який в grapes.csv)
print("\n\nЗаписання plants.csv...")
with open(output_plants_path, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['id', 'category', 'name', 'location', 'row', 'position', 'rating', 'comment', 'photo_id'])
    
    plant_id = 1
    for category in sorted(plants_by_category.keys()):
        if category == "Виноград":
            continue  # Виноград буде у грapes.csv
        
        varieties = plants_by_category[category]
        for variety in varieties:
            # Для культур з Лист1: не маємо row/position, тому -1
            writer.writerow([
                plant_id,
                category,
                variety,
                "",  # location
                -1,  # row
                -1,  # position
                0,  # rating
                "",  # comment
                ""  # photo_id
            ])
            plant_id += 1

print(f"Записано {plant_id - 1} записів у plants.csv")

# Запис grapes.csv для Винограду
print("Запис grapes.csv...")
with open(output_grapes_path, 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['id', 'category', 'name', 'location', 'row', 'position', 'rating', 'comment', 'photo_id'])
    
    for grape in grapes_data:
        writer.writerow([
            grape['id'],
            grape['category'],
            grape['name'],
            grape['location'],
            grape['row'],
            grape['position'],
            grape['rating'],
            grape['comment'],
            grape['photo_id']
        ])

print(f"Записано {len(grapes_data)} записів у grapes.csv")

print("\n✅ Конвертація завершена!")
print(f"   - plants.csv: {plant_id - 1} записів")
print(f"   - grapes.csv: {len(grapes_data)} записів")
print(f"   - Всього категорій: {len(plants_by_category)}")

