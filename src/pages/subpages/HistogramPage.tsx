import React, { useState, useRef, useEffect } from 'react';
import { ArrowLeft, Upload, Eye, AlertTriangle } from 'lucide-react';

const HistogramPage: React.FC = () => {
  const [selectedChannel, setSelectedChannel] = useState('rgb');
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const channels = [
    { id: 'rgb', name: 'RGB' },
    { id: 'red', name: '红' },
    { id: 'green', name: '绿' },
    { id: 'blue', name: '蓝' },
    { id: 'luminance', name: '亮度' },
  ];

  const stats = {
    mean: 128,
    median: 125,
    stdDev: 45,
    dynamicRange: 210,
  };

  const issues = [
    { type: 'warning', text: '高光区域可能过曝' },
    { type: 'info', text: '色彩分布均匀' },
  ];

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;
    ctx.clearRect(0, 0, width, height);

    const data = Array.from({ length: 256 }, () => Math.random() * height * 0.8);
    const maxVal = Math.max(...data);

    const colors = {
      rgb: ['rgba(255,0,0,0.6)', 'rgba(0,255,0,0.6)', 'rgba(0,0,255,0.6)'],
      red: ['rgba(255,100,100,0.8)'],
      green: ['rgba(100,255,100,0.8)'],
      blue: ['rgba(100,100,255,0.8)'],
      luminance: ['rgba(200,200,200,0.8)'],
    };

    const channelColors = colors[selectedChannel as keyof typeof colors];

    channelColors.forEach((color, idx) => {
      const offset = idx * 2 - 1;
      ctx.fillStyle = color;
      data.forEach((val, x) => {
        const barHeight = (val / maxVal) * height * 0.9;
        const xPos = (x / 255) * width;
        const yPos = height - barHeight;
        ctx.fillRect(xPos, yPos, width / 255 + 1, barHeight);
      });
    });

  }, [selectedChannel]);

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">直方图</h1>
          <p className="text-xs text-white/50">色彩分布分析</p>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Upload */}
        <button className="w-full border-2 border-dashed border-white/20 rounded-2xl p-6 flex flex-col items-center gap-2 mb-4 hover:border-[#FF6B35] transition-all">
          <Upload size={24} className="text-white/40" />
          <span className="text-sm text-white/60">导入图片分析</span>
        </button>

        {/* Histogram Canvas */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <canvas 
            ref={canvasRef}
            width={300}
            height={200}
            className="w-full rounded-xl bg-[#1A1A1A]"
          />
        </div>

        {/* Channel Selection */}
        <h3 className="text-sm font-semibold mb-3 text-white/90">显示通道</h3>
        <div className="flex gap-2 mb-6 overflow-x-auto pb-2">
          {channels.map((ch) => (
            <button
              key={ch.id}
              onClick={() => setSelectedChannel(ch.id)}
              className={`flex-shrink-0 px-4 py-2 rounded-xl text-sm font-medium transition-all ${
                selectedChannel === ch.id ? 'bg-[#FF6B35] text-white' : 'bg-white/5 text-white/60'
              }`}
            >
              {ch.name}
            </button>
          ))}
        </div>

        {/* Statistics */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <h3 className="text-sm font-semibold mb-4 flex items-center gap-2">
            <Eye size={16} className="text-[#FF6B35]" />
            统计信息
          </h3>
          <div className="grid grid-cols-2 gap-4">
            {Object.entries(stats).map(([key, val]) => (
              <div key={key} className="bg-white/5 rounded-xl p-3">
                <p className="text-xs text-white/50 mb-1">
                  {key === 'mean' ? '平均值' : 
                   key === 'median' ? '中位数' : 
                   key === 'stdDev' ? '标准差' : '动态范围'}
                </p>
                <p className="text-lg font-bold text-[#FF6B35]">{val}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Issues */}
        <div className="space-y-3 mb-6">
          <h3 className="text-sm font-semibold text-white/90">检测提示</h3>
          {issues.map((issue, idx) => (
            <div key={idx} className={`flex items-start gap-3 p-4 rounded-xl ${
              issue.type === 'warning' ? 'bg-yellow-500/10 border border-yellow-500/30' : 
              'bg-blue-500/10 border border-blue-500/30'
            }`}>
              <AlertTriangle size={20} className={
                issue.type === 'warning' ? 'text-yellow-400' : 'text-blue-400'
              } />
              <span className="text-sm">{issue.text}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default HistogramPage;
