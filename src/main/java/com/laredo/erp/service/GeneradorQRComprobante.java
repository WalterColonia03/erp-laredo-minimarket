package com.laredo.erp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.laredo.erp.modelo.Cliente;
import com.laredo.erp.modelo.Comprobante;

import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;

/**
 * Genera el QR del comprobante electrónico (distinto al QR de Izipay).
 * Datos codificados según docs/04-Diseno-Facturacion-SUNAT.md §"El QR del comprobante":
 *   RUCEmisor|TipoDoc|Serie-Numero|Fecha|TipoDocCliente|NroDocCliente|MontoTotal|Moneda
 */
public class GeneradorQRComprobante {

    private static final String RUC_EMISOR = "20601030013";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * @param comprobante  datos del comprobante (tipo, serie, numero, fecha)
     * @param cliente      puede ser null (venta anónima)
     * @return imagen 200×200 del QR
     */
    public BufferedImage generarQR(Comprobante comprobante, Cliente cliente) throws Exception {
        String datos = construirDatosQR(comprobante, cliente);
        BitMatrix matrix = new MultiFormatWriter().encode(datos, BarcodeFormat.QR_CODE, 200, 200);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /**
     * Devuelve la cadena de datos que se codifica en el QR.
     * Se guarda también en la columna qr_data de comprobantes para auditoría.
     */
    public String construirDatosQR(Comprobante comprobante, Cliente cliente) {
        String tipoDoc     = comprobante.getCodigoTipoSunat();
        String serieNumero = comprobante.getIdentificadorCompleto();
        String fecha       = comprobante.getFechaEmision() != null
                           ? comprobante.getFechaEmision().format(FMT)
                           : java.time.LocalDate.now().toString();

        String tipoDocCliente = "0"; // 0=Sin doc (anónimo)
        String nroDocCliente  = "00000000";
        if (cliente != null) {
            if (cliente.getDni() != null) {
                tipoDocCliente = "1"; // 1=DNI
                nroDocCliente  = cliente.getDni();
            } else if (cliente.getRuc() != null) {
                tipoDocCliente = "6"; // 6=RUC
                nroDocCliente  = cliente.getRuc();
            }
        }

        // Formato: pipe-separado como usan varios OSE peruanos
        return RUC_EMISOR + "|" + tipoDoc + "|" + serieNumero + "|"
             + fecha + "|" + tipoDocCliente + "|" + nroDocCliente + "|"
             + (comprobante.getQrData() != null ? "" : "0.00") + "|PEN";
    }
}
