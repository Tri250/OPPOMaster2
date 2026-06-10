#version 100
// 蒙版渲染专用顶点着色器（与 image_adjust.vert 兼容）

attribute vec4 aPosition;
attribute vec2 aTextureCoord;

varying vec2 vTextureCoord;

void main() {
    gl_Position = aPosition;
    vTextureCoord = aTextureCoord;
}
