#version 300 es

precision lowp sampler2D;

layout( location = 0 ) in vec3 attrib_pos;
layout( location = 3 ) in vec2 attrib_uv;

out vec2 var_uv;

layout(std140) uniform glfx_GLOBAL
{
    mat4 glfx_MVP;
    mat4 glfx_PROJ;
    mat4 glfx_MV;
    vec4 glfx_QUAT;

    vec4 js_pos;
};

layout(std140) uniform glfx_BASIS_DATA
{
    // reserved for future use
    vec4 reserved;
    // framebuffer width, height, 1/width, 1/height
    vec4 glfx_SCREEN;
};

uniform sampler2D tex;

float texture_aspect(sampler2D s)
{
	vec2 sz = vec2(textureSize(s,0));
	return sz.x/sz.y;
}

void main()
{
    vec4 var_pos = js_pos;

    float x_scale = var_pos.z;
    
	vec2 translation = vec2(var_pos.x,var_pos.y);

	vec2 quad_size = vec2(x_scale, x_scale*(glfx_SCREEN.x/glfx_SCREEN.y));

	vec2 vpos = translation + (attrib_pos.xy)*quad_size;

    gl_Position = vec4(vpos, 0., 1.);

    var_uv = attrib_uv;
}
