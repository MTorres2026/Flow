package com.example.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FlowSelectorProductosManual(
    productos: List<ProductoFlow>,
    idsYaIncluidos: Set<Int>,
    onAgregar: (ProductoFlow) -> Unit,
    onCerrar: () -> Unit
) {

    var textoBusqueda by remember {
        mutableStateOf("")
    }

    val productosDisponibles =
        productos
            .filter { producto ->

                producto.idVista !in idsYaIncluidos &&
                        producto.idConway.isNotBlank() &&
                        producto.idConway != "-" &&
                        producto.idConway != "—" &&
                        producto.idErp.isNotBlank() &&
                        producto.umErp.isNotBlank() &&
                        producto.unidadPedidoFlow.isNotBlank()
            }
            .distinctBy {
                it.idVista
            }
            .filter { producto ->

                val busqueda =
                    textoBusqueda
                        .trim()
                        .uppercase()

                if (busqueda.isBlank()) {
                    true
                } else {

                    producto.producto
                        .uppercase()
                        .contains(busqueda) ||
                            producto.idConway
                                .uppercase()
                                .contains(busqueda) ||
                            producto.idErp
                                .uppercase()
                                .contains(busqueda)
                }
            }

    AlertDialog(
        onDismissRequest = onCerrar,

        title = {
            Text(
                text = "AÑADIR PRODUCTO"
            )
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth()
            ) {

                OutlinedTextField(
                    value =
                        textoBusqueda,

                    onValueChange = {
                        textoBusqueda = it
                    },

                    label = {
                        Text(
                            "Buscar producto"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                if (
                    productosDisponibles.isEmpty()
                ) {

                    Text(
                        text =
                            "No hay productos Conway válidos " +
                                    "que coincidan con la búsqueda."
                    )

                } else {

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(
                                    max = 480.dp
                                ),

                        verticalArrangement =
                            Arrangement.spacedBy(6.dp)
                    ) {

                        items(
                            items =
                                productosDisponibles,

                            key = {
                                it.idVista
                            }
                        ) { producto ->

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 2.dp
                                        ),

                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {

                                Column(
                                    modifier =
                                        Modifier.weight(1f)
                                ) {

                                    Text(
                                        text =
                                            producto.producto
                                    )

                                    Text(
                                        text =
                                            "Conway ${producto.idConway} · " +
                                                    producto.unidadPedidoFlow
                                    )
                                }

                                Button(
                                    onClick = {
                                        onAgregar(
                                            producto
                                        )
                                    }
                                ) {

                                    Text(
                                        "AÑADIR"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick = onCerrar
            ) {

                Text(
                    "CERRAR"
                )
            }
        }
    )
}

