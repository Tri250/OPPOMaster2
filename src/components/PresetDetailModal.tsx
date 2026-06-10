import React, { useState, useRef, useCallback, useEffect } from 'react';
import { 
  Upload, Download, Share2, RotateCcw, CheckCircle, 
  Sparkles, Heart, X, Image as ImageIcon, Loader2,
  ChevronLeft, ChevronRight, ZoomIn, ZoomOut
} from 'lucide-react';
import { Preset, HasselbladColors } from '../store/appStore';
import { 
  getImageProcessor, 
  ImageProcessResult, 
  ProcessState,
  convertPresetToRenderParams
} from '../services/imageProcessor';
import { AIFineTuneParams } from '../services/aiInferenceService';
import PresetImageGallery from './PresetImageGallery';
import PresetParameters, { PresetStats, ShootingTipsCard, UserComments } from './PresetParameters';

// ============================================
// 类型定义
// ============================================

interface PresetDetailModalProps {
  preset: Preset;
  isFavorite: boolean;
  onToggleFavorite: () => void;
  onClose: () => void;
  onNavigate?: (presetId: string) => void;
  relatedPresets?: Array<{ id: string; name: string; coverPath: string }>;
}

// ============================================
// 主组件
// ============================================

const PresetDetailModal: React.FC<PresetDetailModalProps> = ({
  preset,
  isFavorite,
  onToggleFavorite,
  onClose,
  relatedPresets,
}) => {
  // 状态
  const [userImage, setUserImage] = useState<string | null>(null);
  const [processedImage, setProcessedImage] = useState<string | null>(null);
  const [processState, setProcessState] = useState<ProcessState>('idle');
  const [processProgress, setProcessProgress] = useState(0);
  const [processMessage, setProcessMessage] = useState('');
  const [showComparison, setShowComparison] = useState(false);
  const [comparisonPosition, setComparisonPosition] = useState(50);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [currentGalleryIndex, setCurrentGalleryIndex] = useState(0);
  
  // 引用
  const fileInputRef = useRef<HTMLInputElement>(null);
  const comparisonRef = useRef<HTMLDivElement>(null);
  const processor = getImageProcessor();
  
  // 处理进度回调
  const handleProgress = useCallback((state: ProcessState, progress: number, message: string) => {
    setProcessState(state);
    setProcessProgress(progress);
    setProcessMessage(message);
  }, []);
  
  // 上传图片
  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };
  
  // 处理文件选择
  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      alert('请选择图片文件');
      return;
    }
    
    // 验证文件大小（最大 10MB）
    if (file.size > 10 * 1024 * 1024) {
      alert('图片大小不能超过 10MB');
      return;
    }
    
    try {
      // 加载图片
      handleProgress('loading', 0.1, '加载图片...');
      const img = await processor.loadImage(file);
      
      // 显示原图
      const originalDataUrl = await createDataUrlFromImage(img);
      setUserImage(originalDataUrl);
      setProcessedImage(null);
      handleProgress('idle', 0, '');
      
    } catch (error) {
      handleProgress('error', 0, '图片加载失败');
      console.error('Image load error:', error);
    }
    
    // 清空 input
    e.target.value = '';
  };
  
  // 从 HTMLImageElement 创建 DataUrl
  const createDataUrlFromImage = async (img: HTMLImageElement): Promise<string> => {
    const canvas = document.createElement('canvas');
    canvas.width = img.naturalWidth || img.width;
    canvas.height = img.naturalHeight || img.height;
    const ctx = canvas.getContext('2d');
    ctx?.drawImage(img, 0, 0);
    return canvas.toDataURL('image/png');
  };
  
  // 一键应用哈苏配方
  const handleApplyPreset = async () => {
    if (!userImage) {
      alert('请先上传图片');
      return;
    }
    
    try {
      handleProgress('processing', 0.2, '解析哈苏配方参数...');
      
      // 应用预设
      const result = await processor.applyPreset(preset, handleProgress);
      
      if (result.success && result.dataUrl) {
        setProcessedImage(result.dataUrl);
        setShowComparison(true);
        handleProgress('completed', 1.0, '哈苏配方应用完成');
        
        // 3秒后重置状态
        setTimeout(() => {
          setProcessState('idle');
          setProcessProgress(0);
          setProcessMessage('');
        }, 3000);
      } else {
        handleProgress('error', 0, result.error || '处理失败');
      }
      
    } catch (error) {
      handleProgress('error', 0, '处理失败');
      console.error('Apply preset error:', error);
    }
  };
  
  // 下载处理后的图片
  const handleDownload = () => {
    if (!processedImage) return;
    
    const link = document.createElement('a');
    link.download = `omaster-${preset.name}-${Date.now()}.png`;
    link.href = processedImage;
    link.click();
  };
  
  // 重置图片
  const handleReset = () => {
    setUserImage(null);
    setProcessedImage(null);
    setShowComparison(false);
    setZoomLevel(1);
    handleProgress('idle', 0, '');
  };
  
  // 比较滑块拖动
  const handleComparisonDrag = useCallback((e: React.MouseEvent | React.TouchEvent) => {
    if (!comparisonRef.current) return;
    
    const rect = comparisonRef.current.getBoundingClientRect();
    const clientX = 'touches' in e ? e.touches[0].clientX : e.clientX;
    const position = ((clientX - rect.left) / rect.width) * 100;
    setComparisonPosition(Math.max(0, Math.min(100, position)));
  }, []);
  
  // 缩放控制
  const handleZoomIn = () => setZoomLevel(Math.min(zoomLevel + 0.25, 3));
  const handleZoomOut = () => setZoomLevel(Math.max(zoomLevel - 0.25, 0.5));
  
  // 获取渲染参数显示
  const renderParams = convertPresetToRenderParams(preset);
  
  // 清理
  useEffect(() => {
    return () => {
      // 组件卸载时清理状态
      if (processState !== 'idle') {
        handleProgress('idle', 0, '');
      }
    };
  }, [handleProgress, processState]);
  
  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm"
      onClick={onClose}
    >
      <div
        className="relative w-full max-w-md bg-[#0a0a0a] rounded-2xl overflow-hidden shadow-2xl animate-in fade-in zoom-in duration-200 max-h-[90vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 顶栏 */}
        <div className="sticky top-0 z-10 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 flex items-center justify-between border-b border-white/5">
          <span className="text-white/50 text-sm">哈苏大师配方详情</span>
          <div className="flex items-center gap-2">
            <button
              onClick={onToggleFavorite}
              className={`p-1.5 rounded-lg transition-colors ${
                isFavorite ? 'bg-red-500/20' : 'bg-white/5 hover:bg-white/10'
              }`}
            >
              <Heart size={16} className={isFavorite ? 'text-red-400 fill-red-400' : 'text-white/50'} />
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg bg-white/5 hover:bg-white/10 transition-colors"
            >
              <X size={16} className="text-white/50" />
            </button>
          </div>
        </div>

        {/* 图片处理区域 */}
        <div className="px-4 pt-4">
          {/* 用户图片上传/预览 */}
          {!userImage ? (
            <div className="relative rounded-xl overflow-hidden bg-[#1a1a1a] aspect-[4/3] flex flex-col items-center justify-center border border-white/10">
              {/* 上传按钮 */}
              <button
                onClick={handleUploadClick}
                className="flex flex-col items-center justify-center gap-3 p-8 rounded-xl hover:bg-white/5 transition-all cursor-pointer"
              >
                <div className="w-16 h-16 rounded-full bg-gradient-to-br from-[#FF6B35]/20 to-[#FF6B35]/5 flex items-center justify-center">
                  <Upload size={24} className="text-[#FF6B35]" />
                </div>
                <span className="text-white/60 text-sm">上传图片体验哈苏配方</span>
                <span className="text-white/30 text-xs">支持 JPG、PNG、WebP</span>
              </button>
              
              {/* 隐藏的文件输入 */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="hidden"
              />
              
              {/* 或使用示例图片 */}
              <button
                onClick={() => setUserImage(preset.coverPath)}
                className="mt-4 px-4 py-2 rounded-lg bg-white/5 text-white/50 text-xs hover:bg-white/10 transition-colors"
              >
                使用示例图片
              </button>
            </div>
          ) : (
            <div className="relative rounded-xl overflow-hidden bg-[#1a1a1a]">
              {/* 比较视图 */}
              {showComparison && processedImage ? (
                <div 
                  ref={comparisonRef}
                  className="relative aspect-[4/3] overflow-hidden cursor-ew-resize"
                  onMouseMove={handleComparisonDrag}
                  onTouchMove={handleComparisonDrag}
                >
                  {/* 原图 */}
                  <img
                    src={userImage}
                    alt="原图"
                    className="absolute inset-0 w-full h-full object-cover"
                    style={{ transform: `scale(${zoomLevel})` }}
                  />
                  
                  {/* 处理后的图片 */}
                  <div 
                    className="absolute inset-0 overflow-hidden"
                    style={{ clipPath: `inset(0 ${100 - comparisonPosition}% 0 0)` }}
                  >
                    <img
                      src={processedImage}
                      alt="处理后"
                      className="absolute inset-0 w-full h-full object-cover"
                      style={{ transform: `scale(${zoomLevel})` }}
                    />
                  </div>
                  
                  {/* 分割线 */}
                  <div 
                    className="absolute top-0 bottom-0 w-1 bg-white shadow-lg"
                    style={{ left: `${comparisonPosition}%` }}
                  >
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white flex items-center justify-center shadow-lg">
                      <ChevronLeft size={14} className="text-gray-800 -ml-1" />
                      <ChevronRight size={14} className="text-gray-800 -mr-1" />
                    </div>
                  </div>
                  
                  {/* 标签 */}
                  <div className="absolute top-3 left-3 px-2 py-1 bg-black/60 rounded-lg text-xs text-white">
                    原图
                  </div>
                  <div className="absolute top-3 right-3 px-2 py-1 bg-[#FF6B35]/80 rounded-lg text-xs text-white">
                    哈苏配方
                  </div>
                </div>
              ) : (
                /* 单图预览 */
                <div className="relative aspect-[4/3] overflow-hidden">
                  <img
                    src={processedImage || userImage}
                    alt={processedImage ? '处理后' : '原图'}
                    className="w-full h-full object-cover transition-transform duration-300"
                    style={{ transform: `scale(${zoomLevel})` }}
                  />
                  
                  {/* 处理状态指示 */}
                  {processState === 'processing' && (
                    <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/50">
                      <Loader2 size={32} className="text-[#FF6B35] animate-spin mb-3" />
                      <span className="text-white text-sm">{processMessage}</span>
                      <div className="mt-2 w-32 h-1 bg-white/20 rounded-full overflow-hidden">
                        <div 
                          className="h-full bg-[#FF6B35] transition-all duration-300"
                          style={{ width: `${processProgress * 100}%` }}
                        />
                      </div>
                    </div>
                  )}
                  
                  {/* 成功状态 */}
                  {processState === 'completed' && processedImage && (
                    <div className="absolute inset-0 flex items-center justify-center bg-black/30 animate-in fade-in duration-500">
                      <div className="flex flex-col items-center gap-2">
                        <CheckCircle size={48} className="text-green-500" />
                        <span className="text-white text-sm font-medium">哈苏配方已应用</span>
                      </div>
                    </div>
                  )}
                </div>
              )}
              
              {/* 图片操作栏 */}
              <div className="absolute bottom-0 left-0 right-0 p-3 bg-gradient-to-t from-black/80 to-transparent">
                <div className="flex items-center justify-between">
                  {/* 左侧操作 */}
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleUploadClick}
                      className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                      title="更换图片"
                    >
                      <Upload size={16} className="text-white" />
                    </button>
                    <button
                      onClick={handleReset}
                      className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                      title="重置"
                    >
                      <RotateCcw size={16} className="text-white" />
                    </button>
                  </div>
                  
                  {/* 缩放控制 */}
                  <div className="flex items-center gap-2">
                    <button
                      onClick={handleZoomOut}
                      className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                      disabled={zoomLevel <= 0.5}
                    >
                      <ZoomOut size={16} className="text-white" />
                    </button>
                    <span className="text-white/60 text-xs">{Math.round(zoomLevel * 100)}%</span>
                    <button
                      onClick={handleZoomIn}
                      className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                      disabled={zoomLevel >= 3}
                    >
                      <ZoomIn size={16} className="text-white" />
                    </button>
                  </div>
                  
                  {/* 右侧操作 */}
                  <div className="flex items-center gap-2">
                    {processedImage && (
                      <>
                        <button
                          onClick={() => setShowComparison(!showComparison)}
                          className={`p-2 rounded-lg transition-colors ${
                            showComparison ? 'bg-[#FF6B35]' : 'bg-white/10 hover:bg-white/20'
                          }`}
                          title="对比查看"
                        >
                          <ImageIcon size={16} className="text-white" />
                        </button>
                        <button
                          onClick={handleDownload}
                          className="p-2 rounded-lg bg-white/10 hover:bg-white/20 transition-colors"
                          title="下载"
                        >
                          <Download size={16} className="text-white" />
                        </button>
                      </>
                    )}
                  </div>
                </div>
              </div>
              
              {/* 隐藏的文件输入 */}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                className="hidden"
              />
            </div>
          )}
        </div>

        {/* 预设信息 */}
        <div className="px-4 py-4">
          <div className="flex items-center gap-2 mb-1">
            <h2 className="text-lg font-bold text-white">{preset.name}</h2>
            {preset.isHncs && (
              <span className="px-1.5 py-0.5 rounded bg-[#FF6B35] text-[9px] font-bold text-white flex items-center gap-0.5">
                <Sparkles size={10} />
                HNCS
              </span>
            )}
          </div>
          <p className="text-white/50 text-sm mb-2">@{preset.author}</p>
          
          {/* 标签 */}
          <div className="flex flex-wrap gap-1.5 mb-4">
            {preset.tags.map((tag) => (
              <span
                key={tag}
                className="px-2 py-0.5 bg-white/5 rounded-full text-[10px] text-white/60"
              >
                #{tag}
              </span>
            ))}
          </div>

          {/* 统计数据 */}
          <PresetStats
            downloads={preset.downloads || 12580}
            rating={preset.rating || 4.9}
            ratingCount={preset.ratingCount || 856}
          />
        </div>

        {/* 拍摄建议 */}
        {preset.description && (
          <div className="px-4 pb-4">
            <ShootingTipsCard description={preset.description} />
          </div>
        )}

        {/* 调色参数 */}
        <div className="px-4 pb-4">
          <PresetParameters sections={preset.sections} />
          
          {/* 实时参数显示 */}
          {userImage && (
            <div className="mt-4 p-3 bg-white/5 rounded-xl">
              <h4 className="text-white/60 text-xs font-medium mb-2 flex items-center gap-2">
                <div className="w-1 h-3 rounded-full bg-[#FF6B35]" />
                实时渲染参数
              </h4>
              <div className="grid grid-cols-3 gap-2 text-xs">
                <div className="text-white/40">饱和度</div>
                <div className="text-[#FF6B35] font-bold">{renderParams.saturation > 0 ? '+' : ''}{renderParams.saturation}</div>
                <div className="text-white/40">对比度</div>
                <div className="text-[#FF6B35] font-bold">{renderParams.contrast > 0 ? '+' : ''}{renderParams.contrast}</div>
                <div className="text-white/40">色温</div>
                <div className="text-[#FF6B35] font-bold">{renderParams.warmth > 0 ? '+' : ''}{renderParams.warmth}</div>
                <div className="text-white/40">锐度</div>
                <div className="text-[#FF6B35] font-bold">{renderParams.sharpness}</div>
                <div className="text-white/40">清晰度</div>
                <div className="text-[#FF6B35] font-bold">{renderParams.clarity > 0 ? '+' : ''}{renderParams.clarity}</div>
              </div>
            </div>
          )}
        </div>

        {/* 关联推荐 */}
        {relatedPresets && relatedPresets.length > 0 && (
          <div className="px-4 pb-4">
            <div className="bg-white/5 rounded-xl p-4">
              <h3 className="text-white/70 text-sm font-medium mb-3 flex items-center gap-2">
                <span className="text-lg">🎞️</span>
                关联推荐
              </h3>
              <div className="flex gap-2 overflow-x-auto scrollbar-hide">
                {relatedPresets.map((rp) => (
                  <button
                    key={rp.id}
                    className="flex-shrink-0 w-20 cursor-pointer hover:opacity-80 transition-opacity"
                  >
                    <div className="aspect-square rounded-lg overflow-hidden bg-[#1a1a1a]">
                      <img
                        src={rp.coverPath}
                        alt={rp.name}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    </div>
                    <p className="text-white/60 text-[10px] mt-1 truncate">{rp.name}</p>
                  </button>
                ))}
              </div>
            </div>
          </div>
        )}

        {/* 用户评价 */}
        <div className="px-4 pb-4">
          <UserComments
            comments={[
              { id: 'c1', user: '摄影爱好者', content: '非常好用的预设！色彩还原很准确', rating: 5 },
              { id: 'c2', user: '专业摄影师', content: '配合哈苏大师模式使用效果绝佳', rating: 5 },
            ]}
            onViewAll={() => console.log('View all comments')}
          />
        </div>

        {/* 底部操作栏 */}
        <div className="sticky bottom-0 bg-[#0a0a0a]/95 backdrop-blur-sm px-4 py-3 border-t border-white/5 flex gap-3">
          <button
            onClick={onToggleFavorite}
            className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-all ${
              isFavorite
                ? 'bg-red-500/20 text-red-400 border border-red-500/30'
                : 'bg-white/5 text-white/80 border border-white/10 hover:bg-white/10'
            }`}
          >
            <Heart size={16} className={isFavorite ? 'fill-current' : ''} />
            {isFavorite ? '已收藏' : '收藏'}
          </button>
          
          {/* 一键应用按钮 */}
          <button
            onClick={handleApplyPreset}
            disabled={!userImage || processState === 'processing'}
            className={`flex-1 py-3 rounded-xl text-sm font-medium flex items-center justify-center gap-2 transition-all ${
              processState === 'completed'
                ? 'bg-green-500 text-white'
                : 'bg-[#FF6B35] text-white hover:bg-[#FF8C42]'
            } disabled:opacity-50 disabled:cursor-not-allowed`}
          >
            {processState === 'processing' ? (
              <>
                <Loader2 size={16} className="animate-spin" />
                {processMessage}
              </>
            ) : processState === 'completed' ? (
              <>
                <CheckCircle size={16} />
                已应用哈苏配方
              </>
            ) : (
              <>
                <Sparkles size={16} />
                一键应用哈苏配方
              </>
            )}
          </button>
        </div>
      </div>
      
      {/* Styles */}
      <style>{`
        .scrollbar-hide::-webkit-scrollbar { display: none; }
        .scrollbar-hide { -ms-overflow-style: none; scrollbar-width: none; }
        .animate-in { animation: animateIn 0.2s ease-out; }
        @keyframes animateIn {
          from { opacity: 0; transform: scale(0.95); }
          to { opacity: 1; transform: scale(1); }
        }
      `}</style>
    </div>
  );
};

export default PresetDetailModal;