uniform sampler2D u_texture0;
uniform vec4 u_circ;
uniform float u_seed;
uniform float u_static;
varying float v_brightness;
varying vec2 v_uv;
varying vec4 v_color;
float rand(vec2 co){
  return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}
void main()
{
  float dist = length(gl_FragCoord.xy-u_circ.xy);
  if (dist<u_circ.z){
    vec4 color = texture2D(u_texture0, v_uv)*v_color;
    if(dist>u_circ.w){
      color=color*(1.0-(dist-u_circ.w)/(u_circ.z-u_circ.w));
    }
    color=vec4(color.rgb*v_brightness, color.a);
    color=(1.0-u_static)*color+(u_static)*vec4(rand(gl_FragCoord.xy+vec2(u_seed,1.0-u_seed)),rand(gl_FragCoord.xy+vec2(1.0-u_seed,u_seed)),rand(gl_FragCoord.xy+vec2(u_seed,u_seed)),color.a);
    gl_FragColor = color;
  }else{
    gl_FragColor = vec4(1,1,1,0);
  }
}
