import os
import re

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    replacements = {
        r'Enum::nombre': 'Enum::name',
        r'getEstado\(\)\.nombre\(\)': 'getEstado().name()',
        r'rol\.nombre\(\)': 'rol.name()',
        r'ResponseEntity\.estado\(': 'ResponseEntity.status(',
        r'this\.email = user\.getCorreo\(\);': 'this.correo = user.getCorreo();',
        r'public String getContrasena\(\)': 'public String getPassword()',
        r'this\.description = descripcion;': 'this.descripcion = descripcion;',
        r'this\.email = correo;': 'this.correo = correo;',
        r'this\.firstName = nombre;': 'this.nombre = nombre;',
        r'this\.lastName = apellido;': 'this.apellido = apellido;'
    }

    new_content = content
    for old, new in replacements.items():
        new_content = re.sub(old, new, new_content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed compilation: {file_path}")

def main():
    for root_dir in ['src/main/java', 'src/test/java']:
        for subdir, dirs, files in os.walk(root_dir):
            for file in files:
                if file.endswith('.java'):
                    process_file(os.path.join(subdir, file))

if __name__ == "__main__":
    main()
