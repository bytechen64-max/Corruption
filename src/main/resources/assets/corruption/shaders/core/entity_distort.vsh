#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform float GameTime;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;
out vec2 texCoord2;
out vec4 normal;

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) return length(pos);
    else return length(pos.xz);
}

void main() {
    float t = GameTime * 1200.0;
    float scale = 1.18;
    vec3 pos = Position * scale;

    float dispAmp = 0.08;
    pos.x += sin(pos.y * 9.0 + t * 3.5) * dispAmp;
    pos.z += cos(pos.y * 7.0 + t * 2.9) * dispAmp;
    pos.x += sin(pos.z * 4.0 + t * 1.4) * dispAmp * 0.6;
    pos.z += cos(pos.x * 3.5 + t * 1.1) * dispAmp * 0.6;
    pos.y += sin(pos.x * 5.0 + t * 2.0) * dispAmp * 0.4;

    vec4 worldPos = ModelViewMat * vec4(pos, 1.0);
    gl_Position = ProjMat * worldPos;

    vertexDistance = fog_distance(worldPos.xyz, FogShape);
    vertexColor = Color;
    texCoord0 = UV0;
    texCoord2 = UV2;
    normal = ProjMat * ModelViewMat * vec4(Normal, 0.0);
}