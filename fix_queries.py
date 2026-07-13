import os
import re

src_dir = r'C:\Users\sabas\Documentos\ecommerce-platform\src\main\java\com\ecommerce\modules'

# Old names and their capitalized Spanish equivalents for method names
replacements = {
    'CompareAtPrice': 'PrecioComparacion',
    'CostPrice': 'PrecioCosto',
    'Barcode': 'CodigoBarras',
    'InventoryTrackEnabled': 'RastreoInventarioHabilitado',
    'AllowBackorder': 'PermitirReserva',
    'BannerAltText': 'TextoAlternativoBanner',
    'BannerUrl': 'UrlBanner',
    'BillingAddress': 'DireccionFacturacion',
    'CategoryName': 'NombreCategoria',
    'CommissionRate': 'TasaComision',
    'ConfirmPassword': 'ConfirmarContrasena',
    'CustomerEmail': 'CorreoCliente',
    'CustomerId': 'IdCliente',
    'CustomerName': 'NombreCliente',
    'EndDate': 'FechaFin',
    'ErrorMessage': 'MensajeError',
    'EventId': 'IdEvento',
    'EventType': 'TipoEvento',
    'ExpiresAt': 'ExpiraEn',
    'ExternalId': 'IdExterno',
    'FullName': 'NombreCompleto',
    'LogoAltText': 'TextoAlternativoLogo',
    'LogoUrl': 'UrlLogo',
    'NewStatus': 'NuevoEstado',
    'NotificationType': 'TipoNotificacion',
    'OccurredAt': 'OcurrioEn',
    'OldStatus': 'EstadoAnterior',
    'OrderNumber': 'NumeroOrden',
    'OwnerId': 'IdPropietario',
    'ParentId': 'IdPadre',
    'PasswordHash': 'HashContrasena',
    'PaymentMethod': 'MetodoPago',
    'PlanId': 'IdPlan',
    'PlanType': 'TipoPlan',
    'PreviousStatus': 'EstadoPrevio',
    'ProcessedAt': 'ProcesadoEn',
    'RecipientEmail': 'CorreoDestinatario',
    'SentAt': 'EnviadoEn',
    'ShippingAddress': 'DireccionEnvio',
    'ShippingAmount': 'MontoEnvio',
    'StartDate': 'FechaInicio',
    'StatusHistory': 'HistorialEstados',
    'TaxAmount': 'MontoImpuesto',
    'ZipCode': 'CodigoPostal',
}

files_to_check = []
for root, _, files in os.walk(src_dir):
    for f in files:
        if f.endswith("Repository.java") or f.endswith("UseCase.java") or f.endswith("Service.java") or f.endswith("Controller.java"):
            files_to_check.append(os.path.join(root, f))

changed_files = 0
for filepath in files_to_check:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for eng, spa in replacements.items():
        new_content = re.sub(r'\b(findBy|existsBy|findAllBy|countBy|deleteBy|getBy)([A-Za-z0-9_]*)' + eng + r'\b', r'\1\2' + spa, new_content)
        new_content = re.sub(r'\b(findBy|existsBy|findAllBy|countBy|deleteBy|getBy)' + eng + r'([A-Za-z0-9_]*)\b', r'\1' + spa + r'\2', new_content)
        # also inside the string just in case, but safe
        # wait, regex needs to find the exact substring inside method names like findAllByCustomerId
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        changed_files += 1

print(f"Updated {changed_files} files with Repository query fixes.")
