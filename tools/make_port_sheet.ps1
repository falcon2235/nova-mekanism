# Composes every port skin (7 port types x 11 styles) into one magnified contact
# sheet so the set can be judged as a set — inconsistencies between the scripts that
# generated them are invisible one file at a time.
Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
$out = $args[0]
if (-not $out) { $out = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\build\port_sheet.png')) }

$styles = @('default','heat_proof','ptfe','stainless','brick','alloy','frost','assembly','electrolyzer','centrifuge','fusion')
$ports  = @('energy_port','item_input_port','item_output_port','gas_input_port','gas_output_port','fluid_input_port','fluid_output_port')

$scale = 5
$cell  = 16 * $scale          # 80
$pad   = 4
$labelW = 130
$labelH = 16

$w = $labelW + $styles.Count * ($cell + $pad) + $pad
$h = $labelH + $ports.Count * ($cell + $pad) + $pad

$bmp = New-Object System.Drawing.Bitmap $w, $h
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.Clear([System.Drawing.Color]::FromArgb(255, 30, 30, 34))

$font = New-Object System.Drawing.Font('Consolas', 9)
$brush = [System.Drawing.Brushes]::White
$dim = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 255, 120, 120))

# column headers
for ($c = 0; $c -lt $styles.Count; $c++) {
    $x = $labelW + $c * ($cell + $pad)
    $g.DrawString($styles[$c].Substring(0, [Math]::Min(9, $styles[$c].Length)), $font, $brush, $x, 1)
}

for ($r = 0; $r -lt $ports.Count; $r++) {
    $y = $labelH + $r * ($cell + $pad)
    $g.DrawString($ports[$r], $font, $brush, 2, ($y + $cell / 2 - 7))
    for ($c = 0; $c -lt $styles.Count; $c++) {
        $name = if ($styles[$c] -eq 'default') { $ports[$r] } else { $ports[$r] + '_' + $styles[$c] }
        $path = Join-Path $dir ($name + '.png')
        $x = $labelW + $c * ($cell + $pad)
        if (Test-Path -LiteralPath $path) {
            $img = [System.Drawing.Image]::FromFile($path)
            $g.DrawImage($img, $x, $y, $cell, $cell)
            $img.Dispose()
        } else {
            $g.DrawString('MISSING', $font, $dim, $x, ($y + $cell / 2 - 7))
        }
    }
}
$g.Dispose()
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($out)) | Out-Null
$bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()
Write-Host $out
