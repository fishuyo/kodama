#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_texture0;

// uniform float width;
// uniform float height;
// uniform float dt;
// uniform float feed;
// uniform float kill;
uniform vec2 brush;

float width = 800.0;
float height = 800.0;
float dt = 0.015;

float dx = 1.0/width;
float dy = 1.0/height;

void main()
{
    if(brush.x < -5.0)
    {
        gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        return;
    }
    
    float feed = v_uv.y * 0.083;
    float kill = v_uv.x * 0.073;
    
    vec2 uv = texture2D(u_texture0, v_uv).rg;
    vec2 uv0 = texture2D(u_texture0, v_uv+vec2(-dx, 0.0)).rg;
    vec2 uv1 = texture2D(u_texture0, v_uv+vec2(dx, 0.0)).rg;
    vec2 uv2 = texture2D(u_texture0, v_uv+vec2(0.0, -dy)).rg;
    vec2 uv3 = texture2D(u_texture0, v_uv+vec2(0.0, dy)).rg;
    
    vec2 lapl = (uv0 + uv1 + uv2 + uv3 - 4.0*uv);//10485.76;
    float du = /*0.00002*/0.2097*lapl.r - uv.r*uv.g*uv.g + feed*(1.0 - uv.r);
    float dv = /*0.00001*/0.105*lapl.g + uv.r*uv.g*uv.g - (feed+kill)*uv.g;
    vec2 dst = uv + dt*vec2(du, dv);
    
    if(brush.x > 0.0)
    {
        vec2 diff = (v_uv - brush)/vec2(dx,dy);
        float dist = dot(diff, diff);
        if(dist < 5.0)
            dst.g = 0.9;
    }
    
    gl_FragColor = vec4(dst.r, dst.g, 0.0, 1.0);
}