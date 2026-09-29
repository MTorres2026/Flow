package com.example.flow

import android.content.Context
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Contexto semanal que el motor puede recibir sin convertir asistencia
 * directamente en cantidad de producto.
 *
 * multiplicadorDemanda solo debe venir de una capa de contexto que haya
 * demostrado relación con el producto. Por defecto NO se aplica.
 */
data class ContextoPedidoSemanal(
    val multiplicadorDemanda: Double = 1.0,
    val aplicarAjusteDemanda: Boolean = false,
    val referencia: String = "",
    val nivelConfianza: String = "NO_APLICADO"
)

data class PropuestaPedido(
    val idVista: Int,
    val producto: String,
    val estado: String,
    val cantidadSugerida: Double,
    val unidadPedido: String,
    val motivo: String,
    val nivelEvidencia: String,

    // Información interna para explicación/revisión.
    val tipoProducto: String = "NORMAL",
    val necesitaRevision: Boolean = false,
    val consumoPrevisto: Double = 0.0,
    val stockObjetivo: Double = 0.0,
    val cantidadNecesariaUnidades: Double = 0.0,
    val pedidoHistoricoMediano: Double? = null,
    val frecuenciaPedidoHistorico: Double = 0.0
)

object FlowMotorPedido {

    private const val TIPO_CRITICO = "CRITICO"
    private const val TIPO_NORMAL = "NORMAL"
    private const val TIPO_AMBIGUO = "AMBIGUO"
    private const val TIPO_EXCLUIDO = "EXCLUIDO"

    /**
     * Nombres que el usuario ha definido expresamente como ambiguos.
     * No se deben resolver automáticamente aunque exista información parcial
     * en el maestro.
     */
    private val productosAmbiguos = listOf(
        "ESTUCHADOS 3.90",
        "ESTUCHADOS 3,90",
        "KIT KAT",
        "KOJACK",
        "KOJAK",
        "HARIBO 100GR",
        "HARIBO 100 GRS",
        "HARIBO 100GRS",
        "HARIBO 100 GR"
    )

    /**
     * Bebidas que no se deben pedir por Conway según la regla operativa actual.
     * La cerveza es una excepción documentada y queda fuera de esta lista.
     */
    private val patronesBebidasExcluidas = listOf(
        "AGUA",
        "JARABE",
        "MONSTER",
        "CACAOLAT",
        "BEBIDA",
        "REFRESCO",
        "COCA-COLA",
        "COCA COLA",
        "FANTA",
        "SPRITE",
        "PEPSI",
        "KAS"
    )

    /**
     * Productos donde un quiebre puede ser especialmente problemático y,
     * por tanto, se usa una cobertura algo mayor.
     */
    private val patronesCriticos = listOf(
        "MAIZ",
        "MAÍZ",
        "ACEITE",
        "SAL",
        "VASO",
        "CARTON",
        "CARTÓN",
        "DORITOS",
        "CHEDDAR",
        "SAZONADOR",
        "MANTEQUILLA",
        "AZUCAR",
        "AZÚCAR"
    )

    /**
     * Entrada compatible con el código actual.
     */
    fun analizar(
        producto: ProductoFlow,
        historico: ResumenHistoricoProducto?
    ): PropuestaPedido {
        return analizar(
            producto = producto,
            historico = historico,
            pedidosHistoricos = emptyList(),
            contexto = ContextoPedidoSemanal()
        )
    }

