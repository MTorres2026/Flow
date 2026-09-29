$ErrorActionPreference = 'Stop'

$main = Join-Path $PSScriptRoot 'MainActivity.kt'

if (-not (Test-Path -LiteralPath $main)) {
    Write-Host 'ERROR: No encuentro MainActivity.kt en esta carpeta.' -ForegroundColor Red
    exit 1
}

$original = [System.IO.File]::ReadAllText($main)
$start = $original.IndexOf('        bottomBar = {')
if ($start -lt 0) {
    Write-Host 'ERROR: No encuentro la barra inferior actual.' -ForegroundColor Red
    exit 1
}
$endMarker = '    ) { innerPadding ->'
$end = $original.IndexOf($endMarker, $start)
if ($end -lt 0) {
    Write-Host 'ERROR: No encuentro el final de la barra inferior.' -ForegroundColor Red
    exit 1
}

$bottomBarLines = @(
'        bottomBar = {'
'            if ('
'                !mostrarConfiguracion &&'
'                !mostrarPedido &&'
'                !mostrarPedidoManual'
'            ) {'
'                Row('
'                    modifier = Modifier'
'                        .fillMaxWidth()'
'                        .height(78.dp)'
'                        .background(MaterialTheme.colorScheme.surface),'
'                    verticalAlignment = Alignment.CenterVertically'
'                ) {'
'                    Box('
'                        modifier = Modifier'
'                            .weight(1f)'
'                            .fillMaxSize()'
'                            .clickable {'
'                                mostrarPedidoManual = false'
'                                mostrarPedido = true'
'                                pedidoSugeridoActual?.let {'
'                                    Log.d('
'                                        "FLOW PEDIDO",'
'                                        "PEDIDO SUGERIDO pulsado - propuestas: ${it.productos.size}"'
'                                    )'
'                                }'
'                            },'
'                        contentAlignment = Alignment.Center'
'                    ) {'
'                        FlowBottomBarItem('
'                            label = "SUGERIDO",'
'                            contentDescription = "Pedido sugerido"'
'                        ) {'
'                            FlowPackageIconPro()'
'                        }'
'                    }'
''
'                    Box('
'                        modifier = Modifier'
'                            .weight(1f)'
'                            .fillMaxSize()'
'                            .clickable {'
'                                mostrarPedido = false'
'                                mostrarPedidoManual = true'
'                                Log.d("FLOW PEDIDO", "PEDIDO MANUAL pulsado")'
'                            },'
'                        contentAlignment = Alignment.Center'
'                    ) {'
'                        FlowBottomBarItem('
'                            label = "MANUAL",'
'                            contentDescription = "Pedido manual",'
'                        ) {'
'                            FlowClipboardIconPro()'
'                        }'
'                    }'
''
'                    Box('
'                        modifier = Modifier'
'                            .weight(1f)'
'                            .fillMaxSize()'
'                            .clickable {'
'                                mostrarConfiguracion = true'
'                                mensajeConfiguracion = ""'
'                            },'
'                        contentAlignment = Alignment.Center'
'                    ) {'
'                        FlowBottomBarItem('
'                            label = "AJUSTES",'
'                            contentDescription = "Configuracion",'
'                        ) {'
'                            FlowGearIconPro()'
'                        }'
'                    }'
'                }'
'            }'
'        }'
) -join "`r`n"
$newText = $original.Substring(0, $start) + $bottomBarLines + $original.Substring($end)

$importAnchor = 'import androidx.compose.foundation.verticalScroll'
if (-not $newText.Contains('import androidx.compose.foundation.Canvas')) {
    $newText = $newText.Replace($importAnchor, $importAnchor + "`r`nimport androidx.compose.foundation.Canvas")
}

$marker = 'private fun leerCampoIdNumericoComoTexto('
if (-not $newText.Contains($marker)) {
    Write-Host 'ERROR: No encuentro el punto de insercion de los iconos.' -ForegroundColor Red
    exit 1
}

