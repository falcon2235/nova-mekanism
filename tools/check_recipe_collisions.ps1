# Finds crafting recipes that are indistinguishable from each other.
#
# Vanilla resolves a crafting grid by returning the FIRST matching recipe, so two
# shaped recipes with the same pattern and the same ingredients are ambiguous: one
# of them can never be crafted, and which one loses is arbitrary. That is invisible
# until a player tries to make the loser — exactly how the engine / assembly-line
# casing clash was found.
#
# Recipes whose conditions are mutually exclusive (mod_loaded X vs not mod_loaded X)
# are never active together, so those pairs are not collisions.
$ErrorActionPreference = 'Stop'
$dir = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\src\main\resources\data\mekanism_more_multiblock\recipes'))

function Get-IngredientId($ing) {
    if ($null -eq $ing) { return '?' }
    if ($ing.PSObject.Properties.Name -contains 'item') { return 'i:' + $ing.item }
    if ($ing.PSObject.Properties.Name -contains 'tag')  { return 't:' + $ing.tag }
    return '?'
}

function Get-ConditionKey($r) {
    if (-not ($r.PSObject.Properties.Name -contains 'conditions')) { return '' }
    return ($r.conditions | ForEach-Object { $_ | ConvertTo-Json -Depth 8 -Compress }) -join '|'
}

# true when two condition sets can never both be satisfied
function Test-Exclusive([string]$a, [string]$b) {
    if ($a -eq $b) { return $false }
    foreach ($pair in @(@($a, $b), @($b, $a))) {
        $x = $pair[0]; $y = $pair[1]
        if ($x -match '"modid":"([a-z0-9_]+)"' ) {
            $mod = $Matches[1]
            if ($y -match 'forge:not' -and $y -match [regex]::Escape($mod)) { return $true }
        }
    }
    return $false
}

$sigs = @{}
$n = 0
Get-ChildItem -LiteralPath $dir -Filter *.json | ForEach-Object {
    $r = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
    $type = $r.type
    if ($type -ne 'minecraft:crafting_shaped' -and $type -ne 'minecraft:crafting_shapeless') { return }
    $n++
    if ($type -eq 'minecraft:crafting_shaped') {
        $rows = @()
        foreach ($row in $r.pattern) {
            $cells = @()
            foreach ($ch in $row.ToCharArray()) {
                if ($ch -eq ' ') { $cells += '_' }
                else { $cells += (Get-IngredientId $r.key.$ch) }
            }
            $rows += ($cells -join ',')
        }
        $sig = 'shaped|' + ($rows -join '/')
    } else {
        $ids = @()
        foreach ($ing in $r.ingredients) { $ids += (Get-IngredientId $ing) }
        $sig = 'shapeless|' + (($ids | Sort-Object) -join ',')
    }
    $entry = [pscustomobject]@{
        File   = $_.Name
        Result = $r.result.item
        Cond   = (Get-ConditionKey $r)
    }
    if ($sigs.ContainsKey($sig)) { $sigs[$sig] += $entry } else { $sigs[$sig] = @($entry) }
}

$collisions = 0
foreach ($sig in $sigs.Keys) {
    $group = @($sigs[$sig])
    if ($group.Count -lt 2) { continue }
    for ($i = 0; $i -lt $group.Count; $i++) {
        for ($j = $i + 1; $j -lt $group.Count; $j++) {
            if (Test-Exclusive $group[$i].Cond $group[$j].Cond) { continue }
            # same output from the same grid is a duplicate, not a clash the player sees
            if ($group[$i].Result -eq $group[$j].Result) { continue }
            $collisions++
            Write-Host ("COLLISION: " + $group[$i].File + " (" + $group[$i].Result + ")")
            Write-Host ("       vs: " + $group[$j].File + " (" + $group[$j].Result + ")")
            Write-Host ("    shape: " + $sig)
        }
    }
}
Write-Host ("checked $n crafting recipes")
if ($collisions -eq 0) { Write-Host 'NO COLLISIONS' } else { Write-Host "$collisions COLLISION(S) FOUND" }
