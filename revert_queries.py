import os

def replace_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    new_content = content.replace("ByIdCliente", "ByCustomerId")

    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk("src/main/java"):
    for file in files:
        if file.endswith(".java"):
            replace_in_file(os.path.join(root, file))
