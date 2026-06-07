/**
 * 图片上传与AI分析服务
 * 支持用户上传图片，进行真实AI分析并推荐参数
 */

import { userImageStore, UserImage } from '../store/userImageStore';

export interface ImageAnalysis {
  brightness: number;        // 平均亮度 0-255
  contrast: number;          // 对比度 0-1
  saturation: number;        // 饱和度 0-1
  warmth: number;            // 冷暖倾向 -1~1
  sharpness: number;         // 边缘锐度 0-1
  noiseLevel: number;        // 噪点等级 0-1
  dominantColors: string[];  // 主色调列表
  detectedScene: string;     // 推测场景
  detectedTime: 'day' | 'sunset' | 'night';  // 推测时间
  faces?: number;            // 检测到的人脸数
  skyRatio: number;          // 天空占比 0-1
  vegetationRatio: number;    // 植被占比 0-1
}

export interface RecommendedParams {
  saturation: number;
  contrast: number;
  brightness: number;
  warmth: number;
  sharpness: number;
  highlights: number;
  shadows: number;
  clarity: number;
  noiseReduction: number;
  skinSmooth: number;
  style: string;            // 推荐的哈苏风格
  reason: string;            // 推荐理由
}

class ImageAnalysisService {
  /**
   * 读取用户上传的文件
   */
  async loadFromFile(file: File): Promise<UserImage> {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const dataUrl = e.target?.result as string;
        const img = new Image();
        img.onload = () => {
          const userImage: UserImage = {
            dataUrl,
            width: img.width,
            height: img.height,
            size: file.size,
            fileName: file.name,
            uploadedAt: Date.now(),
          };
          userImageStore.set(userImage);
          resolve(userImage);
        };
        img.onerror = () => reject(new Error('图片加载失败'));
        img.src = dataUrl;
      };
      reader.onerror = () => reject(new Error('文件读取失败'));
      reader.readAsDataURL(file);
    });
  }

  /**
   * 加载示例图片
   */
  async loadFromUrl(url: string, fileName = 'sample.jpg'): Promise<UserImage> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas 2D context 不可用'));
          return;
        }
        ctx.drawImage(img, 0, 0);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.9);
        const userImage: UserImage = {
          dataUrl,
          width: img.width,
          height: img.height,
          size: Math.round(dataUrl.length * 0.75),
          fileName,
          uploadedAt: Date.now(),
        };
        userImageStore.set(userImage);
        resolve(userImage);
      };
      img.onerror = () => reject(new Error('示例图片加载失败'));
      img.src = url;
    });
  }

  /**
   * 真实分析图片 - 使用 Canvas 像素分析
   */
  async analyze(image: UserImage): Promise<ImageAnalysis> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => {
        // 缩放到 200x200 进行快速分析
        const canvas = document.createElement('canvas');
        const SIZE = 200;
        const scale = Math.min(SIZE / img.width, SIZE / img.height);
        canvas.width = img.width * scale;
        canvas.height = img.height * scale;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) {
          reject(new Error('无法获取 canvas 上下文'));
          return;
        }
        ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const data = imageData.data;

        let totalR = 0, totalG = 0, totalB = 0;
        let totalBrightness = 0;
        let warmPixels = 0, coolPixels = 0;
        let skyPixels = 0, vegetationPixels = 0, skinPixels = 0;
        let darkPixels = 0, brightPixels = 0;
        let edgeSum = 0, edgeCount = 0;
        const colorBuckets: Record<string, number> = {};

        const width = canvas.width;
        const height = canvas.height;
        const pixelCount = width * height;

        for (let y = 0; y < height; y++) {
          for (let x = 0; x < width; x++) {
            const i = (y * width + x) * 4;
            const r = data[i];
            const g = data[i + 1];
            const b = data[i + 2];

            totalR += r;
            totalG += g;
            totalB += b;

            const brightness = (r + g + b) / 3;
            totalBrightness += brightness;

            // 冷暖判断
            if (r > b + 30) warmPixels++;
            else if (b > r + 30) coolPixels++;

            // 天空识别（上部 + 蓝色调）
            if (y < height * 0.4 && b > 150 && b > r && b > g - 20) {
              skyPixels++;
            }

            // 植被识别（绿色调）
            if (g > r + 10 && g > b + 10 && g > 80) {
              vegetationPixels++;
            }

            // 肤色识别
            if (r > 95 && g > 40 && b > 20 && r > g && r > b && Math.abs(r - g) > 15) {
              const max = Math.max(r, g, b);
              const min = Math.min(r, g, b);
              if (max - min > 15 && r - b > 15) {
                skinPixels++;
              }
            }

            // 明暗
            if (brightness < 50) darkPixels++;
            if (brightness > 200) brightPixels++;

            // 边缘检测 (简化版)
            if (x > 0 && y > 0) {
              const iPrev = ((y - 1) * width + x) * 4;
              const iLeft = (y * width + (x - 1)) * 4;
              const dr = Math.abs(data[i] - data[iPrev]);
              const dg = Math.abs(data[i + 1] - data[iPrev + 1]);
              const db = Math.abs(data[i + 2] - data[iPrev + 2]);
              const dr2 = Math.abs(data[i] - data[iLeft]);
              const dg2 = Math.abs(data[i + 1] - data[iLeft + 1]);
              const db2 = Math.abs(data[i + 2] - data[iLeft + 2]);
              edgeSum += (dr + dg + db + dr2 + dg2 + db2);
              edgeCount++;
            }

            // 颜色直方图
            const bucket = `${Math.floor(r / 32) * 32}-${Math.floor(g / 32) * 32}-${Math.floor(b / 32) * 32}`;
            colorBuckets[bucket] = (colorBuckets[bucket] || 0) + 1;
          }
        }

        const avgBrightness = totalBrightness / pixelCount;
        const avgR = totalR / pixelCount;
        const avgG = totalG / pixelCount;
        const avgB = totalB / pixelCount;

        // 计算对比度 (标准差近似)
        let variance = 0;
        for (let y = 0; y < height; y++) {
          for (let x = 0; x < width; x++) {
            const i = (y * width + x) * 4;
            const brightness = (data[i] + data[i + 1] + data[i + 2]) / 3;
            variance += Math.pow(brightness - avgBrightness, 2);
          }
        }
        const stdDev = Math.sqrt(variance / pixelCount);
        const contrast = Math.min(stdDev / 80, 1);

        // 饱和度
        const max = Math.max(avgR, avgG, avgB);
        const min = Math.min(avgR, avgG, avgB);
        const saturation = max === 0 ? 0 : (max - min) / max;

        // 冷暖
        const warmth = (avgR - avgB) / 255;

        // 锐度 (边缘密度)
        const sharpness = Math.min(edgeSum / edgeCount / 30, 1);

        // 噪点估算 (暗部高频变化)
        const noiseLevel = darkPixels / pixelCount > 0.3 && avgBrightness < 100 ? 0.6 : 0.2;

        // 场景推测
        const skyRatio = skyPixels / pixelCount;
        const vegetationRatio = vegetationPixels / pixelCount;
        const skinRatio = skinPixels / pixelCount;
        const darkRatio = darkPixels / pixelCount;

        let detectedScene = '通用';
        if (darkRatio > 0.6) detectedScene = '夜景';
        else if (skyRatio > 0.25 && vegetationRatio < 0.1) detectedScene = '风景';
        else if (skinRatio > 0.15) detectedScene = '人像';
        else if (vegetationRatio > 0.4) detectedScene = '自然';
        else if (saturation > 0.5 && avgR > 150) detectedScene = '美食';
        else if (contrast > 0.5) detectedScene = '街拍';

        // 时间推测
        let detectedTime: 'day' | 'sunset' | 'night' = 'day';
        if (darkRatio > 0.5) detectedTime = 'night';
        else if (warmth > 0.1 && avgR > 180) detectedTime = 'sunset';

        // 主色调
        const dominantColors = Object.entries(colorBuckets)
          .sort((a, b) => b[1] - a[1])
          .slice(0, 3)
          .map(([k]) => {
            const [r, g, b] = k.split('-').map(Number);
            return `rgb(${r + 16}, ${g + 16}, ${b + 16})`;
          });

        resolve({
          brightness: avgBrightness,
          contrast,
          saturation,
          warmth,
          sharpness,
          noiseLevel,
          dominantColors,
          detectedScene,
          detectedTime,
          skyRatio,
          vegetationRatio,
        });
      };
      img.onerror = () => reject(new Error('图片加载失败'));
      img.src = image.dataUrl;
    });
  }

  /**
   * 基于分析结果推荐哈苏参数
   */
  recommendHasselbladParams(analysis: ImageAnalysis): RecommendedParams {
    const { brightness, contrast, saturation, warmth, sharpness, detectedScene, detectedTime } = analysis;

    // 根据场景选择基础风格
    let baseStyle = 'hncs_natural';
    let baseParams = {
      saturation: 5, contrast: 8, brightness: 0, warmth: 2, sharpness: 10,
      highlights: 0, shadows: 0, clarity: 10, noiseReduction: 0, skinSmooth: 0,
    };
    let reason = '基于图片整体色彩平衡的通用推荐';

    if (detectedScene === '夜景') {
      baseStyle = 'hncs_night';
      baseParams = {
        saturation: 8, contrast: 15, brightness: -3, warmth: -8, sharpness: 18,
        highlights: -15, shadows: 12, clarity: 18, noiseReduction: 35, skinSmooth: 0,
      };
      reason = '夜景需高对比度与降噪，保留暗部细节';
    } else if (detectedScene === '人像') {
      baseStyle = 'hncs_portrait';
      baseParams = {
        saturation: 8, contrast: 3, brightness: 5, warmth: 8, sharpness: 10,
        highlights: -8, shadows: 5, clarity: 12, noiseReduction: 10, skinSmooth: 25,
      };
      reason = '人像强调肤色自然与美肤，柔和高光';
    } else if (detectedScene === '风景') {
      baseStyle = 'hncs_rich';
      baseParams = {
        saturation: 15, contrast: 12, brightness: 0, warmth: -3, sharpness: 18,
        highlights: -8, shadows: 5, clarity: 20, noiseReduction: 0, skinSmooth: 0,
      };
      reason = '风景适合浓郁色彩与高清晰度，突出层次';
    } else if (detectedScene === '美食') {
      baseStyle = 'hncs_food';
      baseParams = {
        saturation: 15, contrast: 10, brightness: 5, warmth: 20, sharpness: 15,
        highlights: -5, shadows: 5, clarity: 12, noiseReduction: 0, skinSmooth: 0,
      };
      reason = '美食需要暖色调激发食欲';
    } else if (detectedScene === '自然') {
      baseStyle = 'hncs_green';
      baseParams = {
        saturation: 12, contrast: 8, brightness: 3, warmth: 0, sharpness: 15,
        highlights: -5, shadows: 3, clarity: 18, noiseReduction: 0, skinSmooth: 0,
      };
      reason = '自然场景适合清新的绿色调';
    } else if (detectedScene === '街拍') {
      baseStyle = 'hncs_street';
      baseParams = {
        saturation: 5, contrast: 15, brightness: 0, warmth: 5, sharpness: 20,
        highlights: -10, shadows: 8, clarity: 18, noiseReduction: 0, skinSmooth: 0,
      };
      reason = '街拍需要高对比度与胶片质感';
    }

    // 根据图片特征微调
    if (brightness < 80) {
      baseParams.brightness += 10;
      baseParams.shadows += 8;
      reason += ' · 图片偏暗已自动提亮';
    } else if (brightness > 200) {
      baseParams.brightness -= 10;
      baseParams.highlights -= 10;
      reason += ' · 图片过曝已压缩高光';
    }

    if (contrast < 0.3) {
      baseParams.contrast += 8;
      baseParams.clarity += 5;
      reason += ' · 低对比度已增强';
    }

    if (saturation < 0.3) {
      baseParams.saturation += 8;
      reason += ' · 低饱和已增强';
    } else if (saturation > 0.7) {
      baseParams.saturation -= 5;
      reason += ' · 高饱和已微降';
    }

    if (detectedTime === 'sunset' && warmth < 0.2) {
      baseParams.warmth += 8;
      reason += ' · 日落暖调增强';
    }

    return {
      ...baseParams,
      style: baseStyle,
      reason,
    };
  }

  /**
   * 推荐多种哈苏风格供用户选择
   */
  recommendMultipleStyles(analysis: ImageAnalysis): RecommendedParams[] {
    const { detectedScene } = analysis;
    const baseParams = this.recommendHasselbladParams(analysis);

    // 通用 3 套风格
    const styles: RecommendedParams[] = [
      {
        ...baseParams,
        style: 'recommended',
        reason: 'AI 智能推荐 · 最适合当前图片',
      },
    ];

    // 根据场景添加专属风格
    if (detectedScene === '夜景') {
      styles.push({
        ...baseParams,
        style: 'hncs_night_vivid',
        saturation: 12,
        contrast: 20,
        brightness: -5,
        warmth: -12,
        sharpness: 22,
        highlights: -25,
        shadows: 18,
        clarity: 22,
        noiseReduction: 40,
        skinSmooth: 0,
        reason: '夜景霓虹风格 · 高对比饱和',
      });
      styles.push({
        ...baseParams,
        style: 'hncs_night_calm',
        saturation: -5,
        contrast: 10,
        brightness: 0,
        warmth: 0,
        sharpness: 12,
        highlights: -10,
        shadows: 5,
        clarity: 10,
        noiseReduction: 25,
        skinSmooth: 0,
        reason: '夜景静谧风格 · 冷静克制',
      });
    } else if (detectedScene === '人像') {
      styles.push({
        ...baseParams,
        style: 'hncs_portrait_film',
        saturation: -5,
        contrast: 5,
        brightness: 3,
        warmth: 12,
        sharpness: 8,
        highlights: -5,
        shadows: 5,
        clarity: 8,
        noiseReduction: 5,
        skinSmooth: 30,
        reason: '胶片人像风格 · 复古柔美',
      });
      styles.push({
        ...baseParams,
        style: 'hncs_portrait_fresh',
        saturation: 12,
        contrast: 0,
        brightness: 8,
        warmth: 5,
        sharpness: 10,
        highlights: -10,
        shadows: 8,
        clarity: 12,
        noiseReduction: 8,
        skinSmooth: 20,
        reason: '清新氧气风格 · 透亮自然',
      });
    } else if (detectedScene === '风景') {
      styles.push({
        ...baseParams,
        style: 'hncs_landscape_vivid',
        saturation: 25,
        contrast: 15,
        brightness: 0,
        warmth: -5,
        sharpness: 22,
        highlights: -12,
        shadows: 8,
        clarity: 25,
        noiseReduction: 0,
        skinSmooth: 0,
        reason: '风景鲜明风格 · 浓郁饱和',
      });
      styles.push({
        ...baseParams,
        style: 'hncs_landscape_film',
        saturation: 5,
        contrast: 12,
        brightness: 0,
        warmth: 8,
        sharpness: 15,
        highlights: -8,
        shadows: 5,
        clarity: 15,
        noiseReduction: 0,
        skinSmooth: 0,
        reason: '风景胶片风格 · 复古质感',
      });
    }

    return styles;
  }
}

export const imageAnalysisService = new ImageAnalysisService();
