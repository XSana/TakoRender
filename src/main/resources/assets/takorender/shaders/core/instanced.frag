#version 330 core

// Instanced Model shader - fragment
// Lambertian diffuse lighting with optional texture
// Same as model.frag (instance transform handled in vertex shader)

in vec3 vNormal;
in vec2 vTexCoord;
in vec3 vViewPos;

// Material properties
uniform vec4 uColor;
uniform sampler2D uTexture;
uniform bool uHasTexture;

// Lighting properties
uniform vec3 uLightDirection;  // Direction TO light source (normalized)
uniform vec3 uLightColor;      // Light color (default white)
uniform vec3 uAmbientColor;    // Ambient light color
uniform float uEmissive;       // Emissive intensity (0-1)

// PBR-like properties
uniform float uMetallic;
uniform float uRoughness;

layout(location = 0) out vec4 FragColor;

void main()
{
    vec3 normal = normalize(vNormal);

    // Base color from texture or uniform
    vec4 baseColor = uColor;
    if (uHasTexture) {
        vec4 texColor = texture(uTexture, vTexCoord);
        baseColor *= texColor;
    }

    // Discard fully transparent pixels
    if (baseColor.a < 0.01) {
        discard;
    }

    // Lambertian diffuse
    float NdotL = max(dot(normal, uLightDirection), 0.0);

    // Ambient + diffuse lighting
    vec3 ambient = uAmbientColor * baseColor.rgb;
    vec3 diffuse = uLightColor * baseColor.rgb * NdotL;

    // Simple specular (Blinn-Phong) for non-zero metallic
    vec3 specular = vec3(0.0);
    if (uMetallic > 0.01 && NdotL > 0.0) {
        vec3 viewDir = normalize(-vViewPos);
        vec3 halfDir = normalize(uLightDirection + viewDir);
        float NdotH = max(dot(normal, halfDir), 0.0);
        // Roughness affects specular power (higher roughness = softer highlights)
        float specPower = mix(128.0, 8.0, uRoughness);
        float spec = pow(NdotH, specPower);
        specular = uLightColor * spec * uMetallic;
    }

    // Emissive contribution
    vec3 emissive = baseColor.rgb * uEmissive;

    // Final color
    vec3 finalColor = ambient + diffuse + specular + emissive;

    FragColor = vec4(finalColor, baseColor.a);
}
