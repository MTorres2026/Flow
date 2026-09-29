package com.example.flow

/**
 * Representa una variante individual perteneciente a una familia
 * que aparece agrupada en el informe de variaciones.
 *
 * IMPORTANTE:
 * La familia NO sustituye a estas variantes.
 * Las variantes son las que podrán viajar posteriormente
 * como líneas individuales del pedido Conway.
 */
data class VarianteFamiliaFlow(
    val familiaIdVista: Int,
    val familiaNombre: String,

    val idErp: String,
    val umErp: String,
    val idConway: String,

    val producto: String,
    val denominacionLargaConway: String,
    val presentacion: String,

    val unidadesPorCajaPack: String,
    val unidadPedidoFlow: String,

    val estado: String,
    val fuente: String,
    val notaRegla: String,

    /**
     * Cantidad individual que posteriormente decidirá
     * FlowMotorPedido.
     *
     * Aquí NO calculamos todavía ninguna cantidad.
     */
    val cantidadSugerida: Double = 0.0
)

/**
 * Resultado de resolver una familia.
 *
 * La familia original se conserva y, además,
 * se conserva el desglose completo de variantes.
 */
data class ResolucionFamiliaFlow(
    val idVistaFamilia: Int,
    val nombreFamilia: String,
    val variantes: List<VarianteFamiliaFlow>
) {

    /**
     * Número de variantes encontradas.data class VarianteFamiliaFlow(
     */
    val totalVariantes: Int
        get() = variantes.size

    /**
     * Variantes que tienen ID Conway utilizable.
     *
     * No significa todavía que haya que pedirlas.
     * Eso lo decidirá posteriormente el motor.
     */
    val variantesConConway: List<VarianteFamiliaFlow>
        get() = variantes.filter {
            it.idConway.isNotBlank() &&
                    it.idConway != "-" &&
                    it.idConway != "—"
        }

    /**
     * Variantes identificadas pero sin ID Conway.
     *
     * Estas NO se deben mandar automáticamente al pedido.
     */
    val variantesSinConway: List<VarianteFamiliaFlow>
        get() = variantes.filter {
            it.idConway.isBlank() ||
                    it.idConway == "-" ||
                    it.idConway == "—"
        }


    /**
     * Suma de las cantidades individuales.
     *
     * Se utilizará posteriormente para mostrar
     * el TOTAL de la familia en la hoja/PDF.resolver(
     */
    val totalFamilia: Double
        get() = variantes.sumOf {
            it.cantidadSugerida
        }
}


/**
 * Resolutor de familias especiales.
 *
 * NO analiza el PDF.
 * NO calcula consumos.
 * NO decide cantidades.
 * NO envía pedidos.
 *
 * Su única responsabilidad es:
 *
 *     familia del PDF
 *          ↓
 *     variantes del SuperMaestro
 *
 * Ejemplo:
 *
 *     ESTUCHADOS 3,90 €
 *          ↓
 *     GBN Conguitos Doypack
 *     GEF Anacardos
 *     GEG Mimomento Pistacho
 *     ...
 *
 * Y:
 *
 *     HARIBO 100 g
 *          ↓
 *     GBU Funky Mix
 *     HKW Teen Pica
 *     ...
 */
object FlowResolutorFamilias {

    /**
     * Familias que actualmente requieren desglose.
     *
     * No estamos diciendo que el PDF contenga las variantes.
     * Precisamente lo contrario:
     * el PDF contiene la familia y nosotros la resolvemos
     * contra el SuperMaestro.
     */
    private val familiasEspeciales = setOf(
        "ESTUCHADOS 3,90",
        "ESTUCHADOS 3.90",
        "ESTUCHADOS",
        "HARIBO 100 G",
        "HARIBO 100G",
        "HARIBO 100GR",
        "HARIBO 100GRS",
        "HARIBO 100 GRAMOS"
    )

