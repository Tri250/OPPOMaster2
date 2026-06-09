/**
 * AI 推理服务
 * 提供真实的 AI 微调推理调用，集成后端 AI 服务
 */

import { HeuristicSceneAnalyzer, AnalysisResult } from '../ai/HeuristicSceneAnalyzer';
import { getHasselbladParams, getRecommendedFilms } from '../ai/SceneToHasselbladMapping';

// ============================================
// 类型定义
// ============================================

/**
 * AI 微调参数（18个参数）
 */
export interface AIFineTuneParams {
  // 基础参数
  exposure: number;      // 曝光 -100~100
  brightness: number;    // 亮度 -100~100
  contrast: number;      // 对比度 -100~100
  saturation: number;    // 饱和度 -100~100
  warmth: number;        // 温 -100~100
  vibrance: number;      // 自然饱和度 -100~100
  
  // 专业参数
  highlights: number;    // 高光 -100~100
  shadows: number;       // 阴影 -100~100
  whites: number;        // 白色色阶 -100~100
  blacks: number;        // 黑色色阶 -100~100
  texture: number;       // 纹理 -100~100
  clarity: number;       // 清晰度 -100~100
  
  // 效果参数
  sharpness: number;     // 锐度 0~100
  dehaze: number;        // 去雾 0~100
  denoise: number;       // 降噪 0~100
  grain: number;         // 颗粒 0~100
  fade: number;          // 褪色 0~100
  skinSmooth: number;    // 肤色平滑 0~100
}

/**
 * AI 推理请求
 */
export interface AIInferenceRequest {
  imageSource: HTMLImageElement | HTMLCanvasElement | ImageData | string;
  currentParams: Partial<AIFineTuneParams>;
  optimizations?: string[];  // 智能优化选项 ID
  styleId?: string;          // 色彩风格 ID
}

/**
 * AI 推理响应
 */
export interface AIInferenceResponse {
  success: boolean;
  params: AIFineTuneParams;
  sceneAnalysis?: AnalysisResult;
  confidence: number;
  processingTime: number;
  recommendations?: AIRecommendation[];
  error?: string;
}

/**
 * AI 推荐项
 */
export interface AIRecommendation {
  type: 'film' | 'style' | 'optimization';
  id: string;
  name: string;
  matchScore: number;
  description: string;
}

/**
 * 推理阶段
 */
export type InferenceStage = 
  | 'idle'
  | 'analyzing'
  | 'detecting_subject'
  | 'analyzing_light'
  | 'computing_params'
  | 'applying_ai'
  | 'completed'
  | 'error';

/**
 * 推理进度回调
 */
export type ProgressCallback = (stage: InferenceStage, progress: number, message: string) => void;

// ============================================
// 默认参数
// ============================================

export const DEFAULT_AI_PARAMS: AIFineTuneParams = {
  exposure: 0,
  brightness: 0,
  contrast: 0,
  saturation: 0,
  warmth: 0,
  vibrance: 0,
  highlights: 0,
  shadows: 0,
  whites: 0,
  blacks: 0,
  texture: 0,
  clarity: 0,
  sharpness: 0,
  dehaze: 0,
  denoise: 0,
  grain: 0,
  fade: 0,
  skinSmooth: 0,
};

// ============================================
// AI 推理服务类
// ============================================

/**
 * AI 推理服务
 * 
 * 实现真实的 AI 微调推理，包含：
 * 1. 场景分析（基于 HeuristicSceneAnalyzer）
 * 2. 参数计算（基于 SceneToHasselbladMapping）
 * 3. 智能优化应用
 * 4. 后端 API 调用（预留接口）
 */
export class AIInferenceService {
  private analyzer: HeuristicSceneAnalyzer;
  private apiEndpoint: string | null = null;
  
  constructor(apiEndpoint?: string) {
    this.analyzer = new HeuristicSceneAnalyzer();
    this.apiEndpoint = apiEndpoint || null;
  }
  
