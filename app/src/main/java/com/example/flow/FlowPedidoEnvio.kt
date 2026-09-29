package com.example.flow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adaptador exclusivo de transporte:
 * FLOW Android -> POST /pedido -> CineStock Windows.
 *
 * Mantiene el JSON del pedido y, cuando se proporciona, adjunta
 * también el PDF final generado por FLOW.
 */
object FlowPedidoEnvio {

    data class ResultadoEnvio(
        val ok: Boolean,
        val mensaje: String,
        val cuerpoRespuesta: String = "",
        val orderId: String = ""
    )

    private const val PREFS = "cinestock_prefs"
    private const val CLAVE_IP = "cinestock_server_ip"
    private const val CLAVE_PUERTO = "cinestock_server_port"
    private const val IP_POR_DEFECTO = "192.168.1.151"
    private const val PUERTO_POR_DEFECTO = 8765

    fun construirPayload(
        pedido: PedidoFinalFlow,
        fechaInforme: String,
        orderId: String,
        timestamp: Long
    ): JSONObject {

        val raiz = JSONObject()

        val bloquePedido = JSONObject()
        bloquePedido.put("orderId", orderId)
        bloquePedido.put("fechaInforme", fechaInforme)
        bloquePedido.put("timestamp", timestamp)
        bloquePedido.put("estado", "CONFIRMADO_POR_ENCARGADA")
        bloquePedido.put("totalLineas", pedido.lineas.size)

        val lineas = JSONArray()

        pedido.lineas.forEach { linea ->

            val item = JSONObject()

            // Campos que consume actualmente Pedidos Conway en Windows.
            item.put("descripcion", linea.producto)
            item.put("codigo", linea.idErp)
            item.put("unidad", linea.umErp)
            item.put("cantidadPedida", linea.cantidad)

            // Datos conservados para trazabilidad de FLOW.
            item.put("idVista", linea.idVista)
            item.put("idErp", linea.idErp)
            item.put("umErp", linea.umErp)
            item.put("idConway", linea.idConway)
            item.put(
                "denominacionLargaConway",
                linea.denominacionLargaConway
            )
            item.put(
                "presentacion",
                linea.presentacion
            )
            item.put(
                "unidadesPorCajaPack",
                linea.unidadesPorCajaPack
            )
            item.put(
                "unidadPedidoFlow",
                linea.unidadPedidoFlow
            )
            item.put(
                "fuente",
                linea.fuente
            )
            item.put(
                "notaRegla",
                linea.notaRegla
            )

            lineas.put(item)
        }

        raiz.put("pedido", bloquePedido)
        raiz.put("lineas", lineas)

        return raiz
    }

    suspend fun enviar(
        context: Context,
        pedido: PedidoFinalFlow,
        fechaInforme: String,
        archivoPdf: File? = null
    ): ResultadoEnvio = withContext(Dispatchers.IO) {

        if (pedido.lineas.isEmpty()) {
            return@withContext ResultadoEnvio(
                ok = false,
                mensaje =
                    "No hay líneas en el pedido final para enviar."
            )
        }

        val prefs =
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )

        val ip =
            prefs.getString(
                CLAVE_IP,
                IP_POR_DEFECTO
            )?.trim().orEmpty()

        if (ip.isBlank()) {
            return@withContext ResultadoEnvio(
                ok = false,
                mensaje =
                    "No hay una IP de CineStock Windows configurada."
            )
        }

        val puerto =
            prefs.getInt(
                CLAVE_PUERTO,
                PUERTO_POR_DEFECTO
            )

        if (puerto !in 1..65535) {
            return@withContext ResultadoEnvio(
                ok = false,
                mensaje =
                    "El puerto de CineStock Windows no es válido: $puerto"
            )
        }

        if (
            archivoPdf != null &&
            !archivoPdf.exists()
        ) {
            return@withContext ResultadoEnvio(
                ok = false,
                mensaje =
                    "El PDF final que se iba a enviar no existe."
            )
        }

        val timestamp =
            System.currentTimeMillis()

        val orderId =
            "FLOW-$timestamp"

