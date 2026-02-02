<# WoW Watchdog – Service Safe (with GUI Heartbeat) #>

param(
    [int]$RestartCooldown  = 5,
    [int]$WorldserverBurst = 300,  # seconds
    [int]$MaxRestarts      = 100,  # max restarts within burst window
    [int]$ConfigRetrySec   = 10,   # if config invalid/missing, re-check every N seconds
    [int]$HeartbeatEverySec = 1,    # heartbeat update cadence
    [int]$ShutdownDelaySec = 8,  # delay between service stops
    [int64]$LogMaxBytes    = 5242880, # 5 MB
    [int]$LogRetainCount   = 5
)

$ErrorActionPreference = 'Stop'

# -------------------------------
# Paths (service / EXE safe)
# -------------------------------
$BaseDir = if ($PSScriptRoot) {
    $PSScriptRoot
} elseif ($MyInvocation.MyCommand.Path) {
    Split-Path -Parent $MyInvocation.MyCommand.Path
} else {
    Split-Path -Parent ([System.Diagnostics.Process]::GetCurrentProcess().MainModule.FileName)
}

$AppName = "WoWWatchdog"
$DataDir = Join-Path $env:ProgramData $AppName
if (-not (Test-Path $DataDir)) {
    New-Item -ItemType Directory -Path $DataDir -Force | Out-Null
}

$LogFile         = Join-Path $DataDir "watchdog.log"
$StopSignalFile  = Join-Path $DataDir "watchdog.stop"
$ConfigPath      = Join-Path $DataDir "config.json"
$HeartbeatFile   = Join-Path $DataDir "watchdog.heartbeat"      # GUI checks timestamp freshness
$StatusFile      = Join-Path $DataDir "watchdog.status.json"    # GUI reads richer status

$CommandDir = $DataDir

# Command files are dropped by the GUI to request immediate actions.
$CommandFiles = @{
    StartMySQL      = Join-Path $CommandDir "command.start.mysql"
    StopMySQL       = Join-Path $CommandDir "command.stop.mysql"
    StartAuthserver = Join-Path $CommandDir "command.start.auth"
    StopAuthserver  = Join-Path $CommandDir "command.stop.auth"
    StartWorld      = Join-Path $CommandDir "command.start.world"
    StopWorld       = Join-Path $CommandDir "command.stop.world"
}

$HoldDir = Join-Path $DataDir "holds"
if (-not (Test-Path $HoldDir)) { New-Item -ItemType Directory -Path $HoldDir -Force | Out-Null }

function Get-HoldFile {
    param([Parameter(Mandatory)][ValidateSet("MySQL","Authserver","Worldserver")][string]$Role)
    Join-Path $HoldDir "$Role.hold"
}

function Is-RoleHeld {
    param([Parameter(Mandatory)][ValidateSet("MySQL","Authserver","Worldserver")][string]$Role)
    # Hold files are created by the GUI to pause restarts for a role.
    return (Test-Path (Get-HoldFile -Role $Role))
}


# Log only on config-state changes (prevents spam during retries)
$global:LastConfigValidity = $null   # $true=valid, $false=invalid, $null=unknown
$global:LastConfigIssueSig = ""      # signature of last issues logged
$global:LastConfigLoadState = ""   # "MissingConfig", "InvalidConfig", or ""

# -------------------------------
# Logging (never throw)
# -------------------------------
function Rotate-LogIfNeeded {
    param(
        [Parameter(Mandatory)][string]$Path,
        [int64]$MaxBytes = 5242880,
        [int]$Keep = 5
    )

    try {
        if (-not (Test-Path $Path)) { return }
        if ($MaxBytes -le 0 -or $Keep -le 0) { return }

        $len = (Get-Item -LiteralPath $Path).Length
        if ($len -lt $MaxBytes) { return }

        for ($i = $Keep - 1; $i -ge 1; $i--) {
            $src = "$Path.$i"
            $dst = "$Path." + ($i + 1)
            if (Test-Path $src) {
                Move-Item -LiteralPath $src -Destination $dst -Force
            }
        }

        Move-Item -LiteralPath $Path -Destination "$Path.1" -Force
    } catch { }
}

