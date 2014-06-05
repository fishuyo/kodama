#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_texture0;
uniform sampler2D u_texture1;

uniform float width;
uniform float height;
// uniform float dt;
uniform float F;
uniform float K;
uniform vec2 brush;

float dt = 1.2; //0.8; //515;

float dx = 1.0/1600.0; //width;
float dy = 1.0/600.0; //height;
// float dy = 1.0/height;

float da = 0.0002;
float db = 0.00001;


// nine point stencil
vec4 laplacian9() {
    return  
    0.5* texture2D( u_texture0,  v_uv + vec2(-dx,-dy)) // first row
    + texture2D( u_texture0,  v_uv + vec2(0,-dy) )
    +  0.5* texture2D( u_texture0,  v_uv + vec2(dx,-dy))
    +  texture2D( u_texture0,  v_uv + vec2(-dx,0)) // seond row
    - 6.0* texture2D( u_texture0,  v_uv )
    +   texture2D( u_texture0,  v_uv + vec2(dx,0))
    +  0.5*texture2D( u_texture0,  v_uv + vec2(-dx,dy))  // third row
    + texture2D( u_texture0,  v_uv + vec2(0,dy) )
    +   0.5*texture2D( u_texture0,  v_uv + vec2(dx,dy));    
}

// five point stencil
vec4  laplacian5() {
    return 
    +  texture2D( u_texture0, v_uv + vec2(0,-dy))
    +  texture2D( u_texture0, v_uv + vec2(-dx,0)) 
    -  4.0 * texture2D( u_texture0,  v_uv )
    +  texture2D( u_texture0, v_uv + vec2(dx,0)) 
    +  texture2D( u_texture0, v_uv + vec2(0,dy));
}

void main(){

    // float F = 0.034; //mitosis
    // float K = 0.063;

    // float F = 0.025; //pulse
    // float K = 0.06;

    // float F = 0.014; //waves
    // float K = 0.045;

    // float F = 0.026; //brains
    // float K = 0.055;

    // float F = 0.082; //worms
    // float K = 0.061;

    // float F = 0.082; //worm channels
    // float K = 0.059;

    // float F = 0.078; // + 0.085*(v_uv.y*0.015); 
    // float K = 0.061;

    //Uskate
    // float F = 0.062;
    // float K = 0.06093;
    // float F = 0.062;
    // float K = 0.0609;
    // float F = 0.062;
    // float K = 0.06093;

    // float F = v_uv.y * 0.083;
    // float K = v_uv.x * 0.073;

    vec2 alpha = vec2(da/(dx*dx), db/(dy*dy));
    alpha = vec2(0.2097, 0.105);
    // alpha = vec2(0.64, 0.32);
    
    vec2 oldV = texture2D(u_texture0, v_uv).rg;
    vec2 L = laplacian9().rg;
    vec2 V = oldV + L*alpha*dt; // diffused value

    // float du = /*0.00002*/0.2097*L.r - oldV.r*oldV.g*oldV.g + F*(1.0 - oldV.r);
    // float dv = /*0.00001*/0.105*L.g + oldV.r*oldV.g*oldV.g - (F+K)*oldV.g;
    
    // grey scott
    float ABB = V.r*V.g*V.g;
    float rA = -ABB + F*(1.0 - V.r);
    float rB = ABB - (F+K)*V.g;

    vec2 R = dt*vec2(rA,rB);

    // output diffusion + reaction
    vec2 dst = V + R;


    // dst = oldV + dt*vec2(du, dv);


    // vec2 dV = vec2( alpha.x * lapl.x - xyy + feed*(1.-uv.x), alpha.y*lapl.y + xyy - (feed+kill)*uv.y);
    // dst = uv + dt*dV;

    vec4 inV = texture2D(u_texture1, v_uv);
    if(brush.x > 0.0)
    {
        vec2 brsh = brush;
        // brsh.y = 1.0 - brsh.y;
        vec2 diff = (v_uv - brsh)/vec2(dx,dy);
        float dist = dot(diff, diff);
        if(dist < 20.0)
            dst.g = 0.9;
    }
    
    gl_FragColor = vec4(dst.r,dst.g, 0.0, 1.0);
}