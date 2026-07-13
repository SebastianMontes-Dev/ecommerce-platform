import os
import re

directory = r'C:\Users\sabas\Documentos\ecommerce-platform'
src_dir = os.path.join(directory, 'src', 'main', 'java', 'com', 'ecommerce', 'modules')
flyway_dir = os.path.join(directory, 'src', 'main', 'resources', 'db', 'migration')
test_script = os.path.join(directory, 'e2e_test.py')

# Precise replacements to avoid hitting SQL keywords or core Spring types randomly
replacements = {
    # fields -> spanish
    r'\bcompareAtPrice\b': 'precioComparacion',
    r'\bcostPrice\b': 'precioCosto',
    r'\bbarcode\b': 'codigoBarras',
    r'\binventoryTrackEnabled\b': 'rastreoInventarioHabilitado',
    r'\ballowBackorder\b': 'permitirReserva',
    r'\bbannerAltText\b': 'textoAlternativoBanner',
    r'\bbannerUrl\b': 'urlBanner',
    r'\bbillingAddress\b': 'direccionFacturacion',
    r'\bcategoryName\b': 'nombreCategoria',
    r'\bcommissionRate\b': 'tasaComision',
    r'\bconfirmPassword\b': 'confirmarContrasena',
    r'\bcustomerEmail\b': 'correoCliente',
    r'\bcustomerId\b': 'idCliente',
    r'\bcustomerName\b': 'nombreCliente',
    r'\bendDate\b': 'fechaFin',
    r'\berrorMessage\b': 'mensajeError',
    r'\beventId\b': 'idEvento',
    r'\beventType\b': 'tipoEvento',
    r'\bexpiresAt\b': 'expiraEn',
    r'\bexternalId\b': 'idExterno',
    r'\bfullName\b': 'nombreCompleto',
    r'\blogoAltText\b': 'textoAlternativoLogo',
    r'\blogoUrl\b': 'urlLogo',
    r'\bnewStatus\b': 'nuevoEstado',
    r'\bnotificationType\b': 'tipoNotificacion',
    r'\boccurredAt\b': 'ocurrioEn',
    r'\boldStatus\b': 'estadoAnterior',
    r'\borderNumber\b': 'numeroOrden',
    r'\bownerId\b': 'idPropietario',
    r'\bparentId\b': 'idPadre',
    r'\bpasswordHash\b': 'hashContrasena',
    r'\bpaymentMethod\b': 'metodoPago',
    r'\bplanId\b': 'idPlan',
    r'\bplanType\b': 'tipoPlan',
    r'\bpreviousStatus\b': 'estadoPrevio',
    r'\bprocessedAt\b': 'procesadoEn',
    r'\brecipientEmail\b': 'correoDestinatario',
    r'\bsentAt\b': 'enviadoEn',
    r'\bshippingAddress\b': 'direccionEnvio',
    r'\bshippingAmount\b': 'montoEnvio',
    r'\bstartDate\b': 'fechaInicio',
    r'\bstatusHistory\b': 'historialEstados',
    r'\btaxAmount\b': 'montoImpuesto',
    r'\bzipCode\b': 'codigoPostal',
    
    # getters/setters mapped
    r'\bgetCompareAtPrice\b': 'getPrecioComparacion',
    r'\bsetCompareAtPrice\b': 'setPrecioComparacion',
    r'\bgetCostPrice\b': 'getPrecioCosto',
    r'\bsetCostPrice\b': 'setPrecioCosto',
    r'\bgetBarcode\b': 'getCodigoBarras',
    r'\bsetBarcode\b': 'setCodigoBarras',
    r'\bisInventoryTrackEnabled\b': 'isRastreoInventarioHabilitado',
    r'\bsetInventoryTrackEnabled\b': 'setRastreoInventarioHabilitado',
    r'\bisAllowBackorder\b': 'isPermitirReserva',
    r'\bsetAllowBackorder\b': 'setPermitirReserva',
    r'\bgetBannerAltText\b': 'getTextoAlternativoBanner',
    r'\bsetBannerAltText\b': 'setTextoAlternativoBanner',
    r'\bgetBannerUrl\b': 'getUrlBanner',
    r'\bsetBannerUrl\b': 'setUrlBanner',
    r'\bgetBillingAddress\b': 'getDireccionFacturacion',
    r'\bsetBillingAddress\b': 'setDireccionFacturacion',
    r'\bgetCategoryName\b': 'getNombreCategoria',
    r'\bsetCategoryName\b': 'setNombreCategoria',
    r'\bgetCommissionRate\b': 'getTasaComision',
    r'\bsetCommissionRate\b': 'setTasaComision',
    r'\bgetConfirmPassword\b': 'getConfirmarContrasena',
    r'\bsetConfirmPassword\b': 'setConfirmarContrasena',
    r'\bgetCustomerEmail\b': 'getCorreoCliente',
    r'\bsetCustomerEmail\b': 'setCorreoCliente',
    r'\bgetCustomerId\b': 'getIdCliente',
    r'\bsetCustomerId\b': 'setIdCliente',
    r'\bgetCustomerName\b': 'getNombreCliente',
    r'\bsetCustomerName\b': 'setNombreCliente',
    r'\bgetEndDate\b': 'getFechaFin',
    r'\bsetEndDate\b': 'setFechaFin',
    r'\bgetErrorMessage\b': 'getMensajeError',
    r'\bsetErrorMessage\b': 'setMensajeError',
    r'\bgetEventId\b': 'getIdEvento',
    r'\bsetEventId\b': 'setIdEvento',
    r'\bgetEventType\b': 'getTipoEvento',
    r'\bsetEventType\b': 'setTipoEvento',
    r'\bgetExpiresAt\b': 'getExpiraEn',
    r'\bsetExpiresAt\b': 'setExpiraEn',
    r'\bgetExternalId\b': 'getIdExterno',
    r'\bsetExternalId\b': 'setIdExterno',
    r'\bgetFullName\b': 'getNombreCompleto',
    r'\bsetFullName\b': 'setNombreCompleto',
    r'\bgetLogoAltText\b': 'getTextoAlternativoLogo',
    r'\bsetLogoAltText\b': 'setTextoAlternativoLogo',
    r'\bgetLogoUrl\b': 'getUrlLogo',
    r'\bsetLogoUrl\b': 'setUrlLogo',
    r'\bgetNewStatus\b': 'getNuevoEstado',
    r'\bsetNewStatus\b': 'setNuevoEstado',
    r'\bgetNotificationType\b': 'getTipoNotificacion',
    r'\bsetNotificationType\b': 'setTipoNotificacion',
    r'\bgetOccurredAt\b': 'getOcurrioEn',
    r'\bsetOccurredAt\b': 'setOcurrioEn',
    r'\bgetOldStatus\b': 'getEstadoAnterior',
    r'\bsetOldStatus\b': 'setEstadoAnterior',
    r'\bgetOrderNumber\b': 'getNumeroOrden',
    r'\bsetOrderNumber\b': 'setNumeroOrden',
    r'\bgetOwnerId\b': 'getIdPropietario',
    r'\bsetOwnerId\b': 'setIdPropietario',
    r'\bgetParentId\b': 'getIdPadre',
    r'\bsetParentId\b': 'setIdPadre',
    r'\bgetPasswordHash\b': 'getHashContrasena',
    r'\bsetPasswordHash\b': 'setHashContrasena',
    r'\bgetPaymentMethod\b': 'getMetodoPago',
    r'\bsetPaymentMethod\b': 'setMetodoPago',
    r'\bgetPlanId\b': 'getIdPlan',
    r'\bsetPlanId\b': 'setIdPlan',
    r'\bgetPlanType\b': 'getTipoPlan',
    r'\bsetPlanType\b': 'setTipoPlan',
    r'\bgetPreviousStatus\b': 'getEstadoPrevio',
    r'\bsetPreviousStatus\b': 'setEstadoPrevio',
    r'\bgetProcessedAt\b': 'getProcesadoEn',
    r'\bsetProcessedAt\b': 'setProcesadoEn',
    r'\bgetRecipientEmail\b': 'getCorreoDestinatario',
    r'\bsetRecipientEmail\b': 'setCorreoDestinatario',
    r'\bgetSentAt\b': 'getEnviadoEn',
    r'\bsetSentAt\b': 'setEnviadoEn',
    r'\bgetShippingAddress\b': 'getDireccionEnvio',
    r'\bsetShippingAddress\b': 'setDireccionEnvio',
    r'\bgetShippingAmount\b': 'getMontoEnvio',
    r'\bsetShippingAmount\b': 'setMontoEnvio',
    r'\bgetStartDate\b': 'getFechaInicio',
    r'\bsetStartDate\b': 'setFechaInicio',
    r'\bgetStatusHistory\b': 'getHistorialEstados',
    r'\bsetStatusHistory\b': 'setHistorialEstados',
    r'\bgetTaxAmount\b': 'getMontoImpuesto',
    r'\bsetTaxAmount\b': 'setMontoImpuesto',
    r'\bgetZipCode\b': 'getCodigoPostal',
    r'\bsetZipCode\b': 'setCodigoPostal',

    # Also update Flyway names if needed, but Flyway is tricky since changing columns means dropping them if they were already created.
    # However, since we are doing a full refactor, we can recreate the DB schema. I'll include flyway column renames:
    r'\bcompare_at_amount\b': 'monto_comparacion',
    r'\bcompare_at_currency\b': 'moneda_comparacion',
    r'\bcost_amount\b': 'monto_costo',
    r'\bcost_currency\b': 'moneda_costo',
    r'\binventory_track_enabled\b': 'rastreo_inventario_habilitado',
    r'\ballow_backorder\b': 'permitir_reserva',
    r'\bbanner_alt_text\b': 'texto_alternativo_banner',
    r'\bbanner_url\b': 'url_banner',
    r'\bcategory_name\b': 'nombre_categoria',
    r'\bcommission_rate\b': 'tasa_comision',
    r'\bcustomer_email\b': 'correo_cliente',
    r'\bcustomer_id\b': 'id_cliente',
    r'\bcustomer_name\b': 'nombre_cliente',
    r'\bend_date\b': 'fecha_fin',
    r'\berror_message\b': 'mensaje_error',
    r'\bevent_id\b': 'id_evento',
    r'\bevent_type\b': 'tipo_evento',
    r'\bexpires_at\b': 'expira_en',
    r'\bexternal_id\b': 'id_externo',
    r'\bfull_name\b': 'nombre_completo',
    r'\blogo_alt_text\b': 'texto_alternativo_logo',
    r'\blogo_url\b': 'url_logo',
    r'\bnew_status\b': 'nuevo_estado',
    r'\bnotification_type\b': 'tipo_notificacion',
    r'\boccurred_at\b': 'ocurrio_en',
    r'\bold_status\b': 'estado_anterior',
    r'\border_number\b': 'numero_orden',
    r'\bowner_id\b': 'id_propietario',
    r'\bparent_id\b': 'id_padre',
    r'\bpassword_hash\b': 'hash_contrasena',
    r'\bpayment_method\b': 'metodo_pago',
    r'\bplan_id\b': 'id_plan',
    r'\bplan_type\b': 'tipo_plan',
    r'\bprevious_status\b': 'estado_previo',
    r'\bprocessed_at\b': 'procesado_en',
    r'\brecipient_email\b': 'correo_destinatario',
    r'\bsent_at\b': 'enviado_en',
    r'\bshipping_amount\b': 'monto_envio',
    r'\bstart_date\b': 'fecha_inicio',
    r'\btax_amount\b': 'monto_impuesto',
    r'\bzip_code\b': 'codigo_postal'
}

files_to_check = []
for root, _, files in os.walk(src_dir):
    for f in files:
        if f.endswith(".java"):
            files_to_check.append(os.path.join(root, f))
            
for root, _, files in os.walk(flyway_dir):
    for f in files:
        if f.endswith(".sql"):
            files_to_check.append(os.path.join(root, f))

files_to_check.append(test_script)

changed_files = 0
for filepath in files_to_check:
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for pattern, repl in replacements.items():
        new_content = re.sub(pattern, repl, new_content)
        
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        changed_files += 1

print(f"Updated {changed_files} files.")
