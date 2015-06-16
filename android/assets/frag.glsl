uniform sampler2D u_texture0;
uniform vec3 u_circ;
varying float v_brightness;
varying vec2 v_uv;
varying vec4 v_color;
void main()
{
  float dist = length(gl_FragCoord.xy-u_circ.xy);
  if (dist<u_circ.z){
    vec4 color = texture2D(u_texture0, v_uv)*v_color;
    gl_FragColor = vec4(color.rgb*v_brightness, color.a);
  }else{
    gl_FragColor = vec4(1,1,1,0);
  }
}
