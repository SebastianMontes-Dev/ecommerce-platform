import os
import re

src_dir = r'C:\Users\sabas\Documentos\ecommerce-platform\src\main\java\com\ecommerce\modules'

replacements = {
    r'\bgetCategoryId\b': 'getIdCategoria',
    r'\bsetCategoryId\b': 'setIdCategoria',
    r'\bcategoryId\b': 'idCategoria',
    
    r'\bgetProductName\b': 'getNombreProducto',
    r'\bsetProductName\b': 'setNombreProducto',
    r'\bproductName\b': 'nombreProducto',
    
    r'\bgetDescription\b': 'getDescripcion',
    r'\bsetDescription\b': 'setDescripcion',
    r'\bdescription\b': 'descripcion',
    
    r'\bgetName\b': 'getNombre',
    r'\bsetName\b': 'setNombre',
    r'\bname\b': 'nombre',

    r'\bgetAmount\b': 'getMonto',
    r'\bsetAmount\b': 'setMonto',

    r'\bgetCurrency\b': 'getMoneda',
    r'\bsetCurrency\b': 'setMoneda',

    r'\bgetPrice\b': 'getPrecio',
    r'\bsetPrice\b': 'setPrecio',
    r'\bprice\b': 'precio',

    r'\bgetInventory\b': 'getInventario',
    r'\bsetInventory\b': 'setInventario',
    r'\binventory\b': 'inventario',
    
    r'\bgetStatus\b': 'getEstado',
    r'\bsetStatus\b': 'setEstado',
    r'\bstatus\b': 'estado',

    r'\bgetBarcode\b': 'getCodigoBarras',
    r'\bsetBarcode\b': 'setCodigoBarras',
    r'\bbarcode\b': 'codigoBarras',
}

files_to_check = []
for root, _, files in os.walk(src_dir):
    for f in files:
        if f.endswith(".java"):
            files_to_check.append(os.path.join(root, f))

changed_files = 0
for filepath in files_to_check:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for pattern, repl in replacements.items():
        new_content = re.sub(pattern, repl, new_content)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        changed_files += 1

print(f"Updated {changed_files} files.")
