package com.example.flow

import org.json.JSONObject

data class EpisodioHistorico(
    val fechaInforme: String,
    val stockInicial: Double?,
    val ingresos: Double?,
    val transferencias: Double?,
    val ajustes: Double?,
    val desperdiciado: Double?,
    val ventas: Double?,
    val uso: Double?,
    val stockFinal: Double?,

    // Cálculo de reconciliación del episodio.
    //
    // Según la estructura del informe:
    // stock inicial
    // + ingresos
    // + transferencias
    // + ajustes
    // - desperdiciado
    // - ventas
    // - uso
    // - stock final
    //
    // Si el resultado es 0, el balance de los datos del informe cuadra.
    // NO significa automáticamente "consumo real".
    val descuadreMovimiento: Double?,

    // Estado estructural del episodio:
    // COMPLETO   = están todos los campos necesarios.
    // INCOMPLETO = falta al menos un dato necesario.
    val estado: String
)

data class ResumenHistoricoProducto(
    val idVista: Int,
    val producto: String,
    val semanas: Int,
    val semanasConDatosCompletos: Int,
    val consumos: List<Double>,
    val stockIniciales: List<Double>,
    val stockFinales: List<Double>,
    val ingresos: List<Double>,
    val mediaConsumo: Double,
    val consumoMinimo: Double,
    val consumoMaximo: Double,
    val variacionAbsoluta: Double,
    val nivelEvidencia: String,

    // Nueva capa de información histórica.
    val episodios: List<EpisodioHistorico> = emptyList(),
    val semanasConUso: Int = 0,
    val semanasSinUso: Int = 0
)

object FlowAnalisisHistorico {

    fun analizarProducto(
        historial: List<JSONObject>,
        idVista: Int
    ): ResumenHistoricoProducto? {

        val registros = historial.filter {
            it.optInt("idVista") == idVista
        }

        if (registros.isEmpty()) {
            return null
        }

        val episodios = registros.map { registro ->

            val fechaInforme =
                registro.optString("fechaInforme")

            val stockInicial =
                leerNumero(registro, "stockInicial")

            val ingresos =
                leerNumero(registro, "ingresos")

            val transferencias =
                leerNumero(registro, "transferencias")

            val ajustes =
                leerNumero(registro, "ajustes")

            val desperdiciado =
                leerNumero(registro, "desperdiciado")

            val ventas =
                leerNumero(registro, "ventas")

            val uso =
                leerNumero(registro, "uso")

            val stockFinal =
                leerNumero(registro, "stockFinal")

            val datosCompletos =
                stockInicial != null &&
                        ingresos != null &&
                        transferencias != null &&
                        ajustes != null &&
                        desperdiciado != null &&
                        ventas != null &&
                        uso != null &&
                        stockFinal != null

            val descuadreMovimiento =
                if (datosCompletos) {
                    stockInicial!! +
                            ingresos!! +
                            transferencias!! +
                            ajustes!! -
                            desperdiciado!! -
                            ventas!! -
                            uso!! -
                            stockFinal!!
                } else {
                    null
                }

            EpisodioHistorico(
                fechaInforme = fechaInforme,
                stockInicial = stockInicial,
                ingresos = ingresos,
                transferencias = transferencias,
                ajustes = ajustes,
                desperdiciado = desperdiciado,
                ventas = ventas,
                uso = uso,
                stockFinal = stockFinal,
                descuadreMovimiento = descuadreMovimiento,
                estado = if (datosCompletos) {
                    "COMPLETO"
                } else {
                    "INCOMPLETO"
                }
            )
        }

        // "consumos" conserva el nombre que ya utiliza FLOW,
        // pero aquí representa exclusivamente el campo OBSERVADO "uso".
        // No lo estamos presentando como un consumo calculado.
        val consumos =
            episodios.mapNotNull { it.uso }

        val stockIniciales =
            episodios.mapNotNull { it.stockInicial }

        val stockFinales =
            episodios.mapNotNull { it.stockFinal }

        val ingresos =
            episodios.mapNotNull { it.ingresos }

        val mediaConsumo =
            consumos.averageOrZero()

        val consumoMinimo =
            consumos.minOrNull() ?: 0.0

        val consumoMaximo =
            consumos.maxOrNull() ?: 0.0

        val variacionAbsoluta =
            if (consumos.isNotEmpty()) {
                consumoMaximo - consumoMinimo
            } else {
                0.0
            }

        val semanasConDatosCompletos =
            episodios.count {
                it.estado == "COMPLETO"
            }

        val nivelEvidencia =
            when {
                semanasConDatosCompletos < 2 ->
                    "INSUFICIENTE"

                semanasConDatosCompletos < 3 ->
                    "INICIAL"

                else ->
                    "OBSERVABLE"
            }

        return ResumenHistoricoProducto(
            idVista = idVista,
            producto = registros.first().optString("producto"),
            semanas = registros.size,
            semanasConDatosCompletos = semanasConDatosCompletos,
            consumos = consumos,
            stockIniciales = stockIniciales,
            stockFinales = stockFinales,
            ingresos = ingresos,
            mediaConsumo = mediaConsumo,
            consumoMinimo = consumoMinimo,
            consumoMaximo = consumoMaximo,
            variacionAbsoluta = variacionAbsoluta,
            nivelEvidencia = nivelEvidencia,

            episodios = episodios,
            semanasConUso = consumos.size,
            semanasSinUso = registros.size - consumos.size
        )
    }

    /**
     * Lee un número sin convertir automáticamente un dato ausente en 0.
     */
    private fun leerNumero(
        registro: JSONObject,
        campo: String
    ): Double? {

        if (!registro.has(campo) || registro.isNull(campo)) {
            return null
        }

        val valor =
            registro.optDouble(campo, Double.NaN)

        return if (valor.isNaN()) {
            null
        } else {
            valor
        }
    }

    private fun List<Double>.averageOrZero(): Double {
        return if (isEmpty()) {
            0.0
        } else {
            average()
        }
    }
}