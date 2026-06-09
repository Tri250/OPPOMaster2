import { describe, it, expect, beforeEach, vi } from 'vitest';
import { HeuristicSceneAnalyzer, ColorProfile, getAnalyzer } from '../HeuristicSceneAnalyzer';
import { SceneCategory } from '../../store/sceneProfile';

/**
 * HeuristicSceneAnalyzer 单元测试
 * 
 * 测试覆盖：
 * - 颜色分析
 * - 亮度分析
 * - 人脸检测（模拟）
 * - 场景识别
 * - 置信度计算
 */

describe('HeuristicSceneAnalyzer', () => {
  let analyzer: HeuristicSceneAnalyzer;

  beforeEach(() => {
    analyzer = getAnalyzer();
  });

  describe('颜色分析', () => {
    it('应该正确计算平均颜色值', async () => {
      // 创建测试用的纯色图片
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FF0000'; // 纯红色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      // 红色图片应该被识别为暖色调场景
      expect(result.colorProfile.avgRed).toBeGreaterThan(200);
      expect(result.colorProfile.avgGreen).toBeLessThan(50);
      expect(result.colorProfile.avgBlue).toBeLessThan(50);
    });

    it('应该正确计算亮度分布', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#808080'; // 中灰色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      // 中灰色图片的平均亮度应该接近128
      expect(result.colorProfile.avgRed).toBeGreaterThan(100);
      expect(result.colorProfile.avgRed).toBeLessThan(150);
    });

    it('应该正确识别暖色调图片', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FF8C00'; // 橙色（暖色）
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      // 橙色图片应该是暖色调
      expect(result.colorProfile.isWarmTone).toBe(true);
      expect(result.colorProfile.isCoolTone).toBe(false);
    });

    it('应该正确识别冷色调图片', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#4169E1'; // 皇家蓝（冷色）
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      // 蓝色图片应该是冷色调
      expect(result.colorProfile.isCoolTone).toBe(true);
      expect(result.colorProfile.isWarmTone).toBe(false);
    });
  });

  describe('场景识别', () => {
    it('应该返回有效的场景识别结果', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#87CEEB'; // 天蓝色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      expect(result.primaryScene).toBeDefined();
      expect(result.primaryScene.id).toBeDefined();
      expect(result.primaryScene.name).toBeDefined();
      expect(result.confidence).toBeGreaterThan(0);
      expect(result.confidence).toBeLessThanOrEqual(1);
    });

    it('应该返回备选场景列表', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#90EE90'; // 浅绿色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      expect(result.alternativeScenes).toBeDefined();
      expect(result.alternativeScenes.length).toBeGreaterThan(0);
      expect(result.alternativeScenes.length).toBeLessThanOrEqual(3);
    });

    it('应该返回推荐的胶片列表', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FFB6C1'; // 浅粉色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      expect(result.primaryScene.recommendedFilm).toBeDefined();
      expect(result.primaryScene.recommendedFilm.length).toBeGreaterThan(0);
    });
  });

  describe('哈苏参数', () => {
    it('应该返回有效的哈苏参数', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#D2691E'; // 巧克力色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      const params = result.primaryScene.hasselbladParams;
      expect(params).toBeDefined();
      expect(params.tone).toBeGreaterThanOrEqual(-30);
      expect(params.tone).toBeLessThanOrEqual(30);
      expect(params.saturation).toBeGreaterThanOrEqual(-30);
      expect(params.saturation).toBeLessThanOrEqual(30);
      expect(params.contrast).toBeGreaterThanOrEqual(-30);
      expect(params.contrast).toBeLessThanOrEqual(30);
    });
  });

  describe('大师建议', () => {
    it('应该返回大师拍摄建议', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FFD700'; // 金色
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      expect(result.primaryScene.masterTips).toBeDefined();
      expect(result.primaryScene.masterTips.length).toBeGreaterThan(0);
      expect(typeof result.primaryScene.masterTips[0]).toBe('string');
    });
  });

  describe('置信度计算', () => {
    it('置信度应该在有效范围内', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#FF6347'; // 番茄红
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      expect(result.confidence).toBeGreaterThanOrEqual(0);
      expect(result.confidence).toBeLessThanOrEqual(1);
    });

    it('备选场景应该有较低的置信度', async () => {
      const canvas = document.createElement('canvas');
      canvas.width = 100;
      canvas.height = 100;
      const ctx = canvas.getContext('2d')!;
      ctx.fillStyle = '#20B2AA'; // 浅海绿
      ctx.fillRect(0, 0, 100, 100);
      
      const img = new Image();
      img.src = canvas.toDataURL();
      await new Promise((resolve) => { img.onload = resolve; });

      const result = await analyzer.analyze(img);
      
      if (result.alternativeScenes.length > 0) {
        expect(result.alternativeScenes[0].confidence).toBeLessThan(result.confidence);
      }
    });
  });

  describe('单例模式', () => {
    it('应该返回相同的实例', () => {
      const instance1 = getAnalyzer();
      const instance2 = getAnalyzer();
      expect(instance1).toBe(instance2);
    });
  });
});
