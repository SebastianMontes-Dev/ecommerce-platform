import subprocess
import re

# Build project to get errors
result = subprocess.run([".\\gradlew.bat", "compileJava"], capture_output=True, text=True)
errors = result.stderr + result.stdout

# Pattern for cannot find symbol method getX()
pattern = r'(.*?):(\d+): error: cannot find symbol\s*\n\s*.*?\n\s*\^\n\s*symbol:\s*method\s*([a-zA-Z0-9_]+)\('
matches = re.findall(pattern, errors)

fixes = {
    'getNombre': 'getNombre',
    'getSlug': 'getSlug',
    'getDescripcion': 'getDescripcion',
    'getPrecio': 'getPrecio',
    'getPrecioComparacion': 'getPrecioComparacion',
    'getPrecioCosto': 'getPrecioCosto',
    'getSku': 'getSku',
    'getBarcode': 'getBarcode',
    'getInventario': 'getInventario',
    'isRastreoInventarioHabilitado': 'isRastreoInventarioHabilitado',
    'getEstado': 'getEstado',
    'getIdCategoria': 'getIdCategoria',
}

# The errors are mostly from CreateProductUseCase.java trying to read variables from CreateProductRequest (which is translated)
# Wait, let's just create a script that maps old English method names to new Spanish ones for the DTOs and Entities.

with open('e2e_test.py', 'a') as f:
    f.write("\n# This is a marker")

