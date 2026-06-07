import React, { useState } from 'react';
import { ArrowLeft, Upload, Play, X, Check, RefreshCw } from 'lucide-react';

const BatchProcessingPage: React.FC = () => {
  const [processing, setProcessing] = useState(false);
  const [progress, setProgress] = useState(0);

  const images = [
    { id: 1, status: 'pending' },
    { id: 2, status: 'pending' },
    { id: 3, status: 'pending' },
  ];

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">批量处理</h1>
          <p className="text-xs text-white/50">多图同时调整</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Upload Section */}
        <button className="w-full border-2 border-dashed border-white/20 rounded-2xl p-8 flex flex-col items-center gap-3 mb-4 hover:border-[#FF6B35] transition-all">
          <Upload size={32} className="text-white/40" />
          <span className="text-sm text-white/60">点击或拖拽添加图片</span>
          <span className="text-xs text-white/40">支持 JPG、PNG、WebP</span>
        </button>

        {/* Image Grid */}
        <div className="grid grid-cols-3 gap-2 mb-6">
          {images.map((img) => (
            <div key={img.id} className="aspect-square bg-white/5 rounded-xl relative overflow-hidden">
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-full h-full bg-gradient-to-br from-[#FF6B35]/20 to-[#4DABF7]/20" />
              </div>
              {img.status === 'success' && (
                <div className="absolute inset-0 bg-black/30 flex items-center justify-center">
                  <Check className="text-green-400" size={24} />
                </div>
              )}
            </div>
          ))}
        </div>

        {/* Processing Parameters */}
        <div className="bg-white/5 rounded-2xl p-4 mb-6">
          <h3 className="text-sm font-semibold mb-4">处理参数</h3>
          
          <div className="space-y-4">
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">饱和度</span>
                <span className="text-sm text-[#FF6B35]">+15</span>
              </div>
              <input type="range" className="w-full" />
            </div>

            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">对比度</span>
                <span className="text-sm text-[#FF6B35]">+10</span>
              </div>
              <input type="range" className="w-full" />
            </div>

            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">明亮度</span>
                <span className="text-sm text-[#FF6B35]">+5</span>
              </div>
              <input type="range" className="w-full" />
            </div>
          </div>
        </div>

        {/* Output Quality */}
        <div className="bg-white/5 rounded-2xl p-4 mb-6">
          <h3 className="text-sm font-semibold mb-4">输出设置</h3>
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm">输出质量</span>
              <span className="text-sm text-[#FF6B35]">85%</span>
            </div>
            <input type="range" className="w-full" />
          </div>
        </div>

        {/* Progress */}
        {processing && (
          <div className="bg-white/5 rounded-2xl p-4 mb-6">
            <div className="flex justify-between mb-2">
              <span className="text-sm">处理中...</span>
              <span className="text-sm text-[#FF6B35]">{progress}%</span>
            </div>
            <div className="w-full h-2 bg-white/10 rounded-full overflow-hidden">
              <div 
                className="h-full bg-[#FF6B35] transition-all duration-300"
                style={{ width: `${progress}%` }}
              />
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button 
            className="flex-1 py-4 rounded-xl border border-white/20 text-sm font-semibold flex items-center justify-center gap-2"
            onClick={() => {
              setProcessing(false);
              setProgress(0);
            }}
          >
            <RefreshCw size={18} />
            重置
          </button>
          <button 
            className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold flex items-center justify-center gap-2"
            onClick={() => {
              setProcessing(true);
              let p = 0;
              const interval = setInterval(() => {
                p += Math.random() * 15;
                if (p >= 100) {
                  p = 100;
                  clearInterval(interval);
                }
                setProgress(p);
              }, 500);
            }}
          >
            <Play size={18} />
            {processing ? '处理中...' : '开始处理'}
          </button>
        </div>
      </div>
    </div>
  );
};

export default BatchProcessingPage;
