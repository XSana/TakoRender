#version 330 core

// Instanced Model shader - vertex
// For rendering multiple instances of the same mesh with per-instance transform
// Vertex format: position(vec3) + normal(vec3) + texCoord(vec2)
// Instance data: model matrix (mat4, occupies locations 3-6)

// Per-vertex attributes
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

// Per-instance attributes (model matrix as 4 vec4 columns)
layout (location = 3) in vec4 aModelCol0;
layout (location = 4) in vec4 aModelCol1;
layout (location = 5) in vec4 aModelCol2;
layout (location = 6) in vec4 aModelCol3;

// Camera matrices
uniform mat4 uView;
uniform mat4 uProjection;

out vec3 vNormal;
out vec2 vTexCoord;
out vec3 vViewPos;

void main()
{
    // Reconstruct model matrix from instance attributes
    mat4 modelMatrix = mat4(aModelCol0, aModelCol1, aModelCol2, aModelCol3);

    // Calculate model-view matrix
    mat4 modelView = uView * modelMatrix;

    // Transform position
    vec4 viewPos = modelView * vec4(aPosition, 1.0);
    gl_Position = uProjection * viewPos;

    // Calculate normal matrix (inverse transpose of upper-left 3x3)
    // For uniform scale, we can use mat3(modelView) directly
    mat3 normalMatrix = mat3(modelView);
    vNormal = normalize(normalMatrix * aNormal);

    vTexCoord = aTexCoord;
    vViewPos = viewPos.xyz;
}
