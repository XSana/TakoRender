#version 330 core

// Model shader - vertex
// For rendering 3D models with normal-based lighting
// Vertex format: position(vec3) + normal(vec3) + texCoord(vec2)

layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

uniform mat4 uModelView;
uniform mat4 uProjection;
uniform mat3 uNormalMatrix;  // inverse transpose of upper-left 3x3 of modelview

out vec3 vNormal;
out vec2 vTexCoord;
out vec3 vViewPos;

void main()
{
    vec4 viewPos = uModelView * vec4(aPosition, 1.0);
    gl_Position = uProjection * viewPos;

    // Transform normal to view space
    vNormal = normalize(uNormalMatrix * aNormal);
    vTexCoord = aTexCoord;
    vViewPos = viewPos.xyz;
}
