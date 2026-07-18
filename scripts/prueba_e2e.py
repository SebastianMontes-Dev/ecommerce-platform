import requests
import json
import time
import uuid

BASE_URL = 'http://localhost:8081/api/v1'

def print_result(step, response):
    print(f"\n--- {step} ---")
    print(f"Status: {response.status_code}")
    try:
        print(json.dumps(response.json(), indent=2))
    except:
        print(response.text)

# 1. Register User (using random email to avoid duplicate)
uid = str(uuid.uuid4())[:8]
payload_register = {
    "correo": f"testuser_{uid}@example.com",
    "contrasena": "password123",
    "confirmarContrasena": "password123",
    "nombre": "Test",
    "apellido": "User"
}
res = requests.post(f"{BASE_URL}/auth/registro", json=payload_register)
print_result("Register User", res)

# 2. Login
payload_login = {
    "correo": f"testuser_{uid}@example.com",
    "contrasena": "password123"
}
res = requests.post(f"{BASE_URL}/auth/login", json=payload_login)
print_result("Login User", res)
token = res.json().get('accessToken')

if not token:
    print("Login failed, aborting tests.")
    exit(1)

headers = {
    "Authorization": f"Bearer {token}",
    "Content-Type": "application/json"
}

# 3. Create Tenant
payload_tenant = {
    "nombre": f"Test Store {uid}",
    "enlaceCorto": f"test-store-{uid}"
}
res = requests.post(f"{BASE_URL}/inquilinos", json=payload_tenant, headers=headers)
print_result("Create Tenant", res)

# Get tenant details
res = requests.get(f"{BASE_URL}/inquilinos/yo", headers=headers)
print_result("Get My Tenant", res)
tenant = res.json()
if 'id' not in tenant:
    print("No tenant id found, aborting tests.")
    exit(1)
tenant_id = tenant['id']

headers_with_tenant = headers.copy()
headers_with_tenant["X-Inquilino-ID"] = tenant_id

# 4. Create Category
payload_category = {
    "nombre": f"Electronics {uid}",
    "enlaceCorto": f"electronics-{uid}",
    "descripcion": "Gadgets and tech"
}
res = requests.post(f"{BASE_URL}/catalogo/categorias", json=payload_category, headers=headers_with_tenant)
print_result("Create Category", res)
try:
    category_id = res.json().get('id')
except:
    category_id = None

# 5. Create Product
if category_id:
    payload_product = {
        "nombre": f"Test Laptop {uid}",
        "enlaceCorto": f"test-laptop-{uid}",
        "descripcion": "A powerful test laptop",
        "precio": 999.99,
        "sku": f"LAP123-{uid}",
        "inventario": 10,
        "idCategoria": category_id
    }
    res = requests.post(f"{BASE_URL}/catalogo/productos", json=payload_product, headers=headers_with_tenant)
    print_result("Create Product", res)
    try:
        product_id = res.json().get('id')
    except:
        product_id = None
else:
    product_id = None

# 6. Add to Cart
if product_id:
    payload_cart = {
        "idProducto": product_id,
        "cantidad": 1
    }
    res = requests.post(f"{BASE_URL}/carrito/articulos", json=payload_cart, headers=headers_with_tenant)
    print_result("Add to Cart", res)

    # 7. Get Cart
    res = requests.get(f"{BASE_URL}/carrito", headers=headers_with_tenant)
    print_result("Get Cart", res)

    # 8. Create Order (Checkout)
    payload_checkout = {
        "direccionEnvio": {
            "street": "123 Main St",
            "city": "Test City",
            "state": "Test State",
            "codigoPostal": "12345",
            "country": "Test Country",
            "additionalInfo": "Leave at door"
        },
        "direccionFacturacion": {
            "street": "123 Main St",
            "city": "Test City",
            "state": "Test State",
            "codigoPostal": "12345",
            "country": "Test Country",
            "additionalInfo": "Same as shipping"
        },
        "notes": "Test order notes"
    }
    res = requests.post(f"{BASE_URL}/ordenes/checkout", json=payload_checkout, headers=headers_with_tenant)
    print_result("Create Order (Checkout)", res)
    try:
        order_id = res.json().get('id')
    except:
        order_id = None
        
    if order_id:
        # 9. Create Review
        payload_review = {
            "idOrden": order_id,
            "calificacion": 5,
            "titulo": "Excellent product!",
            "comentario": "Really loved it."
        }
        res = requests.post(f"{BASE_URL}/productos/{product_id}/resenas", json=payload_review, headers=headers_with_tenant)
        print_result("Create Review", res)

        # 10. Create Checkout Session (Payment)
        res = requests.post(f"{BASE_URL}/pagos/checkout/{order_id}", headers=headers_with_tenant)
        print_result("Create Payment Checkout", res)

# This is a marker