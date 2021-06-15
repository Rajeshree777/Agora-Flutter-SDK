#version 300 es

#define TEETH_WHITENING
#define teethWhiteningCoeff 1.0
#define EYES_WHITENING
#define eyesWhiteningCoeff 0.5
#define EYES_HIGHLIGHT
#define SOFT_LIGHT_LAYER
#define NORMAL_LAYER
#define SOFT_SKIN
#define skinSoftIntensity 0.7
#define SHARPEN_TEETH
#define teethSharpenIntensity 0.2
#define SHARPEN_EYES
#define eyesSharpenIntensity 0.3
#define PSI 0.1

layout( location = 0 ) in vec3 attrib_pos;
layout( location = 1 ) in vec2 attrib_uv;

layout(std140) uniform glfx_GLOBAL
{
    mat4 glfx_MVP;
    mat4 glfx_PROJ;
    mat4 glfx_MV;
};

out vec2 var_uv;
out vec2 var_bg_uv;

out vec4 sp0;
out vec4 sp1;
out vec4 sp2;
out vec4 sp3;

invariant gl_Position;

const float dx = 1.0 / 720.0;
const float dy = 1.0 / 1280.0;

const float delta = 5.;

const float sOfssetXneg = -delta * dx;
const float sOffsetYneg = -delta * dy;
const float sOffsetXpos = delta * dx;
const float sOffsetYpos = delta * dy;

#ifdef GLFX_OCCLUSION
layout(std140) uniform glfx_OCCLUSION_DATA
{
    vec4 glfx_OCCLUSION_RECT;
};
out vec2 glfx_OCCLUSION_UV;
#endif

void main()
{
    gl_Position = glfx_MVP * vec4( attrib_pos, 1. );
    var_uv = attrib_uv;
    var_bg_uv  = (gl_Position.xy / gl_Position.w) * 0.5 + 0.5;
    
    sp0.xy = var_bg_uv + vec2(sOfssetXneg, sOffsetYneg);
    sp1.xy = var_bg_uv + vec2(sOfssetXneg, sOffsetYpos);
    sp2.xy = var_bg_uv + vec2(sOffsetXpos, sOffsetYneg);
    sp3.xy = var_bg_uv + vec2(sOffsetXpos, sOffsetYpos);
    
    vec2 delta = vec2(dx, dy);
    sp0.zw = var_bg_uv + vec2(-delta.x, -delta.y);
    sp1.zw = var_bg_uv + vec2(delta.x, -delta.y);
    sp2.zw = var_bg_uv + vec2(-delta.x, delta.y);
    sp3.zw = var_bg_uv + vec2(delta.x, delta.y);

#ifdef GLFX_OCCLUSION
    glfx_OCCLUSION_UV = (gl_Position.xy / gl_Position.w - glfx_OCCLUSION_RECT.xy) / glfx_OCCLUSION_RECT.zw;
    glfx_OCCLUSION_UV = glfx_OCCLUSION_UV * 0.5 + 0.5;
    glfx_OCCLUSION_UV.y = 1.0 - glfx_OCCLUSION_UV.y;
#endif

}