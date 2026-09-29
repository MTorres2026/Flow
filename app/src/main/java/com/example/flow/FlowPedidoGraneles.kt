package com.example.flow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Producto de la plantilla oficial de Graneles.
 *
 * La identidad operativa de un granel es:
 * FAMILIA ERP + UM concreta.
 */
data class ProductoGranel(
    val categoria: String,
    val umErp: String,
    val nombreCorto: String,
    val descripcionCompleta: String,
    val idErpFamilia: String,
    val idConway: String,
    val denominacionLargaConway: String,
    val presentacion: String,
    val estado: String,
    val fuente: String,
    val celdaCantidadPlantilla: String
)

data class SugerenciaGranel(
    val producto: ProductoGranel,
    val cantidadSugerida: Double?,
    val estado: String,
    val necesitaRevision: Boolean,
    val motivo: String,
    val nivelEvidencia: String,
    val cantidadesHistoricas: List<Double> = emptyList()
)

data class PedidoGranelSugerido(
    val productos: List<SugerenciaGranel>,
    val productosParaPedir: List<SugerenciaGranel>,
    val productosSinEvidencia: List<SugerenciaGranel>,
    val productosRevisar: List<SugerenciaGranel>
)

/**
 * Mapa estable de la plantilla oficial recibida.
 * No se modifica la plantilla. Solo se registra dónde debe escribirse
 * la cantidad cuando el pedido final quede confirmado.
 */
object FlowPlantillaGraneles {

    private val celdasCantidad = mapOf(
        // GOMA GRANEL - izquierda
        "GAD" to "D8",
        "GAE" to "D9",
        "GEJ" to "D10",
        "GGC" to "D11",
        "HBH" to "D12",
        "HBI" to "D13",
        "HBJ" to "D14",
        "HBM" to "D15",
        "HBN" to "D16",
        "HBR" to "D17",
        "HBS" to "D18",
        "HBT" to "D19",
        "HBU" to "D20",
        "HBV" to "D21",
        "HBW" to "D22",
        "HBX" to "D23",
        "HCD" to "D24",
        "HCP" to "D25",
        "HDP" to "D26",
        "HEL" to "D27",
        "HFE" to "D28",
        "HFF" to "D29",
        "HFP" to "D30",
        "HFY" to "D31",
        "HFZ" to "D32",
        "HGA" to "D33",
        "HGC" to "D34",
        "HGF" to "D35",
        "HGG" to "D36",
        "HGI" to "D37",
        "HGJ" to "D38",
        "HGK" to "D39",
        "HGL" to "D40",
        "HGS" to "D41",
        "HGT" to "D42",
        "HGV" to "D43",

        // GOMA GRANEL - derecha
        "HGW" to "I8",
        "HGZ" to "I9",
        "HHG" to "I10",
        "HHH" to "I11",
        "HHL" to "I12",
        "HHO" to "I13",
        "HHP" to "I14",
        "HHQ" to "I15",
        "HHR" to "I16",
        "HHU" to "I17",
        "HHW" to "I18",
        "HIB" to "I19",
        "HIC" to "I20",
        "HIG" to "I21",
        "HIJ" to "I22",
        "HIK" to "I23",
        "HIL" to "I24",
        "HIN" to "I25",
        "HIQ" to "I26",
        "HJD" to "I27",
        "HYW" to "I28",
        "HYX" to "I29",
        "IAC" to "I30",
        "IAD" to "I31",
        "IAX" to "I32",
        "IAZ" to "I33",
        "IER" to "I34",
        "IEW" to "I35",
        "IGR" to "I36",
        "IHG" to "I37",
        "IHH" to "I38",
        "IHI" to "I39",
        "IHJ" to "I40",
        "IHK" to "I41",
        "KGS_GOMA" to "I42",

        // CHOCOLATE GRANEL
        "GFX" to "D47",
        "HCG" to "D48",
        "HCH" to "D49",
        "HCI" to "D50",
        "HCL" to "D51",
        "HCN" to "D52",
        "HCR" to "D53",
        "HCS" to "D54",
        "HCW" to "D55",
        "HCX" to "D56",
        "HDE" to "D57",
        "HDG" to "D58",
        "HDH" to "D59",
        "HDI" to "D60",
        "HDR" to "D61",
        "HZA" to "I47",
        "HZC" to "I48",
        "IAD_CHOCOLATE" to "I49",
        "IBF" to "I50",
        "IBH" to "I51",
        "IBI" to "I52",
        "IBK" to "I53",
        "IBL" to "I54",
        "IBM" to "I55",
        "IDL" to "I56",
        "IGM" to "I57",
        "IGN" to "I58",
        "IGO" to "I59",
        "KGS_CHOCOLATE" to "I60",

        // CARAMELO GRANEL
        "HBL" to "D65",
        "HBR_CARAMELO" to "D66",
        "HBS_CARAMELO" to "D67",
        "HBT_CARAMELO" to "D68",
        "HBU_CARAMELO" to "D69",
        "HBV_CARAMELO" to "D70",
        "HBW_CARAMELO" to "I65",
        "HCC" to "I66",
        "HCD_CARAMELO" to "I67",
        "HEB" to "I68",
        "HGJ_CARAMELO" to "I69",
        "KGS_CARAMELO" to "I70"
    )

