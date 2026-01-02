#version 330 core

// 9-tap Gaussian blur shader
// Separable blur: horizontal and vertical passes

in vec2 vTexCoord;
out vec4 FragColor;

uniform sampler2D uSourceTexture;
uniform vec2 uTexelSize;
uniform int uHorizontal;
uniform float uBlurScale;

void main() {
    // 9-tap Gaussian weights (sigma ~= 2.0)
    float weight0 = 0.227027;
    float weight1 = 0.1945946;
    float weight2 = 0.1216216;
    float weight3 = 0.054054;
    float weight4 = 0.016216;

    vec2 texOffset = uTexelSize * uBlurScale;
    vec3 result = texture(uSourceTexture, vTexCoord).rgb * weight0;

    if (uHorizontal == 1) {
        // Horizontal blur
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 1.0, 0.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 1.0, 0.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 2.0, 0.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 2.0, 0.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 3.0, 0.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 3.0, 0.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord + vec2(texOffset.x * 4.0, 0.0)).rgb * weight4;
        result += texture(uSourceTexture, vTexCoord - vec2(texOffset.x * 4.0, 0.0)).rgb * weight4;
    } else {
        // Vertical blur
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 1.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 1.0)).rgb * weight1;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 2.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 2.0)).rgb * weight2;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 3.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 3.0)).rgb * weight3;
        result += texture(uSourceTexture, vTexCoord + vec2(0.0, texOffset.y * 4.0)).rgb * weight4;
        result += texture(uSourceTexture, vTexCoord - vec2(0.0, texOffset.y * 4.0)).rgb * weight4;
    }

    FragColor = vec4(result, 1.0);
}
