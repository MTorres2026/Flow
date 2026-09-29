package com.example.flow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LineaPedidoFinal(
    val idVista: Int,
    val producto: String,
    val idErp: String,
    val umErp: String,
    val idConway: String,
    val denominacionLargaConway: String,
    val presentacion: String,
    val unidadesPorCajaPack: String,
    val unidadPedidoFlow: String,
    val cantidad: Double,
    val fuente: String,
    val notaRegla: String
)

data class PedidoFinalFlow(
    val estado: String,
    val lineas: List<LineaPedidoFinal>
)

object FlowPedidoFinal {

    private const val NOMBRE_ARCHIVO = "flow_pedido_final.json"

    fun crear(
        productos: List<OrderSuggestion>,
        cantidades: Map<Int, String>,
        familias: List<LineaPedidoFamiliaFlow> = emptyList(),
        cantidadesFamilias: Map<String, String> = emptyMap()
    ): PedidoFinalFlow {

        val lineasNormales =
            productos.mapNotNull { producto ->

                val textoCantidad =
                    cantidades[producto.idVista]
                        ?.trim()
                        ?: throw IllegalArgumentException(
                            "Falta la cantidad final para: ${producto.producto}"
                        )

                val cantidad =
                    textoCantidad.toDoubleOrNull()
                        ?: throw IllegalArgumentException(
                            "Cantidad final no válida para: ${producto.producto}"
                        )

                if (cantidad < 0.0) {
                    throw IllegalArgumentException(
                        "La cantidad final no puede ser negativa: ${producto.producto}"
                    )
                }

                if (cantidad == 0.0) {
                    return@mapNotNull null
                }

                LineaPedidoFinal(
                    idVista = producto.idVista,
                    producto = producto.producto,
                    idErp = producto.idErp,
                    umErp = producto.umErp,
                    idConway = producto.idConway,
                    denominacionLargaConway = producto.denominacionLargaConway,
                    presentacion = producto.presentacion,
                    unidadesPorCajaPack = producto.unidadesPorCajaPack,
                    unidadPedidoFlow = producto.unidadPedidoFlow,
                    cantidad = cantidad,
                    fuente = producto.fuente,
                    notaRegla = producto.notaRegla
                )
            }

        val lineasFamilias =
            familias.mapNotNull { linea ->

                val tieneConway =
                    linea.idConway.isNotBlank() &&
                            linea.idConway != "-" &&
                            linea.idConway != "—"

                if (!tieneConway) {
                    return@mapNotNull null
                }

                val clave =
                    claveVarianteFamilia(linea)

                val textoCantidad =
                    cantidadesFamilias[clave]
                        ?.trim()
                        ?: throw IllegalArgumentException(
                            "Falta la cantidad final para la variante de familia: ${linea.producto}"
                        )

                val cantidad =
                    textoCantidad.toDoubleOrNull()
                        ?: throw IllegalArgumentException(
                            "Cantidad final no válida para la variante de familia: ${linea.producto}"
                        )

                if (cantidad < 0.0) {
                    throw IllegalArgumentException(
                        "La cantidad final no puede ser negativa: ${linea.producto}"
                    )
                }

                if (cantidad == 0.0) {
                    return@mapNotNull null
                }

                LineaPedidoFinal(
                    idVista = linea.idVistaFamilia,
                    producto = linea.producto,
                    idErp = linea.idErp,
                    umErp = linea.umErp,
                    idConway = linea.idConway,
                    denominacionLargaConway = linea.denominacionLargaConway,
                    presentacion = linea.presentacion,
                    unidadesPorCajaPack = linea.unidadesPorCajaPack,
                    unidadPedidoFlow = linea.unidadPedido,
                    cantidad = cantidad,
                    fuente = "FAMILIA",
                    notaRegla = linea.motivoNoEnvio
                )
            }

        val lineas =
            lineasNormales + lineasFamilias

        if (lineas.isEmpty()) {
            throw IllegalArgumentException(
                "La encargada no ha dejado ninguna línea con cantidad mayor que 0."
            )
        }

        return PedidoFinalFlow(
            estado = "CONFIRMADO_POR_ENCARGADA",
            lineas = lineas
        )
    }

