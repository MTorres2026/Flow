package com.example.flow

data class EvaluacionFlow(
    val idVista: Int,
    val producto: String,

    val puedeAnalizarse: Boolean,

    val nivelEvidencia: String,

    val stockActualRegistrado: Double?,
    val consumoHistoricoMedio: Double?,
    val consumoHistoricoMinimo: Double?,
    val consumoHistoricoMaximo: Double?,

    val variacionHistorica: Double?,

    val numeroEpisodios: Int,

    val observacion: String
)

object FlowEngine {

    fun evaluarProducto(
        producto: ProductoFlow,
        historial: List<org.json.JSONObject>
    ): EvaluacionFlow {

        val resumen =
            FlowAnalisisHistorico.analizarProducto(
                historial = historial,
                idVista = producto.idVista
            )

        if (resumen == null) {

            return EvaluacionFlow(
                idVista = producto.idVista,
                producto = producto.producto,
                puedeAnalizarse = false,
                nivelEvidencia = "SIN_HISTORIAL",
                stockActualRegistrado = producto.stockFinal,
                consumoHistoricoMedio = null,
                consumoHistoricoMinimo = null,
                consumoHistoricoMaximo = null,
                variacionHistorica = null,
                numeroEpisodios = 0,
                observacion = "No existe histórico disponible para este producto."
            )
        }

        val puedeAnalizarse =
            resumen.semanasConDatosCompletos >= 2

        val observacion =
            when {
                !puedeAnalizarse ->
                    "Histórico insuficiente para establecer un patrón."

                resumen.nivelEvidencia == "INICIAL" ->
                    "Existe histórico, pero todavía es limitado."

                resumen.nivelEvidencia == "OBSERVABLE" ->
                    "Existe evidencia histórica suficiente para observar comportamiento."

                else ->
                    "Existe histórico, pero requiere revisión."
            }

        return EvaluacionFlow(
            idVista = producto.idVista,
            producto = producto.producto,

            puedeAnalizarse = puedeAnalizarse,

            nivelEvidencia = resumen.nivelEvidencia,

            stockActualRegistrado = producto.stockFinal,

            consumoHistoricoMedio = resumen.mediaConsumo,

            consumoHistoricoMinimo = resumen.consumoMinimo,

            consumoHistoricoMaximo = resumen.consumoMaximo,

            variacionHistorica = resumen.variacionAbsoluta,

            numeroEpisodios = resumen.semanas,

            observacion = observacion
        )
    }

    fun evaluarProductos(
        productos: List<ProductoFlow>,
        historial: List<org.json.JSONObject>
    ): List<EvaluacionFlow> {

        return productos.map { producto ->
            evaluarProducto(
                producto = producto,
                historial = historial
            )
        }
    }

    fun crearSugerencia(
        producto: ProductoFlow,
        historial: List<org.json.JSONObject>
    ): OrderSuggestion {

        val evaluacion = evaluarProducto(producto, historial)

        return OrderSuggestion(
            idVista = producto.idVista,
            producto = producto.producto,

            idErp = producto.idErp,
            umErp = producto.umErp,
            idConway = producto.idConway,
            denominacionLargaConway = producto.denominacionLargaConway,
            presentacion = producto.presentacion,
            unidadesPorCajaPack = producto.unidadesPorCajaPack,
            unidadPedidoFlow = producto.unidadPedidoFlow,
            estado = producto.estado,
            fuente = producto.fuente,
            notaRegla = producto.notaRegla,

            debeRevisarse = !evaluacion.puedeAnalizarse,
            cantidadSugerida = null,
            nivelEvidencia = evaluacion.nivelEvidencia,
            numeroEpisodios = evaluacion.numeroEpisodios,
            consumoMedio = evaluacion.consumoHistoricoMedio,
            stockActualRegistrado = evaluacion.stockActualRegistrado,
            motivo = if (evaluacion.puedeAnalizarse) {
                "Producto con histórico suficiente para continuar el análisis. La cantidad todavía no se calcula."
            } else {
                evaluacion.observacion
            }
        )
    }

    fun crearSugerencias(
        productos: List<ProductoFlow>,
        historial: List<org.json.JSONObject>
    ): List<OrderSuggestion> {

        return productos.map { producto ->
            crearSugerencia(
                producto = producto,
                historial = historial
            )
        }
    }
}