    /**
     * Motor completo para un producto.
     *
     * Orden de trabajo:
     * 1) identificar/clasificar;
     * 2) consumo previsto;
     * 3) aplicar contexto si existe;
     * 4) decidir si hace falta reponer;
     * 5) convertir necesidad de inventario a cajas/packs;
     * 6) marcar incertidumbre sin inventar datos.
     */
    fun analizar(
        producto: ProductoFlow,
        historico: ResumenHistoricoProducto?,
        pedidosHistoricos: List<LineaPedidoHistorico>,
        contexto: ContextoPedidoSemanal = ContextoPedidoSemanal()
    ): PropuestaPedido {

        val tipo = clasificarProducto(producto.producto)

        if (tipo == TIPO_AMBIGUO) {
            return propuestaEspecial(
                producto = producto,
                estado = "AMBIGUO",
                tipoProducto = TIPO_AMBIGUO,
                motivo = "Producto definido como ambiguo por las reglas de FLOW. Requiere revisión de la encargada antes de poder incorporarlo al pedido.",
                nivelEvidencia = historico?.nivelEvidencia ?: "DESCONOCIDO",
                revision = true
            )
        }

        if (tipo == TIPO_EXCLUIDO) {
            return propuestaEspecial(
                producto = producto,
                estado = "NO PEDIR",
                tipoProducto = TIPO_EXCLUIDO,
                motivo = "Producto identificado como bebida fuera de Conway según la regla operativa actual.",
                nivelEvidencia = historico?.nivelEvidencia ?: "OBSERVADO",
                revision = false
            )
        }

        if (producto.idVista <= 0) {
            return propuestaEspecial(
                producto = producto,
                estado = "SIN EVIDENCIA",
                tipoProducto = tipo,
                motivo = "Producto sin ID VISTA válido.",
                nivelEvidencia = "INSUFICIENTE",
                revision = true
            )
        }

        if (
            producto.idErp.isBlank() ||
            producto.umErp.isBlank() ||
            producto.idConway.isBlank() ||
            producto.unidadPedidoFlow.isBlank()
        ) {
            return propuestaEspecial(
                producto = producto,
                estado = "SIN EVIDENCIA",
                tipoProducto = tipo,
                motivo = "Faltan datos estructurales necesarios para generar un pedido seguro. No se inventa ninguna correspondencia.",
                nivelEvidencia = "INSUFICIENTE",
                revision = true
            )
        }

        val consumoPrevistoBase = calcularConsumoPrevisto(
            producto = producto,
            historico = historico
        )

        val consumoPrevisto = aplicarContexto(
            consumo = consumoPrevistoBase,
            contexto = contexto
        )

        /*
         * COBERTURA ACTUAL DEFINIDA PARA FLOW:
         * críticos = 2,5 semanas
         * normales = 2 semanas
         */
        val coberturaSemanas =
            if (tipo == TIPO_CRITICO) 2.5 else 2.0

        val variabilidad = historico?.let {
            max(
                0.0,
                it.consumoMaximo - it.consumoMinimo
            )
        } ?: 0.0

        // La variabilidad aporta un colchón moderado.
        val seguridad = calcularSeguridad(
            consumoPrevisto = consumoPrevisto,
            variabilidad = variabilidad,
            tipo = tipo
        )

        val stockObjetivo =
            (consumoPrevisto * coberturaSemanas) + seguridad

        val stockActual = max(
            0.0,
            producto.stockFinal
        )

        val necesidadUnidades =
            max(
                0.0,
                stockObjetivo - stockActual
            )

        val pedidosProducto = pedidosHistoricos.filter {
            it.idConway == producto.idConway
        }

        val cantidadesPedido =
            pedidosProducto
                .map { it.cantidad }
                .filter { it > 0.0 }

        val pedidoHistoricoMediano =
            mediana(cantidadesPedido)

        val semanasConPedido =
            pedidosProducto
                .filter { it.cantidad > 0.0 }
                .map { it.fechaPedido }
                .filter { it.isNotBlank() }
                .distinct()
                .size

        val semanasReferencia =
            historico?.semanas ?: 0

        val frecuenciaPedidoHistorico =
            if (semanasReferencia > 0) {
                (
                        semanasConPedido.toDouble() /
                                semanasReferencia.toDouble()
                        ).coerceIn(0.0, 1.0)
            } else {
                0.0
            }

        /*
         * Sin consumo observable, sin histórico y sin pedidos reales:
         * no hay base suficiente para afirmar que hay que pedir.
         */
        if (
            consumoPrevisto <= 0.0 &&
            cantidadesPedido.isEmpty()
        ) {
            return PropuestaPedido(
                idVista = producto.idVista,
                producto = producto.producto,
                estado = "NO PEDIR",
                cantidadSugerida = 0.0,
                unidadPedido = producto.unidadPedidoFlow,
                motivo = "No existe consumo observable ni histórico de pedidos suficiente para justificar reposición automática.",
                nivelEvidencia = historico?.nivelEvidencia ?: "INSUFICIENTE",
                tipoProducto = tipo,
                necesitaRevision = historico == null,
                consumoPrevisto = consumoPrevisto,
                stockObjetivo = stockObjetivo,
                cantidadNecesariaUnidades = 0.0,
                pedidoHistoricoMediano = pedidoHistoricoMediano,
                frecuenciaPedidoHistorico = frecuenciaPedidoHistorico
            )
        }

        /*
         * PRIMERA DECISIÓN:
         * ¿HAY QUE PEDIR?
         */
        if (necesidadUnidades <= 0.0) {

            val motivo =
                if (stockActual >= stockObjetivo) {
                    "Stock actual suficiente para cubrir el objetivo calculado con el consumo previsto y la cobertura del producto."
                } else {
                    "No se detecta déficit de reposición con los datos disponibles."
                }

            return PropuestaPedido(
                idVista = producto.idVista,
                producto = producto.producto,
                estado = "NO PEDIR",
                cantidadSugerida = 0.0,
                unidadPedido = producto.unidadPedidoFlow,
                motivo = motivo,
                nivelEvidencia = historico?.nivelEvidencia ?: "OBSERVADO",
                tipoProducto = tipo,
                necesitaRevision = false,
                consumoPrevisto = consumoPrevisto,
                stockObjetivo = stockObjetivo,
                cantidadNecesariaUnidades = 0.0,
                pedidoHistoricoMediano = pedidoHistoricoMediano,
                frecuenciaPedidoHistorico = frecuenciaPedidoHistorico
            )
        }

        /*
         * CONVERSIÓN SEGURA:
         * primero se mira la presentación física y su unidad;
         * después se usa UNIDADES POR CAJA/PACK.
         *
         * Ejemplo:
         *   PZA + "Caja 10 Piezas" -> 10
         *   KGS + "Caja 10 Kilos"  -> 10
         *   LTS + "Caja 15 Litros" -> 15
         *
         * Esto evita tratar "Caja con 1 caja" como una unidad física.
         */
        val unidadesPorCajaPack = parsearUnidadesPorPack(
            unidadesPorCajaPack = producto.unidadesPorCajaPack,
            presentacion = producto.presentacion,
            umInforme = producto.umInforme
        )

        if (
            unidadesPorCajaPack == null ||
            unidadesPorCajaPack <= 0.0
        ) {
            return PropuestaPedido(
                idVista = producto.idVista,
                producto = producto.producto,
                estado = "REVISAR",
                cantidadSugerida = 0.0,
                unidadPedido = producto.unidadPedidoFlow,
                motivo = "Existe necesidad de reposición, pero no se ha podido convertir de forma segura la necesidad de inventario a cajas/packs de Conway.",
                nivelEvidencia = historico?.nivelEvidencia ?: "OBSERVADO",
                tipoProducto = tipo,
                necesitaRevision = true,
                consumoPrevisto = consumoPrevisto,
                stockObjetivo = stockObjetivo,
                cantidadNecesariaUnidades = necesidadUnidades,
                pedidoHistoricoMediano = pedidoHistoricoMediano,
                frecuenciaPedidoHistorico = frecuenciaPedidoHistorico
            )
        }

        var cantidadPacks =
            ceil(
                necesidadUnidades / unidadesPorCajaPack
            )
                .toInt()
                .coerceAtLeast(1)

        /*
         * El histórico real de pedidos se usa como referencia de plausibilidad,
         * no como sustituto del cálculo de necesidad.
         */
        if (
            cantidadPacks == 1 &&
            pedidoHistoricoMediano != null &&
            stockActual <= consumoPrevisto * 0.25 &&
            pedidoHistoricoMediano >= 2.0
        ) {
            cantidadPacks =
                pedidoHistoricoMediano
                    .roundToInt()
                    .coerceAtLeast(1)
        }

        val revisionPorDesviacion =
            pedidoHistoricoMediano?.let {
                cantidadPacks > (it * 2.0) ||
                        cantidadPacks < (it * 0.5)
            } ?: false

        val motivo = buildString {

            append("Reposición necesaria. ")

            append("Consumo previsto: ")
            append(formatear(consumoPrevisto))
            append(" ")
            append(producto.umInforme)

            append("; stock actual: ")
            append(formatear(stockActual))

            append("; stock objetivo: ")
            append(formatear(stockObjetivo))
            append("; necesidad: ")
            append(formatear(necesidadUnidades))
            append(" ")
            append(producto.umInforme)

            append("; pedido: ")
            append(cantidadPacks)
            append(" ")
            append(producto.unidadPedidoFlow)

            if (pedidoHistoricoMediano != null) {
                append(". Mediana histórica de pedido: ")
                append(formatear(pedidoHistoricoMediano))
                append(" ")
                append(producto.unidadPedidoFlow)
            }

            if (revisionPorDesviacion) {
                append(". La cantidad resultante se desvía mucho del patrón histórico y debe revisarse.")
            }

            if (contexto.aplicarAjusteDemanda) {
                append(". Ajuste de contexto aplicado: ")
                append(formatear(contexto.multiplicadorDemanda))

                if (contexto.referencia.isNotBlank()) {
                    append(" (")
                    append(contexto.referencia)
                    append(")")
                }
            }
        }

        return PropuestaPedido(
            idVista = producto.idVista,
            producto = producto.producto,
            estado = "PEDIR",
            cantidadSugerida = cantidadPacks.toDouble(),
            unidadPedido = producto.unidadPedidoFlow,
            motivo = motivo,
            nivelEvidencia = historico?.nivelEvidencia ?: "OBSERVADO",
            tipoProducto = tipo,
            necesitaRevision =
                revisionPorDesviacion ||
                        historico == null ||
                        (
                                contexto.aplicarAjusteDemanda &&
                                        contexto.nivelConfianza != "ALTO"
                                ),
            consumoPrevisto = consumoPrevisto,
            stockObjetivo = stockObjetivo,
            cantidadNecesariaUnidades = necesidadUnidades,
            pedidoHistoricoMediano = pedidoHistoricoMediano,
            frecuenciaPedidoHistorico = frecuenciaPedidoHistorico
        )
    }

