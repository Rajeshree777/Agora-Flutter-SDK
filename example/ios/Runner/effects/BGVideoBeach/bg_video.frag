#version 300 es

precision highp float;

in vec2 var_uv;
in vec2 var_bgmask_uv;
in vec4 bg_color;

layout( location = 0 ) out vec4 F;

uniform sampler2D glfx_VIDEO;
uniform sampler2D glfx_BACKGROUND;
uniform sampler2D glfx_BG_MASK;

layout(std140) uniform glfx_BASIS_DATA
{
	vec4 unused;
	vec4 glfx_SCREEN;
	vec4 glfx_BG_MASK_T[2];
	vec4 glfx_HAIR_MASK_T[2];
	vec4 glfx_LIPS_MASK_T[2];
	vec4 glfx_L_EYE_MASK_T[2];
	vec4 glfx_R_EYE_MASK_T[2];
	vec4 glfx_SKIN_MASK_T[2];
	vec4 glfx_OCCLUSION_MASK_T[2];
};

vec4 cubic(float v) {
    vec4 n = vec4(1.0, 2.0, 3.0, 4.0) - v;
    vec4 s = n * n * n;
    float x = s.x;
    float y = s.y - 4.0 * s.x;
    float z = s.z - 4.0 * s.y + 6.0 * s.x;
    float w = 6.0 - x - y - z;
    return vec4(x, y, z, w) * (1.0 / 6.0);
}

vec4 textureBicubic(sampler2D sampler, vec2 texCoords){
    vec2 texSize = vec2(textureSize(sampler, 0));
    vec2 invTexSize = 1.0 / texSize;

    texCoords = texCoords * texSize - 0.5;

    vec2 fxy = fract(texCoords);
    texCoords -= fxy;

    vec4 xcubic = cubic(fxy.x);
    vec4 ycubic = cubic(fxy.y);

    vec4 c = texCoords.xxyy + vec2(-0.5, +1.5).xyxy;

    vec4 s = vec4(xcubic.xz + xcubic.yw, ycubic.xz + ycubic.yw);
    vec4 offset = c + vec4(xcubic.yw, ycubic.yw) / s;

    offset *= invTexSize.xxyy;

    vec4 sample0 = texture(sampler, offset.xz);
    vec4 sample1 = texture(sampler, offset.yz);
    vec4 sample2 = texture(sampler, offset.xw);
    vec4 sample3 = texture(sampler, offset.yw);

    float sx = s.x / (s.x + s.y);
    float sy = s.z / (s.z + s.w);

    return mix(
        mix(sample3, sample2, sx), mix(sample1, sample0, sx)
    , sy);
}

vec2 rgb_hs(vec3 rgb)
{
    float cmax = max(rgb.r, max(rgb.g, rgb.b));
    float cmin = min(rgb.r, min(rgb.g, rgb.b));
    float delta = cmax - cmin;
    vec2 hs = vec2(0.0);

    if (cmax > cmin) {
        hs.y = delta/cmax;
        if (rgb.r == cmax)
            hs.x = (rgb.g - rgb.b) / delta;
        else 
        {
            if (rgb.g == cmax)
                hs.x = 2.0 + (rgb.b - rgb.r) / delta;
            else
                hs.x = 4.0 + (rgb.r - rgb.g) / delta;
        }
        hs.x = fract(hs.x / 6.0);
    }
    
    return hs;
}

float rgb_v(vec3 rgb)
{
    return max(rgb.r, max(rgb.g, rgb.b));
}

vec3 hsv_rgb(float h, float s, float v)
{
    return v * mix(vec3(1.0), clamp(abs(fract(vec3(1.0, 2.0 / 3.0, 1.0 / 3.0) + h) * 6.0 - 3.0) - 1.0, 0.0, 1.0), s);
}

vec3 blendColor(vec3 base, vec3 blend) {
    float v = rgb_v(base);
    vec2 hs = rgb_hs(blend);
    return hsv_rgb(hs.x, hs.y, v);
}

vec3 blendColor(vec3 base, vec3 blend, float opacity) {
    return (blendColor(base, blend) * opacity + base * (1.0 - opacity));
}

float texture_aspect(sampler2D s)
{
	vec2 sz = vec2(textureSize(s,0));
	return sz.x/sz.y;
}

void main()
{
    float screen_aspect = glfx_SCREEN.x/glfx_SCREEN.y;
    float tex_aspect = texture_aspect(glfx_VIDEO);

    vec2 uv = var_uv;
    vec3 bg = texture(glfx_BACKGROUND, uv).xyz;

    uv.y = 1. - uv.y;
    uv.x = uv.x * screen_aspect;
    uv.x -= (screen_aspect - tex_aspect)/2.;
    uv.x /= tex_aspect;

    vec4 bg_video = texture(glfx_VIDEO, uv);
    const float threshold = 0.2;
    float mask = max((textureBicubic(glfx_BG_MASK,var_bgmask_uv).x - threshold) / (1.0 - threshold), 0.0);
    // F = vec4(bg_tex.rgb, mask);
    F = vec4(mix(bg, bg_video.xyz, mask), 1.0);
}
