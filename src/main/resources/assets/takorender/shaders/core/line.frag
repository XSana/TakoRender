#version 330 core

// Line shader - fragment
// Used by LineRenderSystem for line/wireframe rendering

in vec4 vColor;

uniform float uAlpha;     // Global alpha multiplier (default 1.0)
uniform float uEmissive;  // Emissive intensity for Bloom (default 1.0)

layout(location = 0) out vec4 FragColor;

void main()
{
    vec4 finalColor = vColor;
    finalColor.a *= uAlpha;

    // Emissive effect - increase brightness to trigger Bloom
    finalColor.rgb *= uEmissive;

    FragColor = finalColor;
}
