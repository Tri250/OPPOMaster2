import React, { useState, useRef } from 'react';
import { Upload, Image, X, Camera, Sparkles } from 'lucide-react';

interface ImageUploaderProps {
  onImageSelect: (imageUrl: string) => void;
  currentImage?: string;
  title?: string;
  description?: string;
}

const ImageUploader: React.FC<ImageUploaderProps> = ({
  onImageSelect,
  currentImage,
  title = '上传照片',
  description = '选择或拍摄照片进行分析'
}) => {
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    
    const files = e.dataTransfer.files;
    if (files.length > 0 && files[0].type.startsWith('image/')) {
      handleFile(files[0]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (files && files.length > 0) {
      handleFile(files[0]);
    }
  };

  const handleFile = (file: File) => {
    setIsUploading(true);
    
    // 创建本地预览URL
    const reader = new FileReader();
    reader.onload = (e) => {
      const imageUrl = e.target?.result as string;
      setTimeout(() => {
        setIsUploading(false);
        onImageSelect(imageUrl);
      }, 500);
    };
    reader.readAsDataURL(file);
  };

  const handleClick = () => {
    fileInputRef.current?.click();
  };

  const handleRemove = () => {
    onImageSelect('');
  };

  // 示例图片
  const sampleImages = [
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=300&fit=crop',
    'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=300&fit=crop',
  ];

  if (currentImage) {
    return (
      <div className="relative">
        <img
          src={currentImage}
          alt="Selected image"
          className="w-full aspect-video object-cover rounded-2xl"
        />
        <button
          onClick={handleRemove}
          className="absolute top-2 right-2 p-2 rounded-full bg-black/50 backdrop-blur-sm hover:bg-black/70 transition-colors"
        >
          <X size={16} className="text-white" />
        </button>
        <div className="absolute bottom-2 left-2 px-2 py-1 rounded-lg bg-black/50 backdrop-blur-sm">
          <span className="text-white/70 text-xs flex items-center gap-1">
            <Image size={12} />
            已选择照片
          </span>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-3">
      {/* Upload Area */}
      <div
        onClick={handleClick}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        className={`relative aspect-video rounded-2xl border-2 cursor-pointer transition-all ${
          isDragging
            ? 'border-[#FF6B35] bg-[#FF6B35]/10'
            : 'border-white/10 bg-white/5 hover:border-white/20 hover:bg-white/10'
        }`}
      >
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          onChange={handleFileSelect}
          className="hidden"
        />

        {isUploading ? (
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <div className="w-12 h-12 rounded-full border-4 border-[#FF6B35] border-t-transparent animate-spin" />
            <span className="text-white/70 text-sm mt-3">上传中...</span>
          </div>
        ) : (
          <div className="absolute inset-0 flex flex-col items-center justify-center">
            <div className="w-16 h-16 rounded-2xl bg-[#FF6B35]/20 flex items-center justify-center mb-3">
              <Upload size={24} className="text-[#FF6B35]" />
            </div>
            <span className="text-white text-sm font-medium">{title}</span>
            <span className="text-white/50 text-xs mt-1">{description}</span>
          </div>
        )}
      </div>

      {/* Sample Images */}
      <div>
        <p className="text-white/50 text-xs mb-2">或选择示例照片</p>
        <div className="grid grid-cols-4 gap-2">
          {sampleImages.map((img, idx) => (
            <button
              key={idx}
              onClick={() => onImageSelect(img)}
              className="aspect-square rounded-xl overflow-hidden bg-white/5 hover:ring-2 hover:ring-[#FF6B35] transition-all"
            >
              <img
                src={img}
                alt={`Sample ${idx + 1}`}
                className="w-full h-full object-cover"
              />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ImageUploader;