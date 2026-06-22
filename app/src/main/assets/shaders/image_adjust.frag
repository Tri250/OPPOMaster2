#version 300 es
// OpenGL ES 3.0 片段着色器
// 18参数全通道图像处理渲染器
// 实现饱和度、对比度、亮度、色温、锐度、清晰度、鲜艳度、高光、阴影、
// 白色、黑色、颗粒、褪色、去霾、降噪、肤色平滑、曝光、纹理等18个参数

// 精度设置
precision highp float;

// 输入纹理坐标
in vec2 vTexCoord;

// 输出颜色
out vec4 fragColor;

// 输入纹理
uniform sampler2D uTexture;

// 图像尺寸（用于卷积操作）
uniform vec2 uImageSize;

// 时间（用于颗粒效果随机）
uniform float uTime;

// ========== 18个渲染参数 ==========
// 基础调整参数 [-1, 1] 或 [0, 1]
uniform float uSaturation;      // 饱和度 [-1, 1]
uniform float uContrast;        // 对比度 [-1, 1]
uniform float uBrightness;      // 亮度 [-1, 1]
uniform float uWarmth;          // 色温 [-1, 1]，负值偏冷，正值偏暖

// 细节增强参数 [0, 1]
uniform float uSharpness;       // 锐度 [0, 1]
uniform float uClarity;         // 清晰度 [0, 1]
uniform float uTextureStrength; // 纹理 [-1, 1]（原 uTexture 名称与 sampler2D uTexture 冲突，已重命名）

// 色彩调整参数 [-1, 1]
uniform float uVibrance;        // 鲜艳度 [-1, 1]

// 光影调整参数 [-1, 1]
uniform float uHighlights;      // 高光 [-1, 1]
uniform float uShadows;         // 阴影 [-1, 1]
uniform float uWhites;          // 白色色阶 [-1, 1]
uniform float uBlacks;          // 黑色色阶 [-1, 1]
uniform float uExposure;        // 曝光 [-1, 1]

// 效果参数 [0, 1]
uniform float uGrain;           // 颗粒 [0, 1]
uniform float uFade;            // 褪色 [0, 1]
uniform float uDehaze;          // 去霾 [0, 1]

// 降噪与平滑参数 [0, 1]
uniform float uDenoise;         // 降噪 [0, 1]
uniform float uSkinSmooth;      // 肤色平滑 [0, 1]

// ========== 辅助函数 ==========

/**
 * RGB转HSL
 * 用于饱和度和鲜艳度调整
 */
vec3 rgb2hsl(vec3 rgb) {
    float maxC = max(max(rgb.r, rgb.g), rgb.b);
    float minC = min(min(rgb.r, rgb.g), rgb.b);
    float delta = maxC - minC;
    
    float l = (maxC + minC) / 2.0;
    float h = 0.0;
    float s = 0.0;
    
    if (delta > 0.0001) {
        if (l < 0.5) {
            s = delta / (maxC + minC);
        } else {
            s = delta / (2.0 - maxC - minC);
        }
        
        if (rgb.r >= maxC) {
            h = (rgb.g - rgb.b) / delta;
        } else if (rgb.g >= maxC) {
            h = 2.0 + (rgb.b - rgb.r) / delta;
        } else {
            h = 4.0 + (rgb.r - rgb.g) / delta;
        }
        
        h = h / 6.0;
        if (h < 0.0) h += 1.0;
    }
    
    return vec3(h, s, l);
}

/**
 * HSL转RGB
 */
float hue2rgb(float p, float q, float t) {
    if (t < 0.0) t += 1.0;
    if (t > 1.0) t -= 1.0;
    if (t < 1.0/6.0) return p + (q - p) * 6.0 * t;
    if (t < 1.0/2.0) return q;
    if (t < 2.0/3.0) return p + (q - p) * (2.0/3.0 - t) * 6.0;
    return p;
}