    private fun claveVarianteFamilia(
        linea: LineaPedidoFamiliaFlow
    ): String {

        return when {

            linea.idErp.isNotBlank() ->
                "F|${linea.idVistaFamilia}|ERP|${linea.idErp}|UM|${linea.umErp}"

            linea.idConway.isNotBlank() &&
                    linea.idConway != "-" &&
                    linea.idConway != "—" ->
                "F|${linea.idVistaFamilia}|CONWAY|${linea.idConway}"

            else ->
                "F|${linea.idVistaFamilia}|PRODUCTO|${linea.producto.trim().uppercase()}"
        }
    }

    fun guardar(
        context: Context,
        pedido: PedidoFinalFlow
    ) {

        val raiz =
            JSONObject()

        raiz.put(
            "tipo",
            "FLOW_PEDIDO_FINAL"
        )

        raiz.put(
            "estado",
            pedido.estado
        )

        val lineas =
            JSONArray()

        pedido.lineas.forEach { linea ->

            val item =
                JSONObject()

            item.put(
                "idVista",
                linea.idVista
            )

            item.put(
                "producto",
                linea.producto
            )

            item.put(
                "idErp",
                linea.idErp
            )

            item.put(
                "umErp",
                linea.umErp
            )

            item.put(
                "idConway",
                linea.idConway
            )

            item.put(
                "denominacionLargaConway",
                linea.denominacionLargaConway
            )

            item.put(
                "presentacion",
                linea.presentacion
            )

            item.put(
                "unidadesPorCajaPack",
                linea.unidadesPorCajaPack
            )

            item.put(
                "unidadPedidoFlow",
                linea.unidadPedidoFlow
            )

            item.put(
                "cantidad",
                linea.cantidad
            )

            item.put(
                "fuente",
                linea.fuente
            )

            item.put(
                "notaRegla",
                linea.notaRegla
            )

            lineas.put(
                item
            )
        }

        raiz.put(
            "lineas",
            lineas
        )

        context
            .getFileStreamPath(
                NOMBRE_ARCHIVO
            )
            .writeText(
                raiz.toString(2)
            )
    }

    fun cargar(
        context: Context
    ): PedidoFinalFlow? {

        val archivo =
            context.getFileStreamPath(
                NOMBRE_ARCHIVO
            )

        if (!archivo.exists()) {
            return null
        }

        val raiz =
            JSONObject(
                archivo.readText()
            )

        val lineasJson =
            raiz.optJSONArray(
                "lineas"
            )
                ?: JSONArray()

        val lineas =
            mutableListOf<LineaPedidoFinal>()

        for (
        i in 0 until lineasJson.length()
        ) {

            val item =
                lineasJson.getJSONObject(
                    i
                )

            lineas.add(
                LineaPedidoFinal(

                    idVista =
                        item.optInt(
                            "idVista",
                            0
                        ),

                    producto =
                        item.optString(
                            "producto",
                            ""
                        ),

                    idErp =
                        item.optString(
                            "idErp",
                            ""
                        ),

                    umErp =
                        item.optString(
                            "umErp",
                            ""
                        ),

                    idConway =
                        item.optString(
                            "idConway",
                            ""
                        ),

                    denominacionLargaConway =
                        item.optString(
                            "denominacionLargaConway",
                            ""
                        ),

                    presentacion =
                        item.optString(
                            "presentacion",
                            ""
                        ),

                    unidadesPorCajaPack =
                        item.optString(
                            "unidadesPorCajaPack",
                            ""
                        ),

                    unidadPedidoFlow =
                        item.optString(
                            "unidadPedidoFlow",
                            ""
                        ),

                    cantidad =
                        item.optDouble(
                            "cantidad",
                            0.0
                        ),

                    fuente =
                        item.optString(
                            "fuente",
                            ""
                        ),

                    notaRegla =
                        item.optString(
                            "notaRegla",
                            ""
                        )
                )
            )
        }

        return PedidoFinalFlow(
            estado =
                raiz.optString(
                    "estado",
                    "CONFIRMADO_POR_ENCARGADA"
                ),

            lineas =
                lineas
        )
    }
}