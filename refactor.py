import os
import re

def process_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Dictionary of replacements (Old -> New)
    # We must be careful to match exact words to not break package names or class names.
    replacements = {
        # Fields and Variables
        r'\btenantId\b': 'idTienda',
        r'\bcategoryId\b': 'idCategoria',
        r'\bproductId\b': 'idProducto',
        r'\borderId\b': 'idOrden',
        r'\bname\b': 'nombre',
        r'\bdescription\b': 'descripcion',
        r'\bprice\b': 'precio',
        r'\binventory\b': 'inventario',
        r'\bimageUrl\b': 'urlImagen',
        r'\bstatus\b': 'estado',
        r'\bfirstName\b': 'nombre',
        r'\blastName\b': 'apellido',
        r'\bemail\b': 'correo',
        r'\bpassword\b': 'contrasena',
        r'\brole\b': 'rol',
        r'\bcreatedAt\b': 'creadoEn',
        r'\bupdatedAt\b': 'actualizadoEn',
        r'\bquantity\b': 'cantidad',
        r'\bsubtotal\b': 'subtotal',
        r'\btax\b': 'impuesto',
        r'\bshippingFee\b': 'costoEnvio',
        r'\btotal\b': 'total',
        r'\baverageRating\b': 'calificacionPromedio',
        r'\breviewCount\b': 'conteoResenas',
        r'\bclientReferenceId\b': 'idReferenciaCliente',
        
        # Getters and Setters
        r'\bgetTenantId\b': 'getIdTienda',
        r'\bsetTenantId\b': 'setIdTienda',
        r'\bgetCategoryId\b': 'getIdCategoria',
        r'\bsetCategoryId\b': 'setIdCategoria',
        r'\bgetProductId\b': 'getIdProducto',
        r'\bsetProductId\b': 'setIdProducto',
        r'\bgetOrderId\b': 'getIdOrden',
        r'\bsetOrderId\b': 'setIdOrden',
        r'\bgetName\b': 'getNombre',
        r'\bsetName\b': 'setNombre',
        r'\bgetDescription\b': 'getDescripcion',
        r'\bsetDescription\b': 'setDescripcion',
        r'\bgetPrice\b': 'getPrecio',
        r'\bsetPrice\b': 'setPrecio',
        r'\bgetInventory\b': 'getInventario',
        r'\bsetInventory\b': 'setInventario',
        r'\bgetImageUrl\b': 'getUrlImagen',
        r'\bsetImageUrl\b': 'setUrlImagen',
        r'\bgetStatus\b': 'getEstado',
        r'\bsetStatus\b': 'setEstado',
        r'\bgetFirstName\b': 'getNombre',
        r'\bsetFirstName\b': 'setNombre',
        r'\bgetLastName\b': 'getApellido',
        r'\bsetLastName\b': 'setApellido',
        r'\bgetEmail\b': 'getCorreo',
        r'\bsetEmail\b': 'setCorreo',
        r'\bgetPassword\b': 'getContrasena',
        r'\bsetPassword\b': 'setContrasena',
        r'\bgetRole\b': 'getRol',
        r'\bsetRole\b': 'setRol',
        r'\bgetCreatedAt\b': 'getCreadoEn',
        r'\bsetCreatedAt\b': 'setCreadoEn',
        r'\bgetUpdatedAt\b': 'getActualizadoEn',
        r'\bsetUpdatedAt\b': 'setActualizadoEn',
        r'\bgetQuantity\b': 'getCantidad',
        r'\bsetQuantity\b': 'setCantidad',
        r'\bgetSubtotal\b': 'getSubtotal',
        r'\bsetSubtotal\b': 'setSubtotal',
        r'\bgetTax\b': 'getImpuesto',
        r'\bsetTax\b': 'setImpuesto',
        r'\bgetShippingFee\b': 'getCostoEnvio',
        r'\bsetShippingFee\b': 'setCostoEnvio',
        r'\bgetTotal\b': 'getTotal',
        r'\bsetTotal\b': 'setTotal',
        r'\bgetAverageRating\b': 'getCalificacionPromedio',
        r'\bsetAverageRating\b': 'setCalificacionPromedio',
        r'\bgetReviewCount\b': 'getConteoResenas',
        r'\bsetReviewCount\b': 'setConteoResenas',
        
        # Spring Data JPA Finders
        r'\bfindByName\b': 'findByNombre',
        r'\bfindByEmail\b': 'findByCorreo',
        r'\bfindByTenantId\b': 'findByIdTienda',
        r'\bfindByOrderId\b': 'findByIdOrden',
        r'\bcountByTenantId\b': 'countByIdTienda'
    }

    new_content = content
    for old, new in replacements.items():
        new_content = re.sub(old, new, new_content)

    if new_content != content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated: {file_path}")

def main():
    root_dir = 'src/main/resources'
    for subdir, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.sql'):
                process_file(os.path.join(subdir, file))

if __name__ == "__main__":
    main()