vec3 hsl2rgb(vec3 hsl) {
    float h = hsl.x;
    float s = hsl.y;
    float l = hsl.z;
    
    if (s < 0.0001) {
        return vec3(l, l, l);
    }
    
    float q = l < 0.5 ? l * (1.0 + s) : l + s - l * s;
    float p = 2.0 * l - q;
    
    return vec3(
        hue2rgb(p, q, h + 1.0/3.0),
        hue2rgb(p, q, h),
        hue2rgb(p, q, h - 1.0/3.0)
    );
}

/**
 * RGB转LAB
 * 用于肤色检测和肤色平滑
 */
vec3 rgb2lab(vec3 rgb) {
    // RGB转XYZ
    float r = rgb.r > 0.04045 ? pow((rgb.r + 0.055) / 1.055, 2.4) : rgb.r / 12.92;
    float g = rgb.g > 0.04045 ? pow((rgb.g + 0.055) / 1.055, 2.4) : rgb.g / 12.92;
    float b = rgb.b > 0.04045 ? pow((rgb.b + 0.055) / 1.055, 2.4) : rgb.b / 12.92;
    
    vec3 xyz = vec3(
        r * 0.4124564 + g * 0.3575761 + b * 0.1804375,
        r * 0.2126729 + g * 0.7151522 + b * 0.0721750,
        r * 0.0193339 + g * 0.1191920 + b * 0.9503041
    );
    
    // XYZ转LAB
    xyz.x = xyz.x / 0.95047;
    xyz.y = xyz.y / 1.0;
    xyz.z = xyz.z / 1.08883;
    
    float fx = xyz.x > 0.008856 ? pow(xyz.x, 1.0/3.0) : 7.787 * xyz.x + 16.0/116.0;
    float fy = xyz.y > 0.008856 ? pow(xyz.y, 1.0/3.0) : 7.787 * xyz.y + 16.0/116.0;
    float fz = xyz.z > 0.008856 ? pow(xyz.z, 1.0/3.0) : 7.787 * xyz.z + 16.0/116.0;
    
    return vec3(
        116.0 * fy - 16.0,      // L
        500.0 * (fx - fy),      // A
        200.0 * (fy - fz)       // B
    );
}

/**
 * LAB转RGB
 */
vec3 lab2rgb(vec3 lab) {
    float y = (lab.x + 16.0) / 116.0;
    float x = lab.y / 500.0 + y;
    float z = y - lab.z / 200.0;
    
    float fx = x > 0.206893 ? pow(x, 3.0) : 0.128419 * (x - 0.137931);
    float fy = y > 0.206893 ? pow(y, 3.0) : 0.128419 * (y - 0.137931);
    float fz = z > 0.206893 ? pow(z, 3.0) : 0.128419 * (z - 0.137931);
    
    vec3 xyz = vec3(
        fx * 0.95047,
        fy * 1.0,
        fz * 1.08883
    );
    
    // XYZ转RGB
    float r = xyz.x * 3.2404542 - xyz.y * 1.5371385 - xyz.z * 0.4985314;
    float g = -xyz.x * 0.9692660 + xyz.y * 1.8760108 + xyz.z * 0.0415560;
    float b = xyz.x * 0.0556434 - xyz.y * 0.2040259 + xyz.z * 1.0572252;
    
    r = r > 0.0031308 ? 1.055 * pow(r, 1.0/2.4) - 0.055 : 12.92 * r;
    g = g > 0.0031308 ? 1.055 * pow(g, 1.0/2.4) - 0.055 : 12.92 * g;
    b = b > 0.0031308 ? 1.055 * pow(b, 1.0/2.4) - 0.055 : 12.92 * b;
    
    return vec3(r, g, b);
}

/**
 * 检测是否为肤色
 * 使用LAB颜色空间进行肤色检测
 */
bool isSkinColor(vec3 rgb) {
    vec3 lab = rgb2lab(rgb);
    
    // 肤色范围（基于LAB颜色空间）
    // L: 20-80, A: 5-25, B: 10-30
    bool isSkin = lab.x > 20.0 && lab.x < 80.0 &&
                  lab.y > 5.0 && lab.y < 25.0 &&
                  lab.z > 10.0 && lab.z < 30.0;
    
    return isSkin;
}

/**
 * 伪随机数生成
 * 用于颗粒效果
 */
