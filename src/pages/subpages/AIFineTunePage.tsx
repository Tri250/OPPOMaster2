import React, { useReducer, useCallback, useMemo, useEffect, useRef, useState } from 'react';
import { useAppStore } from '../../store/appStore';
import { 
  ArrowLeft, RefreshCw, Check, Wand2, Sparkles, Sun, Moon, Palette, 
  Camera, Aperture, Zap, Eye, Contrast, Droplets, Layers, Sliders,
  Target, TrendingUp, Circle, Brush, RotateCcw, Heart, Crown, Search,
  Upload, Download, Share2, Image, FileImage, X
} from 'lucide-react';

// 导入状态管理和 AI 推理服务
import {
  FineTuneState,
  FineTuneActionType,
  fineTuneReducer,
  createInitialState,
  canUndo,
} from '../../services/fineTuneState';
import {
  performAutoTune,
  AIInferenceRequest,
} from '../../services/aiInferenceService';
import { AIFineTuneParams } from '../../services/aiInferenceService';

// ============================================
// 常量配置
// ============================================

// 12+ 色彩风格预设
const COLOR_STYLES = [
  { id: 'natural', name: '自然', icon: Sun, color: '#4CAF50', params: { saturation: 5, contrast: 5, warmth: 0, vibrance: 5 }, desc: '自然真实色彩' },
  { id: 'vivid', name: '鲜艳', icon: Palette, color: '#FF5722', params: { saturation: 25, contrast: 15, warmth: 5, vibrance: 20 }, desc: '浓郁鲜艳色彩' },
  { id: 'warm', name: '暖调', icon: Sun, color: '#FF9800', params: { saturation: 10, contrast: 8, warmth: 20, vibrance: 10 }, desc: '温暖阳光感' },
  { id: 'cool', name: '冷调', icon: Moon, color: '#2196F3', params: { saturation: 8, contrast: 10, warmth: -20, vibrance: 8 }, desc: '清冷高级感' },
  { id: 'film', name: '胶片', icon: Camera, color: '#795548', params: { saturation: -10, contrast: 15, warmth: 5, grain: 15, fade: 10 }, desc: '经典胶片质感' },
  { id: 'bw', name: '黑白', icon: Contrast, color: '#9E9E9E', params: { saturation: -100, contrast: 20, warmth: 0, clarity: 15 }, desc: '经典黑白摄影' },
  { id: 'vintage', name: '复古', icon: Layers, color: '#8D6E63', params: { saturation: -15, contrast: 5, warmth: 15, fade: 20, grain: 10 }, desc: '怀旧复古风格' },
  { id: 'cinematic', name: '电影', icon: Aperture, color: '#607D8B', params: { saturation: 5, contrast: 25, warmth: 10 }, desc: '电影大片感' },
  { id: 'moody', name: '情绪', icon: Moon, color: '#3F51B5', params: { saturation: -5, contrast: 30, warmth: -10, shadows: 20, highlights: -15 }, desc: '情绪氛围感' },
  { id: 'pastel', name: '柔和', icon: Brush, color: '#E1BEE7', params: { saturation: -10, contrast: -10, warmth: 5, brightness: 10, fade: 15 }, desc: '柔和粉彩风' },
  { id: 'dramatic', name: '戏剧', icon: Zap, color: '#FF5722', params: { saturation: 15, contrast: 35, warmth: 5, clarity: 20, highlights: -20 }, desc: '戏剧性光影' },
  { id: 'hdr', name: 'HDR', icon: TrendingUp, color: '#00BCD4', params: { saturation: 10, contrast: 20, warmth: 0, highlights: -30, shadows: 30, clarity: 25 }, desc: '高动态范围' },
];

// 10+ 智能优化选项
const SMART_OPTIMIZATIONS = [
  { id: 'hdr', name: 'HDR 增强', icon: Zap, desc: '扩展动态范围，保留更多细节', color: '#FF6B35', pro: false },
  { id: 'denoise', name: '智能降噪', icon: Droplets, desc: '减少噪点，保持细节', color: '#4CAF50', pro: false },
  { id: 'sharpen', name: '智能锐化', icon: Eye, desc: '增强边缘清晰度', color: '#2196F3', pro: false },
  { id: 'dehaze', name: '去雾', icon: Sun, desc: '去除雾气，提升通透感', color: '#9C27B0', pro: false },
  { id: 'skin', name: '肤色优化', icon: Sparkles, desc: '智能美化肤色', color: '#E91E63', pro: false },
  { id: 'sky', name: '天空增强', icon: Moon, desc: '增强天空色彩和细节', color: '#00BCD4', pro: true },
  { id: 'ai-composition', name: 'AI构图', icon: Target, desc: '智能裁剪优化构图', color: '#FF9800', pro: true },
  { id: 'portrait-bokeh', name: '人像虚化', icon: Circle, desc: '模拟大光圈虚化效果', color: '#795548', pro: true },
  { id: 'color-match', name: '色彩匹配', icon: Palette, desc: '匹配参考图色彩风格', color: '#607D8B', pro: true },
  { id: 'smart-light', name: '智能补光', icon: Sun, desc: 'AI分析并补光阴影区域', color: '#FFEB3B', pro: true },
];

