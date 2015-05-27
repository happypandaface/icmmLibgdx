varying float v_brightness;
void main()
{
    gl_FragColor = vec4(vec3(1.0, 1.0, 1.0)*v_brightness, 1.0);
}
