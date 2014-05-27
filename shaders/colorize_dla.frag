
#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_texture0;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;
uniform vec4 color5;


void main()
{
    vec4 color = texture2D(u_texture0, v_uv);
    float V = color.g;
    //int step = int(floor(V));
    //float a = fract(V);
    float a;
    vec3 col;
    
    if(V <= color1.a)
        col = color1.rgb;
    if(V > color1.a && V <= color2.a)
    {
        a = (V - color1.a)/(color2.a - color1.a);
        col = mix(color1.rgb, color2.rgb, a);
    }
    if(V > color2.a && V <= color3.a)
    {
        a = (V - color2.a)/(color3.a - color2.a);
        col = mix(color2.rgb, color3.rgb, a);
    }
    if(V > color3.a && V <= color4.a)
    {
        a = (V - color3.a)/(color4.a - color3.a);
        col = mix(color3.rgb, color4.rgb, a);
    }
    if(V > color4.a && V <= color5.a)
    {
        a = (V - color4.a)/(color5.a - color4.a);
        col = mix(color4.rgb, color5.rgb, a);
    }
    if(V > color5.a)
        col = color5.rgb;
    
	gl_FragColor = vec4(col.r+color.r, col.g, col.b+color.b, 1.0);
	// gl_FragColor = vec4(color.g,color.r,color.b,1.0);
}