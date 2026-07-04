Add-Type -AssemblyName System.Drawing
$src = 'c:\Project\PhishTrack\icon\high-resolution-color-logo.png'
$image = [System.Drawing.Image]::FromFile($src)
$bmp = New-Object System.Drawing.Bitmap($image)
$bg = $bmp.GetPixel(0, 0)
Write-Output "Width: $($image.Width), Height: $($image.Height), BG: $($bg.Name)"
$bmp.Dispose()
$image.Dispose()
