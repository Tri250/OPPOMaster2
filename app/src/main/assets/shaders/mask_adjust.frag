#version 100
// Mask shader for AdjustmentMask
// 支持多蒙版叠加渲染（每个蒙版一个通道）
// 蒙版通过 uniform 数组传入

precision highp float;

varying vec2 vTextureCoord;

uniform sampler2D uSourceTexture;        // 原图
uniform sampler2D uMaskTexture1;         // 蒙版 1 (灰度)
uniform sampler2D uMaskTexture2;         // 蒙版 2 (灰度)
uniform sampler2D uMaskTexture3;         // 蒙版 3 (灰度)
uniform sampler2D uMaskTexture4;         // 蒙版 4 (灰度)

uniform int uMaskCount;                  // 启用的蒙版数量 [0, 4]

// 蒙版参数 (4 个蒙版，每个蒙版 18 个参数)
uniform vec4 uMaskOpacityBlend;          // 蒙版 1-4 的不透明度与混合模式
// x = mask1 opacity, y = mask2 opacity, z = mask3 opacity, w = mask4 opacity
// 整数部分 (取整后) = 混合模式 (0=OVERLAY, 1=REPLACE, 2=MULTIPLY, 3=SCREEN)

uniform vec3 uMaskParams1;               // 蒙版 1 的局部参数 (主色彩调整)
uniform vec3 uMaskParams2;
uniform vec3 uMaskParams3;
uniform vec3 uMaskParams4;

// 基础参数（应用到全图）
uniform float uSaturation;
uniform float uContrast;
uniform float uBrightness;
uniform float uWarmth;
uniform float uSharpness;
uniform float uVibrance;
uniform float uHighlights;
uniform float uShadows;
uniform float uExposure;

// === 色彩调整函数（与 image_adjust.frag 一致） ===

vec3 adjustSaturation(vec3 color, float sat) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return mix(vec3(gray), color, 1.0 + sat);
}

vec3 adjustContrast(vec3 color, float c) {
    return (color - 0.5) * (1.0 + c) + 0.5;
}

vec3 adjustBrightness(vec3 color, float b) {
    return color + b;
}

vec3 adjustWarmth(vec3 color, float w) {
    color.r += w * 0.3;
    color.b -= w * 0.3;
    return color;
}

vec3 adjustExposure(vec3 color, float e) {
    return color * pow(2.0, e);
}

vec3 adjustHighlights(vec3 color, float h) {
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    float mask = smoothstep(0.5, 1.0, lum);
    return color - vec3(h * 0.3) * mask;
}

vec3 adjustShadows(vec3 color, float s) {
    float lum = dot(color, vec3(0.299, 0.587, 0.114));
    float mask = 1.0 - smoothstep(0.0, 0.5, lum);
    return color + vec3(s * 0.3) * mask;
}

// 应用基础参数
vec3 applyBaseParams(vec3 color) {
    color = adjustExposure(color, uExposure);
    color = adjustBrightness(color, uBrightness);
    color = adjustContrast(color, uContrast);
    color = adjustSaturation(color, uSaturation);
    color = adjustWarmth(color, uWarmth);
    color = adjustHighlights(color, uHighlights);
    color = adjustShadows(color, uShadows);
    color = adjustSaturation(color, uVibrance * 0.5);
    return color;
}

// 应用蒙版参数（简化版：每个蒙版只用一个参数组）
vec3 applyMaskParams(vec3 color, vec3 params) {
    // params: x = 曝光, y = 饱和度, z = 对比度
    color = adjustExposure(color, params.x);
    color = adjustSaturation(color, params.y);
    color = adjustContrast(color, params.z);
    return color;
}

// 蒙版混合模式
vec3 blendWithMask(vec3 base, vec3 masked, float maskStrength, int blendMode) {
    if (blendMode == 0) {
        // OVERLAY: 插值
        return mix(base, masked, maskStrength);
    } else if (blendMode == 1) {
        // REPLACE: 替换
        return mix(base, masked, maskStrength);
    } else if (blendMode == 2) {
        // MULTIPLY: 相乘
        return mix(base, base * masked * 2.0, maskStrength);
    } else if (blendMode == 3) {
        // SCREEN: 屏幕
        return mix(base, 1.0 - (1.0 - base) * (1.0 - masked), maskStrength);
    }
    return base;
}

void main() {
    vec4 srcColor = texture2D(uSourceTexture, vTextureCoord);
    vec3 color = srcColor.rgb;

    // 1. 应用基础参数
    vec3 baseColor = applyBaseParams(color);

    // 2. 依次应用蒙版
    vec3 result = baseColor;
    if (uMaskCount >= 1) {
        float m = texture2D(uMaskTexture1, vTextureCoord).r;
        int blendMode = int(mod(uMaskOpacityBlend.x * 10.0, 10.0));
        float opacity = fract(uMaskOpacityBlend.x);
        float strength = m * opacity;
        vec3 masked = applyMaskParams(baseColor, uMaskParams1);
        result = blendWithMask(result, masked, strength, blendMode);
    }
    if (uMaskCount >= 2) {
        float m = texture2D(uMaskTexture2, vTextureCoord).r;
        int blendMode = int(mod(uMaskOpacityBlend.y * 10.0, 10.0));
        float opacity = fract(uMaskOpacityBlend.y);
        float strength = m * opacity;
        vec3 masked = applyMaskParams(result, uMaskParams2);
        result = blendWithMask(result, masked, strength, blendMode);
    }
    if (uMaskCount >= 3) {
        float m = texture2D(uMaskTexture3, vTextureCoord).r;
        int blendMode = int(mod(uMaskOpacityBlend.z * 10.0, 10.0));
        float opacity = fract(uMaskOpacityBlend.z);
        float strength = m * opacity;
        vec3 masked = applyMaskParams(result, uMaskParams3);
        result = blendWithMask(result, masked, strength, blendMode);
    }
    if (uMaskCount >= 4) {
        float m = texture2D(uMaskTexture4, vTextureCoord).r;
        int blendMode = int(mod(uMaskOpacityBlend.w * 10.0, 10.0));
        float opacity = fract(uMaskOpacityBlend.w);
        float strength = m * opacity;
        vec3 masked = applyMaskParams(result, uMaskParams4);
        result = blendWithMask(result, masked, strength, blendMode);
    }

    gl_FragColor = vec4(clamp(result, 0.0, 1.0), srcColor.a);
}
