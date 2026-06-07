import React, { useState, useRef, useEffect } from 'react';
import { ArrowLeft, RefreshCw } from 'lucide-react';

const ToneCurvePage: React.FC = () => {
  const [selectedChannel, setSelectedChannel] = useState('rgb');
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [points, setPoints] = useState([
    { x: 0, y: 1 },
    { x: 0.5, y: 0.5 },
    { x: 1, y: 0 }
  ]);

  const channels = [
    { id: 'rgb', name: 'RGB', color: 'white' },
    { id: 'red', name: '红色', color: '#FF6B6B' },
    { id: 'green', name: '绿色', color: '#69DB7C' },
    { id: 'blue', name: '蓝色', color: '#4DABF7' },
  ];

  const presets = [
    { id: 1, name: '线性' },
    { id: 2, name: '对比度' },
    { id: 3, name: '褪色' },
    { id: 4, name: '阴影高光' },
  ];

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    ctx.clearRect(0, 0, width, height);
    ctx.fillStyle = '#1A1A1A';
    ctx.fillRect(0, 0, width, height);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) {
      const pos = (i / 4) * width;
      ctx.beginPath();
      ctx.moveTo(pos, 0);
      ctx.lineTo(pos, height);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(0, pos);
      ctx.lineTo(width, pos);
      ctx.stroke();
    }

    const gradient = ctx.createLinearGradient(0, 0, width, 0);
    gradient.addColorStop(0, 'black');
    gradient.addColorStop(1, 'white');
    const bg = ctx.createLinearGradient(0, height, 0, 0);
    bg.addColorStop(0, 'rgba(0,0,0,0.5)');
    bg.addColorStop(1, 'rgba(255,255,255,0.1)');

    ctx.strokeStyle = selectedChannel === 'rgb' ? '#FF6B35' : 
                     selectedChannel === 'red' ? '#FF6B6B' : 
                     selectedChannel === 'green' ? '#69DB7C' : '#4DABF7';
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();

    points.forEach((p, i) => {
      const x = p.x * width;
      const y = p.y * height;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    points.forEach((p) => {
      const x = p.x * width;
      const y = p.y * height;
      ctx.beginPath();
      ctx.arc(x, y, 8, 0, Math.PI * 2);
      ctx.fillStyle = '#FF6B35';
      ctx.fill();
      ctx.strokeStyle = 'white';
      ctx.lineWidth = 2;
      ctx.stroke();
    });
  }, [points, selectedChannel]);

  return (
    <div className="h-full w-full bg-[#0A0A0A] text-white flex flex-col overflow-hidden">
      {/* Top Bar */}
      <div className="p-4 flex items-center gap-3 border-b border-white/10">
        <button className="text-white">
          <ArrowLeft size={24} />
        </button>
        <div className="flex-1">
          <h1 className="text-lg font-bold">色调曲线</h1>
          <p className="text-xs text-white/50">精准影调控制</p>
        </div>
        <button className="text-[#FF6B35]">
          <RefreshCw size={24} />
        </button>
      </div>

      <div className="flex-1 overflow-y-auto p-4">
        {/* Curve Canvas */}
        <div className="bg-white/5 rounded-2xl p-4 mb-4">
          <canvas 
            ref={canvasRef}
            width={300}
            height={300}
            className="w-full rounded-xl bg-[#1A1A1A]"
          />
        </div>

        {/* Channel Selection */}
        <h3 className="text-sm font-semibold mb-3 text-white/90">通道</h3>
        <div className="flex gap-2 mb-6">
          {channels.map((ch) => (
            <button
              key={ch.id}
              onClick={() => setSelectedChannel(ch.id)}
              className={`flex-1 py-3 rounded-xl text-sm font-medium transition-all ${
                selectedChannel === ch.id ? 'bg-[#FF6B35] text-white' : 'bg-white/5 text-white/60'
              }`}
            >
              {ch.name}
            </button>
          ))}
        </div>

        {/* Presets */}
        <h3 className="text-sm font-semibold mb-3 text-white/90">曲线预设</h3>
        <div className="grid grid-cols-2 gap-3 mb-6">
          {presets.map((preset) => (
            <button
              key={preset.id}
              className="bg-gradient-to-br from-white/10 to-white/5 rounded-xl p-4 text-left hover:from-white/15 transition-all"
            >
              <span className="text-sm">{preset.name}</span>
            </button>
          ))}
        </div>

        {/* Info */}
        <div className="bg-white/5 rounded-2xl p-4 mb-6">
          <p className="text-xs text-white/60">
            点击画布添加控制点，拖动调整曲线形状
          </p>
        </div>

        {/* Action Buttons */}
        <div className="flex gap-3">
          <button className="flex-1 py-4 rounded-xl border border-white/20 text-sm font-semibold">
            重置曲线
          </button>
          <button className="flex-1 py-4 rounded-xl bg-[#FF6B35] text-sm font-semibold">
            应用
          </button>
        </div>
      </div>
    </div>
  );
};

export default ToneCurvePage;
