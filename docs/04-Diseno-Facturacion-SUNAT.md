# Diseño: Facturación Electrónica formato SUNAT (sandbox/simulado) + Devoluciones

## Lo que confirmé antes de diseñar esto

La facturación electrónica peruana usa el formato **UBL 2.1** (XML), obligatorio desde 2018. Cosas puntuales y verificadas:
- **Boleta:** serie de 4 caracteres empezando con **B** (ej. `B001`), correlativo de hasta 8 dígitos.
- **Factura:** serie de 4 caracteres empezando con **F** (ej. `F001`), mismo correlativo.
- El documento se firma digitalmente y se envía a SUNAT o a un **OSE** (Operador de Servicios Electrónicos); la respuesta es una **CDR** (Constancia de Recepción) que indica ACEPTADO o RECHAZADO.
- Existe un proyecto open-source llamado **Greenter** (`greenter.dev`, PHP) que es la referencia más usada en Perú para generar estos XML — no lo van a usar directamente (está en PHP, ustedes en Java), pero si quieren ver la estructura exacta de un XML real, su documentación es la fuente más confiable y gratuita que hay.

**Por qué esto tiene que ser simulado y no real:** enviar un comprobante real a SUNAT o a un OSE requiere estar registrado como **emisor electrónico homologado**, lo cual exige un RUC real de una empresa real. Como no la tienen, lo correcto — y lo que el profesor está pidiendo con "aplíquenlo en sandbox" — es generar el XML con la estructura y los campos reales, y simular la respuesta (el CDR), sin someterlo de verdad a SUNAT. Eso demuestra que entienden el formato sin necesitar algo que es legalmente imposible de conseguir para un proyecto universitario.

## Regla de negocio: ¿boleta o factura?

- Cliente identificado solo por **DNI**, o venta anónima → **Boleta** (serie B001).
- Cliente con **RUC** registrado (empresa) → **Factura** (serie F001).

Esto requiere que `clientes` pueda representar tanto personas (DNI) como empresas (RUC) — ya está reflejado en `schema_mysql_v2.sql` con un campo `tipo_cliente` y `ruc`/`razon_social` opcionales.

## Estructura del XML (simplificada, fiel a UBL 2.1)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="urn:oasis:names:specification:ubl:schema:xsd:Invoice-2"
         xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
         xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
    <cbc:UBLVersionID>2.1</cbc:UBLVersionID>
    <cbc:ID>B001-00000123</cbc:ID>
    <cbc:IssueDate>2026-07-07</cbc:IssueDate>
    <cbc:InvoiceTypeCode>03</cbc:InvoiceTypeCode> <!-- 01=Factura, 03=Boleta -->
    <cbc:DocumentCurrencyCode>PEN</cbc:DocumentCurrencyCode>

    <cac:AccountingSupplierParty>
        <cac:Party>
            <cac:PartyIdentification><cbc:ID>20601030013</cbc:ID></cac:PartyIdentification>
            <cac:PartyLegalEntity><cbc:RegistrationName>MINIMARKET LAREDO S.A.C.</cbc:RegistrationName></cac:PartyLegalEntity>
        </cac:Party>
    </cac:AccountingSupplierParty>

    <cac:AccountingCustomerParty>
        <cac:Party>
            <cac:PartyIdentification><cbc:ID>12345678</cbc:ID></cac:PartyIdentification>
            <cac:PartyLegalEntity><cbc:RegistrationName>JUAN PEREZ</cbc:RegistrationName></cac:PartyLegalEntity>
        </cac:Party>
    </cac:AccountingCustomerParty>

    <cac:TaxTotal>
        <cbc:TaxAmount currencyID="PEN">3.81</cbc:TaxAmount>
        <cac:TaxSubtotal>
            <cbc:TaxableAmount currencyID="PEN">21.19</cbc:TaxableAmount>
            <cbc:TaxAmount currencyID="PEN">3.81</cbc:TaxAmount>
            <cac:TaxCategory><cac:TaxScheme><cbc:ID>1000</cbc:ID><cbc:Name>IGV</cbc:Name></cac:TaxScheme></cac:TaxCategory>
        </cac:TaxSubtotal>
    </cac:TaxTotal>

    <cac:LegalMonetaryTotal>
        <cbc:PayableAmount currencyID="PEN">25.00</cbc:PayableAmount>
    </cac:LegalMonetaryTotal>

    <cac:InvoiceLine>
        <cbc:ID>1</cbc:ID>
        <cbc:InvoicedQuantity unitCode="NIU">1</cbc:InvoicedQuantity>
        <cbc:LineExtensionAmount currencyID="PEN">21.19</cbc:LineExtensionAmount>
        <cac:Item><cbc:Description>Arroz Costeño 5kg</cbc:Description></cac:Item>
        <cac:Price><cbc:PriceAmount currencyID="PEN">21.19</cbc:PriceAmount></cac:Price>
    </cac:InvoiceLine>
