import React, { useState } from 'react';
import { ArrowLeft, Upload, Sun, Thermometer, Wand2, Settings } from 'lucide-react';

const RAWProcessingPage: React.FC = () => {
  const [wbTemp, setWbTemp] = useState(5000);

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">RAW 处理</h1>
          <p className="text-xs text-white/50">专业格式处理</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Upload Section */}
        <button className="w-full border-2 border-dashed border-white/20 rounded-2xl p-8 flex flex-col items-center gap-3 mb-4 hover:border-[#FF6B35] transition-all">
          <Upload size={32} className="text-white/40" />
          <span className="text-sm text-white/60">选择 RAW 文件</span>
          <span className="text-xs text-white/40">DNG、CR2、NEF、ARW、RAF、ORF、RW2</span>
        </button>

        {/* Image Preview */}
        <div className="aspect-[4/3] bg-gradient-to-br from-[#FF6B35]/10 to-[#4DABF7]/10 rounded-2xl mb-6 overflow-hidden relative">
          <div className="absolute inset-0 flex items-center justify-center text-white/30">
            RAW 预览区域
          </div>
        </div>

        {/* Exposure */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <div className="flex items-center gap-3 mb-4">
            <Sun size={20} className="text-[#FF6B35]" />
            <h3 className="text-sm font-semibold">曝光</h3>
          </div>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">曝光值</span>
                <span className="text-sm text-[#FF6B35]">0.0 EV</span>
              </div>
              <input type="range" className="w-full" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <button className="bg-white/5 rounded-xl py-3 text-sm">高光 -10</button>
              <button className="bg-white/5 rounded-xl py-3 text-sm">阴影 +15</button>
            </div>
          </div>
        </div>

        {/* White Balance */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <div className="flex items-center gap-3 mb-4">
            <Thermometer size={20} className="text-[#FF6B35]" />
            <h3 className="text-sm font-semibold">白平衡</h3>
          </div>
          <div className="flex gap-2 mb-4 overflow-x-auto pb-2">
            {['自动', '日光', '阴天', '钨丝灯', '荧光灯', '闪光灯'].map((wb) => (
              <button 
                key={wb}
                className="flex-shrink-0 px-4 py-2 rounded-lg bg-white/5 text-sm hover:bg-[#FF6B35]/20 transition-all"
              >
                {wb}
              </button>
            ))}
          </div>
          <div>
            <div className="flex justify-between mb-2">
              <span className="text-sm">色温</span>
              <span className="text-sm text-[#FF6B35]">{wbTemp} K</span>
            </div>
            <input 
              type="range" 
              min="2000" 
              max="10000" 
              value={wbTemp} 
              onChange={(e) => setWbTemp(Number(e.target.value))}
              className="w-full" 
            />
          </div>
        </div>

        {/* Effects */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <div className="flex items-center gap-3 mb-4">
            <Wand2 size={20} className="text-[#FF6B35]" />
            <h3 className="text-sm font-semibold">效果</h3>
          </div>
          <div className="grid grid-cols-2 gap-3">
            {['清晰度 +5', '鲜艳度 +10', '饱和度 +5', '锐化 30'].map((eff) => (
              <button 
                key={eff}
                className="bg-white/5 rounded-xl py-3 text-sm hover:bg-white/10 transition-all"
              >
                {eff}
              </button>
            ))}
          </div>
        </div>

        {/* Noise Reduction */}
        <div className="bg-white/5 rounded-2xl p-4 mb-6">
          <div className="flex items-center gap-3 mb-4">
            <Settings size={20} className="text-[#FF6B35]" />
            <h3 className="text-sm font-semibold">降噪</h3>
          </div>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">亮度降噪</span>
                <span className="text-sm text-[#FF6B35]">20</span>
              </div>
              <input type="range" className="w-full" />
            </div>
            <div>
              <div className="flex justify-between mb-2">
                <span className="text-sm">彩色降噪</span>
                <span className="text-sm text-[#FF6B35]">30</span>
              </div>
              <input type="range" className="w-full" />
            </div>
          </div>
        </div>

        {/* Save Button */}
        <button className="w-full py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold flex items-center justify-center gap-2">
          <Upload size={18} />
          导出 JPG
        </button>
      </div>
    </div>
  );
};

export default RAWProcessingPage;