    /**
     * Mantiene la llamada colectiva anterior.
     */
    fun analizarTodos(
        productos: List<ProductoFlow>,
        context: Context
    ): ResultadoPedidoFlow {
        return analizarTodos(
            productos = productos,
            context = context,
            contexto = ContextoPedidoSemanal()
        )
    }

    fun analizarTodos(
        productos: List<ProductoFlow>,
        context: Context,
        contexto: ContextoPedidoSemanal
    ): ResultadoPedidoFlow {

        val pedidosHistoricos =
            try {
                FlowPedidosHistoricos.cargar(context)
            } catch (_: Exception) {
                emptyList()
            }

        val propuestas =
            productos.map { producto ->

                val historial =
                    FlowHistorial.obtenerHistorialProducto(
                        context = context,
                        idVista = producto.idVista
                    )

                analizar(
                    producto = producto,
                    historico =
                        FlowAnalisisHistorico.analizarProducto(
                            historial = historial,
                            idVista = producto.idVista
                        ),
                    pedidosHistoricos = pedidosHistoricos,
                    contexto = contexto
                )
            }

        return ResultadoPedidoFlow(
            propuestas = propuestas
        )
    }

    private fun clasificarProducto(
        nombreOriginal: String
    ): String {

        val nombre =
            normalizar(nombreOriginal)

        if (
            productosAmbiguos.any {
                normalizar(it) in nombre
            }
        ) {
            return TIPO_AMBIGUO
        }

        val esCerveza =
            nombre.contains("CERVEZA") ||
                    nombre.contains("ESTRELLA GALICIA") ||
                    nombre.contains("1906")

        if (
            !esCerveza &&
            patronesBebidasExcluidas.any {
                normalizar(it) in nombre
            }
        ) {
            return TIPO_EXCLUIDO
        }

        return if (
            patronesCriticos.any {
                normalizar(it) in nombre
            }
        ) {
            TIPO_CRITICO
        } else {
            TIPO_NORMAL
        }
    }