float random(vec2 st) {
    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
}

/**
 * 高斯模糊采样
 * 用于降噪和肤色平滑
 */
vec3 gaussianBlur(sampler2D tex, vec2 uv, vec2 direction, float strength) {
    vec3 color = vec3(0.0);
    float total = 0.0;
    
    // 5x5高斯核
    float weights[5] = float[](0.05, 0.09, 0.12, 0.15, 0.18);
    
    for (int i = -2; i <= 2; i++) {
        float weight = weights[abs(i)];
        vec2 offset = direction * float(i) * strength;
        color += texture(tex, uv + offset).rgb * weight;
        total += weight;
    }
    
    return color / total;
}

/**
 * 锐化卷积
 * 使用Laplacian算子进行锐化
 */
vec3 sharpen(sampler2D tex, vec2 uv, float strength) {
    vec2 texelSize = 1.0 / uImageSize;
    
    vec3 center = texture(tex, uv).rgb;
    
    // 3x3 Laplacian核
    vec3 neighbors = 
        texture(tex, uv + vec2(-texelSize.x, -texelSize.y)).rgb +
        texture(tex, uv + vec2(texelSize.x, -texelSize.y)).rgb +
        texture(tex, uv + vec2(-texelSize.x, texelSize.y)).rgb +
        texture(tex, uv + vec2(texelSize.x, texelSize.y)).rgb +
        texture(tex, uv + vec2(-texelSize.x, 0.0)).rgb +
        texture(tex, uv + vec2(texelSize.x, 0.0)).rgb +
        texture(tex, uv + vec2(0.0, -texelSize.y)).rgb +
        texture(tex, uv + vec2(0.0, texelSize.y)).rgb;
    
    // 锐化公式: center + strength * (center * 8 - neighbors)
    return center + strength * (center * 8.0 - neighbors);
}

/**
 * 清晰度增强
 * 使用对比度自适应增强
 */
vec3 enhanceClarity(vec3 color, float strength) {
    // 计算局部对比度
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    
    // 根据亮度调整对比度增强强度
    float adaptiveStrength = strength * (1.0 - abs(luminance - 0.5) * 0.5);
    
    // 应用对比度增强
    vec3 mid = vec3(0.5);
    return mix(color, mid + (color - mid) * (1.0 + adaptiveStrength * 2.0), strength);
}

/**
 * 纹理增强
 * 使用高通滤波增强纹理细节
 */
vec3 enhanceTexture(sampler2D tex, vec2 uv, float strength) {
    vec2 texelSize = 1.0 / uImageSize;
    
    vec3 center = texture(tex, uv).rgb;
    
    // 高通滤波（提取高频细节）
    vec3 blur = 
        texture(tex, uv + vec2(-texelSize.x, -texelSize.y)).rgb * 0.0625 +
        texture(tex, uv + vec2(texelSize.x, -texelSize.y)).rgb * 0.0625 +
        texture(tex, uv + vec2(-texelSize.x, texelSize.y)).rgb * 0.0625 +
        texture(tex, uv + vec2(texelSize.x, texelSize.y)).rgb * 0.0625 +
        texture(tex, uv + vec2(-texelSize.x, 0.0)).rgb * 0.125 +
        texture(tex, uv + vec2(texelSize.x, 0.0)).rgb * 0.125 +
        texture(tex, uv + vec2(0.0, -texelSize.y)).rgb * 0.125 +
        texture(tex, uv + vec2(0.0, texelSize.y)).rgb * 0.125 +
        center.rgb * 0.25;
    
    // 高频细节
    vec3 detail = center - blur;
    
    // 添加纹理细节
    return center + detail * strength * 2.0;
}

/**
 * 高光调整
 * 只调整高亮区域
 */
vec3 adjustHighlights(vec3 color, float strength) {
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    
    // 高光区域阈值（亮度 > 0.5）
    float highlightMask = smoothstep(0.5, 1.0, luminance);
    
    // 高光调整
    vec3 adjustment = color * (1.0 + strength * highlightMask);
    
    return mix(color, adjustment, highlightMask);
}

