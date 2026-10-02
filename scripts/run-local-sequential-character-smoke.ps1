param(
    [string]$ComfyUrl = "http://127.0.0.1:8188",
    [string]$WorkspaceRoot = ([IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))),
    [int]$TimeoutMinutes = 20
)

$ErrorActionPreference = "Stop"

$comfyRoot = Join-Path $WorkspaceRoot "tools\image\ComfyUI"
$inputRoot = Join-Path $comfyRoot "input"
$outputRoot = Join-Path $comfyRoot "output"
$targetRoot = Join-Path $WorkspaceRoot "target\experiments\local-ai\aviadores-conditioned-frames"
$compiledRoot = Join-Path $WorkspaceRoot "target\experiments\local-ai\aviadores-storyboards-compiled"
$background = Join-Path $compiledRoot "backgrounds\fragmento_04_stage_plane.png"

New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null

$inputs = @{
    "local-seq-background.png" = $background
    "local-seq-mask-cap.png" = Join-Path $compiledRoot "masks\fragmento_04_capitan.png"
    "local-seq-mask-ten.png" = Join-Path $compiledRoot "masks\fragmento_04_teniente.png"
    "local-seq-capitan.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\capitan_bigote\capitan_bigote_01_frontal.png"
    "local-seq-teniente.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\teniente_tornillo\teniente_tornillo_01_frontal.png"
}

foreach ($entry in $inputs.GetEnumerator()) {
    if (-not (Test-Path -LiteralPath $entry.Value)) {
        throw "No existe la entrada local: $($entry.Value)"
    }
    Copy-Item -LiteralPath $entry.Value -Destination (Join-Path $inputRoot $entry.Key) -Force
}

$captainPrompt = @"
one single full-body male character standing naturally on a theatre stage,
Capitan Bigote, mature tall broad-shouldered veteran aviator, weathered masculine face,
large curled dark moustache, brown leather aviator cap with goggles resting on top,
cream scarf, dark brown shearling leather flight jacket, tan trousers and tall dark boots,
proud stern expression, hands confidently near his belt, warm cinematic stage lighting,
realistic anatomy, period adventure comedy
"@

$lieutenantPrompt = @"
one single full-body male character standing naturally on a theatre stage,
Teniente Tornillo, younger slim aviator with a clearly different youthful face,
small narrow moustache, brown leather aviator cap with goggles resting on top,
no prescription glasses, cream scarf, brown leather jacket over brown waistcoat,
tan trousers and tall lace-up boots, earnest puzzled expression,
holding one rusty screw with a tiny flower, warm cinematic stage lighting,
realistic anatomy, period adventure comedy
"@

$negativePrompt = @"
two people inside one mask, duplicate, clone, twins, extra person, third person,
character lineup, character sheet, military peaked cap, captain uniform, sunglasses,
prescription glasses, modern clothing, swapped face, malformed anatomy, extra limbs,
cropped body, floating body, text, watermark, logo, monochrome, pencil sketch
"@

