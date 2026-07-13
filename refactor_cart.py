import os
import re

cart_dir = r"C:\Users\sabas\Documentos\ecommerce-platform\src\main\java\com\ecommerce\modules\cart"
test_script = r"C:\Users\sabas\Documentos\ecommerce-platform\e2e_test.py"

replacements = {
    # fields
    r'\bproductName\b': 'nombreProducto',
    r'\bvariantId\b': 'idVariante',
    r'\bvariantName\b': 'nombreVariante',
    r'\bunitPrice\b': 'precioUnitario',
    r'\bcurrency\b': 'moneda',
    r'\bitems\b': 'articulos',
    r'\bnewItem\b': 'nuevoArticulo',
    
    # methods
    r'\bgetItems\b': 'getArticulos',
    r'\bsetItems\b': 'setArticulos',
    r'\baddItem\b': 'agregarArticulo',
    r'\bremoveItem\b': 'eliminarArticulo',
    r'\bupdateQuantity\b': 'actualizarCantidad',
    r'\bgetItemCount\b': 'obtenerConteoArticulos',
    r'\bgetDistinctItemCount\b': 'obtenerConteoDistinto',
    r'\bgetTotal\b': 'obtenerTotal',
    r'\bisEmpty\b': 'estaVacio',
    r'\bfindItem\b': 'encontrarArticulo',
    r'\bclear\b': 'limpiar',
    r'\bgetSubtotal\b': 'obtenerSubtotal',
    r'\bgetProductName\b': 'getNombreProducto',
    r'\bsetProductName\b': 'setNombreProducto',
    r'\bgetVariantId\b': 'getIdVariante',
    r'\bsetVariantId\b': 'setIdVariante',
    r'\bgetVariantName\b': 'getNombreVariante',
    r'\bsetVariantName\b': 'setNombreVariante',
    r'\bgetUnitPrice\b': 'getPrecioUnitario',
    r'\bsetUnitPrice\b': 'setPrecioUnitario',
    r'\bgetCurrency\b': 'getMoneda',
    r'\bsetCurrency\b': 'setMoneda',
}

files_to_check = []
for root, _, files in os.walk(cart_dir):
    for f in files:
        if f.endswith(".java"):
            files_to_check.append(os.path.join(root, f))
files_to_check.append(test_script)

for filepath in files_to_check:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for pattern, repl in replacements.items():
        new_content = re.sub(pattern, repl, new_content)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")
