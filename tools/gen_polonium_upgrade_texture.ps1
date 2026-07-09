Add-Type -AssemblyName System.Drawing
$itemDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\item'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
# upgrade module: circuit card with an antimatter (magenta/violet) core
$bmp = New-Object System.Drawing.Bitmap 16,16
for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){ $bmp.SetPixel($x,$y,(C 0 0 0 0)) }}
$card=C 46 40 60; $cardHi=C 84 74 108; $cardLo=C 28 24 40
for($x=2;$x -le 13;$x++){for($y=2;$y -le 13;$y++){
  $c=$card; if($y -eq 2 -or $x -eq 2){$c=$cardHi}; if($y -eq 13 -or $x -eq 13){$c=$cardLo}
  $bmp.SetPixel($x,$y,$c)
}}
# gold contact pins along the bottom
for($x=4;$x -le 11;$x+=2){ $bmp.SetPixel($x,13,(C 230 200 90)); $bmp.SetPixel($x,12,(C 230 200 90)) }
# antimatter core (bright magenta/violet with white centre)
for($x=6;$x -le 9;$x++){for($y=5;$y -le 8;$y++){ $bmp.SetPixel($x,$y,(C 210 60 220)) }}
foreach($p in @(@(7,6),@(8,7))){ $bmp.SetPixel($p[0],$p[1],(C 255 235 255)) }
foreach($p in @(@(6,5),@(9,8),@(9,5),@(6,8))){ $bmp.SetPixel($p[0],$p[1],(C 150 40 180)) }
# traces
foreach($p in @(@(4,4),@(5,4),@(11,4),@(4,10),@(11,10),@(11,11))){ $bmp.SetPixel($p[0],$p[1],(C 120 100 160)) }
$bmp.Save((Join-Path $itemDir 'polonium_synthesis_upgrade.png'),[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Write-Host 'item polonium_synthesis_upgrade'
