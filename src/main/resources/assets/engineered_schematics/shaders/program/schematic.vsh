#version 120

varying vec4 fColor;
varying vec2 texCoord0;

void main() {
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    fColor = gl_Color;
    texCoord0 = gl_MultiTexCoord0.st;
}
