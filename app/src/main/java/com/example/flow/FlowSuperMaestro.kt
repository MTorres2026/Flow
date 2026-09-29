package com.example.flow

import org.json.JSONArray

object FlowSuperMaestro {

    fun convertir(
        superMaestro: JSONArray
    ): List<ProductoFlow> {

        val productos =
            mutableListOf<ProductoFlow>()
        val clavesYaVistas =
            mutableSetOf<String>()

        for (
        i in 0 until superMaestro.length()
        ) {

            val maestro =
                superMaestro.getJSONObject(i)

            val idVista =
                maestro
                    .optDouble(
                        "ID VISTA",
                        -1.0
                    )
                    .toInt()

            if (idVista <= 0) {
                continue
            }
            val idErpTexto =
                maestro
                    .opt(
                        "ID ERP"
                    )
                    ?.toString()
                    ?.trim()
                    ?: ""

            val umErpTexto =
                maestro
                    .optString(
                        "UM ERP",
                        ""
                    )
                    .trim()

            val clave =
                "$idVista|$idErpTexto|$umErpTexto"

            if (!clavesYaVistas.add(clave)) {
                continue
            }
            productos.add(
                ProductoFlow(
                    idVista = idVista,
                    producto =
                        maestro.optString(
                            "PRODUCTO",
                            ""
                        ),
                    umInforme = "",
                    stockInicial = 0.0,
                    ingresos = 0.0,
                    transferencias = 0.0,
                    ajustes = 0.0,
                    desperdiciado = 0.0,
                    ventas = 0.0,
                    uso = 0.0,
                    stockFinal = 0.0,
                    coste = 0.0,
                    idErp =
                        maestro.optString(
                            "ID ERP",
                            ""
                        ),
                    umErp =
                        maestro.optString(
                            "UM ERP",
                            ""
                        ),
                    idConway =
                        maestro.optString(
                            "ID CONWAY",
                            ""
                        ),
                    denominacionLargaConway =
                        maestro.optString(
                            "DENOMINACIÓN LARGA CONWAY",
                            ""
                        ),
                    presentacion =
                        maestro.optString(
                            "PRESENTACIÓN",
                            ""
                        ),
                    unidadesPorCajaPack =
                        maestro.optString(
                            "UNIDADES POR CAJA/PACK",
                            ""
                        ),
                    unidadPedidoFlow =
                        maestro.optString(
                            "UNIDAD PEDIDO FLOW",
                            ""
                        ),
                    estado =
                        maestro.optString(
                            "ESTADO",
                            ""
                        ),
                    fuente =
                        maestro.optString(
                            "FUENTE",
                            ""
                        ),
                    notaRegla =
                        maestro.optString(
                            "NOTA / REGLA",
                            ""
                        )
                )
            )
        }

        return productos
    }
}