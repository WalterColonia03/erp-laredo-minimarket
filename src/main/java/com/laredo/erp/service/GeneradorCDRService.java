package com.laredo.erp.service;

import com.laredo.erp.modelo.Comprobante;

/**
 * Genera el CDR (Constancia de Recepción) simulado.
 * En producción, esto llegaría del OSE/SUNAT real.
 * Estructura según docs/04-Diseno-Facturacion-SUNAT.md §"CDR simulado".
 */
public class GeneradorCDRService {

    public String generarCDR(Comprobante comprobante) {
        String descripcion = String.format(
                "La %s numero %s, ha sido aceptada (SIMULADO)",
                comprobante.getTipo() == Comprobante.Tipo.FACTURA ? "Factura" : "Boleta",
                comprobante.getIdentificadorCompleto()
        );

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
             + "<ApplicationResponse "
             + "xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2\"\n"
             + "   xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">\n"
             + "  <cbc:ResponseCode>0</cbc:ResponseCode> <!-- 0 = Aceptado -->\n"
             + "  <cbc:Description>" + descripcion + "</cbc:Description>\n"
             + "  <!-- SIMULADO — sin validez tributaria real -->\n"
             + "</ApplicationResponse>\n";
    }
}
