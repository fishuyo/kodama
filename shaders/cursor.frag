#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform float time;
uniform float phase;
uniform vec3 color;

void main(){
    vec2 uv = 2. * v_uv - 1.;
    float d = pow(uv.x,2.0) + pow(uv.y,2.0);

    float t = 0.5*(sin(time+phase)+1.0);
    float b = clamp(t - d + 0.5, 0.0,1.0);
    vec3 c = mix( b*color, b*vec3(0.8,0.8,0.8)+b*color, 1.0-b);
    gl_FragColor = vec4(c,b);
}