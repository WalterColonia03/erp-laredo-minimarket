package com.laredo.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Generación de código de barras Code128 con ZXing.
 * docs/05-Diseno-TipoCambio-BI-APIs.md § "Código de barras (sin lector físico)"
 *
 * "Lectura" sin escáner: un JTextField normal en la pantalla de venta
 * donde el cajero escribe o pega el código. Un lector USB real actúa
 * como teclado — funciona sin cambiar nada en el código.
 */
public class BarcodeUtil {

    private static final int ANCHO_DEFAULT = 300;
    private static final int ALTO_DEFAULT = 100;

    /**
     * Genera una imagen de código de barras Code128 para el código dado.
     *
     * @param codigo el texto a codificar (típicamente el campo codigo_barras del producto)
     * @return BufferedImage con el código de barras renderizado
     * @throws WriterException si ZXing no puede generar el código
     */
    public static BufferedImage generarImagen(String codigo) throws WriterException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                codigo, BarcodeFormat.CODE_128, ANCHO_DEFAULT, ALTO_DEFAULT);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    /**
     * Genera un código de barras y lo guarda directamente como PNG en disco.
     * Útil para etiquetas de producto.
     *
     * @param codigo el texto a codificar
     * @param rutaArchivo la ruta donde guardar el PNG
     */
    public static void generarYGuardarPNG(String codigo, Path rutaArchivo) throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                codigo, BarcodeFormat.CODE_128, ANCHO_DEFAULT, ALTO_DEFAULT);
        MatrixToImageWriter.writeToPath(matrix, "PNG", rutaArchivo);
    }
}
