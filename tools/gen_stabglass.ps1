Add-Type -AssemblyName System.Drawing
$root = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures'))
$dir = Join-Path $root 'block'
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
# reinforced containment glass: transparent violet pane with a steel-violet frame + shine
$bmp = New-Object System.Drawing.Bitmap 16,16
$fr = C 150 110 200; $frD = C 96 70 140; $shine = C 220 200 250 170; $tint = C 170 130 220 44
for ($x=0;$x -lt 16;$x++){ for ($y=0;$y -lt 16;$y++){
  if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15){ $c = if((($x+$y)%2) -eq 0){$fr}else{$frD}; $bmp.SetPixel($x,$y,$c) }
  elseif (($x -eq $y -and $x -ge 2 -and $x -le 6) -or (($x-1) -eq $y -and $x -ge 3 -and $x -le 7)){ $bmp.SetPixel($x,$y,$shine) }
  else { $bmp.SetPixel($x,$y,$tint) }
}}
$bmp.Save((Join-Path $dir 'stabilizer_glass.png'),[System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()
Write-Host 'glass stabilizer_glass'
