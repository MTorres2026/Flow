
package com.example.flow

import android.content.Context
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.math.BigDecimal
import kotlin.math.abs

/**
 * Generador del documento PDF final de FLOW.
 *
 * RESPONSABILIDAD EXCLUSIVA:
 * - recibe el PedidoFinalFlow ya confirmado;
 * - NO recalcula cantidades;
 * - NO modifica el PDF original;
 * - crea una copia nueva en el almacenamiento interno de FLOW;
 * - escribe las cantidades finales en la columna que ocupa actualmente
 *   "Stocktake Count", cuya cabecera se sustituye en la copia por
 *   "PEDIDO FINAL".
 *
 * IMPORTANTE:
 * Los productos de familia se dejan fuera de este overlay porque el PDF
 * de Vista contiene la fila de la familia y no las variantes físicas
 * individuales. Esas variantes necesitan su tratamiento documental propio
 * y NO se deben escribir varias cantidades sobre una misma fila.
 */
object FlowPdfPedidoFinal {

    private const val NOMBRE_SALIDA = "flow_pedido_final.pdf"
    private const val NOMBRE_TEMP = "flow_pedido_final.tmp.pdf"

    /**
     * Coordenada horizontal ya comprobada para la columna Stocktake Count
     * del informe Vista.
     *
     * No representa una coordenada de producto: solo la posición X de la
     * columna donde se escribe la cantidad final.
     */
    private const val X_CANTIDAD_FINAL = 556f

    private const val TAMANO_FUENTE_CANTIDAD = 6f
    private const val TAMANO_FUENTE_CABECERA = 7f

    /**
     * Fila numérica del informe.
     *
     * En el PDF real la fila contiene:
     *
     * UNIDAD + 9 valores numéricos.
     *
     * Ejemplo:
     *
     * KGS 1,65 0,00 0,00 0,00 0,00 0,00 0,00 1,65 4,2000
     *
     * Se permiten 8-10 valores numéricos para conservar cierta tolerancia
     * ante pequeñas diferencias de extracción entre páginas.
     */
    private val REGEX_FILA_NUMERICA = Regex(
        "^\\s*\\S+\\s+-?[\\d.,]+(?:\\s+-?[\\d.,]+){8,10}\\s*$"
    )

    data class ResultadoPdf(
        val archivo: File,
        val lineasEscritas: Int,
        val familiasPendientes: Int
    )

    private data class LineaPdf(
        val pagina: Int,
        val texto: String,
        val yDirAdj: Float,
        val xDirAdj: Float
    )

    /**
     * Genera una copia nueva del PDF fuente utilizando exclusivamente
     * las cantidades que ya quedaron confirmadas en PedidoFinalFlow.
     */
    fun generar(
        context: Context,
        archivoOriginal: File,
        pedidoFinal: PedidoFinalFlow
    ): ResultadoPdf {

        require(archivoOriginal.exists()) {
            "No existe el PDF original: ${archivoOriginal.absolutePath}"
        }

        require(pedidoFinal.lineas.isNotEmpty()) {
            "No hay líneas en el pedido final para generar el PDF."
        }

        val lineasNormales =
            pedidoFinal.lineas.filter {
                !it.fuente.equals("FAMILIA", ignoreCase = true)
            }

        val lineasFamilia =
            pedidoFinal.lineas.filter {
                it.fuente.equals("FAMILIA", ignoreCase = true)
            }

        val archivoTemp =
            File(context.filesDir, NOMBRE_TEMP)

        val archivoSalida =
            File(context.filesDir, NOMBRE_SALIDA)

        var lineasEscritas = 0

        if (archivoTemp.exists()) {
            archivoTemp.delete()
        }

        FileInputStreamWrapper.open(archivoOriginal).use { input ->

            val documento = PDDocument.load(input)

            try {

                val extractor = PositionStripperFlow()

                extractor.sortByPosition = true
                extractor.startPage = 1
                extractor.endPage = documento.numberOfPages

                extractor.getText(documento)

                val lineas =
                    extractor.lineas.toList()

                val gruposPorPagina =
                    lineas.groupBy { it.pagina }

                val noEncontradas =
                    mutableListOf<String>()

                lineasNormales.forEach { linea ->

                    val localizacion =
                        localizarLineaProducto(
                            linea = linea,
                            lineas = gruposPorPagina
                        )

                    if (localizacion == null) {

                        noEncontradas.add(
                            "${linea.idVista} | ${linea.producto}"
                        )

                    } else {

                        escribirCantidad(
                            documento = documento,
                            pagina = localizacion.pagina,
                            yDirAdjFilaNumerica = localizacion.yDirAdj,
                            cantidad = linea.cantidad
                        )

                        lineasEscritas++
                    }
                }

                if (noEncontradas.isNotEmpty()) {

                    throw IllegalStateException(
                        "No se pudieron localizar en el PDF estas líneas:\n" +
                                noEncontradas.joinToString("\n")
                    )
                }

                sustituirCabeceraStocktakeCount(
                    documento
                )

                documento.save(
                    archivoTemp
                )

            } finally {

                documento.close()
            }
        }

        if (
            archivoSalida.exists() &&
            !archivoSalida.delete()
        ) {

            throw IllegalStateException(
                "No se pudo sustituir el PDF final anterior."
            )
        }

        if (
            !archivoTemp.renameTo(
                archivoSalida
            )
        ) {

            throw IllegalStateException(
                "No se pudo crear el PDF final: " +
                        archivoSalida.absolutePath
            )
        }

        Log.d(
            "FLOW PDF FINAL",
            "PDF generado: ${archivoSalida.absolutePath} | " +
                    "líneas escritas=$lineasEscritas | " +
                    "familias pendientes=${lineasFamilia.size}"
        )

        return ResultadoPdf(
            archivo =
                archivoSalida,

            lineasEscritas =
                lineasEscritas,

            familiasPendientes =
                lineasFamilia.size
        )
    }

