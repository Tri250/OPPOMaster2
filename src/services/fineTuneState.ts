/**
 * AI 微调状态管理
 * 使用 useReducer + 不可变更新模式管理复杂状态
 */

import { AIFineTuneParams, DEFAULT_AI_PARAMS, InferenceStage, AIRecommendation } from './aiInferenceService';
import { AnalysisResult } from '../ai/HeuristicSceneAnalyzer';

// ============================================
// 状态类型定义
// ============================================

/**
 * HSL 调整值
 */
export interface HSLValue {
  id: string;       // red, orange, yellow, green, cyan, blue, purple, magenta
  name: string;     // 显示名称
  color: string;    // 颜色值
  hue: number;      // 色相 -180~180
  saturation: number; // 饱和度 -100~100
  luminance: number;  // 明度 -100~100
}

/**
 * 曲线控制点
 */
export interface CurvePoint {
  x: number;
  y: number;
}

/**
 * 曲线数据
 */
export interface CurveData {
  rgb: CurvePoint[];
  red: CurvePoint[];
  green: CurvePoint[];
  blue: CurvePoint[];
}

/**
 * 默认 HSL 配置
 */
export const DEFAULT_HSL_VALUES: HSLValue[] = [
  { id: 'red', name: '红色', color: '#FF0000', hue: 0, saturation: 0, luminance: 0 },
  { id: 'orange', name: '橙色', color: '#FF8000', hue: 0, saturation: 0, luminance: 0 },
  { id: 'yellow', name: '黄色', color: '#FFFF00', hue: 0, saturation: 0, luminance: 0 },
  { id: 'green', name: '绿色', color: '#00FF00', hue: 0, saturation: 0, luminance: 0 },
  { id: 'cyan', name: '青色', color: '#00FFFF', hue: 0, saturation: 0, luminance: 0 },
  { id: 'blue', name: '蓝色', color: '#0000FF', hue: 0, saturation: 0, luminance: 0 },
  { id: 'purple', name: '紫色', color: '#8000FF', hue: 0, saturation: 0, luminance: 0 },
  { id: 'magenta', name: '洋红', color: '#FF00FF', hue: 0, saturation: 0, luminance: 0 },
];

/**
 * 默认曲线数据
 */
export const DEFAULT_CURVE_DATA: CurveData = {
  rgb: [{ x: 0, y: 0 }, { x: 255, y: 255 }],
  red: [{ x: 0, y: 0 }, { x: 255, y: 255 }],
  green: [{ x: 0, y: 0 }, { x: 255, y: 255 }],
  blue: [{ x: 0, y: 0 }, { x: 255, y: 255 }],
};

/**
 * AI 微调完整状态
 */
export interface FineTuneState {
  // ========== 核心参数（18个）==========
  params: AIFineTuneParams;
  
  // ========== UI 状态 ==========
  activeTab: 'basic' | 'color' | 'smart' | 'hsl' | 'curve';
  selectedStyle: string | null;
  selectedOptimizations: string[];
  selectedHsl: string;
  selectedCurveChannel: 'rgb' | 'red' | 'green' | 'blue';
  lockedParams: string[];
  searchQuery: string;
  favorites: string[];
  
  // ========== HSL 和曲线 ==========
  hslValues: HSLValue[];
  curveData: CurveData;
  
  // ========== 处理状态 ==========
  isProcessing: boolean;
  processStage: InferenceStage;
  processMessage: string;
  processProgress: number;
  showSuccess: boolean;
  showCompare: boolean;
  
  // ========== 历史记录 ==========
  history: AIFineTuneParams[];
  maxHistorySize: number;
  
  // ========== AI 分析结果 ==========
  sceneAnalysis: AnalysisResult | null;
  confidence: number;
  recommendations: AIRecommendation[];
  
  // ========== 图像源 ==========
  imageSource: string | null;
}

/**
 * 默认状态
 */
export const DEFAULT_FINE_TUNE_STATE: FineTuneState = {
  params: DEFAULT_AI_PARAMS,
  
  activeTab: 'basic',
  selectedStyle: null,
  selectedOptimizations: [],
  selectedHsl: 'red',
  selectedCurveChannel: 'rgb',
  lockedParams: [],
  searchQuery: '',
  favorites: ['cinematic', 'moody'],
  
  hslValues: DEFAULT_HSL_VALUES,
  curveData: DEFAULT_CURVE_DATA,
  
  isProcessing: false,
  processStage: 'idle',
  processMessage: '',
  processProgress: 0,
  showSuccess: false,
  showCompare: false,
  
  history: [],
  maxHistorySize: 15,
  
  sceneAnalysis: null,
  confidence: 0,
  recommendations: [],
  
  imageSource: null,
};

