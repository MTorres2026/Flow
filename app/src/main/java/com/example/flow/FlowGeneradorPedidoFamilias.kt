package com.example.flow

/**
 * Genera las líneas individuales de una familia.
 *
 * IMPORTANTE:
 * Una familia NO se convierte en una única línea.
 * Cada variante conserva su propia identidad y cantidad.
 */
object FlowGeneradorPedidoFamilias {

    fun generar(
        familia: ProductoFlow,
        superMaestro: List<ProductoFlow>
    ): List<LineaPedidoFamiliaFlow> {

        val resolucion = FlowResolutorFamilias.resolver(
            familia = familia,
            superMaestro = superMaestro
        )

        return FlowPedidoFamilias.construirLineas(resolucion)
    }

    fun generarPorIdVista(
        idVista: Int,
        nombreFamilia: String,
        superMaestro: List<ProductoFlow>
    ): List<LineaPedidoFamiliaFlow> {

        val resolucion = FlowResolutorFamilias.resolverPorIdVista(
            idVista = idVista,
            nombreFamilia = nombreFamilia,
            superMaestro = superMaestro
        )

        return FlowPedidoFamilias.construirLineas(resolucion)
    }

    fun totalFamilia(
        familia: ProductoFlow,
        superMaestro: List<ProductoFlow>
    ): Double {

        val resolucion = FlowResolutorFamilias.resolver(
            familia = familia,
            superMaestro = superMaestro
        )

        return FlowPedidoFamilias.calcularTotalFamilia(resolucion)
    }
}

fun diagnosticoFamilia(
    idVista: Int,
    nombreFamilia: String,
    superMaestro: List<ProductoFlow>
): String {

    val resolucion =
        FlowResolutorFamilias.resolverPorIdVista(
            idVista = idVista,
            nombreFamilia = nombreFamilia,
            superMaestro = superMaestro
        )

    return buildString {

        appendLine(
            "FAMILIA=${resolucion.nombreFamilia}"
        )

        appendLine(
            "ID VISTA=${resolucion.idVistaFamilia}"
        )



        appendLine(
            "CON CONWAY=${resolucion.variantesConConway}"
        )

        appendLine(
            "SIN CONWAY=${resolucion.variantesSinConway}"
        )

        resolucion.variantes.forEach { variante ->

            appendLine(
                "${variante.producto} | " +
                        "ERP=${variante.idErp} | " +
                        "UM=${variante.umErp} | " +
                        "CONWAY=${variante.idConway} | " +
                        "PRESENTACION=${variante.presentacion}"
            )
        }
    }
}