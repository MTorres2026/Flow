package com.example.flow

/**
 * Línea individual que puede viajar al pedido Conway.
 *
 * Una familia puede generar varias líneas.
 *
 * Ejemplo:
 *
 * ESTUCHADOS 3,90
 *   ├── Conguitos Doypack   2 CAJAS
 *   ├── Lacasitos Doypack   1 CAJA
 *   └── Conguitos Blancos   3 CAJAS
 *
 * Estas son las líneas que posteriormente utilizará
 * el generador final del pedido.
 */
data class LineaPedidoFamiliaFlow(
    val idVistaFamilia: Int,
    val familia: String,

    val producto: String,
    val idErp: String,
    val umErp: String,
    val idConway: String,

    val denominacionLargaConway: String,
    val presentacion: String,

    val cantidad: Double,
    val unidadPedido: String,

    val unidadesPorCajaPack: String,

    val puedeEnviarseConway: Boolean,

    val motivoNoEnvio: String
)

/**
 * Resumen de una familia después de haber generado
 * sus líneas individuales.
 *
 * El detalle conserva las variantes.
 * El total sirve para la hoja/PDF.
 */
data class ResumenFamiliaPedidoFlow(
    val idVistaFamilia: Int,
    val familia: String,

    val lineas: List<LineaPedidoFamiliaFlow>
) {

    /**
     * Total de todas las cantidades individuales
     * de la familia.
     */
    val totalFamilia: Double
        get() = lineas.sumOf {
            it.cantidad
        }

    /**
     * Líneas que sí pueden viajar a Conway.
     */
    val lineasEnviables: List<LineaPedidoFamiliaFlow>
        get() = lineas.filter {
            it.puedeEnviarseConway &&
                    it.cantidad > 0
        }

    /**
     * Líneas que quedan identificadas pero no pueden
     * enviarse automáticamente.
     */
    val lineasNoEnviables: List<LineaPedidoFamiliaFlow>
        get() = lineas.filter {
            !it.puedeEnviarseConway
        }

    /**
     * Número de variantes que forman la familia.
     */
    val numeroVariantes: Int
        get() = lineas.size

    /**
     * Número de variantes que realmente tienen
     * cantidad de pedido.
     */
    val numeroVariantesConCantidad: Int
        get() = lineas.count {
            it.cantidad > 0
        }
}


/**
 * Convierte una resolución de familia en líneas
 * individuales de pedido.
 *
 * IMPORTANTE:
 *
 * Esta clase NO decide todavía cuánto pedir.
 *
 * Recibe las cantidades ya calculadas y las transporta
 * correctamente hasta la estructura del pedido.
 */
object FlowPedidoFamilias {

    /**
     * Construye las líneas individuales de una familia.
     */
    fun construirLineas(
        resolucion: ResolucionFamiliaFlow
    ): List<LineaPedidoFamiliaFlow> {

        return resolucion.variantes.map { variante ->

            val tieneConway =
                variante.idConway.isNotBlank() &&
                        variante.idConway != "-" &&
                        variante.idConway != "—"

            val cantidadValida =
                variante.cantidadSugerida > 0.0

            val puedeEnviarse =
                tieneConway &&
                        cantidadValida

            val motivoNoEnvio =
                when {

                    !tieneConway ->
                        "SIN ID CONWAY"

                    !cantidadValida ->
                        "SIN CANTIDAD DE PEDIDO"

                    else ->
                        ""
                }

            LineaPedidoFamiliaFlow(

                idVistaFamilia =
                    variante.familiaIdVista,

                familia =
                    variante.familiaNombre,

                producto =
                    variante.producto,

                idErp =
                    variante.idErp,

                umErp =
                    variante.umErp,

                idConway =
                    variante.idConway,

                denominacionLargaConway =
                    variante.denominacionLargaConway,

                presentacion =
                    variante.presentacion,

                cantidad =
                    variante.cantidadSugerida,

                unidadPedido =
                    variante.unidadPedidoFlow,

                unidadesPorCajaPack =
                    variante.unidadesPorCajaPack,

                puedeEnviarseConway =
                    puedeEnviarse,

                motivoNoEnvio =
                    motivoNoEnvio
            )
        }
    }

    /**
     * Construye el pedido completo de una familia.
     */
    fun construirResumen(
        resolucion: ResolucionFamiliaFlow
    ): ResumenFamiliaPedidoFlow {

        val lineas =
            construirLineas(resolucion)

        return ResumenFamiliaPedidoFlow(

            idVistaFamilia =
                resolucion.idVistaFamilia,

            familia =
                resolucion.nombreFamilia,

            lineas =
                lineas
        )
    }

    /**
     * Obtiene únicamente las líneas que realmente
     * pueden viajar al pedido Conway.
     */
    fun obtenerLineasEnviables(
        resolucion: ResolucionFamiliaFlow
    ): List<LineaPedidoFamiliaFlow> {

        return construirLineas(resolucion)
            .filter {
                it.puedeEnviarseConway
            }
    }

    /**
     * Calcula exclusivamente el total de la familia.
     *
     * Este valor será utilizado posteriormente por
     * la hoja/PDF.
     *
     * IMPORTANTE:
     * no sustituye las líneas individuales.
     */
    fun calcularTotalFamilia(
        resolucion: ResolucionFamiliaFlow
    ): Double {

        return resolucion.variantes.sumOf {
            it.cantidadSugerida
        }
    }
}