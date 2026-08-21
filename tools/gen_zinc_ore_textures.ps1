Add-Type -AssemblyName System.Drawing
# Zinc ore blocks, drawn with the same speckled-stone recipe as the other ores in
# this mod so the whole ore set reads as one family.
$blockDir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock\textures\block'))
function C([int]$r,[int]$g,[int]$b){[System.Drawing.Color]::FromArgb(255,$r,$g,$b)}
# fixed seed: the stone noise must be identical every regeneration
$rng = New-Object System.Random 20260822

function New-Ore($name, $baseCol, $baseDark, $speck, $speckHi) {
    $bmp = New-Object System.Drawing.Bitmap 16, 16
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = if ($rng.Next(4) -eq 0) { $baseDark } else { $baseCol }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    foreach ($b in @(@(4,4),@(11,5),@(6,10),@(10,11),@(3,12))) {
        foreach ($d in @(@(0,0),@(1,0),@(0,1),@(1,1),@(-1,0),@(0,-1))) {
            $x = $b[0] + $d[0]; $y = $b[1] + $d[1]
            if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) {
                $c = if (($d[0] -eq 0 -and $d[1] -eq 0)) { $speckHi } else { $speck }
                $bmp.SetPixel($x, $y, $c)
            }
        }
    }
    $bmp.Save((Join-Path $blockDir "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "block $name"
}

$stone = C 127 127 127; $stoneD = C 105 105 105
$deep  = C 80 80 82;    $deepD  = C 66 66 68
# zinc: cool bluish-white specks
New-Ore 'zinc_ore'           $stone $stoneD (C 168 186 196) (C 208 226 236)
New-Ore 'deepslate_zinc_ore' $deep  $deepD  (C 168 186 196) (C 208 226 236)
Write-Host 'done'
