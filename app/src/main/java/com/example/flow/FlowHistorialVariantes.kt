package com.example.flow

/**
 * Historial específico de variantes.
 *
 * IMPORTANTE:
 * Una familia puede tener muchas variantes con el mismo idVista.
 * Por eso aquí la identidad se basa en:
 *
 * idVista + idErp + umErp
 */
data class RegistroHistorialVarianteFlow(
    val clave: ClaveVarianteFlow,
    val producto: String,
    val semana: String,
    val uso: Double,
    val stockFinal: Double
)

object FlowHistorialVariantes {

    private val registros = mutableListOf<RegistroHistorialVarianteFlow>()

    fun guardar(
        producto: ProductoFlow,
        semana: String
    ) {
        val clave = FlowClaveVariante.crear(producto)

        if (!FlowClaveVariante.esValida(clave)) {
            return
        }

        registros.add(
            RegistroHistorialVarianteFlow(
                clave = clave,
                producto = producto.producto,
                semana = semana,
                uso = producto.uso,
                stockFinal = producto.stockFinal
            )
        )
    }

    fun obtener(
        producto: ProductoFlow
    ): List<RegistroHistorialVarianteFlow> {

        val clave = FlowClaveVariante.crear(producto)

        return registros.filter {
            it.clave == clave
        }
    }

    fun obtenerPorClave(
        clave: ClaveVarianteFlow
    ): List<RegistroHistorialVarianteFlow> {

        return registros.filter {
            it.clave == clave
        }
    }

    fun limpiar() {
        registros.clear()
    }
}