$helpers = @(
'@Composable'
'private fun FlowBottomBarItem('
'    label: String,'
'    contentDescription: String,'
'    icon: @Composable () -> Unit'
') {'
'    Column('
'        horizontalAlignment = Alignment.CenterHorizontally,'
'        verticalArrangement = Arrangement.Center'
'    ) {'
'        Box('
'            modifier = Modifier'
'                .height(42.dp)'
'                .width(48.dp)'
'                .background('
'                    color = Color.White.copy(alpha = 0.06f),'
'                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)'
'                ),'
'            contentAlignment = Alignment.Center'
'        ) {'
'            icon()'
'        }'
'        Spacer(modifier = Modifier.height(3.dp))'
'        Text('
'            text = label,'
'            style = MaterialTheme.typography.labelSmall,'
'            color = Color.White.copy(alpha = 0.82f)'
'        )'
'    }'
'}'
''
'@Composable'
'private fun FlowPackageIconPro() {'
'    Canvas(modifier = Modifier.width(31.dp).height(31.dp)) {'
'        val stroke = 2.3f'
'        val white = Color.White'
'        val p = androidx.compose.ui.graphics.Path().apply {'
'            moveTo(4f, 10f)'
'            lineTo(15.5f, 4f)'
'            lineTo(27f, 10f)'
'            lineTo(15.5f, 16f)'
'            close()'
'        }'
'        drawPath(p, white)'
'        drawRoundRect('
'            color = white,'
'            topLeft = androidx.compose.ui.geometry.Offset(4f, 10f),'
'            size = androidx.compose.ui.geometry.Size(23f, 15f),'
'            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f)'
'        )'
'        drawPath('
'            androidx.compose.ui.graphics.Path().apply {'
'                moveTo(15.5f, 16f)'
'                lineTo(15.5f, 25f)'
'                moveTo(4f, 10f)'
'                lineTo(15.5f, 16f)'
'                lineTo(27f, 10f)'
'            },'
'            MaterialTheme.colorScheme.surface,'
'            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.0f)'
'        )'
'        drawRoundRect('
'            color = MaterialTheme.colorScheme.surface,'
'            topLeft = androidx.compose.ui.geometry.Offset(13.8f, 4.8f),'
'            size = androidx.compose.ui.geometry.Size(3.4f, 20f),'
'            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)'
'        )'
'    }'
'}'
''
'@Composable'
'private fun FlowClipboardIconPro() {'
'    Canvas(modifier = Modifier.width(31.dp).height(31.dp)) {'
'        val white = Color.White'
'        val surface = MaterialTheme.colorScheme.surface'
'        drawRoundRect('
'            color = white,'
'            topLeft = androidx.compose.ui.geometry.Offset(6f, 5f),'
'            size = androidx.compose.ui.geometry.Size(19f, 22f),'
'            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)'
'        )'
'        drawRoundRect('
'            color = surface,'
'            topLeft = androidx.compose.ui.geometry.Offset(8.3f, 7.3f),'
'            size = androidx.compose.ui.geometry.Size(14.4f, 17.4f),'
'            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.8f, 1.8f)'
'        )'
'        drawRoundRect('
'            color = white,'
'            topLeft = androidx.compose.ui.geometry.Offset(10.5f, 2.5f),'
'            size = androidx.compose.ui.geometry.Size(10f, 6.5f),'
'            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.3f, 2.3f)'
'        )'
'        drawLine(white, androidx.compose.ui.geometry.Offset(11f, 13f), androidx.compose.ui.geometry.Offset(19.5f, 13f), 1.9f)'
'        drawLine(white, androidx.compose.ui.geometry.Offset(11f, 16.5f), androidx.compose.ui.geometry.Offset(18f, 16.5f), 1.9f)'
'        drawPath('
'            androidx.compose.ui.graphics.Path().apply {'
'                moveTo(10.5f, 21f)'
'                lineTo(13.2f, 23.3f)'
'                lineTo(20.7f, 18.7f)'
'            },'
'            white,'
'            style = androidx.compose.ui.graphics.drawscope.Stroke('
'                width = 2.2f,'
'                cap = androidx.compose.ui.graphics.StrokeCap.Round,'
'                join = androidx.compose.ui.graphics.StrokeJoin.Round'
'            )'
'        )'
'    }'
'}'
''
'@Composable'
'private fun FlowGearIconPro() {'
'    Canvas(modifier = Modifier.width(31.dp).height(31.dp)) {'
'        val white = Color.White'
'        val surface = MaterialTheme.colorScheme.surface'
'        val cx = 15.5f'
'        val cy = 15.5f'
'        val outer = 13.5f'
'        val inner = 10.2f'
'        val gear = androidx.compose.ui.graphics.Path()'
'        for (i in 0 until 32) {'
'            val angle = (-Math.PI / 2.0) + i * (2.0 * Math.PI / 32.0)'
'            val r = if (i % 4 == 0 || i % 4 == 1) outer else inner'
'            val x = cx + kotlin.math.cos(angle).toFloat() * r'
'            val y = cy + kotlin.math.sin(angle).toFloat() * r'
'            if (i == 0) gear.moveTo(x, y) else gear.lineTo(x, y)'
'        }'
'        gear.close()'
'        drawPath(gear, white)'
'        drawCircle(surface, radius = 4.5f, center = androidx.compose.ui.geometry.Offset(cx, cy))'
'        drawCircle(white, radius = 1.6f, center = androidx.compose.ui.geometry.Offset(cx, cy))'
'    }'
'}'
) -join "`r`n"

# Sustituye los helpers de una ejecucion anterior, si existen.
$helperFn = $newText.IndexOf('private fun FlowBottomBarItem(')
$existingHelper = -1
if ($helperFn -ge 0) {
    $existingHelper = $newText.LastIndexOf('@Composable', $helperFn)
}

if ($existingHelper -ge 0) {
    $helperEnd = $newText.IndexOf($marker, $existingHelper)
    if ($helperEnd -lt 0) {
        Write-Host 'ERROR: Encuentro los iconos anteriores, pero no su final.' -ForegroundColor Red
        exit 1
    }
    $newText = $newText.Substring(0, $existingHelper) + $helpers + "`r`n`r`n" + $newText.Substring($helperEnd)
} else {
    $newText = $newText.Replace($marker, $helpers + "`r`n`r`n" + $marker)
}

$backup = $main + '.bak_iconos_pro_' + (Get-Date -Format 'yyyyMMdd_HHmmss')
[System.IO.File]::Copy($main, $backup, $false)
[System.IO.File]::WriteAllText($main, $newText, [System.Text.UTF8Encoding]::new($false))

Write-Host ''
Write-Host 'SUCCESS: iconos PRO aplicados.' -ForegroundColor Green
Write-Host 'Barra nueva: caja 3D + documento/check + engranaje.' -ForegroundColor Green
Write-Host 'No se ha modificado build.gradle.kts.' -ForegroundColor Green
Write-Host "BACKUP: $backup" -ForegroundColor Yellow