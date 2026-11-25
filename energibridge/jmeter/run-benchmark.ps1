[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('auctions-list', 'resales-list', 'auction-detail')]
    [string]$Scenario,

    [ValidateRange(1, 1000)]
    [int]$Threads = 1,

    [ValidateRange(0, 3600)]
    [int]$RampUp = 1,

    [ValidateRange(1, 86400)]
    [int]$Duration = 120,

    [ValidatePattern('^[a-zA-Z0-9_-]+$')]
    [string]$RunId = (Get-Date -Format 'yyyyMMdd-HHmmss')
)

$ErrorActionPreference = 'Stop'

$energyBridge = 'C:\tools\EnergiBridge\energibridge.exe'
$jmeter = 'C:\tools\apache-jmeter-5.6.3\bin\jmeter.bat'

$plans = @{
    'auctions-list' = '01-auctions-list.jmx'
    'resales-list' = '02-resales-list.jmx'
    'auction-detail' = '03-auction-detail.jmx'
}

$plan = Join-Path $PSScriptRoot (Join-Path 'plans' $plans[$Scenario])
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$outputDirectory = Join-Path $repoRoot (Join-Path 'energibridge\results' (Join-Path $RunId $Scenario))

foreach ($requiredFile in @($energyBridge, $jmeter, $plan)) {
    if (-not (Test-Path -LiteralPath $requiredFile -PathType Leaf)) {
        throw "File non trovato: $requiredFile"
    }
}

if (Test-Path -LiteralPath $outputDirectory) {
    throw "La cartella di output esiste già: $outputDirectory. Usa un altro RunId."
}

New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null

$energyCsv = Join-Path $outputDirectory 'energibridge.csv'
$jmeterJtl = Join-Path $outputDirectory 'jmeter.jtl'

$jmeterArguments = @(
    '-n',
    '-t', $plan,
    "-Jthreads=$Threads",
    "-Jrampup=$RampUp",
    "-Jduration=$Duration",
    '-l', $jmeterJtl
)

$energyBridgeArguments = @('-o', $energyCsv, '--', $jmeter) + $jmeterArguments

Write-Host "Scenario: $Scenario"
Write-Host "Utenti: $Threads; ramp-up: $RampUp s; durata: $Duration s"
Write-Host "Risultati: $outputDirectory"

& $energyBridge @energyBridgeArguments
$exitCode = $LASTEXITCODE

if ($exitCode -ne 0) {
    throw "EnergiBridge/JMeter è terminato con codice $exitCode. Controlla l'output sopra."
}

Write-Host 'Esecuzione completata.'
