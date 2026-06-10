/**
 * 图像处理服务
 * 集成 WebGLRenderer，提供完整的图像处理流程
 */

import { WebGLRenderer, createWebGLRenderer, RenderParams, isWebGLSupported } from './webglRenderer';
import { AIFineTuneParams, DEFAULT_AI_PARAMS } from './aiInferenceService';
import { Preset, PresetSection } from '../store/appStore';

// ============================================
// 类型定义
// ============================================

/**
 * 图像处理结果
 */
export interface ImageProcessResult {
  success: boolean;
  imageData?: ImageData;
  canvas?: HTMLCanvasElement;
  dataUrl?: string;
  processingTime: number;
  error?: string;
}

/**
 * 预设参数转换结果
 */
export interface PresetToRenderParams {
  params: AIFineTuneParams;
  sections: PresetSection[];
}

/**
 * 图像处理状态
 */
export type ProcessState = 'idle' | 'loading' | 'processing' | 'completed' | 'error';

/**
 * 处理进度回调
 */
export type ProcessProgressCallback = (state: ProcessState, progress: number, message: string) => void;

// ============================================
// 预设参数转换
// ============================================

/**
 * 从预设 sections 提取参数值
 */
function extractParamFromSections(sections: PresetSection[], paramName: string): number {
  for (const section of sections) {
    for (const item of section.items) {
      const labelLower = item.label.toLowerCase();
      if (labelLower.includes(paramName.toLowerCase())) {
        // 解析数值，支持 "+10", "-5", "10", "复古 100%" 等格式
        const match = item.value.match(/([+-]?\d+)/);
        if (match) {
          return parseInt(match[1], 10);
        }
      }
    }
  }
  return 0;
}

/**
 * 将预设参数转换为渲染参数
 */
export function convertPresetToRenderParams(preset: Preset): AIFineTuneParams {
  // 从 sections 提取参数
  const saturation = preset.saturation ?? extractParamFromSections(preset.sections, '饱和度');
  const contrast = preset.contrast ?? extractParamFromSections(preset.sections, '对比度');
  const warmth = preset.warmth ?? extractParamFromSections(preset.sections, '色温');
  const sharpness = preset.sharpness ?? extractParamFromSections(preset.sections, '锐度');
  const clarity = preset.clarity ?? extractParamFromSections(preset.sections, '清晰度');
  const brightness = preset.brightness ?? extractParamFromSections(preset.sections, '亮度');

  // 构建 AIFineTuneParams
  const params: AIFineTuneParams = {
    ...DEFAULT_AI_PARAMS,
    // 基础参数
    saturation,
    contrast,
    brightness,
    warmth,
    exposure: 0,
    vibrance: saturation * 0.8,
    
    // 专业参数
    highlights: -Math.abs(warmth) * 0.3,
    shadows: Math.abs(warmth) * 0.3,
    whites: brightness * 0.2,
    blacks: -brightness * 0.2,
    texture: sharpness * 0.5,
    clarity: clarity || sharpness * 0.8,
    
    // 效果参数
    sharpness: Math.max(0, sharpness),
    dehaze: 0,
    denoise: 0,
    grain: preset.tags?.includes('film') ? 10 : 0,
    fade: preset.tags?.includes('vintage') ? 15 : 0,
    skinSmooth: preset.tags?.includes('portrait') ? 10 : 0,
  };

  // 根据标签调整参数
  if (preset.tags?.includes('hncs') || preset.isHncs) {
    // 哈苏自然色彩：更自然的饱和度和对比度
    params.vibrance = saturation * 0.6;
    params.clarity = sharpness * 0.5;
  }

  if (preset.tags?.includes('bw') || preset.tags?.includes('黑白')) {
    // 黑白模式
    params.saturation = -100;
    params.contrast = Math.max(contrast, 15);
  }

  if (preset.tags?.includes('夜景') || preset.tags?.includes('night')) {
    // 夜景模式：降噪和提升阴影
    params.denoise = 20;
    params.shadows = 25;
    params.highlights = -15;
  }

  return params;
}

// ============================================
// 图像处理服务类
// ============================================

/**
 * 图像处理服务
 * 
 * 提供完整的图像处理流程：
 * 1. 加载图像
 * 2. 应用预设参数
 * 3. WebGL 渲染处理
 * 4. 导出结果
 */
