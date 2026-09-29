package com.example.flow

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class LineaPedidoHistorico(
    val pedido: String,
    val fechaPedido: String,
    val idConway: String,
    val producto: String,
    val cantidad: Double,
    val unidad: String,
    val fuenteDocumento: String
)

object FlowPedidosHistoricos {

    private const val ARCHIVO = "flow_pedidos_historicos.json"

    fun cargar(
        context: Context
    ): List<LineaPedidoHistorico> {

        val texto =
            context.assets
                .open(ARCHIVO)
                .bufferedReader()
                .use { it.readText() }

        val raiz = JSONObject(texto)

        val pedidos =
            raiz.optJSONArray("PEDIDOS")
                ?: JSONArray()

        val resultado =
            mutableListOf<LineaPedidoHistorico>()

        for (i in 0 until pedidos.length()) {

            val pedido =
                pedidos.getJSONObject(i)

            val numeroPedido =
                pedido.optString("pedido", "")

            val fechaPedido =
                pedido.optString("fechaPedido", "")

            val fuenteDocumento =
                pedido.optString("fuenteDocumento", "")

            val lineas =
                pedido.optJSONArray("lineas")
                    ?: continue

            for (j in 0 until lineas.length()) {

                val linea =
                    lineas.getJSONObject(j)

                val idConway =
                    linea.optString("idConway", "")

                if (idConway.isBlank()) {
                    continue
                }

                resultado.add(
                    LineaPedidoHistorico(
                        pedido = numeroPedido,
                        fechaPedido = fechaPedido,
                        idConway = idConway,
                        producto = linea.optString(
                            "producto",
                            ""
                        ),
                        cantidad = linea.optDouble(
                            "cantidad",
                            0.0
                        ),
                        unidad = linea.optString(
                            "unidad",
                            ""
                        ),
                        fuenteDocumento = fuenteDocumento
                    )
                )
            }
        }

        Log.d(
            "FLOW PEDIDOS HISTORICOS",
            "Pedidos JSON=${pedidos.length()} | Líneas válidas=${resultado.size}"
        )

        return resultado
    }

    fun obtenerPorConway(
        context: Context,
        idConway: String
    ): List<LineaPedidoHistorico> {

        if (idConway.isBlank()) {
            return emptyList()
        }

        return cargar(context)
            .filter {
                it.idConway == idConway
            }
    }
}