function Invoke-WithLogLock {
    param([Parameter(Mandatory)][scriptblock]$Action)

    $mutex = $null
    $hasLock = $false
    try {
        $mutex = New-Object System.Threading.Mutex($false, "Global\\WoWWatchdog_Log")
        $hasLock = $mutex.WaitOne(2000)
    } catch {
        $hasLock = $false
    }

    try {
        & $Action
    } finally {
        if ($hasLock -and $mutex) {
            try { $mutex.ReleaseMutex() } catch { }
        }
        if ($mutex) { $mutex.Dispose() }
    }
}

function Write-AtomicFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$Content,
        [System.Text.Encoding]$Encoding = [System.Text.Encoding]::UTF8
    )

    $dir = Split-Path -Parent $Path
    if ($dir -and -not (Test-Path -LiteralPath $dir)) {
        New-Item -Path $dir -ItemType Directory -Force | Out-Null
    }

    $tmpName = (".{0}.tmp.{1}" -f ([System.IO.Path]::GetFileName($Path)), ([guid]::NewGuid().ToString("N")))
    $tmpPath = Join-Path $dir $tmpName

    try {
        [System.IO.File]::WriteAllText($tmpPath, $Content, $Encoding)
        Move-Item -LiteralPath $tmpPath -Destination $Path -Force
    } finally {
        if (Test-Path -LiteralPath $tmpPath) {
            Remove-Item -LiteralPath $tmpPath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Log {
    param([string]$Message)
    try {
        Invoke-WithLogLock -Action {
            Rotate-LogIfNeeded -Path $LogFile -MaxBytes $LogMaxBytes -Keep $LogRetainCount
            $ts = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
            Add-Content -Path $LogFile -Value "[$ts] $Message" -Encoding UTF8
        }
    } catch { }
}

# -------------------------------
# Status helpers (never throw)
# -------------------------------
function Write-Heartbeat {
    param(
        [string]$State = "Running",
        [hashtable]$Extra = $null
    )

    try {
        $now = Get-Date
        # Heartbeat file: ISO timestamp only (simple + robust)
        Write-AtomicFile -Path $HeartbeatFile -Content ($now.ToString("o"))

        # Optional richer status JSON
        $obj = [ordered]@{
            timestamp   = $now.ToString("o")
            pid         = $PID
            state       = $State
            baseDir     = $BaseDir
            dataDir     = $DataDir
        }

        if ($Extra) {
            foreach ($k in $Extra.Keys) { $obj[$k] = $Extra[$k] }
        }

         $json = $obj | ConvertTo-Json -Depth 6
         Write-AtomicFile -Path $StatusFile -Content $json
    } catch { }
}

function Try-ConsumeCommandFile {
    param([Parameter(Mandatory)][string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) { return $false }

    # Atomically rename the file to claim it; this prevents double-processing.
    $dir = Split-Path -Parent $Path
    $claimed = Join-Path $dir ("{0}.processing.{1}" -f ([System.IO.Path]::GetFileName($Path)), ([guid]::NewGuid().ToString("N")))

    try {
        Move-Item -LiteralPath $Path -Destination $claimed -Force -ErrorAction Stop
    } catch {
        return $false
    }

    try {
        Remove-Item -LiteralPath $claimed -Force -ErrorAction SilentlyContinue
    } catch { }

    return $true
}

# -------------------------------
# Process aliases
# -------------------------------
$ProcessAliases = @{
    MySQL      = @("mysqld","mysqld-nt","mysqld-opt","mariadbd")
    Authserver = @("authserver","bnetserver","logonserver","realmd","auth")
    Worldserver= @("worldserver")
}

function Test-ProcessRoleRunning {
    param(
        [Parameter(Mandatory)]
        [ValidateSet("MySQL","Authserver","Worldserver")]
        [string]$Role,

        [string]$ExpectedPath
    )

    $expectedExe = $null
    if (-not [string]::IsNullOrWhiteSpace($ExpectedPath) -and $ExpectedPath -match '\.exe$') {
        $expectedExe = $ExpectedPath
    }

    # If we have an explicit expected path, confirm the exact executable.
    if ($expectedExe) {
        $expectedExeFull = $expectedExe
        try { $expectedExeFull = [System.IO.Path]::GetFullPath($expectedExe) } catch { }

        $expectedName = [System.IO.Path]::GetFileNameWithoutExtension($expectedExe)
        $procs = @()
        try { $procs = Get-Process -Name $expectedName -ErrorAction SilentlyContinue } catch { }

        foreach ($proc in $procs) {
            try {
                $procPath = $proc.Path
                if (-not $procPath) { $procPath = $proc.MainModule.FileName }
                if (-not $procPath) { continue }
                try { $procPath = [System.IO.Path]::GetFullPath($procPath) } catch { }
                if ($procPath -and ($procPath -ieq $expectedExeFull)) { return $true }
            } catch { }
        }

        return $false
    }

    # Fallback: look for known process name aliases.
    foreach ($p in $ProcessAliases[$Role]) {
        try {
            if (Get-Process -Name $p -ErrorAction SilentlyContinue) { return $true }
        } catch { }
    }
    return $false
}

# -------------------------------
# Restart tracking
# -------------------------------
$LastRestart = @{
    MySQL      = Get-Date "2000-01-01"
    Authserver = Get-Date "2000-01-01"
    Worldserver= Get-Date "2000-01-01"
}

$WorldRestartCount = 0
$WorldBurstStart   = $null

# -------------------------------
# Config loading + validation
# -------------------------------
$DefaultConfig = [ordered]@{
    ServerName  = ""
    Expansion   = "Unknown"
    MySQL       = ""
    Authserver  = ""
    Worldserver = ""
    NTFY = [ordered]@{
        Server           = ""
        Topic            = ""
        Tags             = "wow,watchdog"
        PriorityDefault  = 4
        EnableMySQL      = $true
        EnableAuthserver = $true
        EnableWorldserver= $true
        SendOnDown       = $true
        SendOnUp         = $false
    }
}

function Write-ConfigFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)]$Object
    )

    try {
        $dir = Split-Path -Parent $Path
        if ($dir -and -not (Test-Path -LiteralPath $dir)) {
            New-Item -Path $dir -ItemType Directory -Force | Out-Null
        }
        $Object | ConvertTo-Json -Depth 10 | Set-Content -Path $Path -Encoding UTF8
    } catch {
        Log "ERROR: Failed to write config to $Path. Error: $($_)"
    }
}

