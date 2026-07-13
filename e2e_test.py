import requests
import json
import time
import uuid

BASE_URL = 'http://localhost:8080/api/v1'

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
    "confirmPassword": "password123",
    "nombre": "Test",
    "apellido": "User"
}
res = requests.post(f"{BASE_URL}/auth/register", json=payload_register)
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
    "slug": f"test-store-{uid}"
}
res = requests.post(f"{BASE_URL}/tenants", json=payload_tenant, headers=headers)
print_result("Create Tenant", res)

# Get tenant details
res = requests.get(f"{BASE_URL}/tenants/me", headers=headers)
print_result("Get My Tenant", res)
tenant = res.json()
if 'id' not in tenant:
    print("No tenant id found, aborting tests.")
    exit(1)
tenant_id = tenant['id']

headers_with_tenant = headers.copy()
headers_with_tenant["X-Tenant-ID"] = tenant_id

# 4. Create Category
payload_category = {
    "nombre": f"Electronics {uid}",
    "slug": f"electronics-{uid}",
    "descripcion": "Gadgets and tech"
}
res = requests.post(f"{BASE_URL}/catalog/categories", json=payload_category, headers=headers_with_tenant)
print_result("Create Category", res)
try:
    category_id = res.json().get('id')
except:
    category_id = None

# 5. Create Product
if category_id:
    payload_product = {
        "nombre": f"Test Laptop {uid}",
        "slug": f"test-laptop-{uid}",
        "descripcion": "A powerful test laptop",
        "precio": 999.99,
        "sku": f"LAP123-{uid}",
        "inventario": 10,
        "idCategoria": category_id
    }
    res = requests.post(f"{BASE_URL}/catalog/products", json=payload_product, headers=headers_with_tenant)
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
    res = requests.post(f"{BASE_URL}/cart/items", json=payload_cart, headers=headers_with_tenant)
    print_result("Add to Cart", res)

    # 7. Get Cart
    res = requests.get(f"{BASE_URL}/cart", headers=headers_with_tenant)
    print_result("Get Cart", res)

    # 8. Create Review
    payload_review = {
        "rating": 5,
        "titulo": "Excellent product!",
        "comentario": "Really loved it."
    }
    res = requests.post(f"{BASE_URL}/products/{product_id}/reviews", json=payload_review, headers=headers_with_tenant)
    print_result("Create Review", res)

    # 9. Create Checkout Session (with fake order id)
    fake_order_id = str(uuid.uuid4())
    res = requests.post(f"{BASE_URL}/payments/checkout/{fake_order_id}", headers=headers_with_tenant)
    print_result("Create Checkout", res)
