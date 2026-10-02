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

New-Item -ItemType Directory -Path $targetRoot -Force | Out-Null

$inputs = @{
    "local-final-sb04.png" = Join-Path $compiledRoot "fragmento_04_dignidad_mantenimiento_storyboard_compilado.png"
    "local-final-mask-cap04.png" = Join-Path $compiledRoot "masks\fragmento_04_capitan.png"
    "local-final-mask-ten04.png" = Join-Path $compiledRoot "masks\fragmento_04_teniente.png"
    "local-final-capitan.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\capitan_bigote\capitan_bigote_01_frontal.png"
    "local-final-teniente.png" = Join-Path $WorkspaceRoot "src\main\resources\examples\aviadores-comicos\assets\personajes\teniente_tornillo\teniente_tornillo_01_frontal.png"
}

foreach ($entry in $inputs.GetEnumerator()) {
    if (-not (Test-Path -LiteralPath $entry.Value)) {
        throw "No existe la entrada local: $($entry.Value)"
    }
    Copy-Item -LiteralPath $entry.Value -Destination (Join-Path $inputRoot $entry.Key) -Force
}

$globalPrompt = @"
cinematic realistic theatre production still, frontal audience eye-level camera,
old rural aircraft hangar represented as a physical stage set, proscenium curtains visible,
warm dawn stage lighting, vintage cream fabric and aged brass biplane centered behind the actors,
exactly two male aviator actors with clearly different faces and body types,
Capitan Bigote stands audience-left, Teniente Tornillo stands audience-right,
the younger lieutenant presents one rusty screw with a tiny flower,
the veteran captain reacts with proud dismissive authority,
natural anatomy, coherent scale, period adventure comedy, detailed but believable materials
"@

$captainPrompt = @"
Capitan Bigote, one mature tall broad-shouldered male veteran pilot,
large curled dark moustache, weathered face, brown leather aviator cap with goggles on top,
cream scarf, dark brown shearling leather flight jacket, tan trousers, tall dark boots,
proud stern expression, arms folded or hands confidently near his belt
"@

$lieutenantPrompt = @"
Teniente Tornillo, one younger slim male pilot with a different youthful face,
small narrow moustache, brown leather aviator cap with goggles on top, no prescription glasses,
cream scarf, brown leather jacket over brown waistcoat, tan trousers, tall lace-up boots,
earnest puzzled expression, showing a rusty screw with a tiny flower
"@

$negativePrompt = @"
more than two men, third person, duplicate, clone, twins, character lineup, character sheet,
six pilots, crowd, audience, swapped faces, same face, mixed clothing, captain uniform cap,
military peaked cap, sunglasses, prescription glasses, modern clothing, modern airplane,
extra limbs, malformed hands, fused bodies, floating object, text, letters, watermark, logo,
pencil sketch, monochrome drawing, blank white background, product catalog
"@

