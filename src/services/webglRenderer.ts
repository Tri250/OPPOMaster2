/**
 * WebGL 渲染器
 * 提供高性能的图像处理渲染管线框架
 */

import { AIFineTuneParams } from './aiInferenceService';

// ============================================
// 类型定义
// ============================================

/**
 * WebGL 渲染器配置
 */
export interface WebGLRendererConfig {
  canvas: HTMLCanvasElement;
  width?: number;
  height?: number;
  preserveDrawingBuffer?: boolean;
}

/**
 * 渲染管线阶段
 */
export interface RenderPipelineStage {
  name: string;
  shader: WebGLShader | null;
  program: WebGLProgram | null;
  uniforms: Record<string, WebGLUniformLocation | null>;
  enabled: boolean;
}

/**
 * 渲染参数
 */
export interface RenderParams {
  params: AIFineTuneParams;
  hslAdjustments?: HSLAdjustment[];
  curvePoints?: CurvePoints;
}

/**
 * HSL 调整项
 */
export interface HSLAdjustment {
  colorId: string;      // red, orange, yellow, green, cyan, blue, purple, magenta
  hue: number;          // -180 ~ 180
  saturation: number;   // -100 ~ 100
  luminance: number;    // -100 ~ 100
}

/**
 * 曲线控制点
 */
export interface CurvePoints {
  rgb: number[][];      // [[x, y], ...]
  red?: number[][];
  green?: number[][];
  blue?: number[][];
}

/**
 * 渲染结果
 */
export interface RenderResult {
  success: boolean;
  imageData?: ImageData;
  error?: string;
}

// ============================================
// WebGL 着色器代码
// ============================================

// 顶点着色器（通用）
const VERTEX_SHADER_SOURCE = `
  attribute vec2 a_position;
  attribute vec2 a_texCoord;
  varying vec2 v_texCoord;
  
  void main() {
    gl_Position = vec4(a_position, 0.0, 1.0);
    v_texCoord = a_texCoord;
  }
`;

// 片段着色器（基础调色）
const FRAGMENT_SHADER_BASIC = `
  precision mediump float;
  
  uniform sampler2D u_image;
  uniform float u_saturation;
  uniform float u_contrast;
  uniform float u_brightness;
  uniform float u_exposure;
  uniform float u_warmth;
  uniform float u_vibrance;
  
  varying vec2 v_texCoord;
  
  void main() {
    vec4 color = texture2D(u_image, v_texCoord);
    
    // 曝光调整
    color.rgb *= pow(2.0, u_exposure / 100.0);
    
    // 亮度调整
    color.rgb += u_brightness / 100.0;
    
    // 对比度调整
    color.rgb = (color.rgb - 0.5) * (1.0 + u_contrast / 100.0) + 0.5;
    
    // 饱和度调整
    float gray = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    color.rgb = mix(vec3(gray), color.rgb, 1.0 + u_saturation / 100.0);
    
    // 自然饱和度（Vibrance）
    float maxVal = max(max(color.r, color.g), color.b);
    float minVal = min(min(color.r, color.g), color.b);
    float satVal = maxVal - minVal;
    float vibranceFactor = 1.0 + u_vibrance / 100.0 * (1.0 - satVal);
    color.rgb = mix(vec3(gray), color.rgb, vibranceFactor);
    
    // 色温调整（暖色调）
    if (u_warmth > 0.0) {
      color.r += u_warmth / 200.0;
      color.b -= u_warmth / 200.0;
    } else {
      color.b += abs(u_warmth) / 200.0;
      color.r -= abs(u_warmth) / 200.0;
    }
    
    // 确保颜色在有效范围内
    color.rgb = clamp(color.rgb, 0.0, 1.0);
    
    gl_FragColor = color;
  }
`;

