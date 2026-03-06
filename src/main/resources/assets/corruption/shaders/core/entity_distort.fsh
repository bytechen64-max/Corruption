#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler2;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float GameTime;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord2;
in vec4 normal;

out vec4 fragColor;

vec4 linear_fog(vec4 color, float distance, float start, float end, vec4 fogColor) {
    if (distance <= start) return color;
    if (distance >= end) return fogColor;
    float factor = (distance - start) / (end - start);
    return mix(color, fogColor, factor);
}

float noise2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}
float myNoise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;

    float t = GameTime * 1200.0;

    float pulse = 0.35 + 0.15 * sin(t * 4.2) + 0.10 * sin(t * 7.1 + 1.3);

    vec4 lightColor = texture(Sampler2, texCoord2);
    vec4 shadedColor = vertexColor * lightColor * vec4(1.8, 0.2, 0.2, 1.0);

    float n = myNoise(texCoord0 * 20.0 + vec2(t * 0.3, t * 0.2));
    float noiseBoost = (n - 0.5) * 0.15;

    vec4 auraColor = vec4(
    0.9 + noiseBoost,
    0.02 + abs(noiseBoost) * 0.1,
    0.02 + abs(noiseBoost) * 0.1,
    clamp(pulse + abs(noiseBoost), 0.0, 0.75)
    );

    auraColor *= shadedColor * ColorModulator;
    auraColor.a = clamp(pulse + abs(noiseBoost), 0.0, 0.75);

    fragColor = linear_fog(auraColor, vertexDistance, FogStart, FogEnd, FogColor);
}