# Create INVENTORY_MOVIE_CLIENT_SECRET once and reuse it for this Windows user.
# Run this script in the PowerShell session that will start the local services.

$ErrorActionPreference = "Stop"
$variableName = "INVENTORY_MOVIE_CLIENT_SECRET"
$inventoryMovieClientSecret = [Environment]::GetEnvironmentVariable($variableName, "User")

if ([string]::IsNullOrWhiteSpace($inventoryMovieClientSecret)) {
    $secretBytes = New-Object byte[] 32
    $secureRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()

    try {
        $secureRandom.GetBytes($secretBytes)
    }
    finally {
        $secureRandom.Dispose()
    }

    $inventoryMovieClientSecret = -join (
        $secretBytes | ForEach-Object { $_.ToString("x2") }
    )

    [Environment]::SetEnvironmentVariable($variableName, $inventoryMovieClientSecret, "User")
    Write-Host "Đã tạo secret mới và lưu vào biến môi trường User của Windows."
}
else {
    Write-Host "Đang dùng lại secret đã lưu cho tài khoản Windows này."
}

$env:INVENTORY_MOVIE_CLIENT_SECRET = $inventoryMovieClientSecret

Write-Host "INVENTORY_MOVIE_CLIENT_SECRET đã sẵn sàng trong phiên hiện tại."
Write-Host "Dùng cùng giá trị ở cấu hình xác thực local nếu cần; không commit giá trị này."
