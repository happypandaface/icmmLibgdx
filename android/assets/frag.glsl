uniform sampler2D u_texture0;
varying float v_brightness;
varying vec2 v_uv;
void main()
{
  vec4 color = texture2D(u_texture0, v_uv);
  gl_FragColor = vec4(color.rgb*v_brightness, color.a);
}