$workflow = [ordered]@{
    "1" = @{
        class_type = "CheckpointLoaderSimple"
        inputs = @{ ckpt_name = "DreamShaper_8_pruned.safetensors" }
    }
    "2" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $globalPrompt; clip = @("1", 1) }
    }
    "3" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $negativePrompt; clip = @("1", 1) }
    }
    "4" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $captainPrompt; clip = @("1", 1) }
    }
    "5" = @{
        class_type = "CLIPTextEncode"
        inputs = @{ text = $lieutenantPrompt; clip = @("1", 1) }
    }
    "6" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-final-mask-cap04.png" }
    }
    "7" = @{
        class_type = "ImageToMask"
        inputs = @{ image = @("6", 0); channel = "red" }
    }
    "8" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-final-mask-ten04.png" }
    }
    "9" = @{
        class_type = "ImageToMask"
        inputs = @{ image = @("8", 0); channel = "red" }
    }
    "10" = @{
        class_type = "ConditioningSetMask"
        inputs = @{
            conditioning = @("4", 0)
            mask = @("7", 0)
            strength = 1.15
            set_cond_area = "mask bounds"
        }
    }
    "11" = @{
        class_type = "ConditioningSetMask"
        inputs = @{
            conditioning = @("5", 0)
            mask = @("9", 0)
            strength = 1.15
            set_cond_area = "mask bounds"
        }
    }
    "12" = @{
        class_type = "ConditioningCombine"
        inputs = @{ conditioning_1 = @("2", 0); conditioning_2 = @("10", 0) }
    }
    "13" = @{
        class_type = "ConditioningCombine"
        inputs = @{ conditioning_1 = @("12", 0); conditioning_2 = @("11", 0) }
    }
    "14" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-final-sb04.png" }
    }
    "15" = @{
        class_type = "ImageScale"
        inputs = @{
            image = @("14", 0)
            upscale_method = "lanczos"
            width = 960
            height = 544
            crop = "center"
        }
    }
    "16" = @{
        class_type = "ImageInvert"
        inputs = @{ image = @("15", 0) }
    }
    "17" = @{
        class_type = "ControlNetLoader"
        inputs = @{ control_net_name = "control_v11p_sd15_scribble_fp16.safetensors" }
    }
    "18" = @{
        class_type = "ControlNetApplyAdvanced"
        inputs = @{
            positive = @("13", 0)
            negative = @("3", 0)
            control_net = @("17", 0)
            image = @("16", 0)
            strength = 0.92
            start_percent = 0.0
            end_percent = 0.82
            vae = @("1", 2)
        }
    }
    "19" = @{
        class_type = "IPAdapterUnifiedLoader"
        inputs = @{ model = @("1", 0); preset = "PLUS (high strength)" }
    }
    "20" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-final-capitan.png" }
    }
    "21" = @{
        class_type = "IPAdapterAdvanced"
        inputs = @{
            model = @("19", 0)
            ipadapter = @("19", 1)
            image = @("20", 0)
            weight = 0.58
            weight_type = "linear"
            combine_embeds = "concat"
            start_at = 0.0
            end_at = 0.70
            embeds_scaling = "V only"
            attn_mask = @("7", 0)
        }
    }
    "22" = @{
        class_type = "LoadImage"
        inputs = @{ image = "local-final-teniente.png" }
    }
    "23" = @{
        class_type = "IPAdapterAdvanced"
        inputs = @{
            model = @("21", 0)
            ipadapter = @("19", 1)
            image = @("22", 0)
            weight = 0.58
            weight_type = "linear"
            combine_embeds = "concat"
            start_at = 0.0
            end_at = 0.70
            embeds_scaling = "V only"
            attn_mask = @("9", 0)
        }
    }
    "24" = @{
        class_type = "EmptyLatentImage"
        inputs = @{ width = 960; height = 544; batch_size = 1 }
    }
    "25" = @{
        class_type = "KSampler"
        inputs = @{
            model = @("23", 0)
            seed = 440104
            steps = 34
            cfg = 7.0
            sampler_name = "dpmpp_2m"
            scheduler = "karras"
            positive = @("18", 0)
            negative = @("18", 1)
            latent_image = @("24", 0)
            denoise = 1.0
        }
    }
    "26" = @{
        class_type = "VAEDecode"
        inputs = @{ samples = @("25", 0); vae = @("1", 2) }
    }
    "27" = @{
        class_type = "SaveImage"
        inputs = @{
            images = @("26", 0)
            filename_prefix = "docupodcast-local-conditioned/fragmento_04_dignidad_mantenimiento"
        }
    }
}

$body = @{
    prompt = $workflow
    client_id = "docupodcast-local-conditioned-frame-smoke"
} | ConvertTo-Json -Depth 35

try {
    $queued = Invoke-RestMethod -Uri "$ComfyUrl/prompt" -Method Post -ContentType "application/json" -Body $body -TimeoutSec 60
} catch {
    throw "ComfyUI rechazo el workflow regional: $($_.ErrorDetails.Message)"
}

$promptId = [string]$queued.prompt_id
if ([string]::IsNullOrWhiteSpace($promptId)) {
    throw "ComfyUI no devolvio prompt_id."
}
Write-Host "promptId=$promptId seed=440104"

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
    throw "Timeout esperando el frame condicionado local."
}
if ($entry.status.status_str -ne "success") {
    $messages = $entry.status.messages | ConvertTo-Json -Depth 20 -Compress
    throw "Fallo el frame condicionado local: $messages"
}

$images = $entry.outputs."27".images
if ($null -eq $images -or $images.Count -eq 0) {
    throw "ComfyUI termino sin PNG condicionado."
}

$image = $images[0]
$relative = if ([string]::IsNullOrWhiteSpace([string]$image.subfolder)) {
    [string]$image.filename
} else {
    Join-Path ([string]$image.subfolder) ([string]$image.filename)
}
$source = Join-Path $outputRoot $relative
$target = Join-Path $targetRoot "fragmento_04_dignidad_mantenimiento_frame_local.png"
Copy-Item -LiteralPath $source -Destination $target -Force
$target
