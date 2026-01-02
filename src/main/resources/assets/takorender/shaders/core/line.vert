#version 330 core

// Line shader - vertex
// Used by LineRenderSystem for line/wireframe rendering

layout (location = 0) in vec3 aPos;
layout (location = 1) in vec4 aColor;

uniform mat4 uModelView;   // ModelView matrix
uniform mat4 uProjection;  // Projection matrix

out vec4 vColor;

void main()
{
    gl_Position = uProjection * uModelView * vec4(aPos, 1.0);
    vColor = aColor;
}