    private fun calcularConsumoPrevisto(
        producto: ProductoFlow,
        historico: ResumenHistoricoProducto?
    ): Double {

        val usoActual =
            max(
                0.0,
                producto.uso
            )

        val consumos =
            historico?.consumos
                ?.filter { it >= 0.0 }
                ?: emptyList()

        if (consumos.isEmpty()) {
            return usoActual
        }

        val medianaHistorica =
            mediana(consumos) ?: 0.0

        val mediaHistorica =
            consumos.average()

        val ultimos =
            consumos.takeLast(3)

        val mediaReciente =
            if (ultimos.isNotEmpty()) {
                ultimos.average()
            } else {
                mediaHistorica
            }

        /*
         * Mediana para resistir anomalías,
         * media reciente para reaccionar a cambios reales
         * y uso actual como señal de la semana que acaba.
         */
        return (
                medianaHistorica * 0.40 +
                        mediaHistorica * 0.20 +
                        mediaReciente * 0.20 +
                        usoActual * 0.20
                )
            .coerceAtLeast(0.0)
    }

    private fun aplicarContexto(
        consumo: Double,
        contexto: ContextoPedidoSemanal
    ): Double {

        if (!contexto.aplicarAjusteDemanda) {
            return consumo
        }

        return (
                consumo *
                        contexto.multiplicadorDemanda
                            .coerceIn(
                                0.75,
                                1.30
                            )
                )
            .coerceAtLeast(0.0)
    }

