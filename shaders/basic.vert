
attribute vec4 a_position;
attribute vec4 a_normal;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_projectionViewMatrix;

varying vec2 v_uv;
varying vec3 v_normal;

void main() {
  gl_Position = u_projectionViewMatrix * a_position;
  v_normal = a_normal.xyz;
  v_uv = a_texCoord0;
  // v_color = a_color;
  // v_pos = a_position.xyz;
}