    fun celdaCantidad(umErp: String, categoria: String): String? {
        val clave = when (categoria.uppercase()) {
            "GOMA GRANEL" -> when (umErp) {
                "KGS" -> "KGS_GOMA"
                else -> umErp
            }
            "CHOCOLATE GRANEL" -> when (umErp) {
                "IAD" -> "IAD_CHOCOLATE"
                "KGS" -> "KGS_CHOCOLATE"
                else -> umErp
            }
            "CARAMELO GRANEL" -> when (umErp) {
                "HBR" -> "HBR_CARAMELO"
                "HBS" -> "HBS_CARAMELO"
                "HBT" -> "HBT_CARAMELO"
                "HBU" -> "HBU_CARAMELO"
                "HBV" -> "HBV_CARAMELO"
                "HBW" -> "HBW_CARAMELO"
                "HCD" -> "HCD_CARAMELO"
                "HGJ" -> "HGJ_CARAMELO"
                "KGS" -> "KGS_CARAMELO"
                else -> umErp
            }
            else -> umErp
        }
        return celdasCantidad[clave]
    }
}

object FlowMotorGraneles {

    private const val ARCHIVO_MAESTRO = "supermaestro_flow.json"

    /**
     * Carga exclusivamente las variantes de granel del SUPERMAESTRO.
     * No convierte las referencias de granel en productos normales de Conway.
     */
    fun cargarCatalogo(context: Context): List<ProductoGranel> {
        val texto = context.assets
            .open(ARCHIVO_MAESTRO)
            .bufferedReader()
            .use { it.readText() }

        val raiz = JSONArray(texto)
        val resultado = mutableListOf<ProductoGranel>()

        for (i in 0 until raiz.length()) {
            val item = raiz.optJSONObject(i) ?: continue
            val grupo = item.optString("GRUPO", "").trim()

            if (
                grupo != "GOMA GRANEL" &&
                grupo != "CHOCOLATE GRANEL" &&
                grupo != "CARAMELO GRANEL"
            ) {
                continue
            }

            if (item.optString("TIPO REGISTRO", "").trim() != "VARIANTE") {
                continue
            }

            val um = item.optString("UM ERP", "").trim()
            if (um.isBlank() || um == "KGS") continue

            val celda = FlowPlantillaGraneles.celdaCantidad(
                umErp = um,
                categoria = grupo
            ) ?: continue

            resultado.add(
                ProductoGranel(
                    categoria = grupo,
                    umErp = um,
                    nombreCorto = item.optString("PRODUCTO", ""),
                    descripcionCompleta = item.optString("PRODUCTO", ""),
                    idErpFamilia = idNumericoComoTexto(item, "ID ERP"),
                    idConway = idNumericoComoTexto(item, "ID CONWAY"),
                    denominacionLargaConway = item.optString(
                        "DENOMINACIÓN LARGA CONWAY",
                        ""
                    ),
                    presentacion = item.optString("PRESENTACIÓN", ""),
                    estado = item.optString("ESTADO", ""),
                    fuente = item.optString("FUENTE", ""),
                    celdaCantidadPlantilla = celda
                )
            )
        }

        return resultado
    }

