# Single source of truth for every port skin.
#
# A port is built INTO a machine's wall, so its frame has to be that wall's colour.
# There were only eleven skins for twenty-odd casings, so machines borrowed whichever
# was closest — the oil rig ended up with near-white stainless ports on a charcoal
# hull. Rather than hand-picking palettes again, each style now DERIVES its frame
# from its own casing texture, so a port can never drift from the wall it sits in.
#
# Writes the textures, the block models and the port blockstates together, so the
# three can never disagree.
Add-Type -AssemblyName System.Drawing

$root      = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\assets\mekanism_more_multiblock'))
$texDir    = Join-Path $root 'textures\block'
$modelDir  = Join-Path $root 'models\block'
$stateDir  = Join-Path $root 'blockstates'
$NS = 'mekanism_more_multiblock'

function C([int]$r, [int]$g, [int]$b) { [System.Drawing.Color]::FromArgb(255, $r, $g, $b) }

# style id -> casing texture the palette is taken from.
# The first eleven ids already exist in saved worlds, so their NAMES must not change.
$styleCasing = [ordered]@{
    'default'      = 'multiblock_casing'
    'heat_proof'   = 'heat_proof_casing'
    'ptfe'         = 'ptfe_casing'
    'stainless'    = 'stainless_casing'
    'brick'        = $null            # vanilla bricks: no casing texture of ours
    'alloy'        = 'alloy_blast_casing'
    'frost'        = 'frost_proof_casing'
    'assembly'     = 'assembly_casing'
    'electrolyzer' = 'electrolyzer_casing'
    'centrifuge'   = 'centrifuge_casing'
    'fusion'       = 'fusion_casing'
    # --- added so every machine's ports match its own hull ---
    'oil_rig'      = 'oil_rig_casing'
    'engine'       = 'engine_casing'
    'hazard'       = 'void_miner_casing'
    'inscriber'    = 'inscriber_casing'
    'charger'      = 'charger_casing'
    'livingrock'   = 'livingrock_casing'
    'elven'        = 'elven_gate_casing'
    'terra'        = 'terra_plate_casing'
    'research'     = 'research_casing'
    'assline'      = 'assline_casing'
    'sourcestone'  = 'sourcestone_casing'
    'replicator'   = 'replicator_casing'
    'transdim'     = 'transdimensional_casing'
    'reformer'     = 'reformer_casing'
    'star'         = 'star_casing'
    'neutronium'   = 'neutronium_casing'
    'accelerator'  = 'accelerator_casing'
    'annihilation' = 'annihilation_casing'
}

# Bricks are vanilla, so this one palette stays hand-set.
$brickPalette = @{ base = (C 150 97 83); light = (C 178 124 108); dark = (C 108 66 56); bolt = (C 196 148 132) }

<#
 Derives a four-colour hull palette from a casing texture: the colour it is mostly
 made of, plus a lighter and darker relative of it. Sampling the real texture keeps
 the port and the wall in the same family even when the casing is later repainted.
