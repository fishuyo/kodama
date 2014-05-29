#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
// uniform sampler2D u_texture2;
// uniform sampler2D u_texture3;
uniform sampler2D u_mask;

uniform float u_blend0;
uniform float u_blend1;
uniform float u_blend2;
uniform float u_blend3;

uniform int mode;
uniform int mask;

varying vec2 v_uv;

void main(){

    vec2 uv = 2. * v_uv - 1.;
    float d = pow(uv.x,2.0) + pow(uv.y,2.0);

    float t = 0.5*(sin(1.0)+1.0);
    float b = clamp(t - d, 0.0,1.0);

	// pull everything we want from the textures
	vec4 color0 = texture2D(u_texture0, v_uv) * 0.65; // u_blend0;
	vec4 color1 = texture2D(u_texture1, v_uv) * 0.5; //u_blend1;
	// vec4 color2 = texture2D(u_texture2, v_uv) * u_blend2;
	// vec4 color3 = texture2D(u_texture3, v_uv) * u_blend3;

    vec4 c = mix( b*color0, b*color1, 1.0-b);

	if( mode == 0){
		gl_FragColor = color0 + color1; // + color2 + color3;
	}else {
		gl_FragColor = color0 * color1; // * color2 * color3;
	}

	if( mask == 1){
		gl_FragColor *= texture2D(u_mask, v_uv);
	}

	// gl_FragColor = c;
}