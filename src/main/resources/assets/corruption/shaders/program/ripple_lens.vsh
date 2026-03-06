#version 150

in vec4 Position;
in vec2 UV0;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;
out vec2 oneTexel;

void main() {
    gl_Position = ProjMat * Position;
    texCoord    = UV0;
    oneTexel    = 1.0 / OutSize;
}
