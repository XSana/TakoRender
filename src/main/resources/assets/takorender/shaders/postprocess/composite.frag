#version 330 core

// Bloom composite shader
// Combines scene with bloom and applies HDR/tonemapping

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uSceneTexture;
uniform sampler2D uBloomTexture;
uniform float uBloomIntensity;
uniform float uExposure;
uniform int uEnableTonemap;
uniform float uBloomAlphaScale;

// ACES filmic tonemapping
vec3 toneMapACES(vec3 x) {
    float a = 2.51;
    float b = 0.03;
    float c = 2.43;
    float d = 0.59;
    float e = 0.14;
    return clamp((x * (a * x + b)) / (x * (c * x + d) + e), 0.0, 1.0);
}

void main() {
    vec4 sceneTexel = texture(uSceneTexture, vTexCoord);
    vec3 sceneColor = sceneTexel.rgb;
    float sceneAlpha = sceneTexel.a;

    vec3 bloomColor = texture(uBloomTexture, vTexCoord).rgb;

    // Additive blend
    vec3 result = sceneColor + bloomColor * uBloomIntensity;

    // Exposure
    result *= uExposure;

    // Optional tonemapping
    if (uEnableTonemap == 1) {
        result = toneMapACES(result);
    }

    // Alpha calculation for overlay mode
    float bloomLuminance = dot(bloomColor, vec3(0.2126, 0.7152, 0.0722));
    float bloomAlpha = clamp(bloomLuminance * uBloomIntensity * uBloomAlphaScale, 0.0, 1.0);
    float finalAlpha = max(sceneAlpha, bloomAlpha);

    FragColor = vec4(result, finalAlpha);
}