$workflow = [ordered]@{
    "1" = @{
        class_type = "CheckpointLoaderSimple"
        inputs = @{ ckpt_name = "DreamShaper_8_pruned.safetensors" }
    }
    "2" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $captainPrompt; clip = @("1", 1) }
    }
    "3" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $negativePrompt; clip = @("1", 1) }
    }
    "4" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $lieutenantPrompt; clip = @("1", 1) }
    }
    "5" = @{
        class_type = "IPAdapterUnifiedLoader"
        inputs = @{ model = @("1", 0); preset = "PLUS (high strength)" }
    }
    "6" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-seq-capitan.png" }
    }
    "7" = @{
        class_type = "IPAdapterAdvanced"
        inputs = @{
            model = @("5", 0)
            ipadapter = @("5", 1)
            image = @("6", 0)
            weight = 0.68
            weight_type = "linear"
            combine_embeds = "concat"
            start_at = 0.0
            end_at = 0.76
            embeds_scaling = "V only"
        }
    }
    "8" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-seq-teniente.png" }
    }
    "9" = @{
        class_type = "IPAdapterAdvanced"
        inputs = @{
            model = @("5", 0)
            ipadapter = @("5", 1)
            image = @("8", 0)
            weight = 0.68
            weight_type = "linear"
            combine_embeds = "concat"
            start_at = 0.0
            end_at = 0.76
            embeds_scaling = "V only"
        }
    }
    "10" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-seq-background.png" }
    }
    "11" = @{
        class_type = "ImageScale"
        inputs = @{
            image = @("10", 0)
            upscale_method = "lanczos"
            width = 960
            height = 544
            crop = "center"
        }
    }
    "12" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-seq-mask-cap.png" }
    }
    "13" = @{
        class_type = "ImageToMask"
        inputs = @{ image = @("12", 0); channel = "red" }
    }
    "14" = @{
        class_type = "VAEEncodeForInpaint"
        inputs = @{
            pixels = @("11", 0)
            vae = @("1", 2)
            mask = @("13", 0)
            grow_mask_by = 10
        }
    }
    "15" = @{
        class_type = "KSampler"
        inputs = @{
            model = @("7", 0)
            seed = 550104
            steps = 30
            cfg = 6.5
            sampler_name = "dpmpp_2m"
            scheduler = "karras"
            positive = @("2", 0)
            negative = @("3", 0)
            latent_image = @("14", 0)
            denoise = 0.96
        }
    }
    "16" = @{
        class_type = "VAEDecode"
        inputs = @{ samples = @("15", 0); vae = @("1", 2) }
    }
    "17" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-seq-mask-ten.png" }
    }
    "18" = @{
        class_type = "ImageToMask"
        inputs = @{ image = @("17", 0); channel = "red" }
    }
    "19" = @{
        class_type = "VAEEncodeForInpaint"
        inputs = @{
            pixels = @("16", 0)
            vae = @("1", 2)
            mask = @("18", 0)
            grow_mask_by = 10
        }
    }
    "20" = @{
        class_type = "KSampler"
        inputs = @{
            model = @("9", 0)
            seed = 550105
            steps = 30
            cfg = 6.5
            sampler_name = "dpmpp_2m"
            scheduler = "karras"
            positive = @("4", 0)
            negative = @("3", 0)
            latent_image = @("19", 0)
            denoise = 0.96
        }
    }
    "21" = @{
        class_type = "VAEDecode"
        inputs = @{ samples = @("20", 0); vae = @("1", 2) }
    }
    "22" = @{
        class_type = "SaveImage"
        inputs = @{
            images = @("21", 0)
            filename_prefix = "docupodcast-local-sequential/fragmento_04_dignidad_mantenimiento"
        }
    }
}

$body = @{
    prompt = $workflow
    client_id = "docupodcast-local-sequential-character-smoke"
} | ConvertTo-Json -Depth 35

try {
    $queued = Invoke-RestMethod -Uri "$ComfyUrl/prompt" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 60
} catch {
    throw "ComfyUI rechazo el workflow secuencial: $($_.ErrorDetails.Message)"
}

$promptId = [string]$queued.prompt_id
if ([string]::IsNullOrWhiteSpace($promptId)) {
    throw "ComfyUI no devolvio prompt_id."
}
Write-Host "promptId=$promptId seeds=550104,550105"

$deadline = (Get-Date).AddMinutes($TimeoutMinutes)
$entry = $null
do {
    Start-Sleep -Seconds 2
    $history = Invoke-RestMethod -Uri "$ComfyUrl/history/$promptId" -TimeoutSec 20
    $property = $history.PSObject.Properties[$promptId]
    if ($null -ne $property) {
        $entry = $property.Value
        break
    }
} while ((Get-Date) -lt $deadline)

if ($null -eq $entry) {
    throw "Timeout esperando el frame secuencial local."
}
if ($entry.status.status_str -ne "success") {
    $messages = $entry.status.messages | ConvertTo-Json -Depth 20 -Compress
    throw "Fallo el frame secuencial local: $messages"
}

$images = $entry.outputs."22".images
if ($null -eq $images -or $images.Count -eq 0) {
    throw "ComfyUI termino sin PNG secuencial."
}

$image = $images[0]
$relative = if ([string]::IsNullOrWhiteSpace([string]$image.subfolder)) {
    [string]$image.filename
} else {
    Join-Path ([string]$image.subfolder) ([string]$image.filename)
}
$source = Join-Path $outputRoot $relative
$target = Join-Path $targetRoot "fragmento_04_dignidad_mantenimiento_frame_secuencial_local.png"
Copy-Item -LiteralPath $source -Destination $target -Force
$target
