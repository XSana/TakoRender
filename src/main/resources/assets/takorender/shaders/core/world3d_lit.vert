#version 330 core

// World3D Lit shader - vertex
// Extends world3d with MC lightmap coordinate and normal support
// MC 1.7.10 right-hand coordinate system: +X=East, +Y=Up, +Z=South(toward player)

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;
layout (location = 2) in vec2 aLightCoord;  // MC lightmap coordinates (blockLight, skyLight)
layout (location = 3) in vec3 aNormal;      // Surface normal (MC right-hand coords)

uniform mat4 uModelView;   // MC ModelView matrix
uniform mat4 uProjection;  // MC Projection matrix

out vec4 vColor;
out vec2 vLightCoord;
out vec3 vNormal;
out vec3 vWorldPos;

void main()
{
    vec4 worldPos = uModelView * vec4(aPos, 1.0);
    gl_Position = uProjection * worldPos;

    vColor = aColor;
    vLightCoord = aLightCoord;
    // Transform normal to view space (using upper-left 3x3 of modelview)
    vNormal = normalize(mat3(uModelView) * aNormal);
    vWorldPos = worldPos.xyz;
}
