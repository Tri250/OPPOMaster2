#version 300 es
// OpenGL ES 3.0 顶点着色器
// 标准顶点变换 - 用于图像调整渲染

// 精度设置
precision highp float;

// 输入属性
in vec2 aPosition;      // 顶点位置（标准化设备坐标）
in vec2 aTexCoord;      // 纹理坐标

// 输出到片段着色器
out vec2 vTexCoord;     // 传递给片段着色器的纹理坐标

/**
 * 主函数
 * 执行标准顶点变换，将纹理坐标传递给片段着色器
 */
void main() {
    // 直接使用输入位置（已经是标准化设备坐标）
    gl_Position = vec4(aPosition, 0.0, 1.0);
    
    // 传递纹理坐标到片段着色器
    vTexCoord = aTexCoord;
}