  /**
   * 执行 AI 一键微调
   * 
   * @param request 推理请求
   * @param onProgress 进度回调
   * @returns 推理响应
   */
  async performAutoTune(
    request: AIInferenceRequest,
    onProgress?: ProgressCallback
  ): Promise<AIInferenceResponse> {
    const startTime = Date.now();
    
    try {
      // 阶段 1: 分析图像特征
      onProgress?.('analyzing', 0.1, '分析图像特征...');
      const imageData = await this.getImageData(request.imageSource);
      
      // 阶段 2: 检测主体对象
      onProgress?.('detecting_subject', 0.25, '检测主体对象...');
      const sceneAnalysis = await this.analyzer.analyze(imageData);
      
      // 阶段 3: 分析光照条件
      onProgress?.('analyzing_light', 0.4, '分析光照条件...');
      const lightAnalysis = this.analyzeLightConditions(sceneAnalysis);
      
      // 阶段 4: 计算最佳参数
      onProgress?.('computing_params', 0.6, '计算最佳参数...');
      const computedParams = this.computeOptimalParams(
        sceneAnalysis,
        lightAnalysis,
        request.optimizations,
        request.styleId
      );
      
      // 阶段 5: 应用 AI 优化（如果配置了后端 API）
      onProgress?.('applying_ai', 0.8, '应用AI优化...');
      let finalParams = computedParams;
      
      if (this.apiEndpoint) {
        // 调用后端 AI 服务进行深度优化
        const backendResult = await this.callBackendAPI({
          ...request,
          sceneAnalysis,
          computedParams,
        });
        if (backendResult.success) {
          finalParams = backendResult.params;
        }
      }
      
      // 阶段 6: 完成
      onProgress?.('completed', 1.0, '优化完成');
      
      const processingTime = Date.now() - startTime;
      
      // 构建推荐列表
      const recommendations = this.buildRecommendations(sceneAnalysis);
      
      return {
        success: true,
        params: finalParams,
        sceneAnalysis,
        confidence: sceneAnalysis.confidence,
        processingTime,
        recommendations,
      };
      
    } catch (error) {
      onProgress?.('error', 0, `推理失败: ${error}`);
      return {
        success: false,
        params: DEFAULT_AI_PARAMS,
        confidence: 0,
        processingTime: Date.now() - startTime,
        error: error instanceof Error ? error.message : '未知错误',
      };
    }
  }
  
  /**
   * 获取 ImageData
   */
  private async getImageData(
    source: HTMLImageElement | HTMLCanvasElement | ImageData | string
  ): Promise<ImageData> {
    if (source instanceof ImageData) {
      return source;
    }
    
    if (typeof source === 'string') {
      // 从 URL 加载图片
      const img = await this.loadImageFromUrl(source);
      return this.imageToImageData(img);
    }
    
    if (source instanceof HTMLImageElement) {
      return this.imageToImageData(source);
    }
    
    if (source instanceof HTMLCanvasElement) {
      const ctx = source.getContext('2d')!;
      return ctx.getImageData(0, 0, source.width, source.height);
    }
    
    throw new Error('无法识别的图像源');
  }
  
