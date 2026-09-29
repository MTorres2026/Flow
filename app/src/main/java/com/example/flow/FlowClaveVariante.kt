package com.example.flow

/**
 * Identidad estable de una variante dentro de una familia FLOW.
 *
 * IMPORTANTE:
 * No usamos solamente idVista porque todas las variantes
 * de una misma familia pueden compartirlo.
 */
data class ClaveVarianteFlow(
    val idFamiliaVista: Int,
    val idErp: String,
    val umErp: String
) {

    fun comoTexto(): String {
        return "$idFamiliaVista|$idErp|$umErp"
    }
}

object FlowClaveVariante {

    fun crear(
        producto: ProductoFlow
    ): ClaveVarianteFlow {

        return ClaveVarianteFlow(
            idFamiliaVista = producto.idVista,
            idErp = producto.idErp.trim(),
            umErp = producto.umErp.trim().uppercase()
        )
    }

    fun crear(
        idFamiliaVista: Int,
        idErp: String,
        umErp: String
    ): ClaveVarianteFlow {

        return ClaveVarianteFlow(
            idFamiliaVista = idFamiliaVista,
            idErp = idErp.trim(),
            umErp = umErp.trim().uppercase()
        )
    }

    fun esValida(
        clave: ClaveVarianteFlow
    ): Boolean {

        return clave.idFamiliaVista > 0 &&
                clave.idErp.isNotBlank() &&
                clave.umErp.isNotBlank()
    }
}