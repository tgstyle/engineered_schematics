#version 120

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Time;
uniform vec3 ColorTint;

varying vec4 fColor;
varying vec2 texCoord0;

void main() {
    float baseWave = sin(Time * 0.1);
    float detailWave = sin(Time * 0.05) * 0.3;
    float smoothFade = (baseWave + detailWave) * 0.5 + 0.5;
    float fadeEffect = 0.5 + (smoothFade * 0.5);

    vec4 sampledTexture = texture2D(Sampler0, texCoord0) * fColor * ColorModulator;
    vec4 backgroundColor = vec4(ColorTint, 0.2);
    vec4 tintedTexture = mix(sampledTexture, vec4(ColorTint, sampledTexture.a), 0.3);

    vec4 modifiedTexture = (sampledTexture.a < 0.01) ? backgroundColor : tintedTexture;
    modifiedTexture.a *= fadeEffect;

    gl_FragColor = modifiedTexture;
}
