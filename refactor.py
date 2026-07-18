import os
import re

def refactor_content(content):
    # exact replacements for getters/setters/properties
    content = re.sub(r'getAmount', 'getMonto', content)
    content = re.sub(r'setAmount', 'setMonto', content)
    content = re.sub(r'\bamount\b', 'monto', content)
    content = re.sub(r'\bAmount\b', 'Monto', content)
    
    content = re.sub(r'getNotes', 'getNotas', content)
    content = re.sub(r'setNotes', 'setNotas', content)
    content = re.sub(r'\bnotes\b', 'notas', content)
    content = re.sub(r'\bNotes\b', 'Notas', content)
    
    content = re.sub(r'total_amount', 'monto_total', content)
    content = re.sub(r'subtotal_amount', 'monto_subtotal', content)
    content = re.sub(r'unit_price_amount', 'monto_precio_unitario', content)
    
    return content

if __name__ == "__main__":
    src_dir = os.path.join(os.getcwd(), 'src', 'main', 'java')
    test_dir = os.path.join(os.getcwd(), 'src', 'test', 'java')
    
    # We will modify all EXCEPT Dinero.java and Orden.java
    skip_files = ['Dinero.java', 'Orden.java']
    
    count = 0
    for d in [src_dir, test_dir]:
        for root, dirs, files in os.walk(d):
            for file in files:
                if file.endswith('.java'):
                    if file in skip_files:
                        continue
                    filepath = os.path.join(root, file)
                    try:
                        with open(filepath, 'r', encoding='utf-8') as f:
                            content = f.read()
                    except Exception:
                        continue
                    
                    new_content = refactor_content(content)
                    if new_content != content:
                        with open(filepath, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        count += 1
    print(f"Modified {count} files via python.")
