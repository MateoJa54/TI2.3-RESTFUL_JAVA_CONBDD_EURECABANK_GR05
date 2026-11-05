<#
    EURECABANK - Carga la base de datos en MySQL y verifica el resultado.

    Ejecutar desde la raiz del proyecto:
        .\03. BDD\cargar_bdd.ps1

    Opciones:
        .\03. BDD\cargar_bdd.ps1 -Usuario root -Puerto 3306
        .\03. BDD\cargar_bdd.ps1 -Clave "miClave"     (si no la das, la pide de forma segura)
        .\03. BDD\cargar_bdd.ps1 -Clave ""            (usuario sin clave)
        .\03. BDD\cargar_bdd.ps1 -Mysql "C:\ruta\mysql.exe"

    Que hace:
      1. Crea el esquema `eurekabank` y las 17 tablas.
      2. Carga la semilla de prueba (14 empleados, 20 clientes, 6 cuentas, 74 movimientos...).
      3. Crea el usuario de aplicacion `eurekabank_app` con privilegio minimo.
      4. Verifica que todo quedo bien y muestra los conteos.

    La clave se pide de forma segura y se pasa en un fichero temporal de
    configuracion que se borra al terminar: no queda en el historial de
    PowerShell ni en la linea de ordenes.

    Es idempotente: puedes ejecutarlo otra vez sin romper nada.
#>

param(
    [string]$Usuario = "root",
    [string]$Clave,
    [string]$Host_MySql = "127.0.0.1",
    [int]$Puerto = 3306,
    [string]$Mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
)

$ErrorActionPreference = "Stop"

# El proyecto debe ser la carpeta de trabajo: los scripts .sql usan rutas relativas.
$raiz = Split-Path -Parent $PSScriptRoot
if (-not (Test-Path (Join-Path $raiz "03. BDD"))) {
    Write-Host "ERROR: no se encuentra la carpeta '03. BDD' a partir de $raiz" -ForegroundColor Red
    exit 1
}
Set-Location -LiteralPath $raiz

if (-not (Test-Path -LiteralPath $Mysql)) {
    Write-Host "ERROR: no se encuentra el cliente MySQL en $Mysql" -ForegroundColor Red
    Write-Host "Indica otra ruta con -Mysql ..." -ForegroundColor Yellow
    exit 1
}

# 1) Clave: si no viene en el parametro, se pide de forma segura.
if ($PSBoundParameters.ContainsKey("Clave")) {
    $claveUsada = $Clave
}
else {
    $segura = Read-Host "Clave de MySQL para el usuario '$Usuario'" -AsSecureString
    $apuntador = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($segura)
    $claveUsada = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($apuntador)
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($apuntador)
}

# 2) Fichero temporal de configuracion con las credenciales (se borra al final).
$cfg = Join-Path $env:TEMP ("mysql-eurekabank-" + [guid]::NewGuid().ToString("N").Substring(0, 8) + ".ini")
$driver = Join-Path $env:TEMP ("mysql-eurekabank-" + [guid]::NewGuid().ToString("N").Substring(0, 8) + ".sql")
$claveEscapada = $claveUsada.Replace('\', '\\').Replace('"', '\"')
@"
[client]
user=$Usuario
password="$claveEscapada"
host=$Host_MySql
port=$Puerto
default-character-set=utf8mb4
protocol=TCP
"@ | Set-Content -LiteralPath $cfg -Encoding ASCII

# 3) Fichero driver con los 5 scripts, en orden y con ruta absoluta.
@"
source $(Join-Path $raiz "03. BDD\docker\init\00_esquema.sql")
source $(Join-Path $raiz "03. BDD\docker\init\01_estructura.sql")
source $(Join-Path $raiz "03. BDD\docker\init\02_datos.sql")
source $(Join-Path $raiz "03. BDD\docker\init\03_usuarios_app.sql")
source $(Join-Path $raiz "03. BDD\docker\init\04_carga_prueba.sql")
"@ | Set-Content -LiteralPath $driver -Encoding UTF8

$argumentos = @("--defaults-extra-file=$cfg", "--default-character-set=utf8mb4")

try {
    Write-Host ""
    Write-Host "=== EURECABANK - carga de la base de datos ===" -ForegroundColor Cyan
    Write-Host ("MySQL    : {0}:{1}  usuario '{2}'" -f $Host_MySql, $Puerto, $Usuario)
    Write-Host ("Proyecto : {0}" -f $raiz)

    # 4) Carga los 5 scripts en una sola sesion.
    $salida = & $Mysql @argumentos -e "source `"$driver`"" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "ERROR: la carga fallo. Salida del cliente MySQL:" -ForegroundColor Red
        $salida | ForEach-Object { Write-Host ("  " + $_) -ForegroundColor DarkYellow }
        exit 1
    }
    if ($salida) { $salida | ForEach-Object { Write-Host ("  " + $_) } }

    # 5) Verifica.
    $consulta = "USE eurekabank; SELECT 'tablas' AS concepto, COUNT(*) AS valor FROM information_schema.tables WHERE table_schema='eurekabank' UNION ALL SELECT 'empleados', COUNT(*) FROM eurekabank.empleado UNION ALL SELECT 'clientes', COUNT(*) FROM eurekabank.cliente UNION ALL SELECT 'cuentas', COUNT(*) FROM eurekabank.cuenta UNION ALL SELECT 'movimientos', COUNT(*) FROM eurekabank.movimiento UNION ALL SELECT 'tipos_movimiento', COUNT(*) FROM eurekabank.tipomovimiento UNION ALL SELECT 'parametros', COUNT(*) FROM eurekabank.parametro UNION ALL SELECT 'usuarios', COUNT(*) FROM eurekabank.usuario;"
    $verificacion = & $Mysql @argumentos -e $consulta 2>&1

    Write-Host ""
    Write-Host "--- Verificacion ---" -ForegroundColor Cyan
    $verificacion | ForEach-Object { Write-Host ("  " + $_) }

    $tablas = 0
    foreach ($l in @($verificacion)) {
        if ($l -match "tablas\s+(\d+)") { $tablas = [int]$Matches[1] }
    }

    Write-Host ""
    if ($tablas -eq 17) {
        Write-Host "RESULTADO: base de datos cargada correctamente (17 tablas)." -ForegroundColor Green
        Write-Host "Usuario de aplicacion: eurekabank_app / EurekaBank2026!" -ForegroundColor Green
    }
    else {
        Write-Host ("RESULTADO: se esperaban 17 tablas y hay {0}. Revisa el log anterior." -f $tablas) -ForegroundColor Red
        exit 1
    }
}
finally {
    Remove-Item -LiteralPath $cfg, $driver -Force -ErrorAction SilentlyContinue
}
