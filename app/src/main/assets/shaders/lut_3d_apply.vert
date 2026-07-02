#version 300 es
// 顶点着色器 - 3D LUT 应用
// 全屏四边形渲染

in vec2 aPosition;      // 顶点位置 [-1, 1]
in vec2 aTexCoord;      // 纹理坐标 [0, 1]

out vec2 vTexCoord;

void main() {
    vTexCoord = aTexCoord;
    gl_Position = vec4(aPosition, 0.0, 1.0);
}
