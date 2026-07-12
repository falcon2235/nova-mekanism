Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b,[int]$a=255){[System.Drawing.Color]::FromArgb($a,$r,$g,$b)}
function Save($bmp,$n){$bmp.Save((Join-Path $dir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose();Write-Host $n}

# quantum conduit skin: dark hull with a bright energy channel down the middle;
# the channel colour identifies the type.
function Paint-Conduit($name,$chR,$chG,$chB,$hiR,$hiG,$hiB){
  $bmp=New-Object System.Drawing.Bitmap 16,16
  $hull=C 44 44 54; $hullHi=C 70 70 84; $hullDk=C 26 26 34
  for($x=0;$x -lt 16;$x++){for($y=0;$y -lt 16;$y++){
    $c=$hull
    if($y -eq 0 -or $y -eq 15){$c=$hullDk}
    elseif($y -eq 1 -or $y -eq 14){$c=$hullHi}
    $bmp.SetPixel($x,$y,$c)
  }}
  $ch=C $chR $chG $chB; $hi=C $hiR $hiG $hiB
  for($x=0;$x -lt 16;$x++){
    $bmp.SetPixel($x,7,$ch); $bmp.SetPixel($x,8,$ch)
    if(($x % 4) -eq 1){$bmp.SetPixel($x,7,$hi)}
    if(($x % 4) -eq 3){$bmp.SetPixel($x,8,$hi)}
  }
  for($x=0;$x -lt 16;$x+=4){ $bmp.SetPixel($x,6,$hullDk); $bmp.SetPixel($x,9,$hullDk) }
  Save $bmp $name
}

Paint-Conduit 'quantum_cable'      80 220 255  220 250 255   # cyan (energy)
Paint-Conduit 'quantum_fluid_pipe' 70 130 240  190 215 255   # blue (fluid)
Paint-Conduit 'quantum_gas_tube'   150 230 110 225 255 200   # green (gas)
Paint-Conduit 'quantum_item_pipe'  235 190 80  255 235 180   # gold (items)
Write-Host 'done'
