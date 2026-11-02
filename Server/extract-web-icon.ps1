
param(
    [string]$Url,
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Drawing

Write-Host "Website:"
Write-Host $Url

# --------------------------------------------------
# Make sure the URL has http:// or https://
# --------------------------------------------------

if (-not $Url.StartsWith("http://") -and
    -not $Url.StartsWith("https://")) {

    $Url = "https://" + $Url
}

# --------------------------------------------------
# Download website HTML
# --------------------------------------------------

$response = Invoke-WebRequest `
    -Uri $Url `
    -UseBasicParsing

$html = $response.Content

# --------------------------------------------------
# Find all favicon / icon links
# --------------------------------------------------

$matches = [regex]::Matches(
    $html,
    '<link[^>]+rel\s*=\s*["'']([^"'']*icon[^"'']*)["''][^>]*href\s*=\s*["'']([^"'']+)["''][^>]*>',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)

$iconUrl = $null
$bestSize = 0

foreach ($match in $matches) {

    $rel = $match.Groups[1].Value
    $href = $match.Groups[2].Value

    # ----------------------------------------------
    # Read declared icon size
    # Example: sizes="192x192"
    # ----------------------------------------------

    $sizeMatch = [regex]::Match(
        $match.Value,
        'sizes\s*=\s*["''](\d+)x(\d+)["'']',
        [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
    )

    $size = 0

    if ($sizeMatch.Success) {

        $width =
            [int]$sizeMatch.Groups[1].Value

        $height =
            [int]$sizeMatch.Groups[2].Value

        $size =
            [Math]::Max(
                $width,
                $height
            )
    }

    Write-Host ""
    Write-Host "Found icon:"
    Write-Host "  Rel  : $rel"
    Write-Host "  Size : $size"
    Write-Host "  URL  : $href"

    # ----------------------------------------------
    # Prefer the largest icon
    # ----------------------------------------------

    if ($size -gt $bestSize) {

        $bestSize = $size
        $iconUrl = $href
    }

    # ----------------------------------------------
    # Apple touch icons are normally high resolution
    # ----------------------------------------------

    if ($rel -match "apple-touch-icon" -and
        $size -ge $bestSize) {

        $bestSize = $size
        $iconUrl = $href
    }
}

# --------------------------------------------------
# If no icon was found, use /favicon.ico
# --------------------------------------------------

if (-not $iconUrl) {

    $baseUri =
        [System.Uri]$Url

    $fallbackUri =
        New-Object System.Uri(
            $baseUri,
            "/favicon.ico"
        )

    $iconUrl =
        $fallbackUri.AbsoluteUri

    Write-Host ""
    Write-Host "No icon declared in HTML."
    Write-Host "Using fallback:"
    Write-Host $iconUrl
}

# --------------------------------------------------
# Convert relative URL to absolute URL
# --------------------------------------------------

$baseUri =
    [System.Uri]$Url

$absoluteIconUri =
    New-Object System.Uri(
        $baseUri,
        $iconUrl
    )

Write-Host ""
Write-Host "Selected icon:"
Write-Host $absoluteIconUri.AbsoluteUri

# --------------------------------------------------
# Download icon
# --------------------------------------------------

$iconResponse =
    Invoke-WebRequest `
        -Uri $absoluteIconUri.AbsoluteUri `
        -UseBasicParsing

# --------------------------------------------------
# Temporary file
# --------------------------------------------------

$tempPath =
    Join-Path `
        $env:TEMP `
        "remotepulse-web-icon"

[System.IO.File]::WriteAllBytes(
    $tempPath,
    $iconResponse.Content
)

# --------------------------------------------------
# Load image
# --------------------------------------------------

$image =
    [System.Drawing.Image]::FromFile(
        $tempPath
    )

try {

    # ----------------------------------------------
    # Create 256 x 256 PNG
    # ----------------------------------------------

    $bitmap =
        New-Object System.Drawing.Bitmap(
            256,
            256
        )

    try {

        $graphics =
            [System.Drawing.Graphics]::FromImage(
                $bitmap
            )

        try {

            # Transparent background
            $graphics.Clear(
                [System.Drawing.Color]::Transparent
            )

            # High-quality resizing
            $graphics.InterpolationMode =
                [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic

            $graphics.SmoothingMode =
                [System.Drawing.Drawing2D.SmoothingMode]::HighQuality

            $graphics.PixelOffsetMode =
                [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality

            $graphics.CompositingQuality =
                [System.Drawing.Drawing2D.CompositingQuality]::HighQuality

            # --------------------------------------
            # Draw icon
            # --------------------------------------

            $graphics.DrawImage(
                $image,
                0,
                0,
                256,
                256
            )

        }
        finally {

            $graphics.Dispose()
        }

        # ------------------------------------------
        # Save as PNG
        # ------------------------------------------

        $bitmap.Save(
            $OutputPath,
            [System.Drawing.Imaging.ImageFormat]::Png
        )

    }
    finally {

        $bitmap.Dispose()
    }

}
finally {

    $image.Dispose()
}

# --------------------------------------------------
# Remove temporary file
# --------------------------------------------------

Remove-Item `
    $tempPath `
    -Force `
    -ErrorAction SilentlyContinue

# --------------------------------------------------
# Final result
# --------------------------------------------------

Write-Host ""
Write-Host "Website icon created:"
Write-Host $OutputPath
