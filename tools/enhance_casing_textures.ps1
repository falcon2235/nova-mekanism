Add-Type -AssemblyName System.Drawing
# Post-processes the flat generated casing textures into something with metal grain
# and depth: a deterministic dither/noise pass plus a soft top-left -> bottom-right
# light gradient. Idempotent-ish but meant to run ONCE over freshly generated files;
# re-running deepens the effect slightly, so regenerate the base texture first if you
# want to start over.
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))

# Casing-family textures only: flat metal panels that benefit from grain.
# Glass, coils, conveyors, drills and controller fronts are deliberately excluded —
# they already carry detail and noise would muddy them.
$targets = @(
  'heat_proof_casing','ptfe_casing','stainless_casing','alloy_blast_casing','frost_proof_casing',
  'electrolyzer_casing','centrifuge_casing','assembly_casing','fusion_casing','star_casing',
  'neutronium_casing','accelerator_casing','void_miner_casing','oil_rig_casing','engine_casing',
  'annihilation_casing','inscriber_casing','charger_casing','livingrock_casing','elven_gate_casing',
  'terra_plate_casing','assline_casing','research_casing','sourcestone_casing','multiblock_casing',
  'heat_vent','assline_grate','mana_hatch'
)

function Clamp([int]$v){ if($v -lt 0){0} elseif($v -gt 255){255} else {$v} }

# Deterministic value noise so repeated runs and both dev/prod builds match.
# All arithmetic is masked to 31 bits so PowerShell never promotes to Double.
function Noise([int]$x,[int]$y){
  $mask = 0x7FFFFFFF
  $n = ((($x * 37 + $y * 101) -band $mask) -bxor (($x * 8161 + $y * 271) -band $mask)) -band $mask
  $n = (($n -bxor ($n -shr 13)) * 131) -band $mask
  $n = ($n -bxor ($n -shr 7)) -band $mask
  return ($n % 100) / 100.0
}

$done = 0
foreach($name in $targets){
  $path = Join-Path $dir "$name.png"
  # -LiteralPath: the user's profile path can contain backticks, which Test-Path
  # would otherwise treat as wildcard escapes and fail to find the file.
  if(-not (Test-Path -LiteralPath $path)){ continue }
  $src = [System.Drawing.Bitmap]::FromFile($path)
  $bmp = New-Object System.Drawing.Bitmap $src.Width, $src.Height
  for($x=0;$x -lt $src.Width;$x++){
    for($y=0;$y -lt $src.Height;$y++){
      $p = $src.GetPixel($x,$y)
      if($p.A -eq 0){ $bmp.SetPixel($x,$y,$p); continue }
      # grain: +/- 7 levels of deterministic noise
      $n = [int](((Noise $x $y) - 0.5) * 14)
      # depth: light from the top-left, shade toward the bottom-right
      $grad = [int](((($src.Width - 1 - $x) + ($src.Height - 1 - $y)) / [double](2 * ($src.Width - 1)) - 0.5) * 12)
      $d = $n + $grad
      $c = [System.Drawing.Color]::FromArgb($p.A, (Clamp ($p.R + $d)), (Clamp ($p.G + $d)), (Clamp ($p.B + $d)))
      $bmp.SetPixel($x,$y,$c)
    }
  }
  $src.Dispose()
  $bmp.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  $done++
  Write-Host "enhanced $name"
}
Write-Host "done ($done textures)"