// 基础预设
const BASE_PRESETS = [
  { id: 'portrait', name: '人像优化', params: { saturation: 8, contrast: 10, warmth: 3, sharpness: 18, skinSmooth: 25 }, icon: Target },
  { id: 'landscape', name: '风景增强', params: { saturation: 15, contrast: 12, warmth: 5, sharpness: 22, clarity: 15, dehaze: 10 }, icon: Sun },
  { id: 'night', name: '夜景优化', params: { saturation: 5, contrast: 20, warmth: 10, sharpness: 25, denoise: 20 }, icon: Moon },
  { id: 'food', name: '美食鲜艳', params: { saturation: 25, contrast: 8, warmth: 12, sharpness: 30, brightness: 5 }, icon: Palette },
  { id: 'street', name: '街拍胶片', params: { saturation: 5, contrast: 15, warmth: 0, sharpness: 20, grain: 12 }, icon: Camera },
  { id: 'soft', name: '柔和清新', params: { saturation: 10, contrast: -5, warmth: 8, sharpness: 10, fade: 5 }, icon: Brush },
];

// 曲线预设
const CURVE_PRESETS = [
  { id: 'linear', name: '线性', points: [[0, 0], [255, 255]] },
  { id: 'contrast', name: '高对比', points: [[0, 0], [64, 32], [192, 223], [255, 255]] },
  { id: 'soft', name: '柔和', points: [[0, 0], [64, 48], [192, 207], [255, 255]] },
  { id: 's-curve', name: 'S曲线', points: [[0, 0], [64, 40], [128, 128], [192, 215], [255, 255]] },
  { id: 'invert', name: '反相', points: [[0, 255], [255, 0]] },
];

// 默认图像源
const DEFAULT_IMAGE_SOURCE = 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=600';

// ============================================
// 主组件
// ============================================

