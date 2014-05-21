

#ifdef GL_ES
    precision mediump float;
#endif

// varying vec4 v_color;
// varying vec3 v_pos;
varying vec2 v_uv;
varying vec3 v_normal;


void main(){
    // gl_FragColor = vec4(0.,0,1,0.1);
    gl_FragColor = vec4((v_normal+vec3(1))*0.5,1);
}
