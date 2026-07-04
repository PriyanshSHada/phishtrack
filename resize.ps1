Add-Type -AssemblyName System.Drawing
$src = 'c:\Project\PhishTrack\Logo\high-resolution-color-logo.png'
$resBase = 'c:\Project\PhishTrack\PhishTrack\app\src\main\res'

$sizes = @{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

$image = [System.Drawing.Image]::FromFile($src)

foreach ($folder in $sizes.Keys) {
    $size = $sizes[$folder]
    $bitmap = New-Object System.Drawing.Bitmap($size, $size)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.DrawImage($image, 0, 0, $size, $size)
    $graphics.Dispose()

    $launcherPath = Join-Path $resBase ($folder + '\ic_launcher.png')
    $roundPath = Join-Path $resBase ($folder + '\ic_launcher_round.png')

    $bitmap.Save($launcherPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bitmap.Dispose()
}

$image.Dispose()
