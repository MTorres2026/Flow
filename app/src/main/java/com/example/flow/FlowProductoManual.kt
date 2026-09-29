package com.example.flow

/**
 * Convierte un ProductoFlow ya existente en el informe
 * en una OrderSuggestion que puede incorporarse manualmente
 * al PEDIDO SUGERIDO.
 *
 * No inventa ERP, Conway, UM ni presentación:
 * utiliza exclusivamente los datos ya presentes en ProductoFlow.
 */
object FlowProductoManual {

    fun crearSugerencia(
        producto: ProductoFlow
    ): OrderSuggestion {

        return OrderSuggestion(

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
                "MANUAL",

            fuente =
                producto.fuente,

            notaRegla =
                producto.notaRegla,

            debeRevisarse =
                true,

            cantidadSugerida =
                0.0,

            nivelEvidencia =
                "MANUAL",

            numeroEpisodios =
                0,

            consumoMedio =
                null,

            stockActualRegistrado =
                producto.stockFinal,

            motivo =
                "Añadido manualmente por la encargada al pedido sugerido."
        )
    }

    fun crearSugerencias(
        productos: List<ProductoFlow>
    ): List<OrderSuggestion> {

        return productos.map {
            crearSugerencia(
                producto = it
            )
        }
    }
}