#>
function Get-Palette([string]$casing) {
    if (-not $casing) { return $brickPalette }
    $path = Join-Path $texDir ($casing + '.png')
    if (-not (Test-Path -LiteralPath $path)) {
        Write-Host ("  WARN: no casing texture for " + $casing + ", using default palette")
        return @{ base = (C 74 85 104); light = (C 113 128 150); dark = (C 45 55 72); bolt = (C 160 174 192) }
    }
    $img = [System.Drawing.Bitmap]::FromFile($path)
    $counts = @{}
    $sumR = 0; $sumG = 0; $sumB = 0; $n = 0
    for ($x = 0; $x -lt $img.Width; $x++) {
        for ($y = 0; $y -lt $img.Height; $y++) {
            $p = $img.GetPixel($x, $y)
            if ($p.A -lt 128) { continue }
            $key = "$($p.R),$($p.G),$($p.B)"
            if ($counts.ContainsKey($key)) { $counts[$key]++ } else { $counts[$key] = 1 }
            $sumR += $p.R; $sumG += $p.G; $sumB += $p.B; $n++
        }
    }
    $img.Dispose()
    if ($n -eq 0) { return $brickPalette }

    # the hull's dominant colour, falling back to its average
    $topKey = ($counts.GetEnumerator() | Sort-Object -Property Value -Descending | Select-Object -First 1).Key
    $parts = $topKey.Split(',')
    $br = [int]$parts[0]; $bg = [int]$parts[1]; $bb = [int]$parts[2]
    # a very dark or very bright dominant pixel makes a poor frame; blend toward the
    # average so styles like the star casing (near-black with specks) stay readable.
    $lum = 0.299 * $br + 0.587 * $bg + 0.114 * $bb
    if ($lum -lt 45 -or $lum -gt 225) {
        $br = [int](($br + $sumR / $n) / 2); $bg = [int](($bg + $sumG / $n) / 2); $bb = [int](($bb + $sumB / $n) / 2)
    }
    function Shift([int]$v, [double]$f) { [Math]::Max(0, [Math]::Min(255, [int]($v * $f))) }
    return @{
        base  = (C $br $bg $bb)
        light = (C (Shift $br 1.28) (Shift $bg 1.28) (Shift $bb 1.28))
        dark  = (C (Shift $br 0.62) (Shift $bg 0.62) (Shift $bb 0.62))
        bolt  = (C (Shift $br 1.45) (Shift $bg 1.45) (Shift $bb 1.45))
    }
}

# --- the shared hull + glyph painters (identical to the originals so the set stays consistent) ---
function Paint-GtBase($bmp, $p) {
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $c = $p.base
            if ($x -eq 0 -or $y -eq 0 -or $x -eq 15 -or $y -eq 15) { $c = $p.dark }
            elseif ($x -eq 1 -or $y -eq 1) { $c = $p.light }
            elseif ($x -eq 14 -or $y -eq 14) { $c = $p.dark }
            $bmp.SetPixel($x, $y, $c)
        }
    }
    foreach ($pt in @(@(2,2), @(12,2), @(2,12), @(12,12))) {
        $bx = $pt[0]; $by = $pt[1]
        $bmp.SetPixel($bx, $by, $p.bolt)
        $bmp.SetPixel($bx+1, $by, $p.dark)
        $bmp.SetPixel($bx, $by+1, $p.dark)
        $bmp.SetPixel($bx+1, $by+1, $p.bolt)
    }
}

function Paint-Panel($bmp, $ring, [int]$x0, [int]$y0, [int]$x1, [int]$y1) {
    for ($x = $x0; $x -le $x1; $x++) {
        for ($y = $y0; $y -le $y1; $y++) {
            if ($x -eq $x0 -or $x -eq $x1 -or $y -eq $y0 -or $y -eq $y1) { $bmp.SetPixel($x, $y, $ring) }
            else { $bmp.SetPixel($x, $y, (C 26 32 44)) }
        }
    }
}