    /**
     * Indica si el producto del informe corresponde
     * a una familia que necesita desglose.
     */
    fun esFamiliaEspecial(nombre: String): Boolean {

        val normalizado = normalizar(nombre)

        return familiasEspeciales.any { familia ->

            val familiaNormalizada =
                normalizar(familia)

            normalizado == familiaNormalizada ||
                    normalizado.contains(familiaNormalizada) ||
                    familiaNormalizada.contains(normalizado)
        }
    }

    /**
     * Resuelve una familia utilizando exclusivamente
     * los registros ya cargados en ProductoFlow.
     *
     * Esto evita volver a leer el JSON y evita duplicar
     * la lógica de carga del SuperMaestro.
     */
    fun resolver(
        familia: ProductoFlow,
        superMaestro: List<ProductoFlow>
    ): ResolucionFamiliaFlow {

        val nombreFamilia =
            familia.producto.trim()

        val variantes = superMaestro
            .filter { producto ->

                producto.idVista == familia.idVista &&
                        producto.producto.isNotBlank() &&
                        producto.fuente != "STOCKABLE_MODIFICADO"
            }
            .map { variante ->

                VarianteFamiliaFlow(

                    familiaIdVista =
                        familia.idVista,

                    familiaNombre =
                        nombreFamilia,

                    idErp =
                        variante.idErp,

                    umErp =
                        variante.umErp,

                    idConway =
                        variante.idConway,

                    producto =
                        variante.producto,

                    denominacionLargaConway =
                        variante.denominacionLargaConway,

                    presentacion =
                        variante.presentacion,

                    unidadesPorCajaPack =
                        variante.unidadesPorCajaPack,

                    unidadPedidoFlow =
                        variante.unidadPedidoFlow,

                    estado =
                        variante.estado,

                    fuente =
                        variante.fuente,

                    notaRegla =
                        variante.notaRegla
                )
            }

        return ResolucionFamiliaFlow(

            idVistaFamilia =
                familia.idVista,

            nombreFamilia =
                nombreFamilia,

            variantes =
                variantes
        )
    }

    /**
     * Resuelve directamente por ID VISTA.
     *
     * Es especialmente importante porque el nombre del PDF
     * puede variar ligeramente.
     */
    fun resolverPorIdVista(
        idVista: Int,
        nombreFamilia: String,
        superMaestro: List<ProductoFlow>
    ): ResolucionFamiliaFlow {

        val variantes = superMaestro
            .filter { producto ->

                producto.idVista == idVista &&
                        producto.producto.isNotBlank() &&
                        producto.fuente != "STOCKABLE_MODIFICADO"
            }
            .map { variante ->

                VarianteFamiliaFlow(

                    familiaIdVista =
                        idVista,

                    familiaNombre =
                        nombreFamilia,

                    idErp =
                        variante.idErp,

                    umErp =
                        variante.umErp,

                    idConway =
                        variante.idConway,

                    producto =
                        variante.producto,

                    denominacionLargaConway =
                        variante.denominacionLargaConway,

                    presentacion =
                        variante.presentacion,

                    unidadesPorCajaPack =
                        variante.unidadesPorCajaPack,

                    unidadPedidoFlow =
                        variante.unidadPedidoFlow,

                    estado =
                        variante.estado,

                    fuente =
                        variante.fuente,

                    notaRegla =
                        variante.notaRegla
                )
            }

        return ResolucionFamiliaFlow(

            idVistaFamilia =
                idVista,

            nombreFamilia =
                nombreFamilia,

            variantes =
                variantes
        )
    }

    /**
     * Normalización conservadora.
     *
     * NO modifica el dato original.
     * Solo sirve para comparar nombres.
     */
    private fun normalizar(texto: String): String {

        return texto
            .trim()
            .uppercase()
            .replace("Á", "A")
            .replace("É", "E")
            .replace("Í", "I")
            .replace("Ó", "O")
            .replace("Ú", "U")
            .replace("Ü", "U")
            .replace(Regex("\\s+"), " ")
    }
}