/**
 * 阴影调整
 * 只调整暗部区域
 */
vec3 adjustShadows(vec3 color, float strength) {
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    
    // 阴影区域阈值（亮度 < 0.5）
    float shadowMask = smoothstep(0.5, 0.0, luminance);
    
    // 阴影调整
    vec3 adjustment = color + strength * shadowMask * 0.3;
    
    return mix(color, adjustment, shadowMask);
}

/**
 * 白色色阶调整
 * 调整最亮区域的亮度
 */
vec3 adjustWhites(vec3 color, float strength) {
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    
    // 白色区域阈值（亮度 > 0.7）
    float whiteMask = smoothstep(0.7, 1.0, luminance);
    
    // 白色调整
    vec3 adjustment = vec3(1.0) - (vec3(1.0) - color) * (1.0 - strength * whiteMask);
    
    return mix(color, adjustment, whiteMask);
}

/**
 * 黑色色阶调整
 * 调整最暗区域的亮度
 */
vec3 adjustBlacks(vec3 color, float strength) {
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    
    // 黑色区域阈值（亮度 < 0.3）
    float blackMask = smoothstep(0.3, 0.0, luminance);
    
    // 黑色调整
    vec3 adjustment = color * (1.0 + strength * blackMask);
    
    return mix(color, adjustment, blackMask);
}

/**
 * 去霾效果
 * 增加对比度和饱和度，减少雾感
 */
vec3 dehaze(vec3 color, float strength) {
    // 计算雾度（基于亮度和饱和度）
    vec3 hsl = rgb2hsl(color);
    float fogLevel = hsl.z * (1.0 - hsl.y);
    
    // 去霾强度
    float dehazeStrength = strength * fogLevel;
    
    // 增加对比度
    vec3 mid = vec3(0.5);
    vec3 contrastAdjusted = mid + (color - mid) * (1.0 + dehazeStrength);
    
    // 增加饱和度
    vec3 hslAdjusted = rgb2hsl(contrastAdjusted);
    hslAdjusted.y = min(hslAdjusted.y + dehazeStrength * 0.5, 1.0);
    
    return hsl2rgb(hslAdjusted);
}

/**
 * 褪色效果
 * 降低对比度，增加黑色提升，模拟胶片褪色
 */
vec3 fadeEffect(vec3 color, float strength) {
    // 降低对比度
    vec3 mid = vec3(0.5);
    vec3 faded = mid + (color - mid) * (1.0 - strength * 0.3);
    
    // 提升黑色（增加暗部亮度）
    faded = mix(faded, faded + vec3(0.1) * strength, strength);
    
    // 降低饱和度
    vec3 hsl = rgb2hsl(faded);
    hsl.y = hsl.y * (1.0 - strength * 0.2);
    
    return hsl2rgb(hsl);
}

/**
 * 胶片颗粒效果
 * 添加随机噪点模拟胶片颗粒
 */
vec3 grainEffect(vec3 color, vec2 uv, float strength) {
    // 生成随机颗粒
    float grain = random(uv * uTime) * 2.0 - 1.0;
    
    // 根据亮度调整颗粒强度（暗部颗粒更多）
    float luminance = dot(color, vec3(0.299, 0.587, 0.114));
    float grainStrength = strength * (1.0 + (1.0 - luminance) * 0.5);
    
    // 添加颗粒
    return color + vec3(grain * grainStrength * 0.15);
}

/**
 * 肤色平滑
 * 只对肤色区域进行平滑处理
 */
vec3 skinSmooth(sampler2D tex, vec2 uv, float strength) {
    vec3 original = texture(tex, uv).rgb;
    
    // 检测是否为肤色
    if (isSkinColor(original)) {
        // 对肤色区域进行高斯模糊
        vec2 texelSize = 1.0 / uImageSize;
        vec3 smoothed = gaussianBlur(tex, uv, texelSize, strength * 2.0);
        
        // 混合原始和模糊结果
        return mix(original, smoothed, strength * 0.5);
    }
    
    return original;
}

/**
 * 鲜艳度调整
 * 自然饱和度，保护已饱和的颜色
 */
