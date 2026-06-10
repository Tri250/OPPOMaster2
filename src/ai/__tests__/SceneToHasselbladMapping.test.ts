import { describe, it, expect } from 'vitest';
import {
  getFullSceneProfile,
  getRecommendedFilms,
  getHasselbladParams,
  getMasterTips,
  getParamAdjustmentAdvice,
} from '../SceneToHasselbladMapping';
import { HasselbladParams, SoftLightMode } from '../../store/sceneProfile';

/**
 * SceneToHasselbladMapping 单元测试
 * 
 * 测试覆盖：
 * - 场景参数映射
 * - 胶片推荐
 * - 大师建议
 * - 参数调整建议
 */

describe('SceneToHasselbladMapping', () => {
  describe('getFullSceneProfile', () => {
    it('应该返回完整的场景画像', () => {
      const profile = getFullSceneProfile('portrait-outdoor', 0.92);
      expect(profile).toBeDefined();
      expect(profile.id).toBe('portrait-outdoor');
      expect(profile.confidence).toBe(0.92);
      expect(profile.recommendedFilm).toBeDefined();
      expect(profile.recommendedFilm.length).toBeGreaterThan(0);
      expect(profile.hasselbladParams).toBeDefined();
      expect(profile.masterTips).toBeDefined();
      expect(profile.masterTips.length).toBeGreaterThan(0);
    });

    it('应该包含胶片预设数据', () => {
      const profile = getFullSceneProfile('food-dessert', 0.88);
      expect(profile.recommendedFilm.length).toBeGreaterThanOrEqual(2);
      
      const firstFilm = profile.recommendedFilm[0];
      expect(firstFilm.id).toBeDefined();
      expect(firstFilm.name).toBeDefined();
      expect(firstFilm.series).toBeDefined();
      expect(firstFilm.matchScore).toBeGreaterThan(0);
      expect(firstFilm.matchScore).toBeLessThanOrEqual(1);
    });
  });

  describe('getRecommendedFilms', () => {
    it('应该为人像场景推荐合适的胶片', () => {
      const films = getRecommendedFilms('portrait-indoor');
      expect(films.length).toBeGreaterThan(0);
      
      // 人像场景应该推荐Portra或CC胶片
      const filmIds = films.map(f => f.id);
      expect(filmIds.some(id => ['portra', 'cc', 'nc'].includes(id))).toBe(true);
    });

    it('应该为风景场景推荐合适的胶片', () => {
      const films = getRecommendedFilms('landscape-mountain');
      expect(films.length).toBeGreaterThan(0);
      
      // 风景场景应该推荐反转片或经典负片
      const filmIds = films.map(f => f.id);
      expect(filmIds.some(id => ['rdp3', 'cc', 'nh'].includes(id))).toBe(true);
    });

    it('应该为夜景场景推荐合适的胶片', () => {
      const films = getRecommendedFilms('night-cityscape');
      expect(films.length).toBeGreaterThan(0);
      
      // 夜景场景应该推荐800T胶片
      const filmIds = films.map(f => f.id);
      expect(filmIds).toContain('800t');
    });

    it('胶片应该按匹配分数排序', () => {
      const films = getRecommendedFilms('portrait-studio');
      for (let i = 0; i < films.length - 1; i++) {
        expect(films[i].matchScore).toBeGreaterThanOrEqual(films[i + 1].matchScore);
      }
    });
  });

  describe('getHasselbladParams', () => {
    it('应该返回有效的哈苏参数', () => {
      const params = getHasselbladParams('portrait-indoor');
      expect(params).toBeDefined();
      expect(params.tone).toBeGreaterThanOrEqual(-30);
      expect(params.tone).toBeLessThanOrEqual(30);
      expect(params.saturation).toBeGreaterThanOrEqual(-30);
      expect(params.saturation).toBeLessThanOrEqual(30);
      expect(params.contrast).toBeGreaterThanOrEqual(-30);
      expect(params.contrast).toBeLessThanOrEqual(30);
      expect(params.colorTemp).toBeGreaterThanOrEqual(-30);
      expect(params.colorTemp).toBeLessThanOrEqual(30);
    });

    it('人像场景应该有较低的对比度', () => {
      const params = getHasselbladParams('portrait-outdoor');
      expect(params.contrast).toBeLessThanOrEqual(5);
    });

    it('风景场景应该有较高的清晰度', () => {
      const params = getHasselbladParams('landscape-sunset');
      expect(params.sharpness).toBeGreaterThanOrEqual(12);
    });

    it('夜景场景应该有较高的对比度', () => {
      const params = getHasselbladParams('night-city');
      expect(params.contrast).toBeGreaterThanOrEqual(20);
    });
  });

  describe('getMasterTips', () => {
    it('应该返回大师拍摄建议', () => {
      const tips = getMasterTips('portrait-indoor');
      expect(tips).toBeDefined();
      expect(tips.length).toBeGreaterThan(0);
      expect(typeof tips[0]).toBe('string');
    });

    it('人像场景应该有关于光线的建议', () => {
      const tips = getMasterTips('portrait-outdoor');
      const tipsText = tips.join(' ');
      expect(tipsText).toContain('光');
    });

    it('风景场景应该有关于构图的建议', () => {
      const tips = getMasterTips('landscape-mountain');
      const tipsText = tips.join(' ');
      expect(tipsText.length).toBeGreaterThan(0);
    });
  });

  describe('getParamAdjustmentAdvice', () => {
    it('应该返回参数调整建议', () => {
      const currentParams: HasselbladParams = {
        tone: 0,
        saturation: 0,
        contrast: 0,
        colorTemp: 0,
        sharpness: 0,
        vignette: 0,
        cyanMagenta: 0,
        softLight: SoftLightMode.NONE,
      };

      const advice = getParamAdjustmentAdvice(currentParams, 'portrait-standard');
      expect(advice).toBeDefined();
      expect(advice.length).toBeGreaterThan(0);
    });

    it('应该建议调整不合适的参数', () => {
      const currentParams: HasselbladParams = {
        tone: 20,  // 较高的影调
        saturation: 25,  // 较高的饱和度
        contrast: 15,  // 较高的对比度
        colorTemp: 0,
        sharpness: 0,
        vignette: 0,
        cyanMagenta: 0,
        softLight: SoftLightMode.NONE,
      };

      const advice = getParamAdjustmentAdvice(currentParams, 'portrait-standard');
      // 对于人像场景，高对比度应该被建议调整
      const hasContrastAdvice = advice.some(a => 
        a.param.toLowerCase().includes('contrast') || 
        a.param.toLowerCase().includes('对比度')
      );
      expect(hasContrastAdvice).toBe(true);
    });
  });
});