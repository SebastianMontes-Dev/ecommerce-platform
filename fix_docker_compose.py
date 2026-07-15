
with open(r'docker\docker-compose.yml', 'r') as f:
    content = f.read()

services_addition = '''
  mysql:
    image: mysql:8
    container_name: sql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: my_database
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - ecommerce-net

  pgadmin:
    image: dpage/pgadmin4
    container_name: pgadmin4
    environment:
      PGADMIN_DEFAULT_EMAIL: admin@admin.com
      PGADMIN_DEFAULT_PASSWORD: admin
    ports:
      - "5050:80"
    networks:
      - ecommerce-net
'''

idx = content.find('\nvolumes:\n')
if idx != -1:
    content = content[:idx] + services_addition + content[idx:]
    content = content.replace('  es_data:', '  es_data:\n  mysql_data:')

with open('docker-compose.yml', 'w') as f:
    f.write(content)