    private fun calcularSeguridad(
        consumoPrevisto: Double,
        variabilidad: Double,
        tipo: String
    ): Double {

        if (consumoPrevisto <= 0.0) {
            return 0.0
        }

        val factorVariabilidad =
            if (tipo == TIPO_CRITICO) {
                0.20
            } else {
                0.10
            }

        return variabilidad * factorVariabilidad
    }

    /**
     * Convierte de forma segura el tamaño físico de una caja/pack
     * a la unidad utilizada por el informe.
     *
     * Prioridad:
     * 1. PRESENTACIÓN + unidad física compatible.
     * 2. UNIDADES POR CAJA/PACK.
     *
     * No interpreta "Caja con 1 caja" como 1 unidad física.
     */
    private fun parsearUnidadesPorPack(
        unidadesPorCajaPack: String,
        presentacion: String,
        umInforme: String
    ): Double? {

        fun extraerNumero(
            texto: String
        ): Double? {

            if (!datoDisponible(texto)) {
                return null
            }

            val normalizado =
                texto
                    .replace(',', '.')
                    .replace(" ", "")

            val numero =
                Regex("[0-9]+(?:\\.[0-9]+)?")
                    .find(normalizado)
                    ?.value
                    ?: return null

            return numero.toDoubleOrNull()
        }

        val unidad =
            normalizar(umInforme)

        val presentacionNormalizada =
            normalizar(presentacion)

        /*
         * Piezas / unidades.
         */
        if (
            unidad.contains("PZA") ||
            unidad == "UN" ||
            unidad.contains("UNIDAD")
        ) {

            val numero =
                Regex(
                    """([0-9]+(?:[.,][0-9]+)?)\s*(PIEZAS?|PZAS?|UNIDADES?|UND(?:ADES)?)\b"""
                )
                    .find(presentacionNormalizada)
                    ?.groupValues
                    ?.getOrNull(1)

            if (numero != null) {

                return numero
                    .replace(',', '.')
                    .toDoubleOrNull()
            }
        }

        /*
         * Kilogramos.
         */
        if (
            unidad.contains("KGS") ||
            unidad == "KG"
        ) {

            val numero =
                Regex(
                    """([0-9]+(?:[.,][0-9]+)?)\s*(KILOS?|KGS?|KG)\b"""
                )
                    .find(presentacionNormalizada)
                    ?.groupValues
                    ?.getOrNull(1)

            if (numero != null) {

                return numero
                    .replace(',', '.')
                    .toDoubleOrNull()
            }
        }

        /*
         * Litros.
         */
        if (
            unidad.contains("LTS") ||
            unidad == "LT"
        ) {

            val numero =
                Regex(
                    """([0-9]+(?:[.,][0-9]+)?)\s*(LITROS?|LTS?|LT)\b"""
                )
                    .find(presentacionNormalizada)
                    ?.groupValues
                    ?.getOrNull(1)

            if (numero != null) {

                return numero
                    .replace(',', '.')
                    .toDoubleOrNull()
            }
        }

        /*
         * Si la presentación no tiene tamaño físico interpretable,
         * utilizamos el dato explícito del SUPERMAESTRO.
         *
         * Ejemplo:
         *   "Caja 1000 Piezas" -> ya habría entrado arriba.
         *
         * Pero:
         *   presentacion = "Caja"
         *   unidadesPorCajaPack = "1000"
         *
         * sigue siendo válido.
         *
         * En cambio:
         *   presentacion = "Caja con 1 caja"
         *   unidadesPorCajaPack = "—"
         *
         * devuelve null.
         */
        return extraerNumero(
            unidadesPorCajaPack
        )
    }

