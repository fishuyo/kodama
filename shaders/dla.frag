#ifdef GL_ES
    precision mediump float;
#endif

varying vec2 v_uv;

uniform sampler2D u_texture0;

uniform float width;
uniform float height;
// uniform float dt;
// uniform float feed;
// uniform float kill;
uniform vec2 brush;

float dt = 0.8; //0.8; //515;

float dx = 1.0/width;
float dy = 1.0/height;
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

float rand(vec2 co){
    return fract(sin(dot(co.xy ,vec2(12.9898,78.233))) * 43758.5453);
}

void main(){

    vec2 alpha = vec2(da/(dx*dx), db/(dy*dy));
    alpha = vec2(0.2097, 0.105);
    // alpha = vec2(0.64, 0.32);
    
    vec4 oldV = texture2D(u_texture0, v_uv);
    vec3 L = laplacian9().rgb;
    vec2 V = oldV.rg + L.rg*alpha*dt; // diffused value

    V.r = 0.2425*L.r*rand(vec2(oldV.r,L.r))+ L.g * oldV.g;
    V.r *= 1.0 - oldV.b; //step(0.0,0.5-oldV.b);
    // float du = /*0.00002*/0.2097*L.r - oldV.r*oldV.g*oldV.g + F*(1.0 - oldV.r);
    // float dv = /*0.00001*/0.105*L.g + oldV.r*oldV.g*oldV.g - (F+K)*oldV.g;
    
    // grey scott
    // float ABB = V.r*V.g*V.g;
    // float rA = -ABB + F*(1.0 - V.r);
    // float rB = ABB - (F+K)*V.g;

    // vec2 R = dt*vec2(rA,rB);

    // output diffusion + reaction
    vec3 dst = vec3(V, oldV.b); // + R;

    // dst.r = rand(v_uv*L);

    if( dst.r > 0.9 && L.b > 0.9){
    	// dst.b = 1.0;
    	dst.b = (clamp(dst.r,0.0,1.0)+dst.g) / 2.0; //1.0;
    }

    // if(oldV.b > 0.9) dst.g = 0.600001;
    if(oldV.b > 0.1) dst.g = oldV.b-0.1;

    // dst = oldV + dt*vec2(du, dv);


	// vec2 dV = vec2( alpha.x * lapl.x - xyy + feed*(1.-uv.x), alpha.y*lapl.y + xyy - (feed+kill)*uv.y);
	// dst = uv + dt*dV;

    if(brush.x > 0.0)
    {
        vec2 diff = (v_uv - brush)/vec2(dx,dy);
        float dist = dot(diff, diff);
        if(dist < 100.0)
            dst.b = 1.0;
    }
    
    gl_FragColor = vec4(dst.r,dst.g, dst.b, 1.0);
}