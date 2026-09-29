package com.example.flow

data class ProductoInforme(
    val idVista: Int,
    val um: String,
    val stockInicial: Double,
    val ingresos: Double,
    val transferencias: Double,
    val ajustes: Double,
    val desperdiciado: Double,
    val ventas: Double,
    val uso: Double,
    val stockFinal: Double,
    val coste: Double,
    val nombre: String
)

data class ProductoFlow(
    val idVista: Int,
    val producto: String,

    val umInforme: String,
    val stockInicial: Double,
    val ingresos: Double,
    val transferencias: Double,
    val ajustes: Double,
    val desperdiciado: Double,
    val ventas: Double,
    val uso: Double,
    val stockFinal: Double,
    val coste: Double,

    val idErp: String,
    val umErp: String,
    val idConway: String,
    val denominacionLargaConway: String,
    val presentacion: String,
    val unidadesPorCajaPack: String,
    val unidadPedidoFlow: String,
    val estado: String,
    val fuente: String,
    val notaRegla: String
)

object InformeParser {

    private val patronProducto = Regex(
        """^\s*(\d+)\s+(\S+)\s+(-?[\d.,]+(?:\s+|$)){9}$"""
    )

    private val patronSubtotal = Regex(
        """^\s*-?[\d.,]+(?:\s+-?[\d.,]+){7}\s*$"""
    )

    private fun numero(texto: String): Double {
        return texto
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
    }

    fun analizar(texto: String): List<ProductoInforme> {

        val lineas = texto.lines()
        val productos = mutableListOf<ProductoInforme>()

        var productoActual: ProductoInforme? = null
        val nombrePendiente = mutableListOf<String>()

        fun guardarProducto() {
            val producto = productoActual ?: return

            val nombre = nombrePendiente
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (nombre.isNotEmpty()) {
                productos.add(
                    producto.copy(nombre = nombre)
                )
            }

            productoActual = null
            nombrePendiente.clear()
        }

        for (lineaOriginal in lineas) {

            val linea = lineaOriginal.trim()

            if (linea.isEmpty()) {
                continue
            }

            val coincidencia = patronProducto.find(linea)

            if (coincidencia != null) {

                guardarProducto()

                val partes = linea.split(Regex("\\s+"))

                productoActual = ProductoInforme(
                    idVista = partes[0].toInt(),
                    um = partes[1],
                    stockInicial = numero(partes[2]),
                    ingresos = numero(partes[3]),
                    transferencias = numero(partes[4]),
                    ajustes = numero(partes[5]),
                    desperdiciado = numero(partes[6]),
                    ventas = numero(partes[7]),
                    uso = numero(partes[8]),
                    stockFinal = numero(partes[9]),
                    coste = numero(partes[10]),
                    nombre = ""
                )

                continue
            }

            if (patronSubtotal.matches(linea)) {
                guardarProducto()
                continue
            }

            if (productoActual != null) {

                val esCabecera = linea.startsWith("Yelmo Cines") ||
                        linea.startsWith("Informe de variaciones") ||
                        linea.startsWith("Avd.") ||
                        linea.startsWith("C.C ") ||
                        linea == "Madrid" ||
                        linea.startsWith("For Selected") ||
                        linea.startsWith("From:") ||
                        linea.startsWith("Until:") ||
                        linea.startsWith("Unit Opening") ||
                        linea.startsWith("Balance") ||
                        linea.startsWith("rencias") ||
                        linea.startsWith("diciado") ||
                        linea.startsWith("V:\\ReportFiles")

                if (!esCabecera) {
                    nombrePendiente.add(linea)
                }
            }
        }

        guardarProducto()

        return productos
    }
}