</Invoice>
```

Esto **no es exhaustivo** (un XML real de producción tiene más campos obligatorios: firma digital XAdES, datos de ubigeo completos, catálogos SUNAT específicos para unidades y tipos de tributo, etc.) — pero cubre la estructura central, que es lo que hace que se vea como un documento real y no como una factura inventada con campos propios. Si quieren profundizar más, la documentación de Greenter es la referencia más clara y gratuita.

## El QR del comprobante

Los comprobantes electrónicos peruanos llevan un **código QR** con datos clave del comprobante (RUC del emisor, tipo de documento, serie-número, fecha de emisión, tipo y número de documento del cliente, importe total, moneda). Generen ese mismo QR con ZXing y agréguenlo al PDF — es barato de hacer y es exactamente el tipo de detalle que hace que un comprobante se vea profesional y no genérico.

## CDR simulado

En vez de enviar el XML a un OSE real, generen una respuesta simulada con la misma forma que tendría un CDR real:

```xml
<ApplicationResponse>
    <cbc:ResponseCode>0</cbc:ResponseCode> <!-- 0 = Aceptado -->
    <cbc:Description>La Boleta numero B001-00000123, ha sido aceptada (SIMULADO)</cbc:Description>
</ApplicationResponse>
```

Guarden este CDR simulado junto al comprobante (columna `cdr_simulado` en la tabla `comprobantes`) para poder mostrarlo en la demo como "respuesta del OSE".

---

## Devoluciones — módulo separado de la anulación

La anulación (FR-019B) es para el mismo día, todo o nada. Una **devolución** real de negocio es distinta: puede pasar días después, puede ser parcial (solo algunas líneas o cantidades), y no borra la venta original — la referencia y la ajusta.

**Regla de ventana de tiempo (propuesta, ya que no estaba definida en el documento original):** se acepta devolución hasta **7 días naturales** después de la venta. Es una decisión razonable para un minimarket; ajústenla si tienen un criterio distinto.

**Tipos de resolución:**
- Reembolso en efectivo
- Reembolso al mismo medio de pago (si fue Izipay, un reembolso simulado)
- Nota de crédito (saldo a favor del cliente, aplicable a compras futuras)
- Cambio por otro producto

**Efectos en el sistema (todos deben dispararse juntos, igual que una venta):**
1. Kardex: entrada por devolución (`ENTRADA_DEVOLUCION`), aumenta stock.
2. Asiento contable: reversa proporcional del asiento de ingreso (debe Ingresos por Ventas, haber Caja/CxC según medio) y del asiento de costo de venta (debe Inventario, haber Costo de Ventas) — proporcional a lo devuelto, no al total de la venta.
3. Fidelización: si el cliente ganó puntos por esa venta, se descuentan proporcionalmente.
4. Se emite una **Nota de Crédito Electrónica** que referencia el comprobante original (mismo formato UBL, con `cac:BillingReference` apuntando al comprobante original) y declara un motivo — SUNAT exige que el motivo venga de un catálogo cerrado (ej. "devolución total", "devolución por ítem", "anulación de la operación"), no como texto libre. No hace falta memorizar el catálogo completo oficial; con manejar 3-4 motivos codificados como ENUM ya se ve profesional y correcto en espíritu.

### Tablas necesarias (ya incluidas en `schema_mysql_v2.sql`)

```sql
CREATE TABLE devoluciones (
    id INT AUTO_INCREMENT PRIMARY KEY,
    venta_id INT NOT NULL,
    usuario_id INT NOT NULL,
    fecha DATETIME DEFAULT CURRENT_TIMESTAMP,
    motivo ENUM('DEVOLUCION_TOTAL','DEVOLUCION_ITEM','PRODUCTO_DEFECTUOSO','ERROR_EN_VENTA','OTRO') NOT NULL,
    tipo_resolucion ENUM('REEMBOLSO_EFECTIVO','REEMBOLSO_IZIPAY','NOTA_CREDITO','CAMBIO_PRODUCTO') NOT NULL,
    monto_total DECIMAL(10,2) NOT NULL,
    estado ENUM('PROCESADA','RECHAZADA') NOT NULL DEFAULT 'PROCESADA',
    FOREIGN KEY (venta_id) REFERENCES ventas(id),
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE detalle_devolucion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    devolucion_id INT NOT NULL,
    detalle_venta_id BIGINT NOT NULL,
    cantidad_devuelta INT NOT NULL,
    monto_devuelto DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (devolucion_id) REFERENCES devoluciones(id),
    FOREIGN KEY (detalle_venta_id) REFERENCES detalle_venta(id)
);
```

**Validación importante al programar esto:** antes de aceptar una devolución, verificar que `cantidad_devuelta` acumulada por línea no exceda la cantidad originalmente vendida en esa línea (alguien podría intentar devolver 5 unidades de un producto del que solo compró 2 — hay que bloquear eso a nivel de lógica de negocio, la base de datos sola no lo impide).
