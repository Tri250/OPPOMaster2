import React, { useRef, useState, useEffect } from 'react';
import { Upload, X, Sparkles } from 'lucide-react';
import { userImageStore, UserImage } from '../store/userImageStore';
import { imageAnalysisService } from '../services/imageAnalysisService';

interface ImageUploaderProps {
  onImageLoaded?: (image: UserImage) => void;
  onError?: (error: string) => void;
  // 推荐的示例图片
  sampleImages?: Array<{ url: string; label: string; tag: string }>;
  // 是否显示示例图片（默认显示）
  showSamples?: boolean;
  // 上传按钮文字
  buttonText?: string;
  // 提示文字
  hint?: string;
}

/**
 * 通用图片上传组件
 * 支持本地文件上传和示例图片选择
 */
const ImageUploader: React.FC<ImageUploaderProps> = ({
  onImageLoaded,
  onError,
  sampleImages = [],
  showSamples = true,
  buttonText = '上传图片',
  hint,
}) => {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [currentImage, setCurrentImage] = useState<UserImage | null>(null);
  const [uploading, setUploading] = useState(false);
  const [dragActive, setDragActive] = useState(false);

  useEffect(() => {
    const unsubscribe = userImageStore.subscribe(image => {
      setCurrentImage(image);
    });
    return unsubscribe;
  }, []);

  const handleFileSelect = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      onError?.('请选择图片文件');
      return;
    }
    if (file.size > 20 * 1024 * 1024) {
      onError?.('图片大小不能超过 20MB');
      return;
    }
    setUploading(true);
    try {
      const image = await imageAnalysisService.loadFromFile(file);
      onImageLoaded?.(image);
    } catch (e) {
      onError?.(e instanceof Error ? e.message : '上传失败');
    } finally {
      setUploading(false);
    }
  };

  const handleSampleSelect = async (url: string, label: string) => {
    setUploading(true);
    try {
      const image = await imageAnalysisService.loadFromUrl(url, `${label}.jpg`);
      onImageLoaded?.(image);
    } catch (e) {
      onError?.(e instanceof Error ? e.message : '加载示例失败');
    } finally {
      setUploading(false);
    }
  };

  const handleClear = () => {
    userImageStore.clear();
  };

  const handleDrag = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    const file = e.dataTransfer.files?.[0];
    if (file) handleFileSelect(file);
  };

  // 如果已有图片，显示图片缩略图
  if (currentImage) {
    return (
      <div className="relative w-full">
        <div className="relative aspect-video rounded-2xl overflow-hidden bg-gradient-to-br from-gray-900 to-gray-800">
          <img 
            src={currentImage.dataUrl} 
            alt="上传图片" 
            className="w-full h-full object-cover"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-transparent" />
          
          {/* 已上传徽章 */}
          <div className="absolute top-3 left-3 px-3 py-1.5 rounded-full bg-[#4CAF50]/80 backdrop-blur-sm flex items-center gap-1.5">
            <Sparkles size={12} className="text-white" />
            <span className="text-white text-xs font-medium">已上传</span>
          </div>

          {/* 替换按钮 */}
          <button
            onClick={handleClear}
            className="absolute top-3 right-3 p-1.5 rounded-full bg-black/50 backdrop-blur-sm hover:bg-black/70 transition-colors"
          >
            <X size={14} className="text-white" />
          </button>

          {/* 文件信息 */}
          <div className="absolute bottom-3 left-3 right-3">
            <p className="text-white text-sm font-medium truncate">{currentImage.fileName}</p>
            <p className="text-white/60 text-xs">
              {currentImage.width} × {currentImage.height} · {(currentImage.size / 1024).toFixed(0)} KB
            </p>
          </div>
        </div>
        {hint && (
          <p className="text-white/40 text-xs mt-2 text-center">{hint}</p>
        )}
      </div>
    );
  }

  return (
    <div className="w-full">
      {/* 上传区 */}
      <div
        onDragEnter={handleDrag}
        onDragLeave={handleDrag}
        onDragOver={handleDrag}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        className={`relative aspect-video rounded-2xl border-2 border-dashed transition-all cursor-pointer overflow-hidden ${
          dragActive 
            ? 'border-[#FF6B35] bg-[#FF6B35]/10' 
            : 'border-white/20 bg-white/5 hover:border-white/30 hover:bg-white/10'
        } ${uploading ? 'pointer-events-none' : ''}`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={(e) => {
            const file = e.target.files?.[0];
            if (file) handleFileSelect(file);
          }}
          className="hidden"
        />
        
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-3">
          {uploading ? (
            <>
              <div className="w-12 h-12 rounded-full border-4 border-[#FF6B35] border-t-transparent animate-spin" />
              <p className="text-white text-sm">加载中...</p>
            </>
          ) : (
            <>
              <div className="w-16 h-16 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
                <Upload size={28} className="text-[#FF6B35]" />
              </div>
              <div className="text-center">
                <p className="text-white text-sm font-medium">{buttonText}</p>
                <p className="text-white/50 text-xs mt-1">点击或拖拽图片到此处 · 支持 JPG/PNG/HEIC</p>
                <p className="text-white/30 text-[10px] mt-0.5">最大 20MB</p>
              </div>
            </>
          )}
        </div>
      </div>

      {/* 示例图片 */}
      {showSamples && sampleImages.length > 0 && (
        <div className="mt-3">
          <p className="text-white/40 text-xs mb-2">没有图片？试试这些示例：</p>
          <div className="flex gap-2 overflow-x-auto scrollbar-hide pb-1">
            {sampleImages.map((sample, i) => (
              <button
                key={i}
                onClick={() => handleSampleSelect(sample.url, sample.label)}
                disabled={uploading}
                className="flex-shrink-0 relative rounded-lg overflow-hidden group"
              >
                <img 
                  src={sample.url} 
                  alt={sample.label}
                  className="w-20 h-20 object-cover transition-transform group-hover:scale-110"
                />
                <div className="absolute inset-0 bg-black/40 group-hover:bg-black/20 transition-colors flex items-end p-1.5">
                  <span className="text-white text-[10px] font-medium">{sample.label}</span>
                </div>
                <div className="absolute top-1 right-1 px-1 py-0.5 rounded bg-[#FF6B35]/80 text-white text-[8px]">
                  {sample.tag}
                </div>
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default ImageUploader;
