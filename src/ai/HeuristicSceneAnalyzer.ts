// Layer 2: 大师推理层 - Web端启发式分析器
// 混合推理策略（放弃随机，拥抱真实分析）

import {
  SceneProfile,
  HasselbladParams,
  FilmPreset,
  SceneCategory,
  SoftLightMode,
  SCENE_PRESETS,
  ALL_FILM_PRESETS,
  getScenePresetById,
} from './sceneProfile';

/**
 * 颜色画像
 */
export interface ColorProfile {
  avgRed: number;
  avgGreen: number;
  avgBlue: number;
  warmthRatio: number;      // 暖色调占比 0-1
  greenDominance: number;   // 绿色通道主导度
  blueDominance: number;    // 蓝色通道主导度
  redDominance: number;     // 红色通道主导度
  skinToneRatio: number;    // 肤色占比（YCbCr检测）
  darkPixelRatio: number;   // 暗部像素占比
  highlightRatio: number;   // 高光像素占比
}

/**
 * 亮度等级
 */
export enum BrightnessLevel {
  VERY_DARK = 'VERY_DARK',   // 极暗 0-50
  DARK = 'DARK',             // 暗调 50-100
  NORMAL = 'NORMAL',         // 正常 100-150
  BRIGHT = 'BRIGHT',         // 亮调 150-200
  VERY_BRIGHT = 'VERY_BRIGHT' // 高亮 200-255
}

export const BrightnessLevelMeta: Record<BrightnessLevel, { displayName: string; range: string }> = {
  [BrightnessLevel.VERY_DARK]: { displayName: '极暗', range: '0-50' },
  [BrightnessLevel.DARK]: { displayName: '暗调', range: '50-100' },
  [BrightnessLevel.NORMAL]: { displayName: '正常', range: '100-150' },
  [BrightnessLevel.BRIGHT]: { displayName: '亮调', range: '150-200' },
  [BrightnessLevel.VERY_BRIGHT]: { displayName: '高亮', range: '200-255' },
};

/**
 * 场景候选
 */
export interface SceneCandidate {
  sceneId: string;
  score: number;
  source: 'color' | 'brightness' | 'face' | 'exif' | 'texture';
}

/**
 * 分析结果
 */
export interface AnalysisResult {
  primaryScene: SceneProfile;
  confidence: number;
  alternativeScenes: SceneProfile[];
  colorProfile: ColorProfile;
  brightnessLevel: BrightnessLevel;
  faceCount: number;
  edgeDensity: number;
  analysisDetails: Record<string, number>;
}

/**
 * 用户上下文
 */
export interface UserContext {
  recentScenes: string[];
  preferredCategories: SceneCategory[];
}

/**
 * 启发式场景分析器
 * 
 * 混合推理策略：
 * 优先级 1: 颜色直方图分析（即时，无模型依赖）
 * 优先级 2: EXIF 元数据分析（即时，无模型依赖）
 * 优先级 3: 亮度与纹理分析
 * 优先级 4: 用户上下文推断
 */
export class HeuristicSceneAnalyzer {
  /**
   * 分析图片并返回场景识别结果
   */
  async analyze(
    imageSource: HTMLImageElement | HTMLCanvasElement | ImageData,
    userContext?: UserContext
  ): Promise<AnalysisResult> {
    // 获取ImageData
    const imageData = this.getImageData(imageSource);

    // 1. 颜色分析（采样策略：取中心 60% 区域）
    const colorProfile = this.sampleColorProfile(imageData, 0.6);

    // 2. 亮度分析
    const brightnessLevel = this.computeBrightnessLevel(imageData);

    // 3. 人脸检测（简化版）
    const faceCount = this.detectFaces(colorProfile);

    // 4. 纹理分析（边缘密度）
    const edgeDensity = this.computeEdgeDensity(imageData);

    // 5. 多特征投票
    const candidates: SceneCandidate[] = [];

    // 颜色投票
    candidates.push(...this.voteByColor(colorProfile));
    // 亮度投票
    candidates.push(...this.voteByBrightness(brightnessLevel, colorProfile));
    // 人脸投票
    if (faceCount > 0) candidates.push(...this.voteByFace(faceCount, colorProfile));
    // 纹理投票
    candidates.push(...this.voteByTexture(edgeDensity, colorProfile));

    // 6. 加权融合
    const fused = this.fuseVotes(candidates, userContext);

    // 7. 构建分析详情
    const analysisDetails = this.buildAnalysisDetails(
      colorProfile, brightnessLevel, faceCount, edgeDensity
    );

    return {
      primaryScene: fused.primary,
      confidence: fused.confidence,
      alternativeScenes: fused.alternatives,
      colorProfile,
      brightnessLevel,
      faceCount,
      edgeDensity,
      analysisDetails,
    };
  }

