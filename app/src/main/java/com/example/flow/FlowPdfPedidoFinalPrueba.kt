package com.example.flow

import android.content.Context
import android.util.Log
import java.io.File

/**
 * PRUEBA AISLADA DE FlowPdfPedidoFinal.
 *
 * NO participa en el flujo normal de FLOW.
 *
 * Sirve para comprobar que:
 * 1. se abre el PDF real que FLOW ya tiene en cache;
 * 2. se localiza una fila real del informe;
 * 3. se escribe una cantidad;
 * 4. se genera una copia nueva;
 * 5. el PDF original permanece intacto.
 */
object FlowPdfPedidoFinalPrueba {

    fun ejecutar(
        context: Context
    ): FlowPdfPedidoFinal.ResultadoPdf {

        val archivoOriginal =
            File(
                context.cacheDir,
                "flow_informe.pdf"
            )

        require(archivoOriginal.exists()) {
            "No existe flow_informe.pdf en cache. " +
                    "Primero carga un informe en FLOW."
        }

        val lineaPrueba =
            LineaPedidoFinal(
                idVista = 10321,
                producto = "Salsa Chunky",
                idErp = "1003010155",
                umErp = "HEU",
                idConway = "",
                denominacionLargaConway = "",
                presentacion = "",
                unidadesPorCajaPack = "",
                unidadPedidoFlow = "KGS",
                cantidad = 4.0,
                fuente = "PRUEBA_PDF",
                notaRegla = "PRUEBA AISLADA"
            )

        val pedidoPrueba =
            PedidoFinalFlow(
                estado = "PRUEBA",
                lineas = listOf(
                    lineaPrueba
                )
            )

        Log.d(
            "FLOW PDF PRUEBA",
            "Iniciando prueba con Salsa Chunky / 10321 / cantidad 4"
        )

        Log.d(
            "FLOW PDF PRUEBA",
            "ANTES DE GENERAR PDF"
        )

        val resultado =
            FlowPdfPedidoFinal.generar(
                context = context,
                archivoOriginal = archivoOriginal,
                pedidoFinal = pedidoPrueba
            )

        Log.d(
            "FLOW PDF PRUEBA",
            "DESPUÉS DE GENERAR PDF"
        )

        Log.d(
            "FLOW PDF PRUEBA",
            "Resultado OK"
        )

        Log.d(
            "FLOW PDF PRUEBA",
            "Archivo=${resultado.archivo.absolutePath}"
        )

        Log.d(
            "FLOW PDF PRUEBA",
            "Lineas escritas=${resultado.lineasEscritas}"
        )

        Log.d(
            "FLOW PDF PRUEBA",
            "Familias pendientes=${resultado.familiasPendientes}"
        )

        return resultado
    }
}