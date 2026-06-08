import React, { useState, useEffect, useMemo, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Sun, Thermometer, Sparkles, Camera, Check, RefreshCw, Wand2, Download, Image as ImageIcon, Save, RotateCcw, History, Zap, ChevronDown, ChevronUp, Trash2, Clock, Lightbulb, AlertTriangle, Eye } from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { analyzeImageScene, applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// ============ 类型定义 ============
interface SavedPreset {
  id: string;
  name: string;
  timestamp: number;
  cameraParams: { iso: number; shutter: number; aperture: number; wb: number };
  aiParams: { saturation: number; contrast: number; brightness: number; warmth: number; sharpness: number };
}

interface ParamSnapshot {
  timestamp: number;
  cameraParams: { iso: number; shutter: number; aperture: number; wb: number };
  aiParams: { saturation: number; contrast: number; brightness: number; warmth: number; sharpness: number };
}

// ============ 常量 ============
const STORAGE_KEY = 'omaster_param_presets';
const DEFAULT_CAMERA_PARAMS = { iso: 100, shutter: 125, aperture: 2.8, wb: 5500 };
const DEFAULT_AI_PARAMS = { saturation: 10, contrast: 5, brightness: 0, warmth: 8, sharpness: 15 };

// OPPO哈苏大师风格预设
const hasselbladPresets = [
  {
    id: 'portrait',
    name: '哈苏人像大师',
    icon: Camera,
    color: '#E91E63',
    desc: '柔美肤色，自然光影',
    params: { iso: 200, shutter: 125, aperture: 2.8, wb: 5500, saturation: 10, contrast: 5, warmth: 8, sharpness: 15 },
    imgParams: { saturation: 10, contrast: 5, brightness: 8, warmth: 8, cyanMagenta: 0, sharpness: 15, tone: 5, softLight: 35, vignette: false, filter: '原图' }
  },
  {
    id: 'landscape',
    name: '哈苏风景大师',
    icon: Sun,
    color: '#4CAF50',
    desc: '通透质感，色彩饱满',
    params: { iso: 100, shutter: 60, aperture: 8, wb: 5600, saturation: 20, contrast: 15, warmth: -5, sharpness: 25 },
    imgParams: { saturation: 20, contrast: 15, brightness: 10, warmth: -5, cyanMagenta: -5, sharpness: 25, tone: 15, softLight: 10, vignette: false, filter: '原图' }
  },
  {
    id: 'night',
    name: '哈苏夜景大师',
    icon: Sparkles,
    color: '#3F51B5',
    desc: '降噪增强，氛围感强',
    params: { iso: 3200, shutter: 30, aperture: 2.8, wb: 4000, saturation: 25, contrast: 20, warmth: -10, sharpness: 30 },
    imgParams: { saturation: 25, contrast: 20, brightness: 0, warmth: -10, cyanMagenta: 5, sharpness: 30, tone: 20, softLight: 20, vignette: true, filter: '原图' }
  },
  {
    id: 'film',
    name: '哈苏胶片大师',
    icon: Aperture,
    color: '#FF9800',
    desc: '复古质感，经典色调',
    params: { iso: 400, shutter: 125, aperture: 4, wb: 5200, saturation: 5, contrast: 10, warmth: 15, sharpness: 20 },
    imgParams: { saturation: -5, contrast: 15, brightness: 0, warmth: 25, cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25, vignette: true, filter: '胶片' }
  },
];

const quickPresets = [
  { name: '人像', iso: 200, shutter: 125, aperture: 2.8, wb: 5500 },
  { name: '风景', iso: 100, shutter: 60, aperture: 8, wb: 5600 },
  { name: '夜景', iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 },
  { name: '运动', iso: 800, shutter: 500, aperture: 4, wb: 5500 },
];

// ============ 辅助函数 ============
const shutterToSeconds = (v: number): number => v >= 1000 ? v / 1000 : 1 / v;

const secondsToShutter = (t: number): number => {
  if (t >= 1) return Math.min(1000, Math.round(t * 1000));
  const reciprocal = Math.round(1 / t);
  return Math.max(1, Math.min(1000, reciprocal));
};

const formatShutter = (v: number): string => v >= 1000 ? `${v / 1000}s` : `1/${v}s`;

const formatRelativeTime = (timestamp: number): string => {
  const diff = Date.now() - timestamp;
  if (diff < 5000) return '刚刚';
  if (diff < 60000) return `${Math.floor(diff / 1000)}秒前`;
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`;
  return `${Math.floor(diff / 3600000)}小时前`;
};

// ============ 主组件 ============
const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, aiParams, setAiParam, goBack } = useAppStore();

  // 已有状态
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [recommendedPreset, setRecommendedPreset] = useState<string | null>(null);
  const [appliedPreset, setAppliedPreset] = useState<string | null>(null);
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);

  // 新增状态 - 参数联动
  const [linkageRecommendation, setLinkageRecommendation] = useState<{
    shutter: number | null;
    warmth: number | null;
  }>({ shutter: null, warmth: null });

  // 新增状态 - 参数历史
  const [paramHistory, setParamHistory] = useState<ParamSnapshot[]>([]);
  const [showHistory, setShowHistory] = useState(false);

  // 新增状态 - 预设保存
  const [savedPresets, setSavedPresets] = useState<SavedPreset[]>([]);
  const [showSavedPresets, setShowSavedPresets] = useState(false);
  const [presetName, setPresetName] = useState('');
  const [showSaveDialog, setShowSaveDialog] = useState(false);

  // Refs
  const prevIsoRef = useRef(cameraParams.iso);
  const prevWbRef = useRef(cameraParams.wb);
  const lastPresetApplyTimeRef = useRef(0);
  const lastCommittedRef = useRef({ cameraParams, aiParams });
  const isFirstRenderRef = useRef(true);
  const previewVersionRef = useRef(0);
  const isRollingBackRef = useRef(false);
  const hasLoadedPresetsRef = useRef(false);
  const historyTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // ============ 曝光指示器计算 ============
  const evInfo = useMemo(() => {
    const t = shutterToSeconds(cameraParams.shutter);
    if (t <= 0 || cameraParams.aperture <= 0 || cameraParams.iso <= 0) {
      return { ev: 0, bv: 0, label: '无法计算', status: 'normal' as const, color: '#999' };
    }
    const av = 2 * Math.log2(cameraParams.aperture);
    const tv = -Math.log2(t);
    const sv = Math.log2(cameraParams.iso / 3.125);
    const ev = av + tv;
    const bv = ev - sv;

    let label: string;
    let status: 'over' | 'under' | 'normal';
    let color: string;

    if (bv >= 9) { label = '强光场景'; status = 'under'; color = '#2196F3'; }
    else if (bv >= 7) { label = '晴天户外'; status = 'normal'; color = '#4CAF50'; }
    else if (bv >= 5) { label = '多云/阴天'; status = 'normal'; color = '#4CAF50'; }
    else if (bv >= 3) { label = '室内/日落'; status = 'normal'; color = '#FFC107'; }
    else if (bv >= 1) { label = '昏暗室内'; status = 'over'; color = '#FF9800'; }
    else if (bv >= -1) { label = '夜景/弱光'; status = 'over'; color = '#F44336'; }
    else { label = '极暗环境'; status = 'over'; color = '#F44336'; }

    return { ev: Math.round(ev * 10) / 10, bv: Math.round(bv * 10) / 10, label, status, color };
  }, [cameraParams.iso, cameraParams.shutter, cameraParams.aperture]);

  // ============ Effects ============

  // 加载已保存预设
  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        setSavedPresets(JSON.parse(stored));
      }
    } catch (e) {
      console.error('加载预设失败:', e);
    }
    hasLoadedPresetsRef.current = true;
  }, []);

  // 保存预设到localStorage
  useEffect(() => {
    if (!hasLoadedPresetsRef.current) return;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(savedPresets));
    } catch (e) {
      console.error('保存预设失败:', e);
    }
  }, [savedPresets]);

  // 清除已上传图片时清除预览
  useEffect(() => {
    if (!uploadedImage) {
      setProcessedImage('');
    }
  }, [uploadedImage]);

  // 参数联动：ISO变化推荐快门速度
  useEffect(() => {
    const prevIso = prevIsoRef.current;
    if (prevIso !== cameraParams.iso && prevIso > 0 && cameraParams.iso > 0) {
      const oldShutterTime = shutterToSeconds(cameraParams.shutter);
      const newShutterTime = oldShutterTime * (prevIso / cameraParams.iso);
      const recommendedShutter = secondsToShutter(newShutterTime);
      if (recommendedShutter !== cameraParams.shutter) {
        setLinkageRecommendation(prev => ({ ...prev, shutter: recommendedShutter }));
      }
    }
    prevIsoRef.current = cameraParams.iso;
  }, [cameraParams.iso, cameraParams.shutter]);

  // 参数联动：白平衡变化推荐色温偏移
  useEffect(() => {
    const prevWb = prevWbRef.current;
    if (prevWb !== cameraParams.wb) {
      const recommendedWarmth = Math.round((cameraParams.wb - 5500) / 45);
      const clampedWarmth = Math.max(-100, Math.min(100, recommendedWarmth));
      if (clampedWarmth !== aiParams.warmth) {
        setLinkageRecommendation(prev => ({ ...prev, warmth: clampedWarmth }));
      }
    }
    prevWbRef.current = cameraParams.wb;
  }, [cameraParams.wb, aiParams.warmth]);

  // 手动调整快门时清除快门推荐
  useEffect(() => {
    if (linkageRecommendation.shutter !== null && cameraParams.shutter !== linkageRecommendation.shutter) {
      setLinkageRecommendation(prev => ({ ...prev, shutter: null }));
    }
  }, [cameraParams.shutter, linkageRecommendation.shutter]);

  // 手动调整色温偏移时清除色温推荐
  useEffect(() => {
    if (linkageRecommendation.warmth !== null && aiParams.warmth !== linkageRecommendation.warmth) {
      setLinkageRecommendation(prev => ({ ...prev, warmth: null }));
    }
  }, [aiParams.warmth, linkageRecommendation.warmth]);

  // 实时预览：参数变化时自动生成预览图
  useEffect(() => {
    if (!uploadedImage) return;
    if (Date.now() - lastPresetApplyTimeRef.current < 2000) return;

    const version = ++previewVersionRef.current;

    const timer = setTimeout(async () => {
      setIsProcessing(true);
      try {
        const imgParams: ImageAdjustParams = {
          saturation: aiParams.saturation,
          contrast: aiParams.contrast,
          brightness: aiParams.brightness,
          warmth: aiParams.warmth,
          cyanMagenta: 0,
          sharpness: aiParams.sharpness,
          tone: 0,
          softLight: 0,
          vignette: false,
          filter: '原图',
        };
        const result = await applyImageAdjustments(uploadedImage, imgParams);
        if (version === previewVersionRef.current) {
          setProcessedImage(result);
        }
      } catch (e) {
        console.error('实时预览失败:', e);
      } finally {
        if (version === previewVersionRef.current) {
          setIsProcessing(false);
        }
      }
    }, 800);

    return () => clearTimeout(timer);
  }, [uploadedImage, cameraParams, aiParams]);

  // 参数历史记录
  useEffect(() => {
    if (isFirstRenderRef.current) {
      isFirstRenderRef.current = false;
      lastCommittedRef.current = { cameraParams: { ...cameraParams }, aiParams: { ...aiParams } };
      return;
    }
    if (isRollingBackRef.current) return;

    if (historyTimerRef.current) {
      clearTimeout(historyTimerRef.current);
    }

    const currentSnapshot: ParamSnapshot = {
      timestamp: Date.now(),
      cameraParams: { ...cameraParams },
      aiParams: { ...aiParams },
    };

    historyTimerRef.current = setTimeout(() => {
      setParamHistory(prev => {
        const last = prev[0];
        if (last && JSON.stringify(last.cameraParams) === JSON.stringify(currentSnapshot.cameraParams)
          && JSON.stringify(last.aiParams) === JSON.stringify(currentSnapshot.aiParams)) {
          return prev;
        }
        return [currentSnapshot, ...prev].slice(0, 5);
      });
    }, 1500);

    return () => {
      if (historyTimerRef.current) {
        clearTimeout(historyTimerRef.current);
      }
    };
  }, [cameraParams, aiParams]);

  // ============ 处理函数 ============

  // AI分析图片推荐参数 - 真实像素分析
  const handleAnalyzeImage = async () => {
    if (!uploadedImage) return;

    setIsAnalyzing(true);
    setRecommendedPreset(null);
    setProcessedImage('');

    try {
      const result = await analyzeImageScene(uploadedImage);

      let matchedPreset: typeof hasselbladPresets[0] | undefined;
      if (result.scene === '人像' || result.hasselbladStyle === 'portrait') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'portrait');
      } else if (result.scene === '风景' || result.scene === '花卉' || result.scene === '海景水域' || result.scene === '自然' || result.hasselbladStyle === 'natural') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'landscape');
      } else if (result.scene === '夜景' || result.hasselbladStyle === 'cinematic') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'night');
      } else if (result.scene === '日落黄昏') {
        matchedPreset = hasselbladPresets.find(p => p.id === 'film');
      }

      const finalPreset = matchedPreset || hasselbladPresets[0];
      setRecommendedPreset(finalPreset.id);
    } catch (e) {
      console.error('AI分析失败:', e);
    } finally {
      setIsAnalyzing(false);
    }
  };

  // 应用哈苏大师预设 - 真实处理
  const applyHasselbladPreset = async (preset: typeof hasselbladPresets[0]) => {
    setCameraParam('iso', preset.params.iso);
    setCameraParam('shutter', preset.params.shutter);
    setCameraParam('aperture', preset.params.aperture);
    setCameraParam('wb', preset.params.wb);
    setAiParam('saturation', preset.params.saturation);
    setAiParam('contrast', preset.params.contrast);
    setAiParam('warmth', preset.params.warmth);
    setAiParam('sharpness', preset.params.sharpness);
    setAppliedPreset(preset.id);
    setLinkageRecommendation({ shutter: null, warmth: null });

    lastPresetApplyTimeRef.current = Date.now();

    if (uploadedImage && preset.imgParams) {
      setIsProcessing(true);
      try {
        const result = await applyImageAdjustments(uploadedImage, preset.imgParams);
        setProcessedImage(result);
      } catch (e) {
        console.error('应用参数失败:', e);
      } finally {
        setIsProcessing(false);
      }
    }

    setTimeout(() => setAppliedPreset(null), 3000);
  };

  const applyQuickPreset = (preset: typeof quickPresets[0]) => {
    setCameraParam('iso', preset.iso);
    setCameraParam('shutter', preset.shutter);
    setCameraParam('aperture', preset.aperture);
    setCameraParam('wb', preset.wb);
    setLinkageRecommendation(prev => ({ ...prev, shutter: null }));
  };

  // 保存当前参数预设
  const handleSavePreset = () => {
    if (!presetName.trim()) return;
    const newPreset: SavedPreset = {
      id: `preset_${Date.now()}`,
      name: presetName.trim(),
      timestamp: Date.now(),
      cameraParams: { ...cameraParams },
      aiParams: { ...aiParams },
    };
    setSavedPresets(prev => [newPreset, ...prev]);
    setPresetName('');
    setShowSaveDialog(false);
  };

  // 删除已保存预设
  const handleDeletePreset = (id: string) => {
    setSavedPresets(prev => prev.filter(p => p.id !== id));
  };

  // 加载已保存预设
  const handleLoadPreset = (preset: SavedPreset) => {
    (Object.keys(preset.cameraParams) as Array<keyof typeof preset.cameraParams>).forEach((key) => {
      setCameraParam(key, preset.cameraParams[key]);
    });
    (Object.keys(preset.aiParams) as Array<keyof typeof preset.aiParams>).forEach((key) => {
      setAiParam(key, preset.aiParams[key]);
    });
    setLinkageRecommendation({ shutter: null, warmth: null });
  };

  // 重置所有参数
  const handleResetAll = () => {
    setCameraParam('iso', DEFAULT_CAMERA_PARAMS.iso);
    setCameraParam('shutter', DEFAULT_CAMERA_PARAMS.shutter);
    setCameraParam('aperture', DEFAULT_CAMERA_PARAMS.aperture);
    setCameraParam('wb', DEFAULT_CAMERA_PARAMS.wb);
    setAiParam('saturation', DEFAULT_AI_PARAMS.saturation);
    setAiParam('contrast', DEFAULT_AI_PARAMS.contrast);
    setAiParam('brightness', DEFAULT_AI_PARAMS.brightness);
    setAiParam('warmth', DEFAULT_AI_PARAMS.warmth);
    setAiParam('sharpness', DEFAULT_AI_PARAMS.sharpness);
    setProcessedImage('');
    setLinkageRecommendation({ shutter: null, warmth: null });
  };

  // 回退到历史快照
  const rollbackToSnapshot = (snapshot: ParamSnapshot) => {
    isRollingBackRef.current = true;
    (Object.keys(snapshot.cameraParams) as Array<keyof typeof snapshot.cameraParams>).forEach((key) => {
      setCameraParam(key, snapshot.cameraParams[key]);
    });
    (Object.keys(snapshot.aiParams) as Array<keyof typeof snapshot.aiParams>).forEach((key) => {
      setAiParam(key, snapshot.aiParams[key]);
    });
    setLinkageRecommendation({ shutter: null, warmth: null });
    setTimeout(() => { isRollingBackRef.current = false; }, 2000);
  };

  // 接受快门联动推荐
  const acceptShutterLinkage = () => {
    if (linkageRecommendation.shutter !== null) {
      setCameraParam('shutter', linkageRecommendation.shutter);
      setLinkageRecommendation(prev => ({ ...prev, shutter: null }));
    }
  };

  // 接受色温偏移联动推荐
  const acceptWarmthLinkage = () => {
    if (linkageRecommendation.warmth !== null) {
      setAiParam('warmth', linkageRecommendation.warmth);
      setLinkageRecommendation(prev => ({ ...prev, warmth: null }));
    }
  };

  // ============ 参数配置 ============
  const params = [
    {
      key: 'iso',
      label: 'ISO 感光度',
      icon: Aperture,
      min: 50,
      max: 12800,
      step: 50,
      marks: [50, 100, 200, 400, 800, 1600, 3200, 6400, 12800]
    },
    {
      key: 'shutter',
      label: '快门速度',
      icon: Timer,
      min: 1,
      max: 1000,
      step: 1,
      format: (v: number) => formatShutter(v)
    },
    {
      key: 'aperture',
      label: '光圈',
      icon: Aperture,
      min: 1.4,
      max: 22,
      step: 0.1,
      format: (v: number) => `f/${v.toFixed(1)}`
    },
    {
      key: 'wb',
      label: '白平衡',
      icon: Thermometer,
      min: 2000,
      max: 10000,
      step: 100,
      format: (v: number) => `${v}K`
    },
  ];

  const aiParamsList = [
    { key: 'saturation', label: '饱和度', min: -100, max: 100 },
    { key: 'contrast', label: '对比度', min: -100, max: 100 },
    { key: 'warmth', label: '色温偏移', min: -100, max: 100 },
    { key: 'sharpness', label: '锐度', min: 0, max: 100 },
  ];

  // ============ 渲染 ============
  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 border-b border-white/5">
        <button
          onClick={goBack}
          className="p-2 -ml-2 rounded-full hover:bg-white/10 transition-colors"
        >
          <ArrowLeft size={20} className="text-white" />
        </button>
        <h1 className="text-lg font-bold text-white">参数精细调节</h1>
        {uploadedImage && (
          <div className="ml-auto px-2 py-1 rounded-full bg-[#FF6B35]/20">
            <span className="text-[#FF6B35] text-xs">已上传照片</span>
          </div>
        )}
        <div className="flex items-center gap-2 ml-2">
          <button
            onClick={() => setShowSaveDialog(true)}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
            title="保存当前参数"
          >
            <Save size={16} className="text-[#4CAF50]" />
          </button>
          <button
            onClick={handleResetAll}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 transition-colors"
            title="重置所有参数"
          >
            <RotateCcw size={16} className="text-[#FF6B35]" />
          </button>
        </div>
      </div>

      {/* 曝光指示器 EV */}
      <div className="px-4 py-3 border-b border-white/5">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg flex items-center justify-center" style={{ backgroundColor: `${evInfo.color}20` }}>
              <Sun size={16} style={{ color: evInfo.color }} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <span className="text-white text-sm font-medium">曝光值</span>
                <span className="text-white font-bold text-sm">EV {evInfo.ev}</span>
              </div>
              <span className="text-xs" style={{ color: evInfo.color }}>{evInfo.label}</span>
            </div>
          </div>
          <div className="flex items-center gap-2">
            {evInfo.status === 'over' && (
              <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-yellow-500/20">
                <AlertTriangle size={12} className="text-yellow-500" />
                <span className="text-yellow-500 text-xs">可能过曝</span>
              </div>
            )}
            {evInfo.status === 'under' && (
              <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-blue-500/20">
                <AlertTriangle size={12} className="text-blue-400" />
                <span className="text-blue-400 text-xs">可能欠曝</span>
              </div>
            )}
            {evInfo.status === 'normal' && (
              <div className="flex items-center gap-1 px-2 py-1 rounded-full bg-green-500/20">
                <Check size={12} className="text-green-500" />
                <span className="text-green-500 text-xs">曝光正常</span>
              </div>
            )}
          </div>
        </div>
        {/* EV 进度条 */}
        <div className="mt-2 h-1.5 bg-white/10 rounded-full overflow-hidden relative">
          <div
            className="h-full rounded-full transition-all duration-300"
            style={{
              width: `${Math.max(5, Math.min(100, ((evInfo.bv + 4) / 16) * 100))}%`,
              backgroundColor: evInfo.color,
            }}
          />
          {/* 正常区域标记 */}
          <div className="absolute top-0 h-full border-l border-r border-green-500/30" style={{ left: '43.75%', width: '31.25%' }} />
        </div>
        <div className="flex justify-between mt-1">
          <span className="text-white/20 text-[9px]">极暗</span>
          <span className="text-green-500/40 text-[9px]">正常范围</span>
          <span className="text-white/20 text-[9px]">强光</span>
        </div>
      </div>

      {/* 保存预设对话框 */}
      {showSaveDialog && (
        <div className="px-4 py-3 border-b border-white/5 bg-white/5">
          <p className="text-white text-sm font-medium mb-3">保存参数预设</p>
          <div className="flex gap-2">
            <input
              type="text"
              value={presetName}
              onChange={(e) => setPresetName(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSavePreset()}
              placeholder="输入预设名称..."
              className="flex-1 px-3 py-2 rounded-xl bg-white/10 text-white text-sm placeholder-white/30 outline-none focus:ring-1 focus:ring-[#FF6B35]"
              maxLength={20}
              autoFocus
            />
            <button
              onClick={() => { setShowSaveDialog(false); setPresetName(''); }}
              className="px-3 py-2 rounded-xl bg-white/10 text-white/70 text-sm hover:bg-white/15 transition-colors"
            >
              取消
            </button>
            <button
              onClick={handleSavePreset}
              disabled={!presetName.trim()}
              className="px-4 py-2 rounded-xl bg-[#4CAF50] text-white text-sm font-medium disabled:opacity-50 hover:bg-[#43A047] transition-colors"
            >
              保存
            </button>
          </div>
        </div>
      )}

      {/* Image Upload Section */}
      <div className="px-4 py-4">
        <ImageUploader
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片分析"
          description="AI将分析并推荐哈苏大师参数"
        />
      </div>

      {/* AI Analyze Button */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <button
            onClick={handleAnalyzeImage}
            disabled={isAnalyzing}
            className="w-full py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium transition-all hover:opacity-90 active:scale-98 disabled:opacity-50"
          >
            {isAnalyzing ? (
              <>
                <RefreshCw size={18} className="animate-spin" />
                <span>AI分析中...</span>
              </>
            ) : (
              <>
                <Wand2 size={18} />
                <span>AI分析推荐参数</span>
              </>
            )}
          </button>
        </div>
      )}

      {/* Hasselblad Presets */}
      {uploadedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3">OPPO 哈苏大师风格参数</p>
          <div className="grid grid-cols-2 gap-3">
            {hasselbladPresets.map((preset) => {
              const Icon = preset.icon;
              const isRecommended = recommendedPreset === preset.id;
              const isApplied = appliedPreset === preset.id;

              return (
                <button
                  key={preset.id}
                  onClick={() => applyHasselbladPreset(preset)}
                  className={`relative p-4 rounded-2xl transition-all ${isApplied
                    ? 'bg-[#FF6B35]/30 border border-[#FF6B35]'
                    : isRecommended
                      ? 'bg-[#4CAF50]/20 border border-[#4CAF50]/50'
                      : 'bg-white/5 hover:bg-white/10'
                    }`}
                >
                  {isApplied && (
                    <div className="absolute inset-0 flex items-center justify-center bg-[#FF6B35]/20 rounded-2xl">
                      <div className="w-10 h-10 rounded-full bg-[#FF6B35] flex items-center justify-center">
                        <Check size={20} className="text-white" />
                      </div>
                    </div>
                  )}

                  <div className="flex items-center gap-3 relative z-10">
                    <div
                      className="w-12 h-12 rounded-xl flex items-center justify-center"
                      style={{ backgroundColor: `${preset.color}20` }}
                    >
                      <Icon size={24} style={{ color: preset.color }} />
                    </div>
                    <div className="flex-1">
                      <p className="text-white text-sm font-medium">{preset.name}</p>
                      <p className="text-white/50 text-xs">{preset.desc}</p>
                      {isRecommended && (
                        <span className="text-[#4CAF50] text-xs mt-1 flex items-center gap-1">
                          <Sparkles size={10} />
                          AI推荐
                        </span>
                      )}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}

      {/* 实时预览 - 原图vs调节后对比 */}
      {uploadedImage && processedImage && (
        <div className="px-4 pb-4">
          <p className="text-white/50 text-xs mb-3 flex items-center gap-2">
            <Eye size={12} />
            实时预览 · 原图 vs 调节效果
            {isProcessing && <RefreshCw size={10} className="animate-spin text-[#FF6B35]" />}
          </p>
          <div className="grid grid-cols-2 gap-2 mb-3">
            <div className="rounded-xl overflow-hidden">
              <img src={uploadedImage} alt="原图" className="w-full aspect-video object-cover" />
              <div className="p-2 bg-white/5 text-center">
                <span className="text-white/50 text-xs">原图</span>
              </div>
            </div>
            <div className="rounded-xl overflow-hidden relative">
              <img src={processedImage} alt="调节后" className="w-full aspect-video object-cover" />
              {isProcessing && (
                <div className="absolute inset-0 flex items-center justify-center bg-black/30">
                  <RefreshCw size={18} className="text-white animate-spin" />
                </div>
              )}
              <div className="p-2 bg-[#E91E63]/20 text-center">
                <span className="text-[#E91E63] text-xs">调节效果</span>
              </div>
            </div>
          </div>
          <button
            onClick={() => downloadImage(processedImage, `OMaster_Adjust_${Date.now()}.jpg`)}
            className="w-full py-2.5 rounded-xl bg-gradient-to-r from-[#E91E63] to-[#C2185B] flex items-center justify-center gap-2 text-white text-sm font-medium"
          >
            <Download size={16} />
            <span>保存调节出片</span>
          </button>
        </div>
      )}

      {/* Quick Presets */}
      <div className="px-4 py-4">
        <p className="text-white/50 text-xs mb-3">快捷档位</p>
        <div className="flex gap-2">
          {quickPresets.map((preset) => (
            <button
              key={preset.name}
              onClick={() => applyQuickPreset(preset)}
              className="flex-1 py-2 rounded-xl bg-white/5 text-white text-sm font-medium transition-all hover:bg-white/10 active:scale-95"
            >
              {preset.name}
            </button>
          ))}
        </div>
      </div>

      {/* Param Controls */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* Camera Params */}
        <div className="space-y-4 mb-6">
          <p className="text-white/50 text-xs">相机参数</p>
          {params.map((param) => {
            const Icon = param.icon;
            const value = cameraParams[param.key as keyof typeof cameraParams];
            const isShutterWithLinkage = param.key === 'shutter' && linkageRecommendation.shutter !== null;
            const isWbWithLinkage = param.key === 'wb' && linkageRecommendation.warmth !== null;

            return (
              <div key={param.key} className="bg-white/5 rounded-2xl p-4">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl bg-[#E91E63]/20 flex items-center justify-center">
                    <Icon size={20} className="text-[#E91E63]" />
                  </div>
                  <div className="flex-1">
                    <span className="text-white text-sm font-medium">{param.label}</span>
                    <span className="text-[#E91E63] text-lg font-bold ml-2">
                      {param.format ? param.format(value) : value}
                    </span>
                  </div>
                  {isWbWithLinkage && (
                    <Lightbulb size={14} className="text-[#FF6B35] animate-pulse" />
                  )}
                </div>

                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  className="w-full h-3 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#E91E63]"
                />

                {param.marks && (
                  <div className="flex justify-between mt-2">
                    {param.marks.map((mark) => (
                      <button
                        key={mark}
                        onClick={() => setCameraParam(param.key, mark)}
                        className="text-white/30 text-[10px] hover:text-white/60 transition-colors"
                      >
                        {mark}
                      </button>
                    ))}
                  </div>
                )}

                {/* 快门速度联动推荐 */}
                {isShutterWithLinkage && (
                  <button
                    onClick={acceptShutterLinkage}
                    className="mt-3 w-full px-3 py-2 rounded-xl bg-[#FF6B35]/15 border border-[#FF6B35]/30 text-[#FF6B35] text-xs flex items-center justify-center gap-1.5 hover:bg-[#FF6B35]/25 transition-colors"
                  >
                    <Zap size={12} />
                    <span>ISO变化 · 推荐快门 {formatShutter(linkageRecommendation.shutter!)}</span>
                    <span className="text-[#FF6B35]/60 ml-1">点击应用</span>
                  </button>
                )}

                {/* 白平衡联动提示 */}
                {isWbWithLinkage && (
                  <button
                    onClick={acceptWarmthLinkage}
                    className="mt-3 w-full px-3 py-2 rounded-xl bg-[#FF6B35]/15 border border-[#FF6B35]/30 text-[#FF6B35] text-xs flex items-center justify-center gap-1.5 hover:bg-[#FF6B35]/25 transition-colors"
                  >
                    <Zap size={12} />
                    <span>白平衡变化 · 推荐色温偏移 {linkageRecommendation.warmth! > 0 ? '+' : ''}{linkageRecommendation.warmth}</span>
                    <span className="text-[#FF6B35]/60 ml-1">点击应用</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* AI Params */}
        <div className="space-y-4 mb-6">
          <p className="text-white/50 text-xs">调色参数</p>
          {aiParamsList.map((param) => {
            const value = aiParams[param.key as keyof typeof aiParams];
            const isWarmthWithLinkage = param.key === 'warmth' && linkageRecommendation.warmth !== null;

            return (
              <div key={param.key} className="bg-white/5 rounded-xl p-4">
                <div className="flex items-center justify-between mb-2">
                  <div className="flex items-center gap-2">
                    <span className="text-white text-sm font-medium">{param.label}</span>
                    {isWarmthWithLinkage && (
                      <Lightbulb size={12} className="text-[#FF6B35] animate-pulse" />
                    )}
                  </div>
                  <span className="text-[#FF6B35] text-sm font-bold">
                    {value > 0 ? '+' : ''}{value}
                  </span>
                </div>
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  value={value}
                  onChange={(e) => setAiParam(param.key, parseInt(e.target.value))}
                  className="w-full h-2 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#FF6B35]"
                />

                {/* 色温偏移联动推荐 */}
                {isWarmthWithLinkage && (
                  <button
                    onClick={acceptWarmthLinkage}
                    className="mt-3 w-full px-3 py-2 rounded-xl bg-[#FF6B35]/15 border border-[#FF6B35]/30 text-[#FF6B35] text-xs flex items-center justify-center gap-1.5 hover:bg-[#FF6B35]/25 transition-colors"
                  >
                    <Zap size={12} />
                    <span>白平衡变化 · 推荐色温偏移 {linkageRecommendation.warmth! > 0 ? '+' : ''}{linkageRecommendation.warmth}</span>
                    <span className="text-[#FF6B35]/60 ml-1">点击应用</span>
                  </button>
                )}
              </div>
            );
          })}
        </div>

        {/* 已保存预设列表 */}
        <div className="mb-6">
          <button
            onClick={() => setShowSavedPresets(!showSavedPresets)}
            className="w-full flex items-center justify-between py-2 text-white/50 text-xs"
          >
            <div className="flex items-center gap-2">
              <Save size={12} />
              <span>已保存预设 ({savedPresets.length})</span>
            </div>
            {showSavedPresets ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          </button>

          {showSavedPresets && (
            <div className="mt-2 space-y-2">
              {savedPresets.length === 0 ? (
                <div className="py-6 text-center">
                  <Save size={24} className="text-white/20 mx-auto mb-2" />
                  <p className="text-white/30 text-xs">暂无保存的预设</p>
                  <p className="text-white/20 text-[10px] mt-1">点击顶部保存按钮创建预设</p>
                </div>
              ) : (
                savedPresets.map((preset) => (
                  <div key={preset.id} className="bg-white/5 rounded-xl p-3">
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <span className="text-white text-sm font-medium">{preset.name}</span>
                        <span className="text-white/30 text-[10px]">{formatRelativeTime(preset.timestamp)}</span>
                      </div>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => handleLoadPreset(preset)}
                          className="px-2 py-1 rounded-lg bg-[#4CAF50]/20 text-[#4CAF50] text-[10px] hover:bg-[#4CAF50]/30 transition-colors"
                        >
                          应用
                        </button>
                        <button
                          onClick={() => handleDeletePreset(preset.id)}
                          className="p-1 rounded-lg hover:bg-white/10 transition-colors"
                        >
                          <Trash2 size={12} className="text-white/30" />
                        </button>
                      </div>
                    </div>
                    <div className="flex gap-3 text-white/40 text-[10px]">
                      <span>ISO {preset.cameraParams.iso}</span>
                      <span>{formatShutter(preset.cameraParams.shutter)}</span>
                      <span>f/{preset.cameraParams.aperture.toFixed(1)}</span>
                      <span>{preset.cameraParams.wb}K</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>

        {/* 参数历史记录 */}
        <div className="mb-6">
          <button
            onClick={() => setShowHistory(!showHistory)}
            className="w-full flex items-center justify-between py-2 text-white/50 text-xs"
          >
            <div className="flex items-center gap-2">
              <History size={12} />
              <span>参数历史 ({paramHistory.length})</span>
            </div>
            {showHistory ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
          </button>

          {showHistory && (
            <div className="mt-2 space-y-2">
              {paramHistory.length === 0 ? (
                <div className="py-6 text-center">
                  <Clock size={24} className="text-white/20 mx-auto mb-2" />
                  <p className="text-white/30 text-xs">暂无历史记录</p>
                  <p className="text-white/20 text-[10px] mt-1">调整参数后将自动记录</p>
                </div>
              ) : (
                paramHistory.map((snapshot, index) => (
                  <button
                    key={snapshot.timestamp}
                    onClick={() => rollbackToSnapshot(snapshot)}
                    className="w-full bg-white/5 rounded-xl p-3 text-left hover:bg-white/10 transition-colors"
                  >
                    <div className="flex items-center justify-between mb-1">
                      <div className="flex items-center gap-2">
                        <History size={10} className="text-white/30" />
                        <span className="text-white/50 text-[10px]">{formatRelativeTime(snapshot.timestamp)}</span>
                        {index === 0 && (
                          <span className="text-[#FF6B35] text-[9px] px-1 py-0.5 rounded bg-[#FF6B35]/10">最近</span>
                        )}
                      </div>
                      <span className="text-[#4CAF50] text-[10px]">点击回退</span>
                    </div>
                    <div className="flex gap-3 text-white/40 text-[10px]">
                      <span>ISO {snapshot.cameraParams.iso}</span>
                      <span>{formatShutter(snapshot.cameraParams.shutter)}</span>
                      <span>f/{snapshot.cameraParams.aperture.toFixed(1)}</span>
                      <span>{snapshot.cameraParams.wb}K</span>
                      <span>饱和{snapshot.aiParams.saturation > 0 ? '+' : ''}{snapshot.aiParams.saturation}</span>
                    </div>
                  </button>
                ))
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;
