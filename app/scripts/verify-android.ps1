param(
    [string]$BaseUrl = "http://localhost:8080",
    [switch]$SkipApi,
    [switch]$SkipAdb
)

$ErrorActionPreference = "Stop"
$script:passed = 0
$script:failed = 0
$script:warnings = 0

function Report-Result {
    param([string]$Id, [bool]$Ok, [string]$Detail)
    if ($Ok) {
        $script:passed++
        Write-Output "[PASS] $Id $Detail"
    } else {
        $script:failed++
        Write-Output "[FAIL] $Id $Detail"
    }
}

function Report-Warn {
    param([string]$Id, [bool]$Ok, [string]$Detail)
    if ($Ok) {
        $script:passed++
        Write-Output "[PASS] $Id $Detail"
    } else {
        $script:warnings++
        Write-Output "[WARN] $Id $Detail"
    }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        $Body = $null,
        [string]$Token = ""
    )
    $headers = @{
        "X-Client-Platform"      = "android"
        "X-Client-Version"       = "0.1.0"
        "X-Device-Id"            = "android-verify-script"
        "X-Client-Offset-Minutes" = "480"
    }
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    $request = @{
        Method  = $Method
        Uri     = "$BaseUrl/api/v1$Path"
        Headers = $headers
        TimeoutSec = 15
    }
    if ($null -ne $Body) {
        $request["ContentType"] = "application/json; charset=utf-8"
        $request["Body"] = ($Body | ConvertTo-Json -Depth 6 -Compress)
    }
    return Invoke-RestMethod @request
}

Write-Output "=== 1. Build environment ==="
$java = (Get-Command java -ErrorAction SilentlyContinue)
$javaVersion = if ($java) { (& cmd /c "java -version 2>&1" | Select-Object -First 1) } else { "java not found" }
Report-Result "ENV-JAVA" ([bool]$java) "$javaVersion"

$androidHome = $env:ANDROID_HOME
if (-not $androidHome) { $androidHome = $env:ANDROID_SDK_ROOT }
Report-Warn "ENV-SDK" ([bool]$androidHome -and (Test-Path "$androidHome\platform-tools\adb.exe")) "ANDROID_HOME=$androidHome"

$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb -and $androidHome -and (Test-Path "$androidHome\platform-tools\adb.exe")) {
    $adb = Get-Item "$androidHome\platform-tools\adb.exe"
}
Report-Warn "ENV-ADB" ([bool]$adb) $(if ($adb) { "adb: $($adb.Source)" } else { "adb not found; install Android SDK platform-tools" })

$gradle = Get-Command gradle -ErrorAction SilentlyContinue
$wrapperJar = Join-Path $PSScriptRoot "..\android\gradle\wrapper\gradle-wrapper.jar"
$wrapperJar = [System.IO.Path]::GetFullPath($wrapperJar)
Report-Warn "ENV-GRADLE" ([bool]$gradle -or (Test-Path $wrapperJar)) "gradle CLI or wrapper jar present (wrapper jar: $(Test-Path $wrapperJar))"

$apk = Join-Path $PSScriptRoot "..\android\app\build\outputs\apk\debug\app-debug.apk"
$apk = [System.IO.Path]::GetFullPath($apk)
Write-Output "       APK path: $apk (exists: $(Test-Path $apk))"