vec3 adjustVibrance(vec3 color, float strength) {
    vec3 hsl = rgb2hsl(color);
    
    // 鲜艳度公式：饱和度越低，调整越强
    float vibranceAmount = (1.0 - hsl.y) * strength;
    hsl.y = min(hsl.y + vibranceAmount * 0.5, 1.0);
    
    return hsl2rgb(hsl);
}

/**
 * 主函数
 * 执行18参数全通道图像处理
 */
void main() {
    // 获取原始像素颜色
    vec4 originalColor = texture(uTexture, vTexCoord);
    vec3 color = originalColor.rgb;
    
    // ========== 1. 降噪处理（最先执行）==========
    if (uDenoise > 0.01) {
        vec2 texelSize = 1.0 / uImageSize;
        color = gaussianBlur(uTexture, vTexCoord, texelSize, uDenoise * 1.5);
    }
    
    // ========== 2. 肤色平滑处理 ==========
    if (uSkinSmooth > 0.01) {
        color = skinSmooth(uTexture, vTexCoord, uSkinSmooth);
    }
    
    // ========== 3. 基础调整 ==========
    
    // 曝光调整
    if (abs(uExposure) > 0.01) {
        color = color * pow(2.0, uExposure);
    }
    
    // 亮度调整
    if (abs(uBrightness) > 0.01) {
        color = color + uBrightness * 0.5;
    }
    
    // 对比度调整
    if (abs(uContrast) > 0.01) {
        vec3 mid = vec3(0.5);
        color = mid + (color - mid) * (1.0 + uContrast);
    }
    
    // ========== 4. 色彩调整 ==========
    
    // 饱和度调整
    if (abs(uSaturation) > 0.01) {
        vec3 hsl = rgb2hsl(color);
        hsl.y = clamp(hsl.y + uSaturation, 0.0, 1.0);
        color = hsl2rgb(hsl);
    }
    
    // 鲜艳度调整
    if (abs(uVibrance) > 0.01) {
        color = adjustVibrance(color, uVibrance);
    }
    
    // 色温调整
    if (abs(uWarmth) > 0.01) {
        // 暖色调：增加红色，减少蓝色
        // 冷色调：减少红色，增加蓝色
        color.r = color.r + uWarmth * 0.1;
        color.b = color.b - uWarmth * 0.1;
    }
    
    // ========== 5. 光影调整 ==========
    
    // 高光调整
    if (abs(uHighlights) > 0.01) {
        color = adjustHighlights(color, uHighlights);
    }
    
    // 阴影调整
    if (abs(uShadows) > 0.01) {
        color = adjustShadows(color, uShadows);
    }
    
    // 白色色阶调整
    if (abs(uWhites) > 0.01) {
        color = adjustWhites(color, uWhites);
    }
    
    // 黑色色阶调整
    if (abs(uBlacks) > 0.01) {
        color = adjustBlacks(color, uBlacks);
    }
    
    // ========== 6. 细节增强 ==========
    
    // 纹理增强
    if (abs(uTextureStrength) > 0.01) {
        color = enhanceTexture(uTexture, vTexCoord, uTextureStrength);
    }
    
    // 清晰度增强
    if (uClarity > 0.01) {
        color = enhanceClarity(color, uClarity);
    }
    
    // 锐化处理
    if (uSharpness > 0.01) {
        color = sharpen(uTexture, vTexCoord, uSharpness * 0.5);
    }
    
    // ========== 7. 效果处理 ==========
    
    // 去霾效果
    if (uDehaze > 0.01) {
        color = dehaze(color, uDehaze);
    }
    
    // 褪色效果
    if (uFade > 0.01) {
        color = fadeEffect(color, uFade);
    }
    
    // 胶片颗粒效果（最后执行）
    if (uGrain > 0.01) {
        color = grainEffect(color, vTexCoord, uGrain);
    }
    
    // ========== 输出结果 ==========
    
    // 确保颜色值在有效范围内
    color = clamp(color, 0.0, 1.0);
    
    // 输出最终颜色（保持原始Alpha）
    fragColor = vec4(color, originalColor.a);
}