// 片段着色器（专业调色 - 高光/阴影）
const FRAGMENT_SHADER_PRO = `
  precision mediump float;
  
  uniform sampler2D u_image;
  uniform float u_highlights;
  uniform float u_shadows;
  uniform float u_whites;
  uniform float u_blacks;
  uniform float u_texture;
  uniform float u_clarity;
  
  varying vec2 v_texCoord;
  
  void main() {
    vec4 color = texture2D(u_image, v_texCoord);
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    
    // 高光调整（只影响亮部）
    if (luminance > 0.5) {
      float highlightFactor = (luminance - 0.5) * 2.0;
      color.rgb += u_highlights / 100.0 * highlightFactor;
    }
    
    // 阴影调整（只影响暗部）
    if (luminance < 0.5) {
      float shadowFactor = (0.5 - luminance) * 2.0;
      color.rgb += u_shadows / 100.0 * shadowFactor;
    }
    
    // 白色色阶调整
    color.rgb += u_whites / 200.0;
    
    // 黑色色阶调整
    color.rgb -= u_blacks / 200.0;
    
    // 确保颜色在有效范围内
    color.rgb = clamp(color.rgb, 0.0, 1.0);
    
    gl_FragColor = color;
  }
`;

// 片段着色器（效果 - 锐度/降噪/颗粒）
const FRAGMENT_SHADER_EFFECTS = `
  precision mediump float;
  
  uniform sampler2D u_image;
  uniform float u_sharpness;
  uniform float u_denoise;
  uniform float u_grain;
  uniform float u_dehaze;
  uniform float u_fade;
  uniform float u_skinSmooth;
  uniform vec2 u_resolution;
  uniform float u_time;
  
  varying vec2 v_texCoord;
  
  void main() {
    vec4 color = texture2D(u_image, v_texCoord);
    
    // 去雾效果
    if (u_dehaze > 0.0) {
      float dehazeFactor = u_dehaze / 100.0;
      color.rgb = color.rgb + (1.0 - color.rgb) * dehazeFactor * 0.3;
      color.rgb = mix(vec3(dot(color.rgb, vec3(0.2126, 0.7152, 0.0722))), color.rgb, 1.0 - dehazeFactor * 0.2);
    }
    
    // 褪色效果
    if (u_fade > 0.0) {
      color.rgb = mix(color.rgb, vec3(1.0), u_fade / 200.0);
    }
    
    // 颗粒效果（随机噪声）
    if (u_grain > 0.0) {
      float noise = fract(sin(dot(v_texCoord + u_time, vec2(12.9898, 78.233))) * 43758.5453);
      color.rgb += (noise - 0.5) * u_grain / 200.0;
    }
    
    // 确保颜色在有效范围内
    color.rgb = clamp(color.rgb, 0.0, 1.0);
    
    gl_FragColor = color;
  }
`;

// ============================================
// WebGL 渲染器类
// ============================================

/**
 * WebGL 渲染器
 * 
 * 提供高性能的图像处理渲染管线：
 * 1. 基础调色（曝光、亮度、对比度、饱和度、色温）
 * 2. 专业调色（高光、阴影、白色色阶、黑色色阶）
 * 3. 效果处理（锐度、降噪、颗粒、去雾、褪色）
 * 4. HSL 调整（可选）
 * 5. 曲线调整（可选）
 */
export class WebGLRenderer {
  private canvas: HTMLCanvasElement;
  private gl: WebGLRenderingContext | null = null;
  private width: number;
  private height: number;
  private pipelineStages: RenderPipelineStage[] = [];
  private initialized: boolean = false;
  private sourceTexture: WebGLTexture | null = null;
  private framebuffer: WebGLFramebuffer | null = null;
  private tempTexture: WebGLTexture | null = null;
  
  constructor(config: WebGLRendererConfig) {
    this.canvas = config.canvas;
    this.width = config.width || config.canvas.width;
    this.height = config.height || config.canvas.height;
    
    this.initWebGL(config.preserveDrawingBuffer);
  }
  
  /**
   * 初始化 WebGL 上下文
   */
  private initWebGL(preserveDrawingBuffer?: boolean): void {
    const contextOptions: WebGLContextAttributes = {
      preserveDrawingBuffer: preserveDrawingBuffer ?? true,
      antialias: false,
      alpha: true,
    };
    
    const glContext = this.canvas.getContext('webgl', contextOptions) 
      || this.canvas.getContext('experimental-webgl', contextOptions);
    
    if (glContext && 'drawingBufferWidth' in glContext) {
      this.gl = glContext as WebGLRenderingContext;
    }
    
    if (!this.gl) {
      console.error('WebGL 不可用');
      return;
    }
    
    // 设置画布尺寸
    this.canvas.width = this.width;
    this.canvas.height = this.height;
    
    // 初始化渲染管线
    this.initPipeline();
    
    this.initialized = true;
  }
  