// ============================================
// Action 类型定义
// ============================================

/**
 * Action 类型枚举
 */
export enum FineTuneActionType {
  // 参数调整
  SET_PARAM = 'SET_PARAM',
  SET_PARAMS = 'SET_PARAMS',
  RESET_PARAMS = 'RESET_PARAMS',
  
  // UI 状态
  SET_ACTIVE_TAB = 'SET_ACTIVE_TAB',
  SET_SELECTED_STYLE = 'SET_SELECTED_STYLE',
  TOGGLE_OPTIMIZATION = 'TOGGLE_OPTIMIZATION',
  SET_SELECTED_HSL = 'SET_SELECTED_HSL',
  SET_SELECTED_CURVE_CHANNEL = 'SET_SELECTED_CURVE_CHANNEL',
  TOGGLE_LOCK_PARAM = 'TOGGLE_LOCK_PARAM',
  SET_SEARCH_QUERY = 'SET_SEARCH_QUERY',
  TOGGLE_FAVORITE = 'TOGGLE_FAVORITE',
  SET_SHOW_COMPARE = 'SET_SHOW_COMPARE',
  
  // HSL 和曲线
  SET_HSL_VALUE = 'SET_HSL_VALUE',
  RESET_HSL = 'RESET_HSL',
  SET_CURVE_POINTS = 'SET_CURVE_POINTS',
  RESET_CURVE = 'RESET_CURVE',
  
  // 处理状态
  START_PROCESSING = 'START_PROCESSING',
  UPDATE_PROCESSING = 'UPDATE_PROCESSING',
  COMPLETE_PROCESSING = 'COMPLETE_PROCESSING',
  ERROR_PROCESSING = 'ERROR_PROCESSING',
  SET_SHOW_SUCCESS = 'SET_SHOW_SUCCESS',
  
  // 历史记录
  PUSH_HISTORY = 'PUSH_HISTORY',
  UNDO = 'UNDO',
  CLEAR_HISTORY = 'CLEAR_HISTORY',
  
  // AI 结果
  SET_SCENE_ANALYSIS = 'SET_SCENE_ANALYSIS',
  SET_RECOMMENDATIONS = 'SET_RECOMMENDATIONS',
  
  // 图像源
  SET_IMAGE_SOURCE = 'SET_IMAGE_SOURCE',
  
  // 重置
  RESET_ALL = 'RESET_ALL',
}

/**
 * Action 类型定义
 */
export type FineTuneAction =
  // 参数调整
  | { type: FineTuneActionType.SET_PARAM; key: keyof AIFineTuneParams; value: number }
  | { type: FineTuneActionType.SET_PARAMS; params: Partial<AIFineTuneParams> }
  | { type: FineTuneActionType.RESET_PARAMS }
  
  // UI 状态
  | { type: FineTuneActionType.SET_ACTIVE_TAB; tab: FineTuneState['activeTab'] }
  | { type: FineTuneActionType.SET_SELECTED_STYLE; styleId: string | null }
  | { type: FineTuneActionType.TOGGLE_OPTIMIZATION; optimizationId: string }
  | { type: FineTuneActionType.SET_SELECTED_HSL; hslId: string }
  | { type: FineTuneActionType.SET_SELECTED_CURVE_CHANNEL; channel: FineTuneState['selectedCurveChannel'] }
  | { type: FineTuneActionType.TOGGLE_LOCK_PARAM; paramKey: string }
  | { type: FineTuneActionType.SET_SEARCH_QUERY; query: string }
  | { type: FineTuneActionType.TOGGLE_FAVORITE; styleId: string }
  | { type: FineTuneActionType.SET_SHOW_COMPARE; show: boolean }
  
  // HSL 和曲线
  | { type: FineTuneActionType.SET_HSL_VALUE; hslId: string; field: 'hue' | 'saturation' | 'luminance'; value: number }
  | { type: FineTuneActionType.RESET_HSL }
  | { type: FineTuneActionType.SET_CURVE_POINTS; channel: keyof CurveData; points: CurvePoint[] }
  | { type: FineTuneActionType.RESET_CURVE }
  
  // 处理状态
  | { type: FineTuneActionType.START_PROCESSING }
  | { type: FineTuneActionType.UPDATE_PROCESSING; stage: InferenceStage; progress: number; message: string }
  | { type: FineTuneActionType.COMPLETE_PROCESSING; params: AIFineTuneParams; sceneAnalysis?: AnalysisResult; recommendations?: AIRecommendation[] }
  | { type: FineTuneActionType.ERROR_PROCESSING; error: string }
  | { type: FineTuneActionType.SET_SHOW_SUCCESS; show: boolean }
  
  // 历史记录
  | { type: FineTuneActionType.PUSH_HISTORY; params: AIFineTuneParams }
  | { type: FineTuneActionType.UNDO }
  | { type: FineTuneActionType.CLEAR_HISTORY }
  
  // AI 结果
  | { type: FineTuneActionType.SET_SCENE_ANALYSIS; analysis: AnalysisResult | null }
  | { type: FineTuneActionType.SET_RECOMMENDATIONS; recommendations: AIRecommendation[] }
  
  // 图像源
  | { type: FineTuneActionType.SET_IMAGE_SOURCE; source: string | null }
  
  // 重置
  | { type: FineTuneActionType.RESET_ALL };

