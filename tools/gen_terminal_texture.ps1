Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$itemDir = Join-Path $root 'item'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
$bmp = New-Object System.Drawing.Bitmap 16,16
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) } }
# handheld terminal: dark casing body with a glowing green screen + a few buttons
$body = C 54 60 74; $bodyD = C 32 36 48; $bodyH = C 84 92 112
for ($x=3;$x -le 12;$x++){ for ($y=1;$y -le 14;$y++){
  $c = $body
  if ($x -eq 3 -or $y -eq 1){ $c = $bodyH }
  if ($x -eq 12 -or $y -eq 14){ $c = $bodyD }
  $bmp.SetPixel($x,$y,$c)
}}
# screen (green, with a grid = multiblock hologram vibe)
$scr = C 40 60 48; $grid = C 96 220 150; $gridH = C 190 255 210
for ($x=5;$x -le 10;$x++){ for ($y=3;$y -le 8;$y++){ $bmp.SetPixel($x,$y,$scr) } }
foreach ($p in @(@(6,4),@(8,4),@(6,6),@(8,6),@(7,5),@(9,5),@(5,7),@(7,7),@(9,7))){ $bmp.SetPixel($p[0],$p[1],$grid) }
$bmp.SetPixel(7,5,$gridH); $bmp.SetPixel(8,6,$gridH)
# buttons
foreach ($p in @(@(5,10),@(7,10),@(9,10),@(5,12),@(7,12),@(9,12))){ $bmp.SetPixel($p[0],$p[1],(C 150 160 180)) }
$bmp.SetPixel(9,12,(C 220 90 80))
$bmp.Save((Join-Path $itemDir 'construction_terminal.png'),[System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose(); Write-Host 'item construction_terminal'