export class ImageProcessor {
  private renderer: WebGLRenderer | null = null;
  private canvas: HTMLCanvasElement;
  private currentImage: HTMLImageElement | null = null;
  private currentParams: AIFineTuneParams = DEFAULT_AI_PARAMS;
  
  constructor() {
    // 创建隐藏的 canvas 用于渲染
    this.canvas = document.createElement('canvas');
    this.canvas.style.display = 'none';
    
    // 初始化 WebGL 渲染器
    if (isWebGLSupported()) {
      this.renderer = createWebGLRenderer(this.canvas, {
        preserveDrawingBuffer: true,
      });
    }
  }
  
  /**
   * 加载图像
   */
  async loadImage(source: string | File | HTMLImageElement): Promise<HTMLImageElement> {
    if (source instanceof HTMLImageElement) {
      this.currentImage = source;
      return source;
    }
    
    if (source instanceof File) {
      return this.loadImageFromFile(source);
    }
    
    if (typeof source === 'string') {
      return this.loadImageFromUrl(source);
    }
    
    throw new Error('无法识别的图像源');
  }
  
  /**
   * 从文件加载图像
   */
  private async loadImageFromFile(file: File): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const img = new Image();
        img.onload = () => {
          this.currentImage = img;
          resolve(img);
        };
        img.onerror = () => reject(new Error('图像加载失败'));
        img.src = e.target?.result as string;
      };
      reader.onerror = () => reject(new Error('文件读取失败'));
      reader.readAsDataURL(file);
    });
  }
  
  /**
   * 从 URL 加载图像
   */
  private async loadImageFromUrl(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        this.currentImage = img;
        resolve(img);
      };
      img.onerror = () => reject(new Error(`图像加载失败: ${url}`));
      img.src = url;
    });
  }
  
  /**
   * 应用预设并处理图像
   */
  async applyPreset(
    preset: Preset,
    onProgress?: ProcessProgressCallback
  ): Promise<ImageProcessResult> {
    const startTime = Date.now();
    
    if (!this.currentImage) {
      return {
        success: false,
        error: '请先加载图像',
        processingTime: 0,
      };
    }
    
    try {
      // 阶段 1: 转换预设参数
      onProgress?.('processing', 0.2, '解析预设参数...');
      this.currentParams = convertPresetToRenderParams(preset);
      
      // 阶段 2: 加载图像到渲染器
      onProgress?.('processing', 0.4, '加载图像...');
      
      if (this.renderer) {
        // 使用 WebGL 渲染
        const loaded = this.renderer.loadImage(this.currentImage);
        if (!loaded) {
          throw new Error('图像加载失败');
        }
        
        // 阶段 3: 渲染处理
        onProgress?.('processing', 0.6, '应用哈苏配方...');
        const result = this.renderer.render({ params: this.currentParams });
        
        if (!result.success) {
          throw new Error(result.error || '渲染失败');
        }
        
        // 阶段 4: 完成
        onProgress?.('completed', 1.0, '处理完成');
        
        const processingTime = Date.now() - startTime;
        
        // 生成 dataUrl
        const dataUrl = this.canvas.toDataURL('image/png');
        
        return {
          success: true,
          imageData: result.imageData,
          canvas: this.canvas,
          dataUrl,
          processingTime,
        };
      } else {
        // WebGL 不支持，使用 CSS filter 后备方案
        onProgress?.('processing', 0.6, '使用后备方案处理...');
        
        const result = await this.processWithCanvasFallback();
        
        onProgress?.('completed', 1.0, '处理完成');
        
        return {
          ...result,
          processingTime: Date.now() - startTime,
        };
      }
      
    } catch (error) {
      onProgress?.('error', 0, `处理失败: ${error}`);
      return {
        success: false,
        error: error instanceof Error ? error.message : '处理失败',
        processingTime: Date.now() - startTime,
      };
    }
  }
  
  /**
   * Canvas 后备处理方案（无 WebGL 时使用）
   */
  private async processWithCanvasFallback(): Promise<ImageProcessResult> {
    if (!this.currentImage) {
      return { success: false, error: '无图像', processingTime: 0 };
    }
    
    // 设置 canvas 尺寸
    this.canvas.width = this.currentImage.naturalWidth || this.currentImage.width;
    this.canvas.height = this.currentImage.naturalHeight || this.currentImage.height;
    
    const ctx = this.canvas.getContext('2d');
    if (!ctx) {
      return { success: false, error: '无法获取 Canvas 上下文', processingTime: 0 };
    }
    
    // 应用 CSS filter
    const filterStyle = this.generateCSSFilter(this.currentParams);
    ctx.filter = filterStyle;
    
    // 绘制图像
    ctx.drawImage(this.currentImage, 0, 0);
    
    // 获取 ImageData
    const imageData = ctx.getImageData(0, 0, this.canvas.width, this.canvas.height);
    
    // 生成 dataUrl
    const dataUrl = this.canvas.toDataURL('image/png');
    
    return {
      success: true,
      imageData,
      canvas: this.canvas,
      dataUrl,
      processingTime: 0,
    };
  }
  
  /**
   * 生成 CSS filter 样式
   */
  private generateCSSFilter(params: AIFineTuneParams): string {
    const filters: string[] = [];
    
    // 饱和度
    if (params.saturation !== 0) {
      filters.push(`saturate(${100 + params.saturation}%)`);
    }
    
    // 对比度
    if (params.contrast !== 0) {
      filters.push(`contrast(${100 + params.contrast}%)`);
    }
    
    // 亮度
    if (params.brightness !== 0) {
      filters.push(`brightness(${100 + params.brightness}%)`);
    }
    
    // 色温（使用 sepia 和 hue-rotate）
    if (params.warmth > 0) {
      filters.push(`sepia(${params.warmth * 0.5}%)`);
    } else if (params.warmth < 0) {
      filters.push(`hue-rotate(${params.warmth * 0.5}deg)`);
    }
    
    return filters.join(' ') || 'none';
  }
  
  /**
   * 导出图像
   */
  async exportImage(format: 'png' | 'jpeg' = 'png', quality = 0.9): Promise<string | null> {
    if (!this.canvas) return null;
    
    const mimeType = format === 'jpeg' ? 'image/jpeg' : 'image/png';
    return this.canvas.toDataURL(mimeType, quality);
  }
  
  /**
   * 下载图像
   */
  downloadImage(filename: string = 'omaster-output', format: 'png' | 'jpeg' = 'png'): void {
    const dataUrl = this.canvas.toDataURL(format === 'jpeg' ? 'image/jpeg' : 'image/png');
    
    const link = document.createElement('a');
    link.download = `${filename}.${format}`;
    link.href = dataUrl;
    link.click();
  }
  
  /**
   * 获取当前参数
   */
  getCurrentParams(): AIFineTuneParams {
    return this.currentParams;
  }
  
  /**
   * 设置参数
   */
  setParams(params: Partial<AIFineTuneParams>): void {
    this.currentParams = { ...this.currentParams, ...params };
  }
  
  /**
   * 获取当前图像
   */
  getCurrentImage(): HTMLImageElement | null {
    return this.currentImage;
  }
  
  /**
   * 获取渲染 Canvas
   */
  getCanvas(): HTMLCanvasElement {
    return this.canvas;
  }
  
  /**
   * 检查是否支持 WebGL
   */
  isWebGLAvailable(): boolean {
    return this.renderer !== null && this.renderer.isAvailable();
  }
  
  /**
   * 销毁服务
   */
  destroy(): void {
    if (this.renderer) {
      this.renderer.destroy();
      this.renderer = null;
    }
    this.currentImage = null;
  }
}

// ============================================
// 单例实例
// ============================================

let processorInstance: ImageProcessor | null = null;

/**
 * 获取图像处理器实例
 */
export function getImageProcessor(): ImageProcessor {
  if (!processorInstance) {
    processorInstance = new ImageProcessor();
  }
  return processorInstance;
}

/**
 * 重置图像处理器
 */
export function resetImageProcessor(): void {
  if (processorInstance) {
    processorInstance.destroy();
    processorInstance = null;
  }
}

/**
 * 快捷方法：应用预设到图像
 */
export async function applyPresetToImage(
  imageSource: string | File | HTMLImageElement,
  preset: Preset,
  onProgress?: ProcessProgressCallback
): Promise<ImageProcessResult> {
  const processor = getImageProcessor();
  
  // 加载图像
  onProgress?.('loading', 0.1, '加载图像...');
  await processor.loadImage(imageSource);
  
  // 应用预设
  return processor.applyPreset(preset, onProgress);
}