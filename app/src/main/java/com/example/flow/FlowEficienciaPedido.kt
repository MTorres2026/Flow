package com.example.flow

data class EvaluacionEficienciaPedido(
    val lineas: Int,
    val basicosPresentes: Int,
    val cumpleMinimoLineas: Boolean,
    val cumpleMinimoBasicos: Boolean,
    val esPedidoEficiente: Boolean,
    val basicosDetectados: List<String>,
    val motivo: String
)

object FlowEficienciaPedido {

    private const val MINIMO_LINEAS = 15
    private const val MINIMO_BASICOS = 2

    fun evaluar(
        productos: List<OrderSuggestion>,
        cantidadesFinales: Map<Int, String>? = null
    ): EvaluacionEficienciaPedido {

        val productosActivos =
            productos
                .filter { producto ->

                    val cantidadFinal =
                        if (cantidadesFinales != null) {
                            cantidadesFinales[
                                producto.idVista
                            ]
                                ?.trim()
                                ?.replace(',', '.')
                                ?.toDoubleOrNull()
                                ?: 0.0
                        } else {
                            producto.cantidadSugerida
                                ?: 0.0
                        }

                    cantidadFinal > 0.0
                }
                .distinctBy {
                    it.idVista
                }

        val categoriasBasicas =
            productosActivos
                .mapNotNull {
                    categoriaBasico(it.producto)
                }
                .distinct()
                .sorted()

        val lineas =
            productosActivos.size

        val basicosPresentes =
            categoriasBasicas.size

        val cumpleMinimoLineas =
            lineas >= MINIMO_LINEAS

        val cumpleMinimoBasicos =
            basicosPresentes >= MINIMO_BASICOS

        val esPedidoEficiente =
            cumpleMinimoLineas &&
                    cumpleMinimoBasicos

        val motivo =
            when {
                esPedidoEficiente ->
                    "Pedido con volumen suficiente y con " +
                            "$basicosPresentes básico(s)."

                !cumpleMinimoLineas &&
                        !cumpleMinimoBasicos ->
                    "Pedido pequeño: $lineas línea(s) y " +
                            "$basicosPresentes básico(s). " +
                            "Para que resulte eficiente se buscan " +
                            "al menos $MINIMO_LINEAS líneas y " +
                            "$MINIMO_BASICOS básicos."

                !cumpleMinimoLineas ->
                    "Pedido pequeño: $lineas línea(s). " +
                            "Para que resulte eficiente se buscan " +
                            "al menos $MINIMO_LINEAS líneas."

                else ->
                    "Faltan básicos: hay " +
                            "$basicosPresentes. " +
                            "Se buscan al menos $MINIMO_BASICOS."
            }

        return EvaluacionEficienciaPedido(
            lineas = lineas,
            basicosPresentes = basicosPresentes,
            cumpleMinimoLineas = cumpleMinimoLineas,
            cumpleMinimoBasicos = cumpleMinimoBasicos,
            esPedidoEficiente = esPedidoEficiente,
            basicosDetectados = categoriasBasicas,
            motivo = motivo
        )
    }

    private fun categoriaBasico(
        nombreOriginal: String
    ): String? {

        val nombre =
            normalizar(nombreOriginal)

        /*
         * MAÍZ DULCE / MUSHROOM
         */
        if (
            nombre.contains("MAIZ") &&
            (
                    nombre.contains("DULCE") ||
                            nombre.contains("MUSHROOM")
                    )
        ) {
            return "MAÍZ DULCE"
        }

        /*
         * MAÍZ SALADO
         */
        if (
            nombre.contains("MAIZ") &&
            (
                    nombre.contains("SALAD") ||
                            nombre.contains("SALADO")
                    )
        ) {
            return "MAÍZ SALADO"
        }

        /*
         * SAL
         *
         * Se exige palabra completa para no confundir
         * "SAL" con "SALSA".
         */
        if (
            Regex("\\bSAL\\b")
                .containsMatchIn(nombre)
        ) {
            return "SAL"
        }

        /*
         * ACEITE
         */
        if (
            nombre.contains("ACEITE")
        ) {
            return "ACEITE"
        }

        /*
         * VASOS
         *
         * Cualquier tamaño cuenta como una única categoría.
         */
        if (
            nombre.contains("VASO")
        ) {
            return "VASOS"
        }

        /*
         * CARTONES
         *
         * Cualquier tamaño cuenta como una única categoría.
         */
        if (
            nombre.contains("CARTON")
        ) {
            return "CARTONES"
        }

        return null
    }

    private fun normalizar(
        textoOriginal: String
    ): String {

        return textoOriginal
            .uppercase()
            .replace('Á', 'A')
            .replace('É', 'E')
            .replace('Í', 'I')
            .replace('Ó', 'O')
            .replace('Ú', 'U')
            .replace('Ü', 'U')
            .replace('Ñ', 'N')
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }
}