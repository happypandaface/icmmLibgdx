attribute vec3 a_position;
attribute vec2 a_uv;
uniform vec4 u_color;
uniform vec3 u_light;

uniform mat4 u_objectMatrix;
uniform mat4 u_projectionViewMatrix;

varying float v_brightness;
varying vec2 v_uv;
varying vec4 v_color;

void main()
{
  vec4 pos = u_objectMatrix * vec4(a_position, 1.0);
  gl_Position =  u_projectionViewMatrix * vec4(pos.xyz, 1.0);
  v_brightness = 1.0-distance(pos.xyz, u_light)/10.0;
  v_uv = a_uv;
  v_color = u_color;
} 
