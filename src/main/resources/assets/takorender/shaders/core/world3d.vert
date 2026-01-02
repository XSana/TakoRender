#version 330 core

// World3D shader - vertex
// Used by World3DBatch for 3D world rendering
// Coordinates are relative to player eye position

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;

uniform mat4 uModelView;   // MC ModelView matrix
uniform mat4 uProjection;  // MC Projection matrix

out vec4 vColor;

void main()
{
    gl_Position = uProjection * uModelView * vec4(aPos, 1.0);
    vColor = aColor;
}