  /**
   * 获取ImageData
   */
  private getImageData(source: HTMLImageElement | HTMLCanvasElement | ImageData): ImageData {
    if (source instanceof ImageData) {
      return source;
    }

    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d')!;

    if (source instanceof HTMLImageElement) {
      canvas.width = source.naturalWidth || source.width;
      canvas.height = source.naturalHeight || source.height;
      ctx.drawImage(source, 0, 0);
    } else {
      canvas.width = source.width;
      canvas.height = source.height;
      ctx.drawImage(source, 0, 0);
    }

    return ctx.getImageData(0, 0, canvas.width, canvas.height);
  }

  /**
   * 颜色直方图采样
   * 采样策略：取中心区域，避免边缘干扰
   */
  private sampleColorProfile(imageData: ImageData, sampleRatio: number): ColorProfile {
    const { width, height, data } = imageData;
    const startX = Math.floor(width * (1 - sampleRatio) / 2);
    const startY = Math.floor(height * (1 - sampleRatio) / 2);
    const sampleW = Math.floor(width * sampleRatio);
    const sampleH = Math.floor(height * sampleRatio);

    let totalR = 0, totalG = 0, totalB = 0;
    let warmPixels = 0, coldPixels = 0;
    let skinPixels = 0, darkPixels = 0, highlightPixels = 0;
    let totalPixels = 0;

    // 采样步长
    const step = width > 500 ? 4 : 2;

    for (let y = startY; y < startY + sampleH; y += step) {
      for (let x = startX; x < startX + sampleW; x += step) {
        const idx = (y * width + x) * 4;
        const r = data[idx];
        const g = data[idx + 1];
        const b = data[idx + 2];

        totalR += r;
        totalG += g;
        totalB += b;
        totalPixels++;

        // 暖色调判定：R > B + 20 且 R > G
        if (r > b + 20 && r > g) warmPixels++;

        // 冷色调判定：B > R + 20 且 B > G
        if (b > r + 20 && b > g) coldPixels++;

        // 肤色检测（YCbCr 色彩空间）
        if (this.isSkinTone(r, g, b)) skinPixels++;

        // 亮度计算
        const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;

        // 暗部判定：亮度 < 50
        if (luminance < 50) darkPixels++;

        // 高光判定：亮度 > 200
        if (luminance > 200) highlightPixels++;
      }
    }

    const avgR = Math.round(totalR / totalPixels);
    const avgG = Math.round(totalG / totalPixels);
    const avgB = Math.round(totalB / totalPixels);
    const avgTotal = (avgR + avgG + avgB) / 3;

    return {
      avgRed: avgR,
      avgGreen: avgG,
      avgBlue: avgB,
      warmthRatio: warmPixels / totalPixels,
      greenDominance: avgTotal > 0 ? avgG / avgTotal : 1,
      blueDominance: avgTotal > 0 ? avgB / avgTotal : 1,
      redDominance: avgTotal > 0 ? avgR / avgTotal : 1,
      skinToneRatio: skinPixels / totalPixels,
      darkPixelRatio: darkPixels / totalPixels,
      highlightRatio: highlightPixels / totalPixels,
    };
  }

  /**
   * 肤色检测（YCbCr 色彩空间）
   */
  private isSkinTone(r: number, g: number, b: number): boolean {
    const y = 16 + 0.257 * r + 0.504 * g + 0.098 * b;
    const cb = 128 - 0.148 * r - 0.291 * g + 0.439 * b;
    const cr = 128 + 0.439 * r - 0.368 * g - 0.071 * b;

    return y >= 80 && y <= 230 && cb >= 77 && cb <= 127 && cr >= 133 && cr <= 173;
  }

  /**
   * 计算亮度等级
   */
  private computeBrightnessLevel(imageData: ImageData): BrightnessLevel {
    const { width, height, data } = imageData;
    let totalLuminance = 0;
    let pixelCount = 0;

    const step = width > 500 ? 4 : 2;

    for (let y = 0; y < height; y += step) {
      for (let x = 0; x < width; x += step) {
        const idx = (y * width + x) * 4;
        const r = data[idx];
        const g = data[idx + 1];
        const b = data[idx + 2];
        const luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        totalLuminance += luminance;
        pixelCount++;
      }
    }

    const avgLuminance = Math.round(totalLuminance / pixelCount);

    if (avgLuminance < 50) return BrightnessLevel.VERY_DARK;
    if (avgLuminance < 100) return BrightnessLevel.DARK;
    if (avgLuminance < 150) return BrightnessLevel.NORMAL;
    if (avgLuminance < 200) return BrightnessLevel.BRIGHT;
    return BrightnessLevel.VERY_BRIGHT;
  }