        val payload =
            construirPayload(
                pedido = pedido,
                fechaInforme = fechaInforme,
                orderId = orderId,
                timestamp = timestamp
            )

        val url =
            URL(
                "http://$ip:$puerto/pedido"
            )

        val boundary =
            "----FLOW-${UUID.randomUUID()}"

        val cuerpoRequest =
            ByteArrayOutputStream()

        DataOutputStream(
            cuerpoRequest
        ).use { output ->

            val jsonBytes =
                payload.toString().toByteArray(
                    Charsets.UTF_8
                )

            output.writeBytes(
                "--$boundary\r\n"
            )

            output.writeBytes(
                "Content-Disposition: form-data; " +
                        "name=\"pedido\"\r\n"
            )

            output.writeBytes(
                "Content-Type: application/json; " +
                        "charset=utf-8\r\n"
            )

            output.writeBytes(
                "Content-Length: ${jsonBytes.size}\r\n"
            )

            output.writeBytes(
                "\r\n"
            )

            output.write(
                jsonBytes
            )

            output.writeBytes(
                "\r\n"
            )

            if (
                archivoPdf != null
            ) {

                val pdfBytes =
                    archivoPdf.readBytes()

                output.writeBytes(
                    "--$boundary\r\n"
                )

                output.writeBytes(
                    "Content-Disposition: form-data; " +
                            "name=\"pdf\"; " +
                            "filename=\"${archivoPdf.name}\"\r\n"
                )

                output.writeBytes(
                    "Content-Type: application/pdf\r\n"
                )

                output.writeBytes(
                    "Content-Length: ${pdfBytes.size}\r\n"
                )

                output.writeBytes(
                    "\r\n"
                )

                output.write(
                    pdfBytes
                )

                output.writeBytes(
                    "\r\n"
                )
            }

            output.writeBytes(
                "--$boundary--\r\n"
            )

            output.flush()
        }

        val requestBytes =
            cuerpoRequest.toByteArray()

        var conexion: HttpURLConnection? = null

        try {

            conexion =
                url.openConnection()
                        as HttpURLConnection

            conexion.requestMethod =
                "POST"

            conexion.doOutput =
                true

            conexion.connectTimeout =
                8000

            conexion.readTimeout =
                15000

            conexion.useCaches =
                false

            conexion.setRequestProperty(
                "Content-Type",
                "multipart/form-data; boundary=$boundary"
            )

            conexion.setRequestProperty(
                "Accept",
                "application/json"
            )

            conexion.setFixedLengthStreamingMode(
                requestBytes.size
            )

            conexion.outputStream.use { stream ->

                stream.write(
                    requestBytes
                )

                stream.flush()
            }

            val codigoHttp =
                conexion.responseCode

            val stream =
                if (
                    codigoHttp in 200..299
                ) {
                    conexion.inputStream
                } else {
                    conexion.errorStream
                }

            val cuerpo =
                stream
                    ?.bufferedReader(
                        Charsets.UTF_8
                    )
                    ?.use {
                        it.readText()
                    }
                    .orEmpty()

            if (
                codigoHttp ==
                HttpURLConnection.HTTP_OK
            ) {

                ResultadoEnvio(
                    ok = true,
                    mensaje =
                        if (archivoPdf != null) {
                            "Pedido y PDF enviados correctamente a CineStock Windows."
                        } else {
                            "Pedido enviado correctamente a CineStock Windows."
                        },
                    cuerpoRespuesta =
                        cuerpo,
                    orderId =
                        orderId
                )

            } else {

                ResultadoEnvio(
                    ok = false,
                    mensaje =
                        "CineStock Windows respondió HTTP $codigoHttp.",
                    cuerpoRespuesta =
                        cuerpo,
                    orderId =
                        orderId
                )
            }

        } catch (e: Exception) {

            ResultadoEnvio(
                ok = false,
                mensaje =
                    "No se pudo enviar el pedido a CineStock Windows: " +
                            (
                                    e.message
                                        ?: e.javaClass.simpleName
                                    ),
                orderId =
                    orderId
            )

        } finally {

            conexion?.disconnect()
        }
    }
}