    private fun claveHistorico(producto: ProductoGranel): String =
        "${producto.categoria}|${producto.umErp}"

    private fun idNumericoComoTexto(
        item: JSONObject,
        campo: String
    ): String {
        if (!item.has(campo) || item.isNull(campo)) {
            return ""
        }

        val numero = item.optDouble(campo, Double.NaN)

        if (!numero.isNaN()) {
            val entero = numero.roundToLong()
            if (kotlin.math.abs(numero - entero.toDouble()) < 0.000001) {
                return entero.toString()
            }
        }

        return item.optString(campo, "").trim()
    }

    /**
     * Genera propuesta solo para variantes con evidencia histórica explícita.
     *
     * cantidadesHistoricas usa unidades de CAJA del pedido de graneles.
     * Sin esa evidencia no inventamos cantidad ni activamos la referencia.
     */
    fun proponer(
        producto: ProductoGranel,
        cantidadesHistoricas: List<Double>
    ): SugerenciaGranel {
        val validas = cantidadesHistoricas
            .filter { it > 0.0 }

        if (validas.isEmpty()) {
            return SugerenciaGranel(
                producto = producto,
                cantidadSugerida = null,
                estado = "SIN EVIDENCIA",
                necesitaRevision = true,
                motivo = "La referencia existe en la plantilla oficial, pero no se dispone de histórico de pedido suficiente para activarla automáticamente.",
                nivelEvidencia = "DESCONOCIDO"
            )
        }

        val ordenadas = validas.sorted()
        val mitad = ordenadas.size / 2
        val mediana = if (ordenadas.size % 2 == 0) {
            (ordenadas[mitad - 1] + ordenadas[mitad]) / 2.0
        } else {
            ordenadas[mitad]
        }

        val cantidad = mediana.roundToInt().coerceAtLeast(1).toDouble()

        return SugerenciaGranel(
            producto = producto,
            cantidadSugerida = cantidad,
            estado = "PROPUESTA",
            necesitaRevision = true,
            motivo = "Referencia con histórico de pedido. La cantidad sugerida procede de la mediana histórica y debe revisarse antes de rellenar la plantilla oficial.",
            nivelEvidencia = when {
                validas.size >= 6 -> "OBSERVABLE"
                validas.size >= 3 -> "INICIAL"
                else -> "LIMITADO"
            },
            cantidadesHistoricas = validas
        )
    }

    /**
     * Convierte un conjunto de históricos por UM en el bloque de sugerencias.
     * No activa automáticamente las referencias que no tengan histórico.
     */
    fun generar(
        context: Context,
        historicoPorClave: Map<String, List<Double>>
    ): PedidoGranelSugerido {
        val catalogo = cargarCatalogo(context)

        val sugerencias = catalogo.map { producto ->
            proponer(
                producto = producto,
                cantidadesHistoricas = historicoPorClave[claveHistorico(producto)]
                    ?: emptyList()
            )
        }

        return PedidoGranelSugerido(
            productos = sugerencias,
            productosParaPedir = sugerencias.filter {
                it.estado == "PROPUESTA" &&
                        (it.cantidadSugerida ?: 0.0) > 0.0
            },
            productosSinEvidencia = sugerencias.filter {
                it.estado == "SIN EVIDENCIA"
            },
            productosRevisar = sugerencias.filter {
                it.necesitaRevision
            }
        )
    }
}