const AIFineTunePage: React.FC = () => {
  const { goBack } = useAppStore();
  
  // 使用 useReducer 管理所有状态
  const [state, dispatch] = useReducer(
    fineTuneReducer,
    createInitialState(DEFAULT_IMAGE_SOURCE)
  );
  
  // 成功提示自动隐藏定时器
  const successTimerRef = useRef<NodeJS.Timeout | null>(null);
  
  // 成功提示自动隐藏
  useEffect(() => {
    if (state.showSuccess) {
      successTimerRef.current = setTimeout(() => {
        dispatch({ type: FineTuneActionType.SET_SHOW_SUCCESS, show: false });
      }, 2000);
    }
    return () => {
      if (successTimerRef.current) {
        clearTimeout(successTimerRef.current);
      }
    };
  }, [state.showSuccess]);
  
  // ========== 计算属性 ==========
  
  // 过滤色彩风格
  const filteredStyles = useMemo(() => {
    if (!state.searchQuery) return COLOR_STYLES;
    const q = state.searchQuery.toLowerCase();
    return COLOR_STYLES.filter(s => 
      s.name.toLowerCase().includes(q) || 
      s.desc.toLowerCase().includes(q)
    );
  }, [state.searchQuery]);
  
  // ========== 操作函数 ==========
  
  // 更新单个参数
  const updateParam = useCallback((key: keyof AIFineTuneParams, value: number) => {
    dispatch({ type: FineTuneActionType.SET_PARAM, key, value });
  }, []);
  
  // 切换参数锁定
  const toggleLock = useCallback((key: string) => {
    dispatch({ type: FineTuneActionType.TOGGLE_LOCK_PARAM, paramKey: key });
  }, []);
  
  // 应用色彩风格
  const applyColorStyle = useCallback((style: typeof COLOR_STYLES[0]) => {
    // 先保存当前参数到历史
    dispatch({ type: FineTuneActionType.PUSH_HISTORY, params: state.params });
    // 应用风格参数
    dispatch({ type: FineTuneActionType.SET_PARAMS, params: style.params });
    // 设置选中风格
    dispatch({ type: FineTuneActionType.SET_SELECTED_STYLE, styleId: style.id });
  }, [state.params]);
  
  // 切换智能优化
  const toggleOptimization = useCallback((id: string) => {
    dispatch({ type: FineTuneActionType.TOGGLE_OPTIMIZATION, optimizationId: id });
  }, []);
  
  // AI 一键微调（真实 AI 推理）
  const handleAutoTune = useCallback(async () => {
    // 开始处理
    dispatch({ type: FineTuneActionType.START_PROCESSING });
    
    try {
      // 构建推理请求
      const request: AIInferenceRequest = {
        imageSource: state.imageSource || DEFAULT_IMAGE_SOURCE,
        currentParams: state.params,
        optimizations: state.selectedOptimizations,
        styleId: state.selectedStyle,
      };
      
      // 执行真实 AI 推理
      const response = await performAutoTune(request, (stage, progress, message) => {
        // 更新处理进度
        dispatch({
          type: FineTuneActionType.UPDATE_PROCESSING,
          stage,
          progress,
          message,
        });
      });
      
      if (response.success) {
        // 完成处理，应用 AI 计算的参数
        dispatch({
          type: FineTuneActionType.COMPLETE_PROCESSING,
          params: response.params,
          sceneAnalysis: response.sceneAnalysis,
          recommendations: response.recommendations,
        });
      } else {
        // 处理失败
        dispatch({
          type: FineTuneActionType.ERROR_PROCESSING,
          error: response.error || 'AI 推理失败',
        });
      }
      
    } catch (error) {
      // 异常处理
      dispatch({
        type: FineTuneActionType.ERROR_PROCESSING,
        error: error instanceof Error ? error.message : '未知错误',
      });
    }
  }, [state.imageSource, state.params, state.selectedOptimizations, state.selectedStyle]);
  
  // 应用基础预设
  const applyPreset = useCallback((preset: typeof BASE_PRESETS[0]) => {
    // 保存当前参数到历史
    dispatch({ type: FineTuneActionType.PUSH_HISTORY, params: state.params });
    // 应用预设参数
    dispatch({ type: FineTuneActionType.SET_PARAMS, params: preset.params });
  }, [state.params]);
  
  // 重置所有参数
  const handleReset = useCallback(() => {
    dispatch({ type: FineTuneActionType.RESET_PARAMS });
  }, []);
  
  // 撤销操作
  const handleUndo = useCallback(() => {
    dispatch({ type: FineTuneActionType.UNDO });
  }, []);
  
  // 切换收藏
  const toggleFavorite = useCallback((id: string) => {
    dispatch({ type: FineTuneActionType.TOGGLE_FAVORITE, styleId: id });
  }, []);
  
  // 切换对比模式
  const toggleCompare = useCallback(() => {
    dispatch({ type: FineTuneActionType.SET_SHOW_COMPARE, show: !state.showCompare });
  }, [state.showCompare]);
  
  // 切换 Tab
  const setActiveTab = useCallback((tab: FineTuneState['activeTab']) => {
    dispatch({ type: FineTuneActionType.SET_ACTIVE_TAB, tab });
  }, []);
  
  // 更新搜索查询
  const setSearchQuery = useCallback((query: string) => {
    dispatch({ type: FineTuneActionType.SET_SEARCH_QUERY, query });
  }, []);
  
  // 更新 HSL 值
  const updateHslValue = useCallback((hslId: string, field: 'hue' | 'saturation' | 'luminance', value: number) => {
    dispatch({ type: FineTuneActionType.SET_HSL_VALUE, hslId, field, value });
  }, []);
  
  // 设置选中的 HSL 颜色
  const setSelectedHsl = useCallback((hslId: string) => {
    dispatch({ type: FineTuneActionType.SET_SELECTED_HSL, hslId });
  }, []);
  
  // 重置 HSL
  const resetHsl = useCallback(() => {
    dispatch({ type: FineTuneActionType.RESET_HSL });
  }, []);
  
  // 应用智能优化
  const applySmartOptimizations = useCallback(() => {
    // 保存当前参数到历史
    dispatch({ type: FineTuneActionType.PUSH_HISTORY, params: state.params });
    
    // 根据选中的优化项计算新参数
    const newParams: Partial<AIFineTuneParams> = {};
    
    if (state.selectedOptimizations.includes('hdr')) {
      newParams.highlights = -15;
      newParams.shadows = 20;
      newParams.clarity = 20;
    }
    if (state.selectedOptimizations.includes('denoise')) {
      newParams.denoise = 25;
    }
    if (state.selectedOptimizations.includes('sharpen')) {
      newParams.sharpness = 30;
    }
    if (state.selectedOptimizations.includes('dehaze')) {
      newParams.dehaze = 20;
    }
    if (state.selectedOptimizations.includes('skin')) {
      newParams.skinSmooth = 30;
    }
    if (state.selectedOptimizations.includes('sky')) {
      newParams.saturation = 20;
      newParams.vibrance = 15;
    }
    
    // 应用新参数
    dispatch({ type: FineTuneActionType.SET_PARAMS, params: newParams });
  }, [state.selectedOptimizations, state.params]);
  
  // ========== 拖拽上传和导出功能 ==========
  
  // 拖拽上传状态
  const [isDragging, setIsDragging] = useState(false);
  const [showExportMenu, setShowExportMenu] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // 处理拖拽进入
  const handleDragEnter = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(true);
  }, []);
  
  // 处理拖拽离开
  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
  }, []);
  
  // 处理拖拽悬停
  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  }, []);
  
  // 处理拖拽放置
  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setIsDragging(false);
    
    const files = e.dataTransfer.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            dispatch({ 
              type: FineTuneActionType.SET_IMAGE_SOURCE, 
              source: event.target.result as string 
            });
          }
        };
        reader.readAsDataURL(file);
      }
    }
  }, []);
  
  // 处理文件选择
  const handleFileSelect = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (file.type.startsWith('image/')) {
        const reader = new FileReader();
        reader.onload = (event) => {
          if (event.target?.result) {
            dispatch({ 
              type: FineTuneActionType.SET_IMAGE_SOURCE, 
              source: event.target.result as string 
            });
          }
        };
        reader.readAsDataURL(file);
      }
    }
  }, []);
  
  // 导出图片
  const handleExport = useCallback((format: 'png' | 'jpg' | 'webp') => {
    const canvas = document.createElement('canvas');
    const img = new window.Image();
    img.crossOrigin = 'anonymous';
    img.src = state.imageSource || DEFAULT_IMAGE_SOURCE;
    
    img.onload = () => {
      canvas.width = img.width;
      canvas.height = img.height;
      const ctx = canvas.getContext('2d');
      if (ctx) {
        // 应用滤镜效果
        ctx.filter = `
          saturate(${100 + state.params.saturation}%) 
          contrast(${100 + state.params.contrast}%) 
          brightness(${100 + state.params.brightness}%)
          sepia(${state.params.warmth > 0 ? state.params.warmth * 0.5 : 0}%)
          hue-rotate(${state.params.warmth < 0 ? state.params.warmth * 0.5 : 0}deg)
        `;
        ctx.drawImage(img, 0, 0);
        
        // 导出
        const mimeType = format === 'png' ? 'image/png' : format === 'jpg' ? 'image/jpeg' : 'image/webp';
        const quality = format === 'jpg' ? 0.9 : 1;
        const dataUrl = canvas.toDataURL(mimeType, quality);
        
        // 下载
        const link = document.createElement('a');
        link.href = dataUrl;
        link.download = `omaster_finetune_${Date.now()}.${format}`;
        link.click();
      }
    };
    
    setShowExportMenu(false);
  }, [state.imageSource, state.params]);
  
  // 分享
  const handleShare = useCallback(async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'OMaster AI 微调效果',
          text: '看看我用 OMaster AI 微调的效果！',
        });
      } catch (err) {
        console.log('分享失败:', err);
      }
    }
  }, []);
  
  // ========== 参数配置 ==========
  
  // 基础参数配置
  const basicParams = [
    { key: 'exposure', label: '曝光', min: -100, max: 100 },
    { key: 'brightness', label: '亮度', min: -100, max: 100 },
    { key: 'contrast', label: '对比度', min: -100, max: 100 },
    { key: 'saturation', label: '饱和度', min: -100, max: 100 },
    { key: 'warmth', label: '色温', min: -100, max: 100 },
    { key: 'vibrance', label: '自然饱和度', min: -100, max: 100 },
  ];
  
  // 专业参数配置
  const proParams = [
    { key: 'highlights', label: '高光', min: -100, max: 100 },
    { key: 'shadows', label: '阴影', min: -100, max: 100 },
    { key: 'whites', label: '白色色阶', min: -100, max: 100 },
    { key: 'blacks', label: '黑色色阶', min: -100, max: 100 },
    { key: 'texture', label: '纹理', min: -100, max: 100 },
    { key: 'clarity', label: '清晰度', min: -100, max: 100 },
  ];
  
  // 效果参数配置
  const effectParams = [
    { key: 'sharpness', label: '锐度', min: 0, max: 100 },
    { key: 'dehaze', label: '去雾', min: 0, max: 100 },
    { key: 'denoise', label: '降噪', min: 0, max: 100 },
    { key: 'grain', label: '颗粒', min: 0, max: 100 },
    { key: 'fade', label: '褪色', min: 0, max: 100 },
    { key: 'skinSmooth', label: '肤色平滑', min: 0, max: 100 },
  ];
  
  // ========== 渲染 ==========
  
  return (
    <div className="min-h-screen bg-[#0a0a0a] text-white">
      {/* Header */}
      <div className="sticky top-0 z-50 bg-[#0a0a0a]/95 backdrop-blur-sm border-b border-white/5">
        <div className="flex items-center justify-between px-4 py-3">
          <div className="flex items-center gap-3">
            <button onClick={goBack} className="p-2 -ml-2 rounded-full hover:bg-white/10">
              <ArrowLeft size={20} className="text-white" />
            </button>
            <div>
              <h1 className="text-lg font-bold">AI 微调</h1>
              <p className="text-xs text-white/50">专业色彩优化引擎 v4.0</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {/* 上传按钮 */}
            <button 
              onClick={() => fileInputRef.current?.click()}
              className="p-2 rounded-full hover:bg-white/10"
              title="上传图片"
            >
              <Upload size={18} className="text-white/50" />
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              onChange={handleFileSelect}
              className="hidden"
            />
            {/* 导出按钮 */}
            <div className="relative">
              <button 
                onClick={() => setShowExportMenu(!showExportMenu)}
                className="p-2 rounded-full hover:bg-white/10"
                title="导出图片"
              >
                <Download size={18} className="text-white/50" />
              </button>
              {/* 导出菜单 */}
              {showExportMenu && (
                <div className="absolute right-0 top-full mt-2 bg-[#1a1a1a] rounded-xl border border-white/10 shadow-lg z-50">
                  <div className="p-2 space-y-1">
                    <button
                      onClick={() => handleExport('png')}
                      className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                    >
                      <FileImage size={14} />
                      PNG (无损)
                    </button>
                    <button
                      onClick={() => handleExport('jpg')}
                      className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                    >
                      <FileImage size={14} />
                      JPG (高质量)
                    </button>
                    <button
                      onClick={() => handleExport('webp')}
                      className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                    >
                      <FileImage size={14} />
                      WebP (现代格式)
                    </button>
                  </div>
                </div>
              )}
            </div>
            {/* 分享按钮 */}
            <button 
              onClick={handleShare}
              className="p-2 rounded-full hover:bg-white/10"
              title="分享"
            >
              <Share2 size={18} className="text-white/50" />
            </button>
            {/* 撤销按钮 */}
            {canUndo(state) && (
              <button onClick={handleUndo} className="p-2 rounded-full hover:bg-white/10" title="撤销">
                <RefreshCw size={18} className="text-white/50" />
              </button>
            )}
            {/* 对比按钮 */}
            <button 
              onClick={toggleCompare}
              className={`p-2 rounded-full ${state.showCompare ? 'bg-[#9C27B0]/20' : 'hover:bg-white/10'}`}
              title="对比"
            >
              <Eye size={18} className={state.showCompare ? 'text-[#9C27B0]' : 'text-white/50'} />
            </button>
            {/* 版本徽章 */}
            <div className="px-2 py-1 rounded-full bg-gradient-to-r from-[#9C27B0] to-[#673AB7] text-white text-xs font-bold">
              v4.0 Pro
            </div>
          </div>
        </div>
      </div>

      {/* Preview Area */}
      <div className="px-4 py-4">
        <div 
          className="relative aspect-video rounded-2xl overflow-hidden bg-[#1a1a1a]"
          onDragEnter={handleDragEnter}
          onDragLeave={handleDragLeave}
          onDragOver={handleDragOver}
          onDrop={handleDrop}
        >
          {/* 拖拽上传提示 */}
          {isDragging && (
            <div className="absolute inset-0 bg-[#9C27B0]/20 border-2 border-[#9C27B0] rounded-2xl flex flex-col items-center justify-center z-50">
              <Upload size={48} className="text-[#9C27B0] mb-4" />
              <span className="text-white text-lg font-medium">拖拽图片到这里</span>
              <span className="text-white/50 text-sm mt-2">支持 JPG、PNG、WebP 格式</span>
            </div>
          )}
          
          {/* 预览图像 */}
          <img 
            src={state.imageSource || DEFAULT_IMAGE_SOURCE}
            alt="Preview"
            className="w-full h-full object-cover"
            style={{
              filter: `
                saturate(${100 + state.params.saturation}%) 
                contrast(${100 + state.params.contrast}%) 
                brightness(${100 + state.params.brightness}%)
                sepia(${state.params.warmth > 0 ? state.params.warmth * 0.5 : 0}%)
                hue-rotate(${state.params.warmth < 0 ? state.params.warmth * 0.5 : 0}deg)
              `,
            }}
          />
          {/* 渐变遮罩 */}
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />

          {/* Compare Mode */}
          {state.showCompare && (
            <div className="absolute inset-0 flex">
              <div className="w-1/2 border-r-2 border-white overflow-hidden">
                <img 
                  src={state.imageSource || DEFAULT_IMAGE_SOURCE}
                  alt="Original"
                  className="w-full h-full object-cover"
                />
                <div className="absolute bottom-2 left-2 px-2 py-1 rounded bg-black/50 text-xs">原图</div>
              </div>
              <div className="w-1/2 overflow-hidden">
                <div className="absolute bottom-2 right-2 px-2 py-1 rounded bg-black/50 text-xs">效果</div>
              </div>
            </div>
          )}
          
          {/* Processing Overlay */}
          {state.isProcessing && (
            <div className="absolute inset-0 bg-black/80 flex items-center justify-center">
              <div className="flex flex-col items-center gap-4">
                {/* 动画圆环 */}
                <div className="relative w-20 h-20">
                  <div className="absolute inset-0 rounded-full border-4 border-white/20" />
                  <div className="absolute inset-0 rounded-full border-4 border-transparent border-t-[#9C27B0] animate-spin" />
                  <div className="absolute inset-2 rounded-full border-4 border-transparent border-b-[#673AB7] animate-spin" style={{ animationDirection: 'reverse' }} />
                  <Wand2 size={28} className="absolute inset-0 m-auto text-[#9C27B0]" />
                </div>
                {/* 处理信息 */}
                <div className="text-center">
                  <span className="text-white text-sm font-medium">{state.processMessage}</span>
                  {/* 进度指示器 */}
                  <div className="mt-2 flex gap-1">
                    {[...Array(5)].map((_, i) => (
                      <div 
                        key={i}
                        className="w-2 h-2 rounded-full bg-[#9C27B0] animate-pulse"
                        style={{ animationDelay: `${i * 0.1}s` }}
                      />
                    ))}
                  </div>
                  {/* 进度条 */}
                  <div className="mt-3 w-48 h-1 bg-white/10 rounded-full overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-[#9C27B0] to-[#673AB7] transition-all duration-300"
                      style={{ width: `${state.processProgress * 100}%` }}
                    />
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* Success Overlay */}
          {state.showSuccess && (
            <div className="absolute inset-0 bg-black/70 flex items-center justify-center">
              <div className="flex flex-col items-center gap-3">
                <div className="w-14 h-14 rounded-full bg-green-500 flex items-center justify-center">
                  <Check size={28} className="text-white" />
                </div>
                <span className="text-white text-sm font-medium">优化完成</span>
                {/* 显示置信度 */}
                {state.confidence > 0 && (
                  <span className="text-white/50 text-xs">
                    置信度: {Math.round(state.confidence * 100)}%
                  </span>
                )}
              </div>
            </div>
          )}

          {/* Params Quick View */}
          <div className="absolute bottom-3 left-3 right-3">
            <div className="flex flex-wrap gap-1.5">
              {basicParams.slice(0, 4).map((param) => (
                state.params[param.key as keyof AIFineTuneParams] !== 0 && (
                  <span 
                    key={param.key}
                    className="px-2 py-1 rounded-full bg-black/50 backdrop-blur-sm text-white text-[10px]"
                  >
                    {param.label}: {state.params[param.key as keyof AIFineTuneParams] > 0 ? '+' : ''}
                    {state.params[param.key as keyof AIFineTuneParams]}
                  </span>
                )
              ))}
            </div>
          </div>
          
          {/* Style Badge */}
          {state.selectedStyle && (
            <div className="absolute top-3 left-3">
              <div className="px-3 py-1.5 rounded-full bg-[#9C27B0]/80 backdrop-blur-sm flex items-center gap-2">
                <Palette size={14} className="text-white" />
                <span className="text-white text-xs font-medium">
                  {COLOR_STYLES.find(s => s.id === state.selectedStyle)?.name}
                </span>
              </div>
            </div>
          )}
          
          {/* AI 分析结果徽章 */}
          {state.sceneAnalysis && (
            <div className="absolute top-3 right-3">
              <div className="px-3 py-1.5 rounded-full bg-green-500/80 backdrop-blur-sm flex items-center gap-2">
                <Sparkles size={14} className="text-white" />
                <span className="text-white text-xs font-medium">
                  {state.sceneAnalysis.primaryScene.name}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Quick Presets */}
      <div className="px-4 pb-4">
        <div className="flex gap-2 overflow-x-auto scrollbar-hide">
          {/* AI 一键微调按钮 */}
          <button
            onClick={handleAutoTune}
            disabled={state.isProcessing}
            className="flex-shrink-0 px-4 py-2.5 rounded-xl bg-gradient-to-r from-purple-600 to-blue-600 flex items-center gap-2 text-white font-medium shadow-lg shadow-purple-500/20 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Wand2 size={16} />
            <span>一键 AI 微调</span>
          </button>
          {/* 基础预设按钮 */}
          {BASE_PRESETS.map((preset) => {
            const Icon = preset.icon;
            return (
              <button
                key={preset.id}
                onClick={() => applyPreset(preset)}
                disabled={state.isProcessing}
                className="flex-shrink-0 px-3 py-2.5 rounded-xl bg-white/5 border border-white/10 text-white/70 text-sm hover:bg-white/10 flex items-center gap-1.5 disabled:opacity-50"
              >
                <Icon size={14} />
                {preset.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab Bar */}
      <div className="px-4 pb-3 border-b border-white/5">
        <div className="flex gap-1 overflow-x-auto scrollbar-hide">
          {[
            { key: 'basic', label: '基础', icon: Sliders },
            { key: 'color', label: '风格', icon: Palette },
            { key: 'smart', label: '智能', icon: Sparkles },
            { key: 'hsl', label: 'HSL', icon: Circle },
            { key: 'curve', label: '曲线', icon: TrendingUp },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => setActiveTab(tab.key as FineTuneState['activeTab'])}
              className={`flex-shrink-0 px-4 py-2.5 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-colors ${
                state.activeTab === tab.key
                  ? 'bg-white/10 text-white'
                  : 'text-white/50 hover:bg-white/5'
              }`}
            >
              <tab.icon size={16} />
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 overflow-y-auto pb-6">
        {/* Basic Tab */}
        {state.activeTab === 'basic' && (
          <div className="px-4 py-4 space-y-6">
            {/* Basic Params */}
            <div>
              <h3 className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Sliders size={12} />
                基础参数
              </h3>
              <div className="space-y-3">
                {basicParams.map((param) => (
                  <div key={param.key} className="bg-white/5 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-white text-sm font-medium">{param.label}</span>
                      <div className="flex items-center gap-2">
                        <span className="text-[#9C27B0] text-sm font-bold">
                          {state.params[param.key as keyof AIFineTuneParams] > 0 ? '+' : ''}
                          {state.params[param.key as keyof AIFineTuneParams]}
                        </span>
                        {/* 锁定按钮 */}
                        <button
                          onClick={() => toggleLock(param.key)}
                          className={`p-1 rounded text-xs ${state.lockedParams.includes(param.key) ? 'text-yellow-500' : 'text-white/30'}`}
                          title={state.lockedParams.includes(param.key) ? '已锁定' : '锁定参数'}
                        >
                          <RefreshCw size={12} />
                        </button>
                      </div>
                    </div>
                    {/* 滑块 */}
                    <input
                      type="range"
                      min={param.min}
                      max={param.max}
                      value={state.params[param.key as keyof AIFineTuneParams]}
                      onChange={(e) => updateParam(param.key as keyof AIFineTuneParams, parseInt(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* Pro Params */}
            <div>
              <h3 className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Target size={12} />
                专业参数
              </h3>
              <div className="space-y-3">
                {proParams.map((param) => (
                  <div key={param.key} className="bg-white/5 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-white text-sm font-medium">{param.label}</span>
                      <span className="text-[#9C27B0] text-sm font-bold">
                        {state.params[param.key as keyof AIFineTuneParams] > 0 ? '+' : ''}
                        {state.params[param.key as keyof AIFineTuneParams]}
                      </span>
                    </div>
                    <input
                      type="range"
                      min={param.min}
                      max={param.max}
                      value={state.params[param.key as keyof AIFineTuneParams]}
                      onChange={(e) => updateParam(param.key as keyof AIFineTuneParams, parseInt(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* Effect Params */}
            <div>
              <h3 className="text-white/50 text-xs mb-3 flex items-center gap-2">
                <Sparkles size={12} />
                效果参数
              </h3>
              <div className="space-y-3">
                {effectParams.map((param) => (
                  <div key={param.key} className="bg-white/5 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-white text-sm font-medium">{param.label}</span>
                      <span className="text-[#9C27B0] text-sm font-bold">
                        {state.params[param.key as keyof AIFineTuneParams]}
                      </span>
                    </div>
                    <input
                      type="range"
                      min={param.min}
                      max={param.max}
                      value={state.params[param.key as keyof AIFineTuneParams]}
                      onChange={(e) => updateParam(param.key as keyof AIFineTuneParams, parseInt(e.target.value))}
                      className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0]"
                    />
                  </div>
                ))}
              </div>
            </div>

            {/* Reset Button */}
            <button
              onClick={handleReset}
              className="w-full py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium hover:bg-white/5 flex items-center justify-center gap-2"
            >
              <RotateCcw size={16} />
              重置所有参数
            </button>
          </div>
        )}

        {/* Color Tab */}
        {state.activeTab === 'color' && (
          <div className="px-4 py-4">
            {/* Search */}
            <div className="relative mb-4">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-white/40" />
              <input
                type="text"
                value={state.searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="搜索色彩风格..."
                className="w-full pl-9 pr-4 py-2.5 rounded-xl bg-white/5 text-white text-sm border border-white/10 focus:border-[#9C27B0] outline-none"
              />
            </div>
            
            <h3 className="text-white/50 text-xs mb-3">色彩风格预设 ({filteredStyles.length})</h3>
            <div className="grid grid-cols-2 gap-3">
              {filteredStyles.map((style) => {
                const isFavorite = state.favorites.includes(style.id);
                return (
                  <button
                    key={style.id}
                    onClick={() => applyColorStyle(style)}
                    disabled={state.isProcessing}
                    className={`relative p-4 rounded-xl text-left transition-all ${
                      state.selectedStyle === style.id
                        ? 'bg-white/10 border border-white/20'
                        : 'bg-white/5 hover:bg-white/10'
                    } disabled:opacity-50`}
                  >
                    {/* Favorite Button */}
                    <button
                      onClick={(e) => { e.stopPropagation(); toggleFavorite(style.id); }}
                      className="absolute top-2 right-2"
                    >
                      <Heart size={14} className={isFavorite ? 'text-red-500 fill-red-500' : 'text-white/30'} />
                    </button>
                    
                    <div className="flex items-center gap-3 mb-2">
                      <div 
                        className="w-10 h-10 rounded-lg flex items-center justify-center"
                        style={{ backgroundColor: `${style.color}30` }}
                      >
                        <style.icon size={20} style={{ color: style.color }} />
                      </div>
                      <span className="text-white font-medium">{style.name}</span>
                    </div>
                    <p className="text-white/40 text-xs mb-2">{style.desc}</p>
                    <div className="flex flex-wrap gap-1">
                      {Object.entries(style.params).slice(0, 3).map(([key, value]) => (
                        <span key={key} className="text-[10px] text-white/30 bg-white/5 px-1.5 py-0.5 rounded">
                          {key}: {value > 0 ? '+' : ''}{value}
                        </span>
                      ))}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* Smart Tab */}
        {state.activeTab === 'smart' && (
          <div className="px-4 py-4">
            <h3 className="text-white/50 text-xs mb-3">智能优化选项</h3>
            <div className="space-y-3">
              {SMART_OPTIMIZATIONS.map((opt) => (
                <button
                  key={opt.id}
                  onClick={() => toggleOptimization(opt.id)}
                  disabled={state.isProcessing}
                  className={`w-full p-4 rounded-xl text-left transition-all flex items-center gap-4 ${
                    state.selectedOptimizations.includes(opt.id)
                      ? 'bg-white/10 border border-white/20'
                      : 'bg-white/5 hover:bg-white/10'
                  } disabled:opacity-50`}
                >
                  <div 
                    className="w-12 h-12 rounded-xl flex items-center justify-center relative"
                    style={{ backgroundColor: `${opt.color}20` }}
                  >
                    <opt.icon size={24} style={{ color: opt.color }} />
                    {opt.pro && (
                      <Crown size={10} className="absolute -top-1 -right-1 text-yellow-400" />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-white font-medium">{opt.name}</h4>
                      {opt.pro && (
                        <span className="px-1.5 py-0.5 rounded bg-yellow-500/20 text-yellow-400 text-[10px]">PRO</span>
                      )}
                    </div>
                    <p className="text-white/50 text-xs">{opt.desc}</p>
                  </div>
                  <div className={`w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                    state.selectedOptimizations.includes(opt.id)
                      ? 'bg-[#9C27B0] border-[#9C27B0]'
                      : 'border-white/30'
                  }`}>
                    {state.selectedOptimizations.includes(opt.id) && <Check size={14} className="text-white" />}
                  </div>
                </button>
              ))}
            </div>

            {/* Apply Smart Optimizations */}
            {state.selectedOptimizations.length > 0 && (
              <button
                onClick={applySmartOptimizations}
                disabled={state.isProcessing}
                className="w-full mt-4 py-3 rounded-xl bg-gradient-to-r from-[#9C27B0] to-[#673AB7] text-white font-medium flex items-center justify-center gap-2 disabled:opacity-50"
              >
                <Sparkles size={18} />
                应用 {state.selectedOptimizations.length} 项优化
              </button>
            )}
          </div>
        )}

        {/* HSL Tab */}
        {state.activeTab === 'hsl' && (
          <div className="px-4 py-4">
            <h3 className="text-white/50 text-xs mb-3">HSL 色彩调节</h3>
            
            {/* Color Selector */}
            <div className="flex gap-2 overflow-x-auto scrollbar-hide mb-4">
              {state.hslValues.map((color) => (
                <button
                  key={color.id}
                  onClick={() => setSelectedHsl(color.id)}
                  disabled={state.isProcessing}
                  className={`flex-shrink-0 w-10 h-10 rounded-xl flex items-center justify-center transition-all ${
                    state.selectedHsl === color.id 
                      ? 'ring-2 ring-white ring-offset-2 ring-offset-[#0a0a0a]' 
                      : ''
                  } disabled:opacity-50`}
                  style={{ backgroundColor: color.color }}
                >
                  {state.selectedHsl === color.id && <Check size={16} className="text-white" />}
                </button>
              ))}
            </div>
            
            {/* HSL Sliders */}
            <div className="space-y-4">
              {(['hue', 'saturation', 'luminance'] as const).map((type) => {
                const currentHsl = state.hslValues.find(h => h.id === state.selectedHsl);
                const value = currentHsl ? currentHsl[type] : 0;
                
                return (
                  <div key={type} className="bg-white/5 rounded-xl p-4">
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-white text-sm font-medium">
                        {type === 'hue' ? '色相' : type === 'saturation' ? '饱和度' : '明度'}
                      </span>
                      <span className="text-[#9C27B0] text-sm font-bold">
                        {value}
                      </span>
                    </div>
                    <input
                      type="range"
                      min={type === 'hue' ? -180 : -100}
                      max={type === 'hue' ? 180 : 100}
                      value={value}
                      onChange={(e) => updateHslValue(state.selectedHsl, type, parseInt(e.target.value))}
                      disabled={state.isProcessing}
                      className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#9C27B0] disabled:opacity-50"
                    />
                  </div>
                );
              })}
            </div>
            
            {/* Reset HSL */}
            <button
              onClick={resetHsl}
              disabled={state.isProcessing}
              className="w-full mt-4 py-3 rounded-xl border border-white/20 text-white/70 text-sm font-medium hover:bg-white/5 disabled:opacity-50"
            >
              重置 HSL
            </button>
          </div>
        )}

        {/* Curve Tab */}
        {state.activeTab === 'curve' && (
          <div className="px-4 py-4">
            <h3 className="text-white/50 text-xs mb-3">曲线调节</h3>
            
            {/* Curve Preview */}
            <div className="bg-white/5 rounded-xl p-4 mb-4">
              <div className="aspect-square relative bg-[#1a1a1a] rounded-lg border border-white/10">
                {/* Grid */}
                <div className="absolute inset-0 grid grid-cols-4 grid-rows-4">
                  {[...Array(16)].map((_, i) => (
                    <div key={i} className="border border-white/5" />
                  ))}
                </div>
                {/* Diagonal */}
                <svg className="absolute inset-0 w-full h-full" viewBox="0 0 256 256">
                  <line x1="0" y1="256" x2="256" y2="0" stroke="rgba(255,255,255,0.2)" strokeWidth="1" />
                  <path
                    d="M 0 256 Q 64 192, 128 128 T 256 0"
                    fill="none"
                    stroke="#9C27B0"
                    strokeWidth="2"
                  />
                </svg>
              </div>
            </div>
            
            {/* Curve Presets */}
            <div className="grid grid-cols-3 gap-2 mb-4">
              {CURVE_PRESETS.map((preset) => (
                <button
                  key={preset.id}
                  disabled={state.isProcessing}
                  className="p-3 rounded-xl bg-white/5 hover:bg-white/10 text-center disabled:opacity-50"
                >
                  <TrendingUp size={20} className="mx-auto mb-1 text-white/50" />
                  <span className="text-xs text-white/70">{preset.name}</span>
                </button>
              ))}
            </div>
            
            {/* Curve Channels */}
            <div className="flex gap-2">
              {['RGB', 'R', 'G', 'B'].map((channel) => (
                <button
                  key={channel}
                  disabled={state.isProcessing}
                  className={`flex-1 py-2 rounded-lg text-xs font-medium ${
                    state.selectedCurveChannel === channel.toLowerCase() 
                      ? 'bg-white/10 text-white' 
                      : 'bg-white/5 text-white/50'
                  } disabled:opacity-50`}
                >
                  {channel}
                </button>
              ))}
            </div>
          </div>
        )}
        
        {/* AI 推荐区域 */}
        {state.recommendations.length > 0 && (
          <div className="px-4 py-4 mt-4">
            <h3 className="text-white/50 text-xs mb-3 flex items-center gap-2">
              <Sparkles size={12} />
              AI 推荐风格
            </h3>
            <div className="flex flex-wrap gap-2">
              {state.recommendations.slice(0, 4).map((rec) => (
                <button
                  key={rec.id}
                  onClick={() => {
                    if (rec.type === 'style') {
                      const style = COLOR_STYLES.find(s => s.id === rec.id);
                      if (style) applyColorStyle(style);
                    }
                  }}
                  disabled={state.isProcessing}
                  className="px-3 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-white/70 text-sm flex items-center gap-2 disabled:opacity-50"
                >
                  {rec.type === 'film' && <Camera size={14} />}
                  {rec.type === 'style' && <Palette size={14} />}
                  {rec.name}
                  <span className="text-white/30 text-xs">{Math.round(rec.matchScore * 100)}%</span>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 自定义样式 */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
      `}</style>
    </div>
  );
};

export default AIFineTunePage;