  /**
   * 从 URL 加载图片
   */
  private async loadImageFromUrl(url: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error(`图片加载失败: ${url}`));
      img.src = url;
    });
  }
  
  /**
   * HTMLImageElement 转 ImageData
   */
  private imageToImageData(img: HTMLImageElement): ImageData {
    const canvas = document.createElement('canvas');
    canvas.width = img.naturalWidth || img.width;
    canvas.height = img.naturalHeight || img.height;
    const ctx = canvas.getContext('2d')!;
    ctx.drawImage(img, 0, 0);
    return ctx.getImageData(0, 0, canvas.width, canvas.height);
  }
  
  /**
   * 分析光照条件
   */
  private analyzeLightConditions(analysis: AnalysisResult): LightCondition {
    const { colorProfile, brightnessLevel } = analysis;
    
    // 判断光照类型
    let lightType: LightType = 'natural';
    let lightDirection: LightDirection = 'front';
    let lightIntensity: LightIntensity = 'normal';
    
    // 根据亮度等级判断光照强度
    switch (brightnessLevel) {
      case 'VERY_DARK':
        lightIntensity = 'low';
        break;
      case 'DARK':
        lightIntensity = 'low';
        break;
      case 'BRIGHT':
        lightIntensity = 'high';
        break;
      case 'VERY_BRIGHT':
        lightIntensity = 'high';
        break;
      default:
        lightIntensity = 'normal';
    }
    
    // 根据暖色调占比判断光照类型
    if (colorProfile.warmthRatio > 0.6) {
      lightType = 'warm';
    } else if (colorProfile.warmthRatio < 0.2) {
      lightType = 'cool';
    }
    
    // 根据高光占比判断光照方向
    if (colorProfile.highlightRatio > 0.15) {
      lightDirection = 'backlit';
    } else if (colorProfile.darkPixelRatio > 0.5) {
      lightDirection = 'side';
    }
    
    return {
      type: lightType,
      direction: lightDirection,
      intensity: lightIntensity,
      warmthRatio: colorProfile.warmthRatio,
    };
  }
  
  /**
   * 计算最优参数
   */
  private computeOptimalParams(
    analysis: AnalysisResult,
    lightCondition: LightCondition,
    optimizations?: string[],
    styleId?: string
  ): AIFineTuneParams {
    const sceneId = analysis.primaryScene.id;
    const hasselbladParams = getHasselbladParams(sceneId);
    
    // 基础参数（从哈苏参数映射）
    let params: AIFineTuneParams = {
      ...DEFAULT_AI_PARAMS,
      // 映射哈苏参数到微调参数
      exposure: hasselbladParams.tone * 0.5,
      brightness: hasselbladParams.tone * 0.3,
      contrast: hasselbladParams.contrast,
      saturation: hasselbladParams.saturation,
      warmth: hasselbladParams.colorTemp,
      vibrance: hasselbladParams.saturation * 0.8,
      highlights: -hasselbladParams.vignette * 0.5,
      shadows: hasselbladParams.vignette * 0.5,
      whites: hasselbladParams.tone * 0.2,
      blacks: -hasselbladParams.tone * 0.2,
      texture: hasselbladParams.sharpness * 0.5,
      clarity: hasselbladParams.sharpness * 0.8,
      sharpness: Math.max(0, hasselbladParams.sharpness),
      dehaze: 0,
      denoise: 0,
      grain: 0,
      fade: 0,
      skinSmooth: 0,
    };
    
    // 根据光照条件微调
    params = this.adjustParamsForLight(params, lightCondition);
    
    // 应用智能优化
    if (optimizations) {
      params = this.applyOptimizations(params, optimizations);
    }
    
    // 应用风格预设
    if (styleId) {
      params = this.applyStylePreset(params, styleId);
    }
    
    // 根据场景置信度调整参数强度
    const confidenceFactor = Math.min(analysis.confidence, 1);
    params = this.scaleParamsByConfidence(params, confidenceFactor);
    
    return params;
  }
  
  /**
   * 根据光照条件调整参数
   */
  private adjustParamsForLight(params: AIFineTuneParams, light: LightCondition): AIFineTuneParams {
    const adjusted = { ...params };
    
    // 低光场景：提升曝光和降噪
    if (light.intensity === 'low') {
      adjusted.exposure += 10;
      adjusted.brightness += 5;
      adjusted.denoise += 15;
      adjusted.shadows += 10;
    }
    
    // 高光场景：降低高光
    if (light.intensity === 'high') {
      adjusted.highlights -= 15;
      adjusted.exposure -= 5;
    }
    
    // 暖光场景：调整色温
    if (light.type === 'warm') {
      adjusted.warmth += 5;
    }
    
    // 冷光场景：调整色温
    if (light.type === 'cool') {
      adjusted.warmth -= 5;
    }
    
    // 逆光场景：提升阴影
    if (light.direction === 'backlit') {
      adjusted.shadows += 20;
      adjusted.highlights -= 10;
    }
    
    return adjusted;
  }
  
  /**
   * 应用智能优化
   */
  private applyOptimizations(params: AIFineTuneParams, optimizations: string[]): AIFineTuneParams {
    const adjusted = { ...params };
    
    for (const optId of optimizations) {
      switch (optId) {
        case 'hdr':
          // HDR 增强：扩展动态范围
          adjusted.highlights -= 15;
          adjusted.shadows += 20;
          adjusted.clarity += 20;
          adjusted.contrast += 10;
          break;
          
        case 'denoise':
          // 智能降噪
          adjusted.denoise += 25;
          adjusted.sharpness = Math.max(adjusted.sharpness - 5, 0);
          break;
          
        case 'sharpen':
          // 智能锐化
          adjusted.sharpness += 30;
          adjusted.clarity += 15;
          break;
          
        case 'dehaze':
          // 去雾
          adjusted.dehaze += 20;
          adjusted.contrast += 10;
          adjusted.clarity += 10;
          break;
          
        case 'skin':
          // 肤色优化
          adjusted.skinSmooth += 30;
          adjusted.saturation += 5;
          adjusted.warmth += 3;
          break;
          
        case 'sky':
          // 天空增强
          adjusted.saturation += 20;
          adjusted.vibrance += 15;
          adjusted.highlights -= 10;
          break;
          
        case 'ai-composition':
          // AI 构图（不直接影响参数）
          break;
          
        case 'portrait-bokeh':
          // 人像虚化（不直接影响参数）
          adjusted.skinSmooth += 15;
          break;
          
        case 'color-match':
          // 色彩匹配（需要参考图）
          break;
          
        case 'smart-light':
          // 智能补光
          adjusted.shadows += 25;
          adjusted.exposure += 5;
          break;
      }
    }
    
    return adjusted;
  }
  
  /**
   * 应用风格预设
   */
  private applyStylePreset(params: AIFineTuneParams, styleId: string): AIFineTuneParams {
    // 风格预设参数映射
    const styleParams: Record<string, Partial<AIFineTuneParams>> = {
      'natural': { saturation: 5, contrast: 5, warmth: 0, vibrance: 5 },
      'vivid': { saturation: 25, contrast: 15, warmth: 5, vibrance: 20 },
      'warm': { saturation: 10, contrast: 8, warmth: 20, vibrance: 10 },
      'cool': { saturation: 8, contrast: 10, warmth: -20, vibrance: 8 },
      'film': { saturation: -10, contrast: 15, warmth: 5, grain: 15, fade: 10 },
      'bw': { saturation: -100, contrast: 20, warmth: 0, clarity: 15 },
      'vintage': { saturation: -15, contrast: 5, warmth: 15, fade: 20, grain: 10 },
      'cinematic': { saturation: 5, contrast: 25, warmth: 10 },
      'moody': { saturation: -5, contrast: 30, warmth: -10, shadows: 20, highlights: -15 },
      'pastel': { saturation: -10, contrast: -10, warmth: 5, brightness: 10, fade: 15 },
      'dramatic': { saturation: 15, contrast: 35, warmth: 5, clarity: 20, highlights: -20 },
      'hdr': { saturation: 10, contrast: 20, warmth: 0, highlights: -30, shadows: 30, clarity: 25 },
    };
    
    const style = styleParams[styleId];
    if (style) {
      return { ...params, ...style };
    }
    
    return params;
  }
  
  /**
   * 根据置信度缩放参数
   */
  private scaleParamsByConfidence(params: AIFineTuneParams, confidence: number): AIFineTuneParams {
    // 置信度越高，参数调整越激进
    const scaleFactor = 0.5 + confidence * 0.5; // 0.5 ~ 1.0
    
    const scaled: AIFineTuneParams = { ...params };
    
    // 只缩放有意义的参数
    const scaleKeys: (keyof AIFineTuneParams)[] = [
      'exposure', 'brightness', 'contrast', 'saturation', 'warmth', 'vibrance',
      'highlights', 'shadows', 'whites', 'blacks', 'texture', 'clarity',
    ];
    
    for (const key of scaleKeys) {
      scaled[key] = Math.round(scaled[key] * scaleFactor);
    }
    
    return scaled;
  }
  
  /**
   * 构建推荐列表
   */
  private buildRecommendations(analysis: AnalysisResult): AIRecommendation[] {
    const recommendations: AIRecommendation[] = [];
    
    // 胶片推荐
    const films = getRecommendedFilms(analysis.primaryScene.id);
    for (const film of films.slice(0, 3)) {
      recommendations.push({
        type: 'film',
        id: film.id,
        name: film.name,
        matchScore: film.matchScore,
        description: film.description,
      });
    }
    
    // 风格推荐（基于场景类别）
    const styleRecommendations = this.getStyleRecommendations(analysis.primaryScene.category);
    recommendations.push(...styleRecommendations);
    
    return recommendations;
  }
  
  /**
   * 获取风格推荐
   */
  private getStyleRecommendations(category: string): AIRecommendation[] {
    const styleMap: Record<string, AIRecommendation[]> = {
      'portrait': [
        { type: 'style', id: 'natural', name: '自然', matchScore: 0.85, description: '自然真实色彩' },
        { type: 'style', id: 'warm', name: '暖调', matchScore: 0.80, description: '温暖阳光感' },
      ],
      'landscape': [
        { type: 'style', id: 'vivid', name: '鲜艳', matchScore: 0.90, description: '浓郁鲜艳色彩' },
        { type: 'style', id: 'hdr', name: 'HDR', matchScore: 0.85, description: '高动态范围' },
      ],
      'night': [
        { type: 'style', id: 'cinematic', name: '电影', matchScore: 0.88, description: '电影大片感' },
        { type: 'style', id: 'moody', name: '情绪', matchScore: 0.75, description: '情绪氛围感' },
      ],
      'food': [
        { type: 'style', id: 'warm', name: '暖调', matchScore: 0.92, description: '温暖阳光感' },
        { type: 'style', id: 'vivid', name: '鲜艳', matchScore: 0.80, description: '浓郁鲜艳色彩' },
      ],
    };
    
    return styleMap[category] || [
      { type: 'style', id: 'natural', name: '自然', matchScore: 0.80, description: '自然真实色彩' },
    ];
  }
  
  /**
   * 调用后端 API（预留接口）
   */
  private async callBackendAPI(request: BackendAPIRequest): Promise<AIInferenceResponse> {
    if (!this.apiEndpoint) {
      // 没有配置后端 API，返回本地计算结果
      return {
        success: true,
        params: request.computedParams,
        confidence: 0.85,
        processingTime: 0,
      };
    }
    
    try {
      const response = await fetch(this.apiEndpoint, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          sceneId: request.sceneAnalysis?.primaryScene.id,
          params: request.computedParams,
          optimizations: request.optimizations,
        }),
      });
      
      if (!response.ok) {
        throw new Error(`API 调用失败: ${response.status}`);
      }
      
      const data = await response.json();
      return {
        success: true,
        params: data.params,
        confidence: data.confidence || 0.9,
        processingTime: data.processingTime || 0,
      };
      
    } catch (error) {
      console.warn('后端 API 调用失败，使用本地计算结果:', error);
      return {
        success: true,
        params: request.computedParams,
        confidence: 0.85,
        processingTime: 0,
      };
    }
  }
}