  /**
   * 人脸检测（简化版，基于肤色推断）
   */
  private detectFaces(colorProfile: ColorProfile): number {
    // 基于肤色占比推断可能的人脸数量
    if (colorProfile.skinToneRatio > 0.15) return 2;
    if (colorProfile.skinToneRatio > 0.08) return 1;
    if (colorProfile.skinToneRatio > 0.03) return 1;
    return 0;
  }

  /**
   * 计算边缘密度（Sobel算子）
   */
  private computeEdgeDensity(imageData: ImageData): number {
    const { width, height, data } = imageData;

    // 缩小采样区域
    const scale = Math.min(100 / width, 100 / height, 1);
    const sampleW = Math.floor(width * scale);
    const sampleH = Math.floor(height * scale);

    // 创建缩小的灰度图
    const gray = new Float32Array(sampleW * sampleH);
    for (let y = 0; y < sampleH; y++) {
      for (let x = 0; x < sampleW; x++) {
        const srcX = Math.floor(x / scale);
        const srcY = Math.floor(y / scale);
        const idx = (srcY * width + srcX) * 4;
        gray[y * sampleW + x] = 0.2126 * data[idx] + 0.7152 * data[idx + 1] + 0.0722 * data[idx + 2];
      }
    }

    let edgeCount = 0;
    let totalPixels = 0;

    // Sobel边缘检测
    for (let y = 1; y < sampleH - 1; y++) {
      for (let x = 1; x < sampleW - 1; x++) {
        const gx = this.sobelX(gray, sampleW, x, y);
        const gy = this.sobelY(gray, sampleW, x, y);
        const gradient = Math.sqrt(gx * gx + gy * gy);

        if (gradient > 50) edgeCount++;
        totalPixels++;
      }
    }

    return edgeCount / totalPixels;
  }

  private sobelX(gray: Float32Array, width: number, x: number, y: number): number {
    const p1 = gray[(y - 1) * width + x - 1];
    const p2 = gray[(y - 1) * width + x];
    const p3 = gray[(y - 1) * width + x + 1];
    const p4 = gray[y * width + x - 1];
    const p6 = gray[y * width + x + 1];
    const p7 = gray[(y + 1) * width + x - 1];
    const p8 = gray[(y + 1) * width + x];
    const p9 = gray[(y + 1) * width + x + 1];

    return -p1 + p3 - 2 * p4 + 2 * p6 - p7 + p9;
  }

  private sobelY(gray: Float32Array, width: number, x: number, y: number): number {
    const p1 = gray[(y - 1) * width + x - 1];
    const p2 = gray[(y - 1) * width + x];
    const p3 = gray[(y - 1) * width + x + 1];
    const p7 = gray[(y + 1) * width + x - 1];
    const p8 = gray[(y + 1) * width + x];
    const p9 = gray[(y + 1) * width + x + 1];

    return -p1 - 2 * p2 - p3 + p7 + 2 * p8 + p9;
  }

  // ==================== 投票机制 ====================

