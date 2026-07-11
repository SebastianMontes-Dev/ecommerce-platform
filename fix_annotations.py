import os
import re

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    replacements = {
        r'@Column\s*\(\s*nombre\s*=': '@Column(name =',
        r'@JoinColumn\s*\(\s*nombre\s*=': '@JoinColumn(name =',
        r'@AttributeOverride\s*\(\s*nombre\s*=': '@AttributeOverride(name =',
        r'@Table\s*\(\s*nombre\s*=': '@Table(name =',
        r'@Tag\s*\(\s*nombre\s*=': '@Tag(name =',
        r'descripcion\s*=': 'description =',
        r'@CollectionTable\s*\(\s*nombre\s*=': '@CollectionTable(name =',
        r'@OpenAPIDefinition\s*\(\s*info\s*=\s*@Info\s*\(\s*nombre\s*=': '@OpenAPIDefinition(info = @Info(name =',
        r'@FilterDef\s*\(\s*nombre\s*=': '@FilterDef(name =',
        r'@ParamDef\s*\(\s*nombre\s*=': '@ParamDef(name =',
        r'@Filter\s*\(\s*nombre\s*=': '@Filter(name =',
        r'correo\s*=\s*': 'email = ',
        r'crypto\.contrasena\.': 'crypto.password.',
        r'@Tag\s*\(\s*name\s*=': '@Tag(name =' # already fixed but just in case
    }

    new_content = content
    for old, new in replacements.items():
        new_content = re.sub(old, new, new_content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Fixed annotations: {file_path}")

def main():
    for root_dir in ['src/main/java', 'src/test/java']:
        for subdir, dirs, files in os.walk(root_dir):
            for file in files:
                if file.endswith('.java'):
                    process_file(os.path.join(subdir, file))

if __name__ == "__main__":
    main()
