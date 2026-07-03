import { describe, it, expect, beforeEach } from 'vitest';
import { HeuristicSceneAnalyzer, getAnalyzer } from '../HeuristicSceneAnalyzer';

/**
 * HeuristicSceneAnalyzer 单元测试
 * 
 * 测试覆盖：
 * - 颜色分析
 * - 亮度分析
 * - 场景推断
 */

describe('HeuristicSceneAnalyzer', () => {
  let analyzer: HeuristicSceneAnalyzer;

  beforeEach(() => {
    analyzer = getAnalyzer();
  });

  describe('analyze', () => {
    it('应该返回分析结果', async () => {
      // 创建测试图像数据
      const mockImageData = new ImageData(100, 100);
      
      // 填充橙色像素（暖色调）
      for (let i = 0; i < mockImageData.data.length; i += 4) {
        mockImageData.data[i] = 255;     // R
        mockImageData.data[i + 1] = 165; // G
        mockImageData.data[i + 2] = 0;   // B
        mockImageData.data[i + 3] = 255; // A
      }

      const result = await analyzer.analyze(mockImageData);
      
      expect(result).toBeDefined();
      expect(result.primaryScene).toBeDefined();
      expect(result.confidence).toBeGreaterThanOrEqual(0);
      expect(result.confidence).toBeLessThanOrEqual(1);
      expect(result.colorProfile).toBeDefined();
    });

    it('应该正确识别暖色调图像', async () => {
      const mockImageData = new ImageData(100, 100);
      
      // 填充橙色像素（暖色调）
      for (let i = 0; i < mockImageData.data.length; i += 4) {
        mockImageData.data[i] = 255;     // R
        mockImageData.data[i + 1] = 165; // G
        mockImageData.data[i + 2] = 0;   // B
        mockImageData.data[i + 3] = 255; // A
      }

      const result = await analyzer.analyze(mockImageData);
      
      // 橙色图片应该是暖色调
      expect(result.colorProfile.warmthRatio).toBeGreaterThan(0.5);
    });

    it('应该正确识别冷色调图像', async () => {
      const mockImageData = new ImageData(100, 100);
      
      // 填充蓝色像素（冷色调）
      for (let i = 0; i < mockImageData.data.length; i += 4) {
        mockImageData.data[i] = 0;       // R
        mockImageData.data[i + 1] = 100; // G
        mockImageData.data[i + 2] = 255; // B
        mockImageData.data[i + 3] = 255; // A
      }

      const result = await analyzer.analyze(mockImageData);
      
      // 蓝色图片应该是冷色调
      expect(result.colorProfile.blueDominance).toBeGreaterThan(result.colorProfile.redDominance);
    });

    it('应该正确识别暗色调图像', async () => {
      const mockImageData = new ImageData(100, 100);
      
      // 填充暗色像素
      for (let i = 0; i < mockImageData.data.length; i += 4) {
        mockImageData.data[i] = 30;      // R
        mockImageData.data[i + 1] = 30;  // G
        mockImageData.data[i + 2] = 30;  // B
        mockImageData.data[i + 3] = 255; // A
      }

      const result = await analyzer.analyze(mockImageData);
      
      expect(result.brightnessLevel).toBeDefined();
    });
  });

  describe('getAnalyzer', () => {
    it('应该返回单例实例', () => {
      const analyzer1 = getAnalyzer();
      const analyzer2 = getAnalyzer();
      
      expect(analyzer1).toBe(analyzer2);
    });
  });
});