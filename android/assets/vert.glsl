attribute vec3 a_position;
uniform vec3 u_light;

uniform mat4 u_projectionViewMatrix;

varying float v_brightness;

void main()
{
    gl_Position =  u_projectionViewMatrix * vec4(a_position, 1.0);
    v_brightness = 1.0-distance(a_position, u_light)/10.0;
} 
