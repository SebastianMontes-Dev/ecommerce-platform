import os
import re

directory = r'C:\Users\sabas\Documentos\ecommerce-platform\src\main\java\com\ecommerce\modules'
fields = set()

for root, _, files in os.walk(directory):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
                # Find all field declarations: private/protected/public [Type] [fieldName];
                # This is a very naive regex but should catch most
                matches = re.findall(r'(?:private|protected|public)\s+(?:final\s+)?(?:static\s+)?([A-Z][a-zA-Z0-9_<>, \?]*\s+)?([a-z][a-zA-Z0-9_]*)(?:\s*=\s*[^;]+)?\s*;', content)
                for match in matches:
                    field_name = match[1]
                    fields.add(field_name)

with open('fields.txt', 'w', encoding='utf-8') as f:
    for field in sorted(fields):
        f.write(field + '\n')
