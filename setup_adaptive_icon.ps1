Add-Type -AssemblyName System.Drawing
$src = 'c:\Project\PhishTrack\icon\ChatGPT Image Jul 4, 2026, 12_48_18 AM.png'
$resBase = 'c:\Project\PhishTrack\PhishTrack\app\src\main\res'

$densities = @{
    'mipmap-mdpi' = @{ full=108; logo=72 }
    'mipmap-hdpi' = @{ full=162; logo=108 }
    'mipmap-xhdpi' = @{ full=216; logo=144 }
    'mipmap-xxhdpi' = @{ full=324; logo=216 }
    'mipmap-xxxhdpi' = @{ full=432; logo=288 }
}

$image = [System.Drawing.Image]::FromFile($src)

foreach ($folder in $densities.Keys) {
    $fullSize = $densities[$folder].full
    $logoSize = $densities[$folder].logo
    
    $bitmap = New-Object System.Drawing.Bitmap($fullSize, $fullSize)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.Clear([System.Drawing.Color]::Black)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    
    $offset = ($fullSize - $logoSize) / 2
    $graphics.DrawImage($image, $offset, $offset, $logoSize, $logoSize)
    $graphics.Dispose()

    $fgPath = Join-Path $resBase ($folder + '\ic_launcher_foreground.png')
    $bitmap.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$image.Dispose()
