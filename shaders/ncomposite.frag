#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;
uniform sampler2D u_texture2;
uniform sampler2D u_texture3;
uniform sampler2D u_mask;

uniform float u_blend0;
uniform float u_blend1;
uniform float u_blend2;
uniform float u_blend3;

uniform int mode;
uniform int mask;

varying vec2 v_texCoords;

void main(){

	// pull everything we want from the textures
	vec4 color0 = texture2D(u_texture0, v_texCoords) * u_blend0;
	vec4 color1 = texture2D(u_texture1, v_texCoords) * u_blend1;
	vec4 color2 = texture2D(u_texture2, v_texCoords) * u_blend2;
	vec4 color3 = texture2D(u_texture3, v_texCoords) * u_blend3;

	if( mode == 0){
		gl_FragColor = color0 + color1 + color2 + color3;
	}else {
		gl_FragColor = color0 * color1 * color2 * color3;
	}

	if( mask == 1){
		gl_FragColor *= texture2D(u_mask, v_texCoords);
	}
}