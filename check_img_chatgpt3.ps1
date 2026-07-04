Add-Type -AssemblyName System.Drawing
$src = 'c:\Project\PhishTrack\icon\ChatGPT Image Jul 4, 2026, 12_52_05 AM.png'
$image = [System.Drawing.Image]::FromFile($src)
$bmp = New-Object System.Drawing.Bitmap($image)
$bg = $bmp.GetPixel(0, 0)
Write-Output "Width: $($image.Width), Height: $($image.Height), BG: $($bg.Name)"
$bmp.Dispose()
$image.Dispose()
