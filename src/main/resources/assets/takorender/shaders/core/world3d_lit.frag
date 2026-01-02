#version 330 core

// World3D Lit shader - fragment
// MC lightmap sampling for realistic world lighting
// Supports day/night cycle, block light sources, and normal-based shading
// MC 1.7.10 right-hand coordinate system: +X=East, +Y=Up, +Z=South(toward player)

in vec4 vColor;
in vec2 vLightCoord;  // blockLight(U), skyLight(V) in 0-1 range
in vec3 vNormal;      // Surface normal in view space
in vec3 vWorldPos;    // Position in view space

uniform float uAlpha;
uniform sampler2D uLightmap;      // MC lightmap texture (16x16)
uniform bool uUseLighting;        // Enable/disable MC lighting
uniform float uLightIntensity;    // Light intensity multiplier (default 1.0)
uniform float uMinBrightness;     // Minimum brightness floor (default 0.1)
uniform bool uUseNormalShading;   // Enable normal-based directional shading

layout(location = 0) out vec4 FragColor;

// Constants for texel center sampling
const float LIGHTMAP_SCALE = 15.0 / 16.0;
const float LIGHTMAP_OFFSET = 0.5 / 16.0;

// Luminance weights (Rec. 709)
const vec3 LUMINANCE_WEIGHTS = vec3(0.2126, 0.7152, 0.0722);

// Simulated sun/sky light direction (from upper-south, matching MC default)
const vec3 SUN_DIR = normalize(vec3(0.2, 0.8, 0.4));

void main()
{
    vec4 finalColor = vColor;
    vec3 normal = normalize(vNormal);

    // Normal-based face culling using view direction
    vec3 viewDir = normalize(-vWorldPos);
    if (dot(normal, viewDir) < 0.0) {
        discard;
    }

    if (uUseLighting) {
        // Sample MC lightmap with texel center offset
        vec2 lightUV = vLightCoord * LIGHTMAP_SCALE + LIGHTMAP_OFFSET;
        vec3 lightColor = texture(uLightmap, lightUV).rgb;

        // Calculate luminance for intensity scaling
        float luminance = dot(lightColor, LUMINANCE_WEIGHTS);

        // Apply intensity multiplier
        float intensity = uLightIntensity > 0.0 ? uLightIntensity : 1.0;
        float minBright = uMinBrightness > 0.0 ? uMinBrightness : 0.1;

        // Scale light with intensity and clamp to min brightness
        vec3 scaledLight = lightColor * intensity;
        float scaledLuminance = max(dot(scaledLight, LUMINANCE_WEIGHTS), minBright);

        // Preserve color tint while applying brightness
        vec3 finalLight = luminance > 0.001
            ? scaledLight * (scaledLuminance / max(dot(scaledLight, LUMINANCE_WEIGHTS), 0.001))
            : vec3(minBright);

        // Apply normal-based directional shading if enabled
        if (uUseNormalShading) {
            float NdotL = max(dot(normal, SUN_DIR), 0.0);
            float skyFactor = normal.y * 0.5 + 0.5;
            float skyLightLevel = vLightCoord.y;
            float directional = mix(0.6, 1.0, NdotL * skyLightLevel);
            float ambient = mix(0.4, 0.7, skyFactor);
            float shadingFactor = mix(ambient, directional, skyLightLevel * 0.5);
            finalLight *= shadingFactor;
        }

        finalColor.rgb *= finalLight;
    }

    finalColor.a *= uAlpha;
    FragColor = finalColor;
}