  /**
   * 颜色→场景投票
   */
  private voteByColor(cp: ColorProfile): SceneCandidate[] {
    const votes: SceneCandidate[] = [];

    // 绿色主导 → 森林/自然
    if (cp.greenDominance > 1.25) {
      const score = 0.70 * Math.min(cp.greenDominance, 1.5);
      votes.push({ sceneId: 'landscape-forest', score, source: 'color' });
      votes.push({ sceneId: 'landscape-standard', score: score * 0.8, source: 'color' });
    }

    // 蓝色主导 → 天空/海滩
    if (cp.blueDominance > 1.20) {
      const score = 0.65 * Math.min(cp.blueDominance, 1.5);
      votes.push({ sceneId: 'landscape-sky', score, source: 'color' });
      votes.push({ sceneId: 'landscape-beach', score: score * 0.85, source: 'color' });
    }

    // 暖色调 > 60% → 日落
    if (cp.warmthRatio > 0.55) {
      const score = Math.min(0.55 + cp.warmthRatio * 0.3, 0.95);
      votes.push({ sceneId: 'landscape-sunset', score, source: 'color' });
    }

    // 暖色调 35-55% → 美食
    if (cp.warmthRatio >= 0.35 && cp.warmthRatio <= 0.55) {
      const score = 0.50 + cp.warmthRatio * 0.2;
      votes.push({ sceneId: 'food-restaurant', score, source: 'color' });
      votes.push({ sceneId: 'food-dessert', score: score * 0.9, source: 'color' });
    }

    // 暗部占比 > 70% → 夜景
    if (cp.darkPixelRatio > 0.70) {
      const score = Math.min(0.60 + cp.darkPixelRatio * 0.25, 0.90);
      votes.push({ sceneId: 'night-city', score, source: 'color' });
      votes.push({ sceneId: 'night-neon', score: score * 0.85, source: 'color' });
    }

    // 肤色检测 → 人像
    if (cp.skinToneRatio > 0.05) {
      const score = Math.min(0.65 + cp.skinToneRatio * 0.3, 0.95);
      votes.push({ sceneId: 'portrait-standard', score, source: 'color' });
      votes.push({ sceneId: 'portrait-backlit', score: score * 0.8, source: 'color' });
    }

    // 高光占比高 → 逆光场景
    if (cp.highlightRatio > 0.15 && cp.warmthRatio > 0.3) {
      votes.push({ sceneId: 'portrait-backlit', score: 0.55 + cp.highlightRatio * 0.2, source: 'color' });
    }

    return votes;
  }

  /**
   * 亮度→场景投票
   */
  private voteByBrightness(level: BrightnessLevel, cp: ColorProfile): SceneCandidate[] {
    const votes: SceneCandidate[] = [];

    switch (level) {
      case BrightnessLevel.VERY_DARK:
        votes.push({ sceneId: 'night-city', score: 0.75, source: 'brightness' });
        votes.push({ sceneId: 'night-starry', score: 0.70, source: 'brightness' });
        votes.push({ sceneId: 'night-neon', score: 0.65, source: 'brightness' });
        break;

      case BrightnessLevel.DARK:
        votes.push({ sceneId: 'night-candle', score: 0.60, source: 'brightness' });
        votes.push({ sceneId: 'urban-cafe', score: 0.55, source: 'brightness' });
        if (cp.warmthRatio > 0.4) {
          votes.push({ sceneId: 'night-candle', score: 0.70, source: 'brightness' });
        }
        break;

      case BrightnessLevel.BRIGHT:
        if (cp.warmthRatio > 0.5) {
          votes.push({ sceneId: 'landscape-sunset', score: 0.65, source: 'brightness' });
        }
        if (cp.blueDominance > 1.2) {
          votes.push({ sceneId: 'landscape-sky', score: 0.60, source: 'brightness' });
        }
        break;

      case BrightnessLevel.VERY_BRIGHT:
        votes.push({ sceneId: 'landscape-beach', score: 0.65, source: 'brightness' });
        votes.push({ sceneId: 'landscape-snow', score: 0.60, source: 'brightness' });
        if (cp.warmthRatio > 0.6) {
          votes.push({ sceneId: 'landscape-sunset', score: 0.75, source: 'brightness' });
        }
        break;
    }

    return votes;
  }

  /**
   * 人脸→场景投票
   */
  private voteByFace(faceCount: number, cp: ColorProfile): SceneCandidate[] {
    const votes: SceneCandidate[] = [];

    switch (faceCount) {
      case 1:
        votes.push({ sceneId: 'portrait-standard', score: 0.85, source: 'face' });
        if (cp.warmthRatio > 0.4) {
          votes.push({ sceneId: 'portrait-backlit', score: 0.70, source: 'face' });
        }
        break;

      case 2:
        votes.push({ sceneId: 'portrait-couple', score: 0.80, source: 'face' });
        votes.push({ sceneId: 'portrait-standard', score: 0.75, source: 'face' });
        break;

      default:
        if (faceCount >= 3 && faceCount <= 5) {
          votes.push({ sceneId: 'portrait-group', score: 0.75, source: 'face' });
        } else if (faceCount > 5) {
          votes.push({ sceneId: 'urban-street', score: 0.65, source: 'face' });
          votes.push({ sceneId: 'event-party', score: 0.60, source: 'face' });
        }
        break;
    }

    return votes;
  }

