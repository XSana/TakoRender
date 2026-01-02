#version 330 core

// Brightness extraction shader for Bloom effect
// Uses soft knee threshold for smooth transition

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uSceneTexture;
uniform float uThreshold;
uniform float uSoftKnee;

void main() {
    vec4 color = texture(uSceneTexture, vTexCoord);

    // Calculate perceptual brightness (ITU-R BT.709)
    float brightness = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));

    // Soft threshold with knee
    float knee = uThreshold * uSoftKnee;
    float soft = brightness - uThreshold + knee;
    soft = clamp(soft, 0.0, 2.0 * knee);
    soft = soft * soft / (4.0 * knee + 0.00001);

    float contribution = max(soft, brightness - uThreshold);
    contribution /= max(brightness, 0.00001);

    FragColor = vec4(color.rgb * contribution, color.a);
}