function Paint-Symbol($bmp, [string]$port) {
    switch ($port) {
        'energy_port' {
            Paint-Panel $bmp (C 128 106 0) 4 4 11 11
            $yellow = C 255 213 0
            foreach ($pt in @(@(8,5),@(7,6),@(7,7),@(8,7),@(9,7),@(8,8),@(7,9),@(7,10))) { $bmp.SetPixel($pt[0], $pt[1], $yellow) }
        }
        'item_input_port' {
            Paint-Panel $bmp (C 33 82 122) 4 4 11 11
            $blue = C 66 165 245
            foreach ($pt in @(@(7,5),@(8,5),@(7,6),@(8,6),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),@(6,8),@(7,8),@(8,8),@(9,8),@(7,9),@(8,9))) { $bmp.SetPixel($pt[0], $pt[1], $blue) }
        }
        'item_output_port' {
            Paint-Panel $bmp (C 128 76 0) 4 4 11 11
            $orange = C 255 152 0
            foreach ($pt in @(@(7,9),@(8,9),@(7,8),@(8,8),@(5,7),@(6,7),@(7,7),@(8,7),@(9,7),@(10,7),@(6,6),@(7,6),@(8,6),@(9,6),@(7,5),@(8,5))) { $bmp.SetPixel($pt[0], $pt[1], $orange) }
        }
        'gas_input_port' {
            Paint-Panel $bmp (C 106 76 148) 3 3 12 10
            $g = C 186 140 240
            foreach ($pt in @(@(6,5),@(7,5),@(8,5),@(9,5),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(6,7),@(8,7))) { $bmp.SetPixel($pt[0], $pt[1], $g) }
            $ac = C 66 165 245
            foreach ($pt in @(@(7,11),@(8,11),@(6,12),@(7,12),@(8,12),@(9,12),@(7,13),@(8,13))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'gas_output_port' {
            Paint-Panel $bmp (C 106 76 148) 3 3 12 10
            $g = C 186 140 240
            foreach ($pt in @(@(6,5),@(7,5),@(8,5),@(9,5),@(5,6),@(6,6),@(7,6),@(8,6),@(9,6),@(10,6),@(6,7),@(8,7))) { $bmp.SetPixel($pt[0], $pt[1], $g) }
            $ac = C 255 152 0
            foreach ($pt in @(@(7,13),@(8,13),@(6,12),@(7,12),@(8,12),@(9,12),@(7,11),@(8,11))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'fluid_input_port' {
            Paint-Panel $bmp (C 40 120 160) 3 3 12 10
            $f = C 100 200 245
            foreach ($pt in @(@(7,4),@(6,5),@(7,5),@(8,5),@(6,6),@(7,6),@(8,6),@(6,7),@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $f) }
            $ac = C 66 165 245
            foreach ($pt in @(@(7,11),@(8,11),@(6,12),@(7,12),@(8,12),@(9,12),@(7,13),@(8,13))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
        'fluid_output_port' {
            Paint-Panel $bmp (C 40 120 160) 3 3 12 10
            $f = C 100 200 245
            foreach ($pt in @(@(7,4),@(6,5),@(7,5),@(8,5),@(6,6),@(7,6),@(8,6),@(6,7),@(7,7),@(8,7),@(7,8),@(8,8))) { $bmp.SetPixel($pt[0], $pt[1], $f) }
            $ac = C 255 152 0
            foreach ($pt in @(@(7,13),@(8,13),@(6,12),@(7,12),@(8,12),@(9,12),@(7,11),@(8,11))) { $bmp.SetPixel($pt[0], $pt[1], $ac) }
        }
    }
}

$ports = @('energy_port','item_input_port','item_output_port','gas_input_port','gas_output_port','fluid_input_port','fluid_output_port')

$made = 0
foreach ($style in $styleCasing.Keys) {
    $palette = Get-Palette $styleCasing[$style]
    $suffix = if ($style -eq 'default') { '' } else { "_$style" }
    foreach ($port in $ports) {
        $name = $port + $suffix
        $bmp = New-Object System.Drawing.Bitmap 16, 16
        Paint-GtBase $bmp $palette
        Paint-Symbol $bmp $port
        $bmp.Save((Join-Path $texDir ($name + '.png')), [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        # model
        $model = '{"parent": "minecraft:block/cube_all", "textures": {"all": "' + $NS + ':block/' + $name + '"}}'
        [System.IO.File]::WriteAllText((Join-Path $modelDir ($name + '.json')), $model + "`n")
        $made++
    }
}

# blockstates: one variant per style, written last so they always list what exists
foreach ($port in $ports) {
    $lines = @()
    foreach ($style in $styleCasing.Keys) {
        $suffix = if ($style -eq 'default') { '' } else { "_$style" }
        $lines += '    "style=' + $style + '": {"model": "' + $NS + ':block/' + $port + $suffix + '"}'
    }
    $json = "{`n  `"variants`": {`n" + ($lines -join ",`n") + "`n  }`n}`n"
    [System.IO.File]::WriteAllText((Join-Path $stateDir ($port + '.json')), $json)
}

Write-Host ("generated $made port textures + models across " + $styleCasing.Count + " styles")
Write-Host ("rewrote " + $ports.Count + " port blockstates")
