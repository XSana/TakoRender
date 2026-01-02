#version 330 core

// World3D shader - fragment
// Used by World3DBatch for 3D world rendering

in vec4 vColor;

uniform float uAlpha;  // Global alpha multiplier

layout(location = 0) out vec4 FragColor;

void main()
{
    vec4 finalColor = vColor;
    finalColor.a *= uAlpha;
    FragColor = finalColor;
}
