Add-Type -AssemblyName System.Drawing
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\effect'))
[System.IO.Directory]::CreateDirectory($dir) | Out-Null
function Save($bmp,$p){$bmp.Save($p,[System.Drawing.Imaging.ImageFormat]::Png);$bmp.Dispose()}
$N = 48; $c = ($N-1)/2.0

# --- star: radial glow, white core -> yellow -> orange -> transparent ---
$bmp = New-Object System.Drawing.Bitmap $N, $N
for ($x=0;$x -lt $N;$x++){ for ($y=0;$y -lt $N;$y++){
  $d = [Math]::Sqrt(($x-$c)*($x-$c)+($y-$c)*($y-$c)) / $c   # 0..~1
  if ($d -ge 1.0){ $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)); continue }
  $t = 1.0 - $d
  # colour ramp
  if ($d -lt 0.28){ $r=255;$g=250;$b=228 }
  elseif ($d -lt 0.55){ $r=255;$g=222;$b=138 }
  else { $r=255;$g=160;$b=70 }
  # alpha: bright core, soft falloff (squared)
  $a = [int]([Math]::Min(255, 255 * [Math]::Pow($t, 1.6) * 1.15))
  $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($a,$r,$g,$b))
}}
Save $bmp (Join-Path $dir 'star.png'); Write-Host 'effect star'

# --- black hole: dark core -> bright orange/violet accretion ring -> transparent ---
$bmp = New-Object System.Drawing.Bitmap $N, $N
for ($x=0;$x -lt $N;$x++){ for ($y=0;$y -lt $N;$y++){
  $d = [Math]::Sqrt(($x-$c)*($x-$c)+($y-$c)*($y-$c)) / $c
  if ($d -ge 1.0){ $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)); continue }
  if ($d -lt 0.34){                    # event horizon: near-black, opaque
    $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(255,6,3,12)); continue
  }
  if ($d -lt 0.46){                    # thin bright inner rim (hot)
    $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(255,255,180,90)); continue
  }
  if ($d -lt 0.66){                    # orange accretion
    $k = ($d-0.46)/0.20
    $r=[int](255); $g=[int](150-60*$k); $b=[int](50+40*$k)
    $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(235,$r,$g,$b)); continue
  }
  # outer violet halo, fading out
  $t = (1.0-$d)/0.34
  $a = [int]([Math]::Max(0,[Math]::Min(220, 220*$t)))
  $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($a,150,80,205))
}}
Save $bmp (Join-Path $dir 'black_hole.png'); Write-Host 'effect black_hole'
Write-Host 'done'