function Ensure-ConfigSchema {
    param(
        [Parameter(Mandatory)]$Cfg,
        [Parameter(Mandatory)]$Defaults
    )

    $changed = $false
    # Ensure any new default properties are backfilled into existing configs.
    foreach ($p in $Defaults.PSObject.Properties) {
        if (-not $Cfg.PSObject.Properties[$p.Name]) {
            $Cfg | Add-Member -MemberType NoteProperty -Name $p.Name -Value $p.Value
            $changed = $true
            continue
        }

        if ($p.Value -is [psobject] -and $Cfg.$($p.Name) -is [psobject]) {
            $nestedChanged = Ensure-ConfigSchema -Cfg $Cfg.$($p.Name) -Defaults $p.Value
            if ($nestedChanged) { $changed = $true }
        }
    }

    return $changed
}

function Load-ConfigSafe {
    if (-not (Test-Path -LiteralPath $ConfigPath)) {
        if ($global:LastConfigLoadState -ne "MissingConfig") {
            Log "config.json missing at $ConfigPath. Watchdog idle (will retry)."
            $global:LastConfigLoadState = "MissingConfig"
        }
        Write-ConfigFile -Path $ConfigPath -Object $DefaultConfig
        Write-Heartbeat -State "Idle" -Extra @{ reason = "MissingConfig"; configPath = $ConfigPath }
        return $null
    }

    try {
        $cfg = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
        if ($cfg) {
            if (Ensure-ConfigSchema -Cfg $cfg -Defaults $DefaultConfig) {
                Write-ConfigFile -Path $ConfigPath -Object $cfg
            }
        }
        $global:LastConfigLoadState = ""
        return $cfg
    }
    catch {
        if ($global:LastConfigLoadState -ne "InvalidConfig") {
            Log "config.json invalid/unparseable. Watchdog idle (will retry). Error: $($_)"
            $global:LastConfigLoadState = "InvalidConfig"
        }
        Write-Heartbeat -State "Idle" -Extra @{ reason = "InvalidConfig"; configPath = $ConfigPath }
        return $null
    }
}