  /**
   * 纹理→场景投票
   */
  private voteByTexture(edgeDensity: number, cp: ColorProfile): SceneCandidate[] {
    const votes: SceneCandidate[] = [];

    // 高边缘密度 → 建筑/街拍/微距
    if (edgeDensity > 0.30) {
      votes.push({ sceneId: 'urban-architecture', score: 0.65 + edgeDensity * 0.2, source: 'texture' });
      votes.push({ sceneId: 'urban-street', score: 0.60 + edgeDensity * 0.15, source: 'texture' });
      votes.push({ sceneId: 'macro-texture', score: 0.55 + edgeDensity * 0.25, source: 'texture' });
    }

    // 中等边缘密度 → 正常场景
    if (edgeDensity >= 0.15 && edgeDensity <= 0.30) {
      votes.push({ sceneId: 'landscape-standard', score: 0.55, source: 'texture' });
      votes.push({ sceneId: 'still-product', score: 0.50, source: 'texture' });
    }

    // 低边缘密度 → 柔光场景
    if (edgeDensity < 0.15) {
      votes.push({ sceneId: 'portrait-standard', score: 0.60, source: 'texture' });
      votes.push({ sceneId: 'portrait-child', score: 0.55, source: 'texture' });
      if (cp.warmthRatio > 0.3) {
        votes.push({ sceneId: 'food-dessert', score: 0.50, source: 'texture' });
      }
    }

    return votes;
  }

  /**
   * 加权融合投票结果
   */
  private fuseVotes(
    candidates: SceneCandidate[],
    userContext?: UserContext
  ): { primary: SceneProfile; confidence: number; alternatives: SceneProfile[] } {
    // 按场景ID分组并累加分数
    const scoreMap: Record<string, number> = {};
    const sourceMap: Record<string, string[]> = {};

    // 权重配置
    const weights: Record<string, number> = {
      color: 1.0,
      brightness: 0.8,
      face: 1.2,      // 人脸检测权重最高
      exif: 0.9,
      texture: 0.7,
    };

    for (const candidate of candidates) {
      const weight = weights[candidate.source] || 1.0;
      const weightedScore = candidate.score * weight;

      scoreMap[candidate.sceneId] = (scoreMap[candidate.sceneId] || 0) + weightedScore;
      if (!sourceMap[candidate.sceneId]) {
        sourceMap[candidate.sceneId] = [];
      }
      sourceMap[candidate.sceneId].push(candidate.source);
    }

    // 用户上下文加成
    if (userContext) {
      for (const recentScene of userContext.recentScenes) {
        if (scoreMap[recentScene]) {
          scoreMap[recentScene] += 0.15;
        }
      }
      // 偏好类别加成（简化处理）
    }

    // 排序并取Top-4
    const sorted = Object.entries(scoreMap).sort((a, b) => b[1] - a[1]);
    const topScenes = sorted.slice(0, 4);

    // 获取场景预设
    const primaryPreset = getScenePresetById(topScenes[0]?.[0] || 'portrait-standard');
    const primary: SceneProfile = {
      ...primaryPreset!,
      confidence: topScenes[0]?.[1] || 0.5,
    };

    const alternatives = topScenes.slice(1)
      .map(([id]) => getScenePresetById(id))
      .filter((p): p is SceneProfile => p !== undefined);

    // 计算置信度
    const totalScore = sorted.reduce((sum, [, score]) => sum + score, 0);
    const confidence = Math.min(topScenes[0]?.[1] / Math.max(totalScore, 1), 1);

    return { primary, confidence, alternatives };
  }

  /**
   * 构建分析详情
   */
  private buildAnalysisDetails(
    cp: ColorProfile,
    brightness: BrightnessLevel,
    faceCount: number,
    edgeDensity: number
  ): Record<string, number> {
    const brightnessValue = {
      [BrightnessLevel.VERY_DARK]: 0,
      [BrightnessLevel.DARK]: 1,
      [BrightnessLevel.NORMAL]: 2,
      [BrightnessLevel.BRIGHT]: 3,
      [BrightnessLevel.VERY_BRIGHT]: 4,
    }[brightness];

    return {
      warmth_ratio: cp.warmthRatio,
      green_dominance: cp.greenDominance,
      blue_dominance: cp.blueDominance,
      red_dominance: cp.redDominance,
      skin_tone_ratio: cp.skinToneRatio,
      dark_pixel_ratio: cp.darkPixelRatio,
      highlight_ratio: cp.highlightRatio,
      brightness_level: brightnessValue,
      face_count: faceCount,
      edge_density: edgeDensity,
    };
  }
}

// 单例实例
let analyzerInstance: HeuristicSceneAnalyzer | null = null;

export function getAnalyzer(): HeuristicSceneAnalyzer {
  if (!analyzerInstance) {
    analyzerInstance = new HeuristicSceneAnalyzer();
  }
  return analyzerInstance;
}