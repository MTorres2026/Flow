package com.example.flow

data class OrderSuggestion(
    val idVista: Int,
    val producto: String,

    // Identidad y datos de pedido
    val idErp: String,
    val umErp: String,
    val idConway: String,
    val denominacionLargaConway: String,
    val presentacion: String,
    val unidadesPorCajaPack: String,
    val unidadPedidoFlow: String,
    val estado: String,
    val fuente: String,
    val notaRegla: String,

    // Resultado del análisis histórico
    val debeRevisarse: Boolean,
    val cantidadSugerida: Double?,
    val nivelEvidencia: String,
    val numeroEpisodios: Int,
    val consumoMedio: Double?,
    val stockActualRegistrado: Double?,
    val motivo: String
)