    private fun localizarLineaProducto(
        linea: LineaPedidoFinal,
        lineas: Map<Int, List<LineaPdf>>
    ): LineaPdf? {

        val idTexto = linea.idVista.toString()
        val nombreNormalizado = normalizar(linea.producto)

        for ((_, lista) in lineas) {

            // Primero buscamos el ID exacto.
            val porId =
                lista.firstOrNull {
                    val texto = normalizar(it.texto)
                    texto == idTexto ||
                            texto.replace(" ", "") == idTexto
                }

            if (porId != null) {
                return porId
            }

            // Si no aparece el ID, buscamos el nombre exacto.
            val porNombre =
                lista.firstOrNull {
                    normalizar(it.texto) == nombreNormalizado
                }

            if (porNombre != null) {
                return porNombre
            }
        }

        return null
    }

    private fun escribirCantidad(
        documento: PDDocument,
        pagina: Int,
        yDirAdjFilaNumerica: Float,
        cantidad: Double
    ) {

        val paginaPdf: PDPage =
            documento.getPage(
                pagina - 1
            )

        val alturaPagina =
            paginaPdf.mediaBox.height

        val targetY =
            alturaPagina -
                    yDirAdjFilaNumerica -
                    2f

        PDPageContentStream(
            documento,
            paginaPdf,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        ).use { stream ->

            stream.beginText()

            stream.setFont(
                PDType1Font.HELVETICA,
                TAMANO_FUENTE_CANTIDAD
            )

            stream.newLineAtOffset(
                X_CANTIDAD_FINAL,
                targetY
            )

            stream.showText(
                formatearCantidad(
                    cantidad
                )
            )

            stream.endText()
        }
    }

    /**
     * Sustituye visualmente solo la cabecera "Stocktake Count" de la copia.
     * La posición se recupera del propio PDF para no depender de una tabla
     * externa de coordenadas.
     */
    private fun sustituirCabeceraStocktakeCount(
        documento: PDDocument
    ) {

        val extractor =
            PositionStripperFlow()

        extractor.sortByPosition =
            true

        extractor.startPage =
            1

        extractor.endPage =
            documento.numberOfPages

        extractor.getText(
            documento
        )

        val stocktake =
            extractor.lineas.firstOrNull {
                normalizar(it.texto) ==
                        "STOCKTAKE"
            }
                ?: return

        val paginaPdf =
            documento.getPage(
                stocktake.pagina - 1
            )

        val alturaPagina =
            paginaPdf.mediaBox.height

        val baseY =
            alturaPagina -
                    stocktake.yDirAdj -
                    1f

        PDPageContentStream(
            documento,
            paginaPdf,
            PDPageContentStream.AppendMode.APPEND,
            true,
            true
        ).use { stream ->

            stream.setNonStrokingColor(
                1f,
                1f,
                1f
            )

            stream.addRect(
                stocktake.xDirAdj - 4f,
                baseY - 20f,
                48f,
                25f
            )

            stream.fill()

            stream.setNonStrokingColor(
                0f,
                0f,
                0f
            )

            stream.beginText()

            stream.setFont(
                PDType1Font.HELVETICA,
                TAMANO_FUENTE_CABECERA
            )

            stream.newLineAtOffset(
                stocktake.xDirAdj - 1f,
                baseY
            )

            stream.showText(
                "PEDIDO"
            )

            stream.newLineAtOffset(
                0f,
                -9f
            )

            stream.showText(
                "FINAL"
            )

            stream.endText()
        }
    }

    private fun normalizar(
        texto: String
    ): String {

        return texto
            .trim()
            .uppercase()
            .replace(
                Regex("\\s+"),
                " "
            )
            .replace(
                "Á",
                "A"
            )
            .replace(
                "É",
                "E"
            )
            .replace(
                "Í",
                "I"
            )
            .replace(
                "Ó",
                "O"
            )
            .replace(
                "Ú",
                "U"
            )
    }

    private fun formatearCantidad(
        cantidad: Double
    ): String {

        require(
            cantidad >= 0.0
        ) {
            "La cantidad del PDF no puede ser negativa."
        }

        if (
            abs(
                cantidad -
                        cantidad.toLong().toDouble()
            ) < 0.0000001
        ) {

            return cantidad
                .toLong()
                .toString()
        }

        return BigDecimal
            .valueOf(
                cantidad
            )
            .stripTrailingZeros()
            .toPlainString()
    }

    private class PositionStripperFlow :
        PDFTextStripper() {

        val lineas =
            mutableListOf<LineaPdf>()

        override fun writeString(
            text: String?,
            textPositions: MutableList<TextPosition>?
        ) {

            if (
                !text.isNullOrBlank() &&
                !textPositions.isNullOrEmpty()
            ) {

                val primera =
                    textPositions.first()

                lineas.add(
                    LineaPdf(
                        pagina =
                            getCurrentPageNo(),

                        texto =
                            text,

                        yDirAdj =
                            primera.yDirAdj,

                        xDirAdj =
                            primera.xDirAdj
                    )
                )
            }

            super.writeString(
                text,
                textPositions
            )
        }
    }

    /**
     * Pequeña envoltura para que el cierre del InputStream quede localizado
     * en un único punto sin introducir dependencias adicionales.
     */
    private object FileInputStreamWrapper {

        fun open(
            file: File
        ) =
            file.inputStream()
    }
}

