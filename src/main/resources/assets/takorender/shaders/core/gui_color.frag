#version 330 core

// GUI color shader - fragment
// Used by SpriteBatch for 2D color rendering

in vec4 vColor;

layout(location = 0) out vec4 FragColor;

void main()
{
    FragColor = vColor;
}