    private fun datoDisponible(
        texto: String
    ): Boolean {

        val normalizado =
            texto
                .trim()
                .uppercase()

        return normalizado.isNotBlank() &&
                normalizado != "—" &&
                normalizado != "-" &&
                normalizado != "N/A" &&
                normalizado != "NULL"
    }

    private fun mediana(
        valoresOriginales: List<Double>
    ): Double? {

        if (valoresOriginales.isEmpty()) {
            return null
        }

        val valores =
            valoresOriginales.sorted()

        val mitad =
            valores.size / 2

        return if (
            valores.size % 2 == 0
        ) {
            (
                    valores[mitad - 1] +
                            valores[mitad]
                    ) / 2.0
        } else {
            valores[mitad]
        }
    }

    private fun propuestaEspecial(
        producto: ProductoFlow,
        estado: String,
        tipoProducto: String,
        motivo: String,
        nivelEvidencia: String,
        revision: Boolean
    ): PropuestaPedido {

        return PropuestaPedido(
            idVista = producto.idVista,
            producto = producto.producto,
            estado = estado,
            cantidadSugerida = 0.0,
            unidadPedido = producto.unidadPedidoFlow,
            motivo = motivo,
            nivelEvidencia = nivelEvidencia,
            tipoProducto = tipoProducto,
            necesitaRevision = revision
        )
    }

    private fun normalizar(
        texto: String
    ): String {

        return texto
            .uppercase()
            .replace('Á', 'A')
            .replace('É', 'E')
            .replace('Í', 'I')
            .replace('Ó', 'O')
            .replace('Ú', 'U')
            .replace(
                Regex("\\s+"),
                " "
            )
            .trim()
    }

    private fun formatear(
        numero: Double
    ): String {

        return if (
            numero % 1.0 == 0.0
        ) {
            numero
                .roundToInt()
                .toString()
        } else {
            "%.2f".format(numero)
        }
    }
}