// ============================================
// 辅助类型
// ============================================

type LightType = 'natural' | 'warm' | 'cool';
type LightDirection = 'front' | 'side' | 'backlit';
type LightIntensity = 'low' | 'normal' | 'high';

interface LightCondition {
  type: LightType;
  direction: LightDirection;
  intensity: LightIntensity;
  warmthRatio: number;
}

interface BackendAPIRequest {
  imageSource?: HTMLImageElement | HTMLCanvasElement | ImageData | string;
  currentParams?: Partial<AIFineTuneParams>;
  sceneAnalysis?: AnalysisResult;
  computedParams: AIFineTuneParams;
  optimizations?: string[];
}

// ============================================
// 单例实例
// ============================================

let serviceInstance: AIInferenceService | null = null;

/**
 * 获取 AI 推理服务实例
 */
export function getAIInferenceService(apiEndpoint?: string): AIInferenceService {
  if (!serviceInstance) {
    serviceInstance = new AIInferenceService(apiEndpoint);
  }
  return serviceInstance;
}

/**
 * 执行一键 AI 微调（便捷方法）
 */
export async function performAutoTune(
  request: AIInferenceRequest,
  onProgress?: ProgressCallback
): Promise<AIInferenceResponse> {
  const service = getAIInferenceService();
  return service.performAutoTune(request, onProgress);
}