// ============================================
// Reducer 函数
// ============================================

/**
 * AI 微调 Reducer
 * 
 * 使用不可变更新模式处理状态变化
 */
export function fineTuneReducer(
  state: FineTuneState,
  action: FineTuneAction
): FineTuneState {
  switch (action.type) {
    // ========== 参数调整 ==========
    
    case FineTuneActionType.SET_PARAM: {
      // 如果参数被锁定，不更新
      if (state.lockedParams.includes(action.key)) {
        return state;
      }
      return {
        ...state,
        params: {
          ...state.params,
          [action.key]: action.value,
        },
      };
    }
    
    case FineTuneActionType.SET_PARAMS: {
      // 过滤掉被锁定的参数
      const unlockedParams: Partial<AIFineTuneParams> = {};
      for (const [key, value] of Object.entries(action.params)) {
        if (!state.lockedParams.includes(key)) {
          unlockedParams[key as keyof AIFineTuneParams] = value as number;
        }
      }
      return {
        ...state,
        params: {
          ...state.params,
          ...unlockedParams,
        },
      };
    }
    
    case FineTuneActionType.RESET_PARAMS: {
      return {
        ...state,
        params: DEFAULT_AI_PARAMS,
        selectedStyle: null,
        selectedOptimizations: [],
        hslValues: DEFAULT_HSL_VALUES,
        curveData: DEFAULT_CURVE_DATA,
      };
    }
    
    // ========== UI 状态 ==========
    
    case FineTuneActionType.SET_ACTIVE_TAB: {
      return {
        ...state,
        activeTab: action.tab,
      };
    }
    
    case FineTuneActionType.SET_SELECTED_STYLE: {
      return {
        ...state,
        selectedStyle: action.styleId,
      };
    }
    
    case FineTuneActionType.TOGGLE_OPTIMIZATION: {
      const optimizations = state.selectedOptimizations.includes(action.optimizationId)
        ? state.selectedOptimizations.filter(id => id !== action.optimizationId)
        : [...state.selectedOptimizations, action.optimizationId];
      return {
        ...state,
        selectedOptimizations: optimizations,
      };
    }
    
    case FineTuneActionType.SET_SELECTED_HSL: {
      return {
        ...state,
        selectedHsl: action.hslId,
      };
    }
    
    case FineTuneActionType.SET_SELECTED_CURVE_CHANNEL: {
      return {
        ...state,
        selectedCurveChannel: action.channel,
      };
    }
    
    case FineTuneActionType.TOGGLE_LOCK_PARAM: {
      const locked = state.lockedParams.includes(action.paramKey)
        ? state.lockedParams.filter(key => key !== action.paramKey)
        : [...state.lockedParams, action.paramKey];
      return {
        ...state,
        lockedParams: locked,
      };
    }
    
    case FineTuneActionType.SET_SEARCH_QUERY: {
      return {
        ...state,
        searchQuery: action.query,
      };
    }
    
    case FineTuneActionType.TOGGLE_FAVORITE: {
      const favorites = state.favorites.includes(action.styleId)
        ? state.favorites.filter(id => id !== action.styleId)
        : [...state.favorites, action.styleId];
      return {
        ...state,
        favorites: favorites,
      };
    }
    
    case FineTuneActionType.SET_SHOW_COMPARE: {
      return {
        ...state,
        showCompare: action.show,
      };
    }
    
    // ========== HSL 和曲线 ==========
    
    case FineTuneActionType.SET_HSL_VALUE: {
      return {
        ...state,
        hslValues: state.hslValues.map(hsl =>
          hsl.id === action.hslId
            ? { ...hsl, [action.field]: action.value }
            : hsl
        ),
      };
    }
    
    case FineTuneActionType.RESET_HSL: {
      return {
        ...state,
        hslValues: DEFAULT_HSL_VALUES,
      };
    }
    
    case FineTuneActionType.SET_CURVE_POINTS: {
      return {
        ...state,
        curveData: {
          ...state.curveData,
          [action.channel]: action.points,
        },
      };
    }
    
    case FineTuneActionType.RESET_CURVE: {
      return {
        ...state,
        curveData: DEFAULT_CURVE_DATA,
      };
    }
    
    // ========== 处理状态 ==========
    
    case FineTuneActionType.START_PROCESSING: {
      return {
        ...state,
        isProcessing: true,
        processStage: 'analyzing',
        processMessage: '开始分析...',
        processProgress: 0,
        showSuccess: false,
      };
    }
    
    case FineTuneActionType.UPDATE_PROCESSING: {
      return {
        ...state,
        processStage: action.stage,
        processProgress: action.progress,
        processMessage: action.message,
      };
    }
    
    case FineTuneActionType.COMPLETE_PROCESSING: {
      // 将当前参数保存到历史记录
      const newHistory = [state.params, ...state.history].slice(0, state.maxHistorySize);
      
      return {
        ...state,
        isProcessing: false,
        processStage: 'completed',
        processProgress: 100,
        processMessage: '优化完成',
        showSuccess: true,
        params: action.params,
        history: newHistory,
        sceneAnalysis: action.sceneAnalysis ?? null,
        confidence: action.sceneAnalysis?.confidence ?? 0,
        recommendations: action.recommendations ?? [],
      };
    }
    
    case FineTuneActionType.ERROR_PROCESSING: {
      return {
        ...state,
        isProcessing: false,
        processStage: 'error',
        processProgress: 0,
        processMessage: action.error,
      };
    }
    
    case FineTuneActionType.SET_SHOW_SUCCESS: {
      return {
        ...state,
        showSuccess: action.show,
      };
    }
    
    // ========== 历史记录 ==========
    
    case FineTuneActionType.PUSH_HISTORY: {
      const newHistory = [action.params, ...state.history].slice(0, state.maxHistorySize);
      return {
        ...state,
        history: newHistory,
      };
    }
    
    case FineTuneActionType.UNDO: {
      if (state.history.length === 0) {
        return state;
      }
      const [previousParams, ...remainingHistory] = state.history;
      return {
        ...state,
        params: previousParams,
        history: remainingHistory,
      };
    }
    
    case FineTuneActionType.CLEAR_HISTORY: {
      return {
        ...state,
        history: [],
      };
    }
    
    // ========== AI 结果 ==========
    
    case FineTuneActionType.SET_SCENE_ANALYSIS: {
      return {
        ...state,
        sceneAnalysis: action.analysis,
        confidence: action.analysis?.confidence ?? 0,
      };
    }
    
    case FineTuneActionType.SET_RECOMMENDATIONS: {
      return {
        ...state,
        recommendations: action.recommendations,
      };
    }
    
    // ========== 图像源 ==========
    
    case FineTuneActionType.SET_IMAGE_SOURCE: {
      return {
        ...state,
        imageSource: action.source,
      };
    }
    
    // ========== 重置 ==========
    
    case FineTuneActionType.RESET_ALL: {
      return {
        ...DEFAULT_FINE_TUNE_STATE,
        favorites: state.favorites, // 保留收藏
        imageSource: state.imageSource, // 保留图像源
      };
    }
    
    default: {
      return state;
    }
  }
}

// ============================================
// 辅助函数
// ============================================

/**
 * 创建初始状态（可自定义图像源）
 */
export function createInitialState(imageSource?: string): FineTuneState {
  return {
    ...DEFAULT_FINE_TUNE_STATE,
    imageSource: imageSource ?? null,
  };
}

/**
 * 检查是否有历史记录可撤销
 */
export function canUndo(state: FineTuneState): boolean {
  return state.history.length > 0;
}

/**
 * 检查是否正在处理
 */
export function isProcessing(state: FineTuneState): boolean {
  return state.isProcessing;
}

/**
 * 获取当前参数的非零值列表
 */
export function getNonZeroParams(state: FineTuneState): Array<{ key: string; value: number }> {
  const result: Array<{ key: string; value: number }> = [];
  for (const [key, value] of Object.entries(state.params)) {
    if (value !== 0) {
      result.push({ key, value });
    }
  }
  return result;
}