function Test-ConfigPaths {
    param($Cfg)

    # Validate that configured paths exist before starting processes.
    $issues = New-Object System.Collections.Generic.List[string]

    $pairs = @(
        @{ Role="MySQL";      Path=[string]$Cfg.MySQL },
        @{ Role="Authserver"; Path=[string]$Cfg.Authserver },
        @{ Role="Worldserver";Path=[string]$Cfg.Worldserver }
    )

    foreach ($p in $pairs) {
        if ([string]::IsNullOrWhiteSpace($p.Path)) {
            $issues.Add("EMPTY path for $($p.Role)")
            continue
        }
        if (-not (Test-Path -LiteralPath $p.Path)) {
            $issues.Add("MISSING path for $($p.Role): $($p.Path)")
        }
    }

    return $issues
}

# -------------------------------
# Start helper (bat/exe safe)
# -------------------------------
function Start-Target {
    param(
        [Parameter(Mandatory)][string]$Role,
        [Parameter(Mandatory)][string]$Path
    )

    # Batch files need cmd.exe for proper execution.
    if ($Path -match '\.bat$') {
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList "/c `"$Path`"" `
            -WorkingDirectory (Split-Path $Path) `
            -WindowStyle Hidden
        return
    }

    # Direct EXE path.
    Start-Process -FilePath $Path `
        -WorkingDirectory (Split-Path $Path) `
        -WindowStyle Hidden
}

# -------------------------------
# Stop a service/role.
# -------------------------------
function Stop-Role {
    param(
        [Parameter(Mandatory)]
        [ValidateSet("MySQL","Authserver","Worldserver")]
        [string]$Role
    )

    # Stop by process name aliases to handle renamed binaries.
    foreach ($p in $ProcessAliases[$Role]) {
        try {
            Get-Process -Name $p -ErrorAction SilentlyContinue |
                Stop-Process -Force -ErrorAction SilentlyContinue
        } catch { }
    }

    Log "$Role stop requested."
}

# -------------------------------
# Stop all roles gracefully
# -------------------------------
function Stop-All-Gracefully {
    param(
        [int]$DelaySec = 5,
        [int]$WaitTimeoutSec = 60,
        $Cfg
    )

    Log "Graceful shutdown initiated."

    # Stop in reverse dependency order: World -> Auth -> DB.
    Stop-Role -Role "Worldserver"
    if (-not (Wait-ForRoleDown -Role "Worldserver" -ExpectedPath ([string]$Cfg.Worldserver) -TimeoutSec $WaitTimeoutSec)) {
        Log "Graceful shutdown wait timed out for Worldserver."
    }
    Start-Sleep -Seconds $DelaySec

    Stop-Role -Role "Authserver"
    if (-not (Wait-ForRoleDown -Role "Authserver" -ExpectedPath ([string]$Cfg.Authserver) -TimeoutSec $WaitTimeoutSec)) {
        Log "Graceful shutdown wait timed out for Authserver."
    }
    Start-Sleep -Seconds $DelaySec

    Stop-Role -Role "MySQL"
    if (-not (Wait-ForRoleDown -Role "MySQL" -ExpectedPath ([string]$Cfg.MySQL) -TimeoutSec $WaitTimeoutSec)) {
        Log "Graceful shutdown wait timed out for MySQL."
    }

    Log "Graceful shutdown completed."
}


# -------------------------------
# Ensure proper startup. DB->Auth->World
# -------------------------------
function Wait-ForRole {
    param(
        [Parameter(Mandatory)]
        [ValidateSet("MySQL","Authserver","Worldserver")]
        [string]$Role,

        [string]$ExpectedPath,

        [int]$TimeoutSec = 120
    )

    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt $TimeoutSec) {
        if (Test-ProcessRoleRunning -Role $Role -ExpectedPath $ExpectedPath) {
            return $true
        }
        Start-Sleep -Seconds 2
    }

    Log "Timeout waiting for $Role to become ready."
    return $false
}

function Wait-ForRoleDown {
    param(
        [Parameter(Mandatory)]
        [ValidateSet("MySQL","Authserver","Worldserver")]
        [string]$Role,

        [string]$ExpectedPath,

        [int]$TimeoutSec = 60
    )

    $start = Get-Date
    while (((Get-Date) - $start).TotalSeconds -lt $TimeoutSec) {
        if (-not (Test-ProcessRoleRunning -Role $Role -ExpectedPath $ExpectedPath)) {
            return $true
        }
        Start-Sleep -Seconds 2
    }

    Log "Timeout waiting for $Role to stop."
    return $false
}


# -------------------------------
# Ensure functions
# -------------------------------
function Ensure-Role {
    param(
        [Parameter(Mandatory)]
        [ValidateSet("MySQL","Authserver","Worldserver")]
        [string]$Role,

        [Parameter(Mandatory)]
        [string]$Path
    )

    # Manual hold (GUI-requested stop) — do not restart.
    if (Is-RoleHeld -Role $Role) {
        return
    }

    if (Test-ProcessRoleRunning -Role $Role -ExpectedPath $Path) { return }

    # Restart cooldown.
    $delta = ((Get-Date) - $LastRestart[$Role]).TotalSeconds
    if ($delta -lt $RestartCooldown) { return }

    # Worldserver crash-loop protection.
    if ($Role -eq "Worldserver") {
        $now = Get-Date
        if (-not $WorldBurstStart) {
            $WorldBurstStart   = $now
            $WorldRestartCount = 0
        } else {
            $burstAge = ($now - $WorldBurstStart).TotalSeconds
            if ($burstAge -gt $WorldserverBurst) {
                $WorldBurstStart   = $now
                $WorldRestartCount = 0
            }
        }

        $WorldRestartCount++
        if ($WorldRestartCount -gt $MaxRestarts) {
            Log "ERROR: Worldserver restart limit exceeded ($WorldRestartCount > $MaxRestarts in $WorldserverBurst sec). Suppressing restarts."
            return
        }
    }

    $LastRestart[$Role] = Get-Date
    Log "$Role not running — starting: $Path"

    try {
        Start-Target -Role $Role -Path $Path
    } catch {
        Log "ERROR starting $Role ($Path): $($_)"
    }
}

# -------------------------------
# Startup
# -------------------------------
Log "Watchdog service starting (PID $PID)"
Write-Heartbeat -State "Starting" -Extra @{ version = "service-safe-heartbeat"; configPath = $ConfigPath }

$lastConfigCheck = Get-Date "2000-01-01"
$cfg = $null
$pathsOk = $false
$issuesLast = @()

# -------------------------------
# Process start commands
# -------------------------------
function Process-Commands {
    param($Cfg)

    # --- START commands (ordered) ---
      if (Try-ConsumeCommandFile -Path $CommandFiles.StartMySQL) {
        Log "Command processed: command.start.mysql"
        Start-Target -Role "MySQL" -Path $Cfg.MySQL
    }

    if (Try-ConsumeCommandFile -Path $CommandFiles.StartAuthserver) {
        Log "Command processed: command.start.auth"

        if (Wait-ForRole -Role "MySQL" -ExpectedPath ([string]$Cfg.MySQL)) {
            Start-Target -Role "Authserver" -Path $Cfg.Authserver
        } else {
            Log "Authserver start blocked: MySQL not ready."
        }
    }

     if (Try-ConsumeCommandFile -Path $CommandFiles.StartWorld) {
        Log "Command processed: command.start.world"

        if (Wait-ForRole -Role "Authserver" -ExpectedPath ([string]$Cfg.Authserver)) {
            Start-Target -Role "Worldserver" -Path $Cfg.Worldserver
        } else {
            Log "Worldserver start blocked: Authserver not ready."
        }
    }

    # --- STOP commands ---
    $StopAllCmd = Join-Path $CommandDir "command.stop.all"

     if (Try-ConsumeCommandFile -Path $StopAllCmd) {
        Log "Command processed: command.stop.all"
        Stop-All-Gracefully -DelaySec $ShutdownDelaySec -Cfg $Cfg
    }


      if (Try-ConsumeCommandFile -Path $CommandFiles.StopWorld) {
        Log "Command processed: command.stop.world"
        Stop-Role -Role "Worldserver"
    }

     if (Try-ConsumeCommandFile -Path $CommandFiles.StopAuthserver) {
        Log "Command processed: command.stop.auth"
        Stop-Role -Role "Authserver"
    }

    if (Try-ConsumeCommandFile -Path $CommandFiles.StopMySQL) {
        Log "Command processed: command.stop.mysql"
        Stop-Role -Role "MySQL"
    }
}


# -------------------------------
# Main loop
# -------------------------------
while ($true) {
    try {
        # Stop signal (GUI writes this) triggers graceful shutdown.
        if (Try-ConsumeCommandFile -Path $StopSignalFile) {
            Log "Stop signal detected ($StopSignalFile). Initiating graceful shutdown."

            Stop-All-Gracefully -DelaySec $ShutdownDelaySec -Cfg $cfg

            Write-Heartbeat -State "Stopping" -Extra @{ reason = "StopSignal" }
            break

        }

        # Reload config periodically or if not loaded.
        $sinceCfg = ((Get-Date) - $lastConfigCheck).TotalSeconds
        if (-not $cfg -or $sinceCfg -ge $ConfigRetrySec -or -not $pathsOk) {
            $lastConfigCheck = Get-Date
            $cfg = Load-ConfigSafe
            $pathsOk = $false

            if ($cfg) {
                $issues = Test-ConfigPaths -Cfg $cfg
                $issuesLast = $issues

                if ($issues.Count -gt 0) {

    # Build a stable signature so we only log when the issue set changes
    $sig = ($issues | Sort-Object) -join " | "

    if ($global:LastConfigValidity -ne $false -or $global:LastConfigIssueSig -ne $sig) {
        Log ("Config path issues: " + $sig)
        $global:LastConfigValidity = $false
        $global:LastConfigIssueSig = $sig
    }

    Write-Heartbeat -State "Idle" -Extra @{ reason = "BadPaths"; issues = $issues }
    Start-Sleep -Seconds $ConfigRetrySec
    continue

} else {

    $pathsOk = $true

    # Only log the success transition once (invalid -> valid, or unknown -> valid)
    if ($global:LastConfigValidity -ne $true) {
        Log "Config loaded and paths validated."
        $global:LastConfigValidity = $true
        $global:LastConfigIssueSig = ""
    }
}

            } else {
                Start-Sleep -Seconds $ConfigRetrySec
                continue
            }
        }

        Process-Commands -Cfg $cfg

        # Ensure roles (dependency order is enforced below).
        Ensure-Role -Role "MySQL" -Path ([string]$cfg.MySQL)

if (Test-ProcessRoleRunning -Role "MySQL" -ExpectedPath ([string]$cfg.MySQL)) {
    Ensure-Role -Role "Authserver" -Path ([string]$cfg.Authserver)
}

if (Test-ProcessRoleRunning -Role "Authserver" -ExpectedPath ([string]$cfg.Authserver)) {
    Ensure-Role -Role "Worldserver" -Path ([string]$cfg.Worldserver)
}


        # Heartbeat + lightweight telemetry for GUI.
            $extra = @{
                mysqlRunning = (Test-ProcessRoleRunning -Role "MySQL" -ExpectedPath ([string]$cfg.MySQL))
                authRunning  = (Test-ProcessRoleRunning -Role "Authserver" -ExpectedPath ([string]$cfg.Authserver))
                worldRunning = (Test-ProcessRoleRunning -Role "Worldserver" -ExpectedPath ([string]$cfg.Worldserver))
                mysqlHeld    = (Is-RoleHeld -Role "MySQL")
                authHeld     = (Is-RoleHeld -Role "Authserver")
                worldHeld    = (Is-RoleHeld -Role "Worldserver")
                worldBurstStart   = if ($WorldBurstStart) { $WorldBurstStart.ToString("o") } else { $null }
                worldRestartCount = $WorldRestartCount
                lastIssues        = $issuesLast
            }
            Write-Heartbeat -State "Running" -Extra $extra

        Start-Sleep -Seconds $HeartbeatEverySec
    }
    catch {
        Log "Unhandled watchdog error: $($_)"
        Write-Heartbeat -State "Error" -Extra @{ error = "$($_)" }
        Start-Sleep -Seconds 5
    }
}

Log "Watchdog service stopped."
Write-Heartbeat -State "Stopped" -Extra @{ reason = "Exited" }
