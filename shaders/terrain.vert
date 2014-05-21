
attribute vec4 a_position;
attribute vec4 a_normal;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_projectionViewMatrix;

uniform sampler2D u_texture0;

varying vec2 v_uv;
varying vec3 v_normal;
varying vec3 v_pos;

void main() {

  v_uv = a_texCoord0;

  float y = texture2D(u_texture0, v_uv).g;

  vec4 p = a_position;
  p.y = y;
  // v_pos = a_position.xyz + vec3(0,v_uv.r,0);
  
  gl_Position = u_projectionViewMatrix * p;
  v_normal = a_normal.xyz;
  // v_color = a_color;
  v_pos = p.xyz;
}