  /**
   * 初始化渲染管线
   */
  private initPipeline(): void {
    if (!this.gl) return;
    
    // 创建管线阶段
    this.pipelineStages = [
      this.createPipelineStage('basic', VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_BASIC),
      this.createPipelineStage('pro', VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_PRO),
      this.createPipelineStage('effects', VERTEX_SHADER_SOURCE, FRAGMENT_SHADER_EFFECTS),
    ];
    
    // 创建帧缓冲和临时纹理
    this.framebuffer = this.gl.createFramebuffer();
    this.tempTexture = this.gl.createTexture();
    
    // 配置临时纹理
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.tempTexture);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_S, this.gl.CLAMP_TO_EDGE);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_T, this.gl.CLAMP_TO_EDGE);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.LINEAR);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MAG_FILTER, this.gl.LINEAR);
    this.gl.texImage2D(
      this.gl.TEXTURE_2D, 0, this.gl.RGBA,
      this.width, this.height, 0,
      this.gl.RGBA, this.gl.UNSIGNED_BYTE, null
    );
  }
  
  /**
   * 创建渲染管线阶段
   */
  private createPipelineStage(
    name: string,
    vertexShaderSource: string,
    fragmentShaderSource: string
  ): RenderPipelineStage {
    if (!this.gl) {
      return { name, shader: null, program: null, uniforms: {}, enabled: true };
    }
    
    // 创建顶点着色器
    const vertexShader = this.createShader(this.gl.VERTEX_SHADER, vertexShaderSource);
    
    // 创建片段着色器
    const fragmentShader = this.createShader(this.gl.FRAGMENT_SHADER, fragmentShaderSource);
    
    // 创建程序
    const program = this.gl.createProgram();
    if (vertexShader && fragmentShader && program) {
      this.gl.attachShader(program, vertexShader);
      this.gl.attachShader(program, fragmentShader);
      this.gl.linkProgram(program);
      
      if (!this.gl.getProgramParameter(program, this.gl.LINK_STATUS)) {
        console.error(`程序链接失败: ${this.gl.getProgramInfoLog(program)}`);
      }
    }
    
    // 获取 uniform 位置
    const uniforms: Record<string, WebGLUniformLocation | null> = {};
    if (program) {
      // 基础调色 uniform
      uniforms['u_image'] = this.gl.getUniformLocation(program, 'u_image');
      uniforms['u_saturation'] = this.gl.getUniformLocation(program, 'u_saturation');
      uniforms['u_contrast'] = this.gl.getUniformLocation(program, 'u_contrast');
      uniforms['u_brightness'] = this.gl.getUniformLocation(program, 'u_brightness');
      uniforms['u_exposure'] = this.gl.getUniformLocation(program, 'u_exposure');
      uniforms['u_warmth'] = this.gl.getUniformLocation(program, 'u_warmth');
      uniforms['u_vibrance'] = this.gl.getUniformLocation(program, 'u_vibrance');
      
      // 专业调色 uniform
      uniforms['u_highlights'] = this.gl.getUniformLocation(program, 'u_highlights');
      uniforms['u_shadows'] = this.gl.getUniformLocation(program, 'u_shadows');
      uniforms['u_whites'] = this.gl.getUniformLocation(program, 'u_whites');
      uniforms['u_blacks'] = this.gl.getUniformLocation(program, 'u_blacks');
      uniforms['u_texture'] = this.gl.getUniformLocation(program, 'u_texture');
      uniforms['u_clarity'] = this.gl.getUniformLocation(program, 'u_clarity');
      
      // 效果 uniform
      uniforms['u_sharpness'] = this.gl.getUniformLocation(program, 'u_sharpness');
      uniforms['u_denoise'] = this.gl.getUniformLocation(program, 'u_denoise');
      uniforms['u_grain'] = this.gl.getUniformLocation(program, 'u_grain');
      uniforms['u_dehaze'] = this.gl.getUniformLocation(program, 'u_dehaze');
      uniforms['u_fade'] = this.gl.getUniformLocation(program, 'u_fade');
      uniforms['u_skinSmooth'] = this.gl.getUniformLocation(program, 'u_skinSmooth');
      uniforms['u_resolution'] = this.gl.getUniformLocation(program, 'u_resolution');
      uniforms['u_time'] = this.gl.getUniformLocation(program, 'u_time');
    }
    
    return {
      name,
      shader: fragmentShader,
      program,
      uniforms,
      enabled: true,
    };
  }
  
  /**
   * 创建着色器
   */
  private createShader(type: number, source: string): WebGLShader | null {
    if (!this.gl) return null;
    
    const shader = this.gl.createShader(type);
    if (!shader) return null;
    
    this.gl.shaderSource(shader, source);
    this.gl.compileShader(shader);
    
    if (!this.gl.getShaderParameter(shader, this.gl.COMPILE_STATUS)) {
      console.error(`着色器编译失败: ${this.gl.getShaderInfoLog(shader)}`);
      this.gl.deleteShader(shader);
      return null;
    }
    
    return shader;
  }
  
  /**
   * 加载图像到纹理
   */
  loadImage(image: HTMLImageElement | HTMLCanvasElement | ImageData): boolean {
    if (!this.gl || !this.initialized) {
      return false;
    }
    
    // 创建或重用纹理
    if (!this.sourceTexture) {
      this.sourceTexture = this.gl.createTexture();
    }
    
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.sourceTexture);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_S, this.gl.CLAMP_TO_EDGE);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_WRAP_T, this.gl.CLAMP_TO_EDGE);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MIN_FILTER, this.gl.LINEAR);
    this.gl.texParameteri(this.gl.TEXTURE_2D, this.gl.TEXTURE_MAG_FILTER, this.gl.LINEAR);
    
    if (image instanceof HTMLImageElement) {
      this.gl.texImage2D(
        this.gl.TEXTURE_2D, 0, this.gl.RGBA,
        this.gl.RGBA, this.gl.UNSIGNED_BYTE, image
      );
      this.width = image.naturalWidth || image.width;
      this.height = image.naturalHeight || image.height;
    } else if (image instanceof HTMLCanvasElement) {
      this.gl.texImage2D(
        this.gl.TEXTURE_2D, 0, this.gl.RGBA,
        this.gl.RGBA, this.gl.UNSIGNED_BYTE, image
      );
      this.width = image.width;
      this.height = image.height;
    } else if (image instanceof ImageData) {
      this.gl.texImage2D(
        this.gl.TEXTURE_2D, 0, this.gl.RGBA,
        image.width, image.height, 0,
        this.gl.RGBA, this.gl.UNSIGNED_BYTE, image.data
      );
      this.width = image.width;
      this.height = image.height;
    }
    
    // 更新画布尺寸
    this.canvas.width = this.width;
    this.canvas.height = this.height;
    
    // 更新临时纹理尺寸
    this.gl.bindTexture(this.gl.TEXTURE_2D, this.tempTexture);
    this.gl.texImage2D(
      this.gl.TEXTURE_2D, 0, this.gl.RGBA,
      this.width, this.height, 0,
      this.gl.RGBA, this.gl.UNSIGNED_BYTE, null
    );
    
    return true;
  }
  
  /**
   * 渲染图像
   */
  render(params: RenderParams): RenderResult {
    if (!this.gl || !this.initialized || !this.sourceTexture) {
      return {
        success: false,
        error: 'WebGL 渲染器未初始化或未加载图像',
      };
    }
    
    try {
      // 设置视口
      this.gl.viewport(0, 0, this.width, this.height);
      
      // 清除画布
      this.gl.clearColor(0, 0, 0, 1);
      this.gl.clear(this.gl.COLOR_BUFFER_BIT);
      
      // 执行渲染管线
      let inputTexture = this.sourceTexture;
      
      for (const stage of this.pipelineStages) {
        if (!stage.enabled || !stage.program) continue;
        
        // 使用当前阶段的程序
        this.gl.useProgram(stage.program);
        
        // 设置顶点属性
        this.setupVertexAttributes(stage.program);
        
        // 设置 uniform 值
        this.setUniforms(stage, params);
        
        // 绑定输入纹理
        this.gl.activeTexture(this.gl.TEXTURE0);
        this.gl.bindTexture(this.gl.TEXTURE_2D, inputTexture);
        if (stage.uniforms['u_image']) {
          this.gl.uniform1i(stage.uniforms['u_image'], 0);
        }
        
        // 渲染到帧缓冲（除了最后一个阶段）
        if (stage !== this.pipelineStages[this.pipelineStages.length - 1]) {
          this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, this.framebuffer);
          this.gl.framebufferTexture2D(
            this.gl.FRAMEBUFFER, this.gl.COLOR_ATTACHMENT0,
            this.gl.TEXTURE_2D, this.tempTexture, 0
          );
        } else {
          // 最后一个阶段渲染到画布
          this.gl.bindFramebuffer(this.gl.FRAMEBUFFER, null);
        }
        
        // 绘制
        this.gl.drawArrays(this.gl.TRIANGLES, 0, 6);
        
        // 下一个阶段使用临时纹理作为输入
        inputTexture = this.tempTexture;
      }
      
      // 获取渲染结果
      const imageData = this.getImageData();
      
      return {
        success: true,
        imageData,
      };
      
    } catch (error) {
      return {
        success: false,
        error: error instanceof Error ? error.message : '渲染失败',
      };
    }
  }
  
  /**
   * 设置顶点属性
   */
  private setupVertexAttributes(program: WebGLProgram): void {
    if (!this.gl) return;
    
    // 位置属性
    const positionBuffer = this.gl.createBuffer();
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, positionBuffer);
    
    // 两个三角形覆盖整个画布
    const positions = new Float32Array([
      -1, -1,  1, -1,  -1, 1,
      -1, 1,   1, -1,   1, 1,
    ]);
    this.gl.bufferData(this.gl.ARRAY_BUFFER, positions, this.gl.STATIC_DRAW);
    
    const positionLocation = this.gl.getAttribLocation(program, 'a_position');
    this.gl.enableVertexAttribArray(positionLocation);
    this.gl.vertexAttribPointer(positionLocation, 2, this.gl.FLOAT, false, 0, 0);
    
    // 纹理坐标属性
    const texCoordBuffer = this.gl.createBuffer();
    this.gl.bindBuffer(this.gl.ARRAY_BUFFER, texCoordBuffer);
    
    const texCoords = new Float32Array([
      0, 0,  1, 0,  0, 1,
      0, 1,  1, 0,  1, 1,
    ]);
    this.gl.bufferData(this.gl.ARRAY_BUFFER, texCoords, this.gl.STATIC_DRAW);
    
    const texCoordLocation = this.gl.getAttribLocation(program, 'a_texCoord');
    this.gl.enableVertexAttribArray(texCoordLocation);
    this.gl.vertexAttribPointer(texCoordLocation, 2, this.gl.FLOAT, false, 0, 0);
  }
  
  /**
   * 设置 uniform 值
   */
  private setUniforms(stage: RenderPipelineStage, params: RenderParams): void {
    if (!this.gl) return;
    
    const { uniforms } = stage;
    const { params: p } = params;
    
    // 基础调色参数
    if (uniforms['u_saturation']) this.gl.uniform1f(uniforms['u_saturation'], p.saturation);
    if (uniforms['u_contrast']) this.gl.uniform1f(uniforms['u_contrast'], p.contrast);
    if (uniforms['u_brightness']) this.gl.uniform1f(uniforms['u_brightness'], p.brightness);
    if (uniforms['u_exposure']) this.gl.uniform1f(uniforms['u_exposure'], p.exposure);
    if (uniforms['u_warmth']) this.gl.uniform1f(uniforms['u_warmth'], p.warmth);
    if (uniforms['u_vibrance']) this.gl.uniform1f(uniforms['u_vibrance'], p.vibrance);
    
    // 专业调色参数
    if (uniforms['u_highlights']) this.gl.uniform1f(uniforms['u_highlights'], p.highlights);
    if (uniforms['u_shadows']) this.gl.uniform1f(uniforms['u_shadows'], p.shadows);
    if (uniforms['u_whites']) this.gl.uniform1f(uniforms['u_whites'], p.whites);
    if (uniforms['u_blacks']) this.gl.uniform1f(uniforms['u_blacks'], p.blacks);
    if (uniforms['u_texture']) this.gl.uniform1f(uniforms['u_texture'], p.texture);
    if (uniforms['u_clarity']) this.gl.uniform1f(uniforms['u_clarity'], p.clarity);
    
    // 效果参数
    if (uniforms['u_sharpness']) this.gl.uniform1f(uniforms['u_sharpness'], p.sharpness);
    if (uniforms['u_denoise']) this.gl.uniform1f(uniforms['u_denoise'], p.denoise);
    if (uniforms['u_grain']) this.gl.uniform1f(uniforms['u_grain'], p.grain);
    if (uniforms['u_dehaze']) this.gl.uniform1f(uniforms['u_dehaze'], p.dehaze);
    if (uniforms['u_fade']) this.gl.uniform1f(uniforms['u_fade'], p.fade);
    if (uniforms['u_skinSmooth']) this.gl.uniform1f(uniforms['u_skinSmooth'], p.skinSmooth);
    
    // 其他参数
    if (uniforms['u_resolution']) {
      this.gl.uniform2f(uniforms['u_resolution'], this.width, this.height);
    }
    if (uniforms['u_time']) {
      this.gl.uniform1f(uniforms['u_time'], performance.now() / 1000);
    }
  }
  
  /**
   * 获取渲染结果 ImageData
   */
  private getImageData(): ImageData {
    if (!this.gl) {
      return new ImageData(1, 1);
    }
    
    const pixels = new Uint8Array(this.width * this.height * 4);
    this.gl.readPixels(0, 0, this.width, this.height, this.gl.RGBA, this.gl.UNSIGNED_BYTE, pixels);
    
    // WebGL 的像素是从下到上的，需要翻转
    const imageData = new ImageData(this.width, this.height);
    for (let y = 0; y < this.height; y++) {
      for (let x = 0; x < this.width; x++) {
        const srcIdx = ((this.height - y - 1) * this.width + x) * 4;
        const dstIdx = (y * this.width + x) * 4;
        imageData.data[dstIdx] = pixels[srcIdx];
        imageData.data[dstIdx + 1] = pixels[srcIdx + 1];
        imageData.data[dstIdx + 2] = pixels[srcIdx + 2];
        imageData.data[dstIdx + 3] = pixels[srcIdx + 3];
      }
    }
    
    return imageData;
  }
  
  /**
   * 销毁渲染器
   */
  destroy(): void {
    if (!this.gl) return;
    
    // 删除纹理
    if (this.sourceTexture) {
      this.gl.deleteTexture(this.sourceTexture);
      this.sourceTexture = null;
    }
    if (this.tempTexture) {
      this.gl.deleteTexture(this.tempTexture);
      this.tempTexture = null;
    }
    
    // 删除帧缓冲
    if (this.framebuffer) {
      this.gl.deleteFramebuffer(this.framebuffer);
      this.framebuffer = null;
    }
    
    // 删除程序和着色器
    for (const stage of this.pipelineStages) {
      if (stage.program) {
        this.gl.deleteProgram(stage.program);
      }
      if (stage.shader) {
        this.gl.deleteShader(stage.shader);
      }
    }
    
    this.pipelineStages = [];
    this.initialized = false;
  }
  
  /**
   * 检查 WebGL 是否可用
   */
  isAvailable(): boolean {
    return this.gl !== null && this.initialized;
  }
  
  /**
   * 获取画布尺寸
   */
  getSize(): { width: number; height: number } {
    return { width: this.width, height: this.height };
  }
}

