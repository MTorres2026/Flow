package com.example.flow

import android.util.Log

data class PedidoSugerido(
    val productos: List<OrderSuggestion>,
    val totalProductos: Int,
    val productosConHistorico: Int,
    val productosSinHistorico: Int,
    val productosPendientesRevision: Int,
    val productosAmbiguos: List<OrderSuggestion> = emptyList()
)

object FlowGeneradorPedido {

    fun generar(
        productos: List<ProductoFlow>,
        context: android.content.Context,
        fechaInformeActual: String? = null,
        contexto: ContextoPedidoSemanal = ContextoPedidoSemanal()
    ): PedidoSugerido {

        val pedidosHistoricos =
            FlowPedidosHistoricos.cargar(context)

        val contextoSemanal =
            FlowContextoSemanal.construirContexto(
                fechaInformeActual
            )

        val conteoEstados =
            mutableMapOf<String, Int>()

        val rechazos =
            mutableListOf<String>()

        val ambiguos =
            mutableListOf<OrderSuggestion>()

        val sugerencias =
            productos.mapNotNull { producto ->

                val historial =
                    FlowHistorial.obtenerHistorialProducto(
                        context = context,
                        idVista = producto.idVista
                    )

                val historialAnterior =
                    fechaInformeActual
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { fecha ->

                            historial.filter {
                                it.optString(
                                    "fechaInforme",
                                    ""
                                ) != fecha
                            }
                        }
                        ?: historial

                /*
                 * El contexto semanal NO multiplica directamente
                 * el consumo.
                 *
                 * Primero intenta identificar semanas históricas
                 * comparables por comportamiento de asistencia.
                 *
                 * Si no hay al menos dos comparables, conserva
                 * todo el histórico para no perder evidencia.
                 */
                val historialContextualizado =
                    FlowContextoSemanal
                        .filtrarHistorialPorSemanasComparables(
                            historial =
                                historialAnterior,
                            fechaInformeActual =
                                fechaInformeActual
                        )

                val resumenHistorico =
                    FlowAnalisisHistorico.analizarProducto(
                        historial =
                            historialContextualizado,
                        idVista =
                            producto.idVista
                    )

                val pedidosDelProducto =
                    pedidosHistoricos.filter {
                        it.idConway ==
                                producto.idConway
                    }

                val contextoFinal =
                    if (
                        contexto !=
                        ContextoPedidoSemanal()
                    ) {
                        contexto
                    } else {
                        contextoSemanal
                    }

                val propuesta =
                    FlowMotorPedido.analizar(
                        producto =
                            producto,
                        historico =
                            resumenHistorico,
                        pedidosHistoricos =
                            pedidosDelProducto,
                        contexto =
                            contextoFinal
                    )

                conteoEstados[
                    propuesta.estado
                ] =
                    (
                            conteoEstados[
                                propuesta.estado
                            ] ?: 0
                            ) + 1

                if (
                    propuesta.estado !=
                    "PEDIR"
                ) {

                    if (
                        rechazos.size < 20
                    ) {

                        rechazos.add(
                            "${producto.idVista} | " +
                                    "${producto.producto} | " +
                                    "estado=${propuesta.estado} | " +
                                    "cantidad=${propuesta.cantidadSugerida} | " +
                                    "motivo=${propuesta.motivo} | " +
                                    "semanas=${resumenHistorico?.semanas ?: 0} | " +
                                    "usoMedio=${resumenHistorico?.mediaConsumo ?: 0.0} | " +
                                    "pedidosConway=${pedidosDelProducto.size}"
                        )
                    }

                    if (
                        propuesta.estado ==
                        "AMBIGUO"
                    ) {

                        ambiguos.add(
                            OrderSuggestion(
                                idVista =
                                    producto.idVista,

                                producto =
                                    producto.producto,

                                idErp =
                                    producto.idErp,

                                umErp =
                                    producto.umErp,

                                idConway =
                                    producto.idConway,

                                denominacionLargaConway =
                                    producto.denominacionLargaConway,

                                presentacion =
                                    producto.presentacion,

                                unidadesPorCajaPack =
                                    producto.unidadesPorCajaPack,

                                unidadPedidoFlow =
                                    producto.unidadPedidoFlow,

                                estado =
                                    propuesta.estado,

                                fuente =
                                    producto.fuente,

                                notaRegla =
                                    producto.notaRegla,

                                debeRevisarse =
                                    true,

                                cantidadSugerida =
                                    propuesta.cantidadSugerida,

                                nivelEvidencia =
                                    propuesta.nivelEvidencia,

                                numeroEpisodios =
                                    resumenHistorico?.semanas
                                        ?: 0,

                                consumoMedio =
                                    resumenHistorico
                                        ?.mediaConsumo,

                                stockActualRegistrado =
                                    producto.stockFinal,

                                motivo =
                                    propuesta.motivo
                            )
                        )
                    }

                    return@mapNotNull null
                }

                val cantidadParaRevision =
                    if (
                        propuesta.cantidadSugerida >
                        0.0
                    ) {
                        propuesta.cantidadSugerida
                    } else {
                        0.0
                    }

                OrderSuggestion(
                    idVista =
                        producto.idVista,

                    producto =
                        producto.producto,

                    idErp =
                        producto.idErp,

                    umErp =
                        producto.umErp,

                    idConway =
                        producto.idConway,

                    denominacionLargaConway =
                        producto.denominacionLargaConway,

                    presentacion =
                        producto.presentacion,

                    unidadesPorCajaPack =
                        producto.unidadesPorCajaPack,

                    unidadPedidoFlow =
                        producto.unidadPedidoFlow,

                    estado =
                        propuesta.estado,

                    fuente =
                        producto.fuente,

                    notaRegla =
                        producto.notaRegla,

                    debeRevisarse =
                        propuesta.necesitaRevision,

                    cantidadSugerida =
                        cantidadParaRevision,

                    nivelEvidencia =
                        propuesta.nivelEvidencia,

                    numeroEpisodios =
                        resumenHistorico?.semanas
                            ?: 0,

                    consumoMedio =
                        resumenHistorico
                            ?.mediaConsumo,

                    stockActualRegistrado =
                        producto.stockFinal,

                    motivo =
                        propuesta.motivo
                )
            }

        Log.d(
            "FLOW MOTOR RESUMEN",
            "Productos entrada=${productos.size} | " +
                    "PEDIR=${conteoEstados["PEDIR"] ?: 0} | " +
                    "NO PEDIR=${conteoEstados["NO PEDIR"] ?: 0} | " +
                    "REVISAR=${conteoEstados["REVISAR"] ?: 0} | " +
                    "AMBIGUO=${conteoEstados["AMBIGUO"] ?: 0} | " +
                    "SIN EVIDENCIA=${conteoEstados["SIN EVIDENCIA"] ?: 0}"
        )

        rechazos.forEach {
                rechazo ->
            Log.d(
                "FLOW MOTOR RECHAZO",
                rechazo
            )
        }

        val productosConHistorico =
            productos.count { producto ->

                val historial =
                    FlowHistorial.obtenerHistorialProducto(
                        context =
                            context,
                        idVista =
                            producto.idVista
                    )

                val historialAnterior =
                    fechaInformeActual
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?.let { fecha ->
                            historial.filter {
                                it.optString(
                                    "fechaInforme",
                                    ""
                                ) != fecha
                            }
                        }
                        ?: historial

                historialAnterior.isNotEmpty()
            }

        val resultado =
            PedidoSugerido(
                productos =
                    sugerencias,

                totalProductos =
                    sugerencias.size,

                productosConHistorico =
                    productosConHistorico,

                productosSinHistorico =
                    productos.size -
                            productosConHistorico,

                productosPendientesRevision =
                    sugerencias.count {
                        it.debeRevisarse
                    },

                productosAmbiguos =
                    ambiguos
            )

        Log.d(
            "FLOW MOTOR FINAL",
            "ProductoSugerido.size=${resultado.productos.size} | " +
                    "totalProductos=${resultado.totalProductos} | " +
                    "conHistorico=${resultado.productosConHistorico} | " +
                    "sinHistorico=${resultado.productosSinHistorico} | " +
                    "revision=${resultado.productosPendientesRevision}"
        )

        return resultado
    }
}