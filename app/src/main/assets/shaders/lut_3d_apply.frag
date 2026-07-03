#version 300 es
// 3D LUT 色彩映射着色器
// 使用 2D 纹理编码的 3D LUT 实现色彩空间映射
// 兼容 OpenGL ES 3.0，无需 sampler3D

precision highp float;

// 输入纹理坐标
in vec2 vTexCoord;

// 输出颜色
out vec4 fragColor;

// 原始图像纹理
uniform sampler2D uTexture;

// 3D LUT 编码为 2D 纹理
// 布局：宽度 = lutSize * lutSize，高度 = lutSize
// 每行是一个 Blue 切片，Red 水平排列，Green 垂直排列
uniform sampler2D uLUT3D;

// LUT 尺寸（如 33）
uniform float uLUTSize;

// LUT 强度 [0, 1]，0 = 原图，1 = 完全应用 LUT
uniform float uLUTStrength;

/**
 * 从 2D 编码纹理中采样 3D LUT
 *
 * @param color 输入颜色 [0, 1]^3
 * @return LUT 映射后的颜色
 */
vec3 sampleLUT3D(vec3 color) {
    float size = uLUTSize;

    // 将颜色值映射到 LUT 索引空间
    // 使用 half-texel 偏移避免采样到相邻切片
    float sliceSize = 1.0 / size;           // 每个切片的归一化高度
    float slicePixelSize = 1.0 / (size * size); // 每个像素的归一化宽度

    // Blue 通道决定切片（y 坐标）
    float b = clamp(color.b, 0.0, 1.0);
    float sliceIndex = floor(b * (size - 1.0));
    float sliceFraction = fract(b * (size - 1.0));

    // 在当前切片和下一个切片之间插值
    float y0 = (sliceIndex + 0.5) / size;
    float y1 = min((sliceIndex + 1.0 + 0.5) / size, 1.0);

    // Red 和 Green 通道决定切片内位置
    float x0_r = (color.r * (size - 1.0) + floor(color.g * (size - 1.0)) * size + 0.5) / (size * size);
    float x1_r = x0_r; // 同一位置

    // 采样两个 Blue 切片
    vec3 color0 = texture(uLUT3D, vec2(x0_r, y0)).rgb;
    vec3 color1 = texture(uLUT3D, vec2(x0_r, y1)).rgb;

    // Blue 方向线性插值
    return mix(color0, color1, sliceFraction);
}

/**
 * 高精度 3D LUT 采样（三线性插值）
 * 对 Red/Green/Blue 三个方向都进行插值，避免切片边界伪影
 */
vec3 sampleLUT3DTrilinear(vec3 color) {
    float size = uLUTSize;
    float maxIndex = size - 1.0;

    // 映射到索引空间
    vec3 coord = clamp(color, 0.0, 1.0) * maxIndex;

    // 整数部分和小数部分
    vec3 coord0 = floor(coord);
    vec3 coord1 = min(coord0 + 1.0, maxIndex);
    vec3 frac = coord - coord0;

    // 计算纹理坐标
    // x = redIndex + greenIndex * lutSize
    // y = blueIndex
    float pixelWidth = 1.0 / (size * size);
    float pixelHeight = 1.0 / size;

    // 8 个角的纹理坐标
    vec2 uv000 = vec2((coord0.r + coord0.g * size + 0.5) / (size * size), (coord0.b + 0.5) / size);
    vec2 uv100 = vec2((coord1.r + coord0.g * size + 0.5) / (size * size), (coord0.b + 0.5) / size);
    vec2 uv010 = vec2((coord0.r + coord1.g * size + 0.5) / (size * size), (coord0.b + 0.5) / size);
    vec2 uv110 = vec2((coord1.r + coord1.g * size + 0.5) / (size * size), (coord0.b + 0.5) / size);
    vec2 uv001 = vec2((coord0.r + coord0.g * size + 0.5) / (size * size), (coord1.b + 0.5) / size);
    vec2 uv101 = vec2((coord1.r + coord0.g * size + 0.5) / (size * size), (coord1.b + 0.5) / size);
    vec2 uv011 = vec2((coord0.r + coord1.g * size + 0.5) / (size * size), (coord1.b + 0.5) / size);
    vec2 uv111 = vec2((coord1.r + coord1.g * size + 0.5) / (size * size), (coord1.b + 0.5) / size);

    // 8 个角采样
    vec3 c000 = texture(uLUT3D, uv000).rgb;
    vec3 c100 = texture(uLUT3D, uv100).rgb;
    vec3 c010 = texture(uLUT3D, uv010).rgb;
    vec3 c110 = texture(uLUT3D, uv110).rgb;
    vec3 c001 = texture(uLUT3D, uv001).rgb;
    vec3 c101 = texture(uLUT3D, uv101).rgb;
    vec3 c011 = texture(uLUT3D, uv011).rgb;
    vec3 c111 = texture(uLUT3D, uv111).rgb;

    // 三线性插值
    vec3 c00 = mix(c000, c100, frac.r);
    vec3 c10 = mix(c010, c110, frac.r);
    vec3 c01 = mix(c001, c101, frac.r);
    vec3 c11 = mix(c011, c111, frac.r);

    vec3 c0 = mix(c00, c10, frac.g);
    vec3 c1 = mix(c01, c11, frac.g);

    return mix(c0, c1, frac.b);
}

void main() {
    vec4 originalColor = texture(uTexture, vTexCoord);
    vec3 inputColor = originalColor.rgb;

    // 3D LUT 映射
    vec3 lutColor = sampleLUT3DTrilinear(inputColor);

    // 按强度混合原图和 LUT 结果
    vec3 result = mix(inputColor, lutColor, uLUTStrength);

    fragColor = vec4(clamp(result, 0.0, 1.0), originalColor.a);
}
