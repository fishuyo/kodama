
attribute vec4 a_position;
attribute vec2 a_texCoord0;
attribute vec4 a_color;

uniform mat4 u_projectionViewMatrix;

varying vec2 v_uv;

void main() {
  gl_Position = u_projectionViewMatrix * a_position;
  v_uv = a_texCoord0;
  // v_color = a_color;
  // v_pos = a_position.xyz;
}