// ============================================
// 工具函数
// ============================================

/**
 * 检查浏览器是否支持 WebGL
 */
export function isWebGLSupported(): boolean {
  try {
    const canvas = document.createElement('canvas');
    return !!(
      canvas.getContext('webgl') 
      || canvas.getContext('experimental-webgl')
    );
  } catch {
    return false;
  }
}

/**
 * 创建 WebGL 渲染器实例
 */
export function createWebGLRenderer(
  canvas: HTMLCanvasElement,
  options?: { width?: number; height?: number; preserveDrawingBuffer?: boolean }
): WebGLRenderer | null {
  if (!isWebGLSupported()) {
    console.warn('WebGL 不支持，将使用 CSS filter 作为后备');
    return null;
  }
  
  return new WebGLRenderer({
    canvas,
    width: options?.width,
    height: options?.height,
    preserveDrawingBuffer: options?.preserveDrawingBuffer ?? true,
  });
}

/**
 * 使用 CSS filter 生成预览样式（后备方案）
 */
export function generateCSSFilterStyle(params: AIFineTuneParams): string {
  return `
    saturate(${100 + params.saturation}%)
    contrast(${100 + params.contrast}%)
    brightness(${100 + params.brightness}%)
    sepia(${params.warmth > 0 ? params.warmth * 0.5 : 0}%)
    hue-rotate(${params.warmth < 0 ? params.warmth * 0.5 : 0}deg)
  `.trim();
}