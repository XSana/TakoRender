#version 330 core

// particle.frag - Particle fragment shader
// Supports textures, color gradients, soft particles, texture animation, and MC lighting

in vec2 vUV;
in vec4 vColor;
in float vLifePercent;
in float vParticleType;

// Uniforms - Texture
uniform sampler2D uTexture;
uniform int uHasTexture;

// Uniforms - Texture Animation (Sprite Sheet)
uniform int uTextureTilesX;      // Horizontal tile count (default: 1)
uniform int uTextureTilesY;      // Vertical tile count (default: 1)
uniform int uAnimationMode;      // 0: by lifetime, 1: by speed (requires velocity input)
uniform float uAnimationSpeed;   // Animation speed multiplier (default: 1.0)

// Uniforms - Soft Particles
uniform int uSoftParticles;
uniform float uSoftDistance;
uniform sampler2D uDepthTexture;
uniform vec2 uScreenSize;
uniform float uNearPlane;
uniform float uFarPlane;

// Uniforms - Color Gradient
uniform sampler1D uColorLUT;
uniform int uUseColorLUT;

// Uniforms - MC Lighting
uniform float uBlockLight;       // Block light level 0-1 (from MC world)
uniform float uSkyLight;         // Sky light level 0-1 (from MC world)
uniform float uEmissive;         // Emissive strength 0-1 (1.0 = fully self-lit)
uniform float uMinBrightness;    // Minimum brightness (default: 0.1)
uniform int uReceiveLighting;    // 0: ignore lighting, 1: apply lighting

// Output
out vec4 fragColor;

// Linearize depth for soft particles
float linearizeDepth(float depth) {
    float z = depth * 2.0 - 1.0;
    return (2.0 * uNearPlane * uFarPlane) / (uFarPlane + uNearPlane - z * (uFarPlane - uNearPlane));
}

// Calculate sprite sheet UV for texture animation
vec2 calculateAnimatedUV(vec2 uv, float animProgress) {
    if (uTextureTilesX <= 1 && uTextureTilesY <= 1) {
        return uv;
    }

    int totalFrames = uTextureTilesX * uTextureTilesY;

    // Determine current frame based on animation progress
    int frame = int(animProgress * float(totalFrames));
    frame = clamp(frame, 0, totalFrames - 1);

    // Calculate tile UV offset
    float tileW = 1.0 / float(uTextureTilesX);
    float tileH = 1.0 / float(uTextureTilesY);
    int tileX = frame % uTextureTilesX;
    int tileY = frame / uTextureTilesX;

    // Transform UV to tile space
    // Y is flipped because texture origin is bottom-left
    vec2 animatedUV = uv * vec2(tileW, tileH);
    animatedUV.x += float(tileX) * tileW;
    animatedUV.y += float(uTextureTilesY - 1 - tileY) * tileH;

    return animatedUV;
}

// Calculate lighting factor from MC light levels
float calculateLighting() {
    if (uReceiveLighting == 0 || uEmissive >= 1.0) {
        return 1.0;
    }

    // Combine block and sky light (take maximum)
    float light = max(uBlockLight, uSkyLight);

    // Apply minimum brightness
    light = max(light, uMinBrightness);

    // Blend with emissive
    light = mix(light, 1.0, uEmissive);

    return light;
}

void main() {
    vec4 texColor = vec4(1.0);

    // Calculate animation progress
    float animProgress = vLifePercent * uAnimationSpeed;
    animProgress = fract(animProgress);  // Loop animation

    if (uHasTexture == 1) {
        // Calculate animated UV
        vec2 animatedUV = calculateAnimatedUV(vUV, animProgress);
        texColor = texture(uTexture, animatedUV);
    } else {
        // Draw soft circle when no texture
        vec2 center = vUV - vec2(0.5);
        float dist = length(center);

        if (dist > 0.5) {
            discard;
        }

        // Soft edge
        texColor.a = smoothstep(0.5, 0.2, dist);

        // Radial gradient for center brightness
        texColor.rgb = vec3(1.0 - dist * 0.3);
    }

    // Apply color
    vec4 finalColor;

    if (uUseColorLUT == 1) {
        // Use color LUT for gradient
        vec4 lutColor = texture(uColorLUT, vLifePercent);
        finalColor = texColor * lutColor;
    } else {
        // Use vertex color
        finalColor = texColor * vColor;
    }

    // Apply MC lighting
    float lightFactor = calculateLighting();
    finalColor.rgb *= lightFactor;

    // Soft particles (requires depth buffer)
    if (uSoftParticles == 1 && uSoftDistance > 0.0) {
        vec2 screenCoord = gl_FragCoord.xy / uScreenSize;
        float sceneDepth = linearizeDepth(texture(uDepthTexture, screenCoord).r);
        float particleDepth = linearizeDepth(gl_FragCoord.z);

        float depthDiff = sceneDepth - particleDepth;
        float softFactor = smoothstep(0.0, uSoftDistance, depthDiff);

        finalColor.a *= softFactor;
    }

    // Discard nearly transparent pixels
    if (finalColor.a < 0.01) {
        discard;
    }

    fragColor = finalColor;
}
