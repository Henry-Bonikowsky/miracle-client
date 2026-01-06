Add-Type -AssemblyName System.Drawing
$iconDir = 'C:\Users\henry\Projects\Miracle Client\mod\src\main\resources\assets\miracle\textures\gui\icons'
Get-ChildItem $iconDir -Filter '*.png' | ForEach-Object {
    $bytes = [System.IO.File]::ReadAllBytes($_.FullName)
    $ms = New-Object System.IO.MemoryStream(,$bytes)
    $img = [System.Drawing.Bitmap]::new($ms)

    $newImg = New-Object System.Drawing.Bitmap($img.Width, $img.Height)
    for ($x = 0; $x -lt $img.Width; $x++) {
        for ($y = 0; $y -lt $img.Height; $y++) {
            $pixel = $img.GetPixel($x, $y)
            $newColor = [System.Drawing.Color]::FromArgb($pixel.A, 255 - $pixel.R, 255 - $pixel.G, 255 - $pixel.B)
            $newImg.SetPixel($x, $y, $newColor)
        }
    }

    $img.Dispose()
    $ms.Dispose()
    $newImg.Save($_.FullName, [System.Drawing.Imaging.ImageFormat]::Png)
    $newImg.Dispose()
    Write-Host "Inverted: $($_.Name)"
}
Write-Host "Done!"
