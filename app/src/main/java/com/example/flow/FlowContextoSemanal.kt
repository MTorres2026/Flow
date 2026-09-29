package com.example.flow

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.abs

/**
 * Perfil semanal de asistencia.
 *
 * La asistencia sirve para identificar semanas comparables.
 * No se transforma directamente en un porcentaje de pedido.
 */
data class PerfilAsistenciaSemana(
    val semanaIso: Int,
    val presupuesto: Double,
    val proyectada: Double,
    val real: Double?
)

object FlowContextoSemanal {

    /*
     * Datos comprobados de la fuente real:
     * Plantilla Proyectados 2026 Los Prados.xlsx
     *
     * Son las semanas que necesitamos inicialmente para poder
     * comparar los históricos de inventario disponibles.
     */
    private val asistencia2026 =
        mapOf(

            29 to PerfilAsistenciaSemana(
                semanaIso = 29,
                presupuesto = 6226.0,
                proyectada = 4597.0,
                real = 6279.0
            ),

            33 to PerfilAsistenciaSemana(
                semanaIso = 33,
                presupuesto = 3632.0,
                proyectada = 5272.0,
                real = 6109.0
            ),

            34 to PerfilAsistenciaSemana(
                semanaIso = 34,
                presupuesto = 3493.0,
                proyectada = 5633.0,
                real = 5939.0
            ),

            40 to PerfilAsistenciaSemana(
                semanaIso = 40,
                presupuesto = 2946.0,
                proyectada = 3130.0,
                real = null
            )
        )

    /**
     * Construye el contexto semanal actual.
     *
     * NO aplica multiplicadores automáticos.
     *
     * La asistencia queda como información contextual hasta que
     * exista evidencia suficiente para relacionarla con cada producto.
     */
    fun construirContexto(
        fechaInformeActual: String?
    ): ContextoPedidoSemanal {

        val semana =
            semanaIsoDeFecha(
                fechaInformeActual
            )

        val perfil =
            semana?.let {
                asistencia2026[it]
            }

        if (perfil == null) {
            return ContextoPedidoSemanal(
                multiplicadorDemanda = 1.0,
                aplicarAjusteDemanda = false,
                referencia =
                    "Sin perfil semanal de asistencia disponible para el informe.",
                nivelConfianza = "NO_APLICADO"
            )
        }

        val diferencia =
            if (perfil.presupuesto > 0.0) {
                (
                        perfil.proyectada /
                                perfil.presupuesto
                        ) - 1.0
            } else {
                0.0
            }

        val referencia =
            "Semana ${perfil.semanaIso} | " +
                    "asistencia presupuestada=" +
                    formatear(perfil.presupuesto) +
                    " | proyectada=" +
                    formatear(perfil.proyectada) +
                    " | desviación respecto al presupuesto=" +
                    formatearPorcentaje(diferencia) +
                    " | contexto informativo, sin multiplicador automático."

        return ContextoPedidoSemanal(
            multiplicadorDemanda = 1.0,
            aplicarAjusteDemanda = false,
            referencia = referencia,
            nivelConfianza = "NO_APLICADO"
        )
    }

    /**
     * Recibe exactamente el tipo que devuelve
     * FlowHistorial.obtenerHistorialProducto():
     *
     * List<JSONObject>
     *
     * Devuelve una nueva lista con las semanas comparables.
     */
    fun filtrarHistorialPorSemanasComparables(
        historial: List<JSONObject>,
        fechaInformeActual: String?
    ): List<JSONObject> {

        val semanaActual =
            semanaIsoDeFecha(fechaInformeActual)
                ?: return historial

        val perfilActual =
            asistencia2026[semanaActual]
                ?: return historial

        if (perfilActual.presupuesto <= 0.0) {
            return historial
        }

        /*
         * Comportamiento de asistencia de la semana actual:
         *
         * proyectada / presupuesto
         */
        val ratioActual =
            perfilActual.proyectada /
                    perfilActual.presupuesto

        val comparables =
            mutableListOf<JSONObject>()

        for (elemento in historial) {

            val fechaHistorica =
                elemento.optString(
                    "fechaInforme",
                    ""
                )

            val semanaHistorica =
                semanaIsoDeFecha(fechaHistorica)
                    ?: continue

            val perfilHistorico =
                asistencia2026[semanaHistorica]
                    ?: continue

            val asistenciaReal =
                perfilHistorico.real
                    ?: continue

            if (perfilHistorico.presupuesto <= 0.0) {
                continue
            }

            /*
             * Comportamiento REAL de esa semana:
             *
             * real / presupuesto
             */
            val ratioHistorico =
                asistenciaReal /
                        perfilHistorico.presupuesto

            val diferenciaRelativa =
                abs(
                    ratioHistorico -
                            ratioActual
                )

            /*
             * Permitimos una diferencia máxima de
             * 20 puntos porcentuales.
             *
             * Esto NO modifica cantidades.
             * Solo decide qué semanas son comparables.
             */
            if (diferenciaRelativa <= 0.20) {
                comparables.add(elemento)
            }
        }

        /*
         * Con menos de 2 semanas comparables,
         * no eliminamos información histórica.
         *
         * Devolvemos todo el histórico.
         */
        if (comparables.size < 2) {
            return historial
        }

        return comparables
    }

    /**
     * Convierte una fecha del histórico a número de semana ISO.
     *
     * Formatos admitidos:
     * yyyy-MM-dd
     * dd/MM/yyyy
     * yyyyMMdd
     *
     * Compatible con minSdk 24:
     * no utiliza java.time.
     */
    private fun semanaIsoDeFecha(
        fechaTexto: String?
    ): Int? {

        if (fechaTexto.isNullOrBlank()) {
            return null
        }

        val texto =
            fechaTexto.trim()

        val formatos =
            listOf(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "yyyyMMdd"
            )

        var fecha: java.util.Date? = null

        for (patron in formatos) {

            try {
                val formato =
                    SimpleDateFormat(
                        patron,
                        Locale.US
                    )

                formato.isLenient = false

                fecha =
                    formato.parse(texto)

                if (fecha != null) {
                    break
                }

            } catch (_: Exception) {
                // Probamos el siguiente formato.
            }
        }

        if (fecha == null) {
            return null
        }

        /*
         * Configuración ISO:
         * lunes = primer día de semana
         * al menos 4 días en la primera semana
         */
        val calendario =
            GregorianCalendar(
                Locale.US
            )

        calendario.firstDayOfWeek =
            Calendar.MONDAY

        calendario.minimalDaysInFirstWeek =
            4

        calendario.time =
            fecha

        return calendario.get(
            Calendar.WEEK_OF_YEAR
        )
    }

    private fun formatear(
        numero: Double
    ): String {

        return if (numero % 1.0 == 0.0) {
            numero
                .toInt()
                .toString()
        } else {
            "%.2f".format(
                Locale.US,
                numero
            )
        }
    }

    private fun formatearPorcentaje(
        numero: Double
    ): String {

        return "%+.1f%%".format(
            Locale.US,
            numero * 100.0
        )
    }
}