if (-not $SkipApi) {
    Write-Output "=== 2. API contract used by the Android client ==="
    $suffix = Get-Random -Minimum 100000 -Maximum 999999
    $username = "m6smoke$suffix"
    $password = "Smoke#$suffix"
    $today = (Get-Date).ToString("yyyy-MM-dd")
    $trigger = (Get-Date).AddMinutes(3).ToString("yyyy-MM-ddTHH:mm:sszzz")

    try {
        $login = Invoke-Api -Method "POST" -Path "/auth/register" -Body @{ username = $username; password = $password }
        $token = $login.data.token
        Report-Result "API-01" ([bool]$token) "android register returns token (user id $($login.data.user.id))"

        $me = Invoke-Api -Method "GET" -Path "/auth/me" -Token $token
        Report-Result "API-02" ($me.data.username -eq $username) "GET /auth/me with Bearer token"

        $task = Invoke-Api -Method "POST" -Path "/tasks" -Token $token -Body @{
            name = "冒烟任务"; period = "daily"; startDate = $today; remindTime = "22:30"; enabled = $true
        }
        Report-Result "API-03" ($task.code -eq 0 -and $task.data.id -gt 0) "POST /tasks"

        $todayList = Invoke-Api -Method "GET" -Path "/tasks/today" -Token $token
        $found = @($todayList.data | Where-Object { $_.id -eq $task.data.id }).Count -gt 0
        Report-Result "API-04" $found "GET /tasks/today contains the new task"

        $checkin = Invoke-Api -Method "POST" -Path "/tasks/$($task.data.id)/checkins" -Token $token -Body @{ date = $today }
        Report-Result "API-05" ($checkin.data.task.checkedToday -eq $true) "POST check-in marks checkedToday"

        $note = Invoke-Api -Method "POST" -Path "/notes" -Token $token -Body @{ title = "冒烟笔记"; body = "hello" }
        $note2 = Invoke-Api -Method "PUT" -Path "/notes/$($note.data.id)" -Token $token -Body @{
            title = "冒烟笔记(已保存)"; body = "hello autosave"; version = $note.data.version
        }
        Report-Result "API-06" ($note2.data.version -gt $note.data.version) "note create + versioned update"

        $birthday = Invoke-Api -Method "POST" -Path "/birthdays" -Token $token -Body @{
            name = "冒烟生日"; category = "birthday"; calendarType = "solar";
            solarDate = "2026-12-31"; advanceDays = 1
        }
        Report-Result "API-07" ($birthday.code -eq 0) "POST /birthdays"

        $course = Invoke-Api -Method "POST" -Path "/courses" -Token $token -Body @{
            name = "冒烟课程"; weekday = 3; startTime = "10:00"; endTime = "11:00";
            repeatType = "weekly"; remindMinutes = 15
        }
        Report-Result "API-08" ($course.code -eq 0) "POST /courses"

        $reminder = Invoke-Api -Method "POST" -Path "/reminders" -Token $token -Body @{
            title = "冒烟提醒"; triggerAt = $trigger
        }
        $list = Invoke-Api -Method "GET" -Path "/reminders" -Token $token
        $reminderCount = @($list.data).Count
        Report-Result "API-09" ($reminderCount -ge 1) "POST /reminders + GET /reminders (n=$reminderCount)"

        $due = Invoke-Api -Method "GET" -Path "/reminders/due" -Token $token
        Report-Result "API-10" ($due.code -eq 0) "GET /reminders/due"

        $device = Invoke-Api -Method "POST" -Path "/devices" -Token $token -Body @{
            deviceId = "android-verify-script"; platform = "android";
            appVersion = "0.1.0"; notificationsEnabled = $true
        }
        Report-Result "API-11" ($device.code -eq 0) "POST /devices registers Android device"

        $settings = Invoke-Api -Method "PATCH" -Path "/settings" -Token $token -Body @{ remindersEnabled = $false }
        $settings2 = Invoke-Api -Method "PATCH" -Path "/settings" -Token $token -Body @{ remindersEnabled = $true }
        Report-Result "API-12" ($settings.data.remindersEnabled -eq $false -and $settings2.data.remindersEnabled -eq $true) "PATCH /settings toggles global reminder switch"

        try {
            $version = Invoke-Api -Method "GET" -Path "/app/version?platform=android&current=0.1.0" -Token ""
            Report-Result "API-13" ($version.code -eq 0) "GET /app/version (configured: latest=$($version.data.latestVersion), readOnly=$($version.data.readOnly))"
        } catch {
            Report-Result "API-13" $false "GET /app/version failed or not configured: $($_.Exception.Message)"
        }

        $logout = Invoke-Api -Method "POST" -Path "/auth/logout" -Token $token -Body @{}
        try {
            Invoke-Api -Method "GET" -Path "/auth/me" -Token $token | Out-Null
            Report-Result "API-14" $false "logout should revoke the bearer token"
        } catch {
            Report-Result "API-14" $true "logout revokes the bearer token"
        }
    } catch {
        Report-Result "API-FATAL" $false $_.Exception.Message
        if ($_.ErrorDetails.Message) {
            Write-Output $_.ErrorDetails.Message
        }
    }
}

if (-not $SkipAdb -and $adb) {
    Write-Output "=== 3. Device smoke (A-01 / A-02) ==="
    $devices = & $adb devices
    $online = @($devices | Select-String "device$")
    Report-Result "ADB-DEVICE" ($online.Count -gt 0) "$($online.Count) device(s) online"
    if ($online.Count -gt 0 -and (Test-Path $apk)) {
        & $adb install -r $apk
        & $adb shell am start -n "com.yujing.tool/.MainActivity"
        Start-Sleep -Seconds 3
        $screenshot = Join-Path $env:TEMP "yujing-android-smoke.png"
        & $adb exec-out screencap -p > $screenshot
        Report-Result "ADB-LAUNCH" (Test-Path $screenshot) "installed and launched; screenshot: $screenshot"
        Write-Output "       Manual steps: register -> kill app -> reopen (A-01); logout -> reopen (A-02)."
    }
}

Write-Output "=== Summary: $script:passed passed, $script:failed failed, $script:warnings warnings ==="
if ($script:failed -gt 0) {
    Write-Output "A failing API check means the Android contract or the running server needs attention."
}
exit $(if ($script:failed -gt 0) { 1 } else { 0 })
