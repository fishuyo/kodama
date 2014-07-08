#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform float time;
uniform vec3 color;

void main(){
    float t = 0.5*(sin(time)+1.0)*0.5;
    float b = clamp(t + 0.5, 0.0,1.0);
    gl_FragColor = vec4(color,b);
    gl_FragColor = vec4(1,1,1,0.2);
}