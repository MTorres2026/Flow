package com.example.flow

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object FlowHistorial {

    private const val NOMBRE_ARCHIVO = "flow_historial_semanal.json"

    fun guardarSemana(
        context: Context,
        fechaInforme: String,
        productos: List<ProductoFlow>
    ) {
        val archivo = context.getFileStreamPath(NOMBRE_ARCHIVO)

        val historialOriginal = if (archivo.exists()) {
            JSONArray(archivo.readText())
        } else {
            JSONArray()
        }

        val historialNuevo = JSONArray()

        for (i in 0 until historialOriginal.length()) {
            val semanaExistente = historialOriginal.getJSONObject(i)

            if (semanaExistente.optString("fechaInforme") != fechaInforme) {
                historialNuevo.put(semanaExistente)
            }
        }

        val semana = JSONObject()
        semana.put("fechaInforme", fechaInforme)

        val productosJson = JSONArray()

        productos.forEach { producto ->

            val item = JSONObject()

            item.put("idVista", producto.idVista)
            item.put("producto", producto.producto)

            item.put("umInforme", producto.umInforme)
            item.put("stockInicial", producto.stockInicial)
            item.put("ingresos", producto.ingresos)
            item.put("transferencias", producto.transferencias)
            item.put("ajustes", producto.ajustes)
            item.put("desperdiciado", producto.desperdiciado)
            item.put("ventas", producto.ventas)
            item.put("uso", producto.uso)
            item.put("stockFinal", producto.stockFinal)
            item.put("coste", producto.coste)

            item.put("idErp", producto.idErp)
            item.put("umErp", producto.umErp)
            item.put("idConway", producto.idConway)
            item.put(
                "denominacionLargaConway",
                producto.denominacionLargaConway
            )
            item.put("presentacion", producto.presentacion)
            item.put(
                "unidadesPorCajaPack",
                producto.unidadesPorCajaPack
            )
            item.put("unidadPedidoFlow", producto.unidadPedidoFlow)
            item.put("estado", producto.estado)
            item.put("fuente", producto.fuente)
            item.put("notaRegla", producto.notaRegla)

            productosJson.put(item)
        }

        semana.put("productos", productosJson)

        historialNuevo.put(semana)

        archivo.writeText(historialNuevo.toString())
    }

    fun cargarHistorial(context: Context): JSONArray {

        val archivo = context.getFileStreamPath(NOMBRE_ARCHIVO)

        if (!archivo.exists()) {
            return JSONArray()
        }

        return JSONArray(archivo.readText())
    }

    fun cargarHistorialInicialDesdeAssets(
        context: Context
    ) {
        val archivo = context.getFileStreamPath(NOMBRE_ARCHIVO)

        if (archivo.exists()) {
            return
        }

        val jsonInicial = context.assets
            .open("flow_historial_inicial.json")
            .bufferedReader()
            .use { it.readText() }

        archivo.writeText(jsonInicial)
    }
    fun numeroSemanas(context: Context): Int {
        return cargarHistorial(context).length()
    }

    fun obtenerProductoDeSemana(
        context: Context,
        indiceSemana: Int,
        idVista: Int

    ): JSONObject? {

        val historial = cargarHistorial(context)

        if (indiceSemana < 0 || indiceSemana >= historial.length()) {
            return null
        }

        val semana = historial.getJSONObject(indiceSemana)
        val productos = semana.optJSONArray("productos") ?: return null

        for (i in 0 until productos.length()) {

            val producto = productos.getJSONObject(i)

            if (producto.optInt("idVista") == idVista) {
                return producto
            }
        }

        return null
    }
    fun obtenerHistorialProducto(
        context: Context,
        idVista: Int
    ): List<JSONObject> {

        val historial = cargarHistorial(context)
        val resultado = mutableListOf<JSONObject>()

        for (i in 0 until historial.length()) {

            val semana = historial.getJSONObject(i)
            val productos = semana.optJSONArray("productos") ?: continue

            for (j in 0 until productos.length()) {

                val producto = productos.getJSONObject(j)

                if (producto.optInt("idVista") == idVista) {
                    resultado.add(
                        JSONObject(producto.toString()).apply {
                            put(
                                "fechaInforme",
                                semana.optString("fechaInforme")
                            )
                        }
                    )
                    break
                }
            }
        }
        return resultado
    }
}