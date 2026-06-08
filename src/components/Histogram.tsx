import React, { useRef, useEffect, useState } from 'react';

interface HistogramProps {
  imageData?: ImageData;
  type: 'rgb' | 'luminance' | 'combined';
}

interface HistogramData {
  r: number[];
  g: number[];
  b: number[];
  luminance: number[];
}

const Histogram: React.FC<HistogramProps> = ({
  imageData,
  type,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [histogramData, setHistogramData] = useState<HistogramData | null>(null);

  // 计算直方图数据
  useEffect(() => {
    if (!imageData) {
      // 如果没有提供图像数据，生成模拟数据
      const simulatedData = generateSimulatedHistogram();
      setHistogramData(simulatedData);
      return;
    }

    const data = calculateHistogram(imageData);
    setHistogramData(data);
  }, [imageData]);

  // 绘制直方图
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !histogramData) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const width = canvas.width;
    const height = canvas.height;

    // 清空画布
    ctx.clearRect(0, 0, width, height);

    // 填充深色背景
    ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
    ctx.fillRect(0, 0, width, height);

    // 绘制边框
    ctx.strokeStyle = '#FF6B35';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, width, height);

    // 找到最大值用于归一化
    const maxValue = Math.max(
      ...histogramData.r,
      ...histogramData.g,
      ...histogramData.b,
      ...histogramData.luminance
    );

    if (maxValue === 0) return;

    const barWidth = width / 256;

    // 根据类型绘制
    if (type === 'rgb' || type === 'combined') {
      // 绘制红色通道
      ctx.beginPath();
      ctx.moveTo(0, height);
      histogramData.r.forEach((value, i) => {
        const x = i * barWidth;
        const y = height - (value / maxValue) * height * 0.9;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(width, height);
      ctx.closePath();
      ctx.fillStyle = 'rgba(255, 0, 0, 0.3)';
      ctx.fill();

      // 绘制绿色通道
      ctx.beginPath();
      ctx.moveTo(0, height);
      histogramData.g.forEach((value, i) => {
        const x = i * barWidth;
        const y = height - (value / maxValue) * height * 0.9;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(width, height);
      ctx.closePath();
      ctx.fillStyle = 'rgba(0, 255, 0, 0.3)';
      ctx.fill();

      // 绘制蓝色通道
      ctx.beginPath();
      ctx.moveTo(0, height);
      histogramData.b.forEach((value, i) => {
        const x = i * barWidth;
        const y = height - (value / maxValue) * height * 0.9;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(width, height);
      ctx.closePath();
      ctx.fillStyle = 'rgba(0, 0, 255, 0.3)';
      ctx.fill();
    }

    if (type === 'luminance' || type === 'combined') {
      // 绘制亮度通道
      ctx.beginPath();
      ctx.moveTo(0, height);
      histogramData.luminance.forEach((value, i) => {
        const x = i * barWidth;
        const y = height - (value / maxValue) * height * 0.9;
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.lineTo(width, height);
      ctx.closePath();
      ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
      ctx.fill();
    }

    // 绘制网格线
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.lineWidth = 1;
    
    // 水平网格线
    for (let i = 1; i < 4; i++) {
      const y = (height / 4) * i;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
      ctx.stroke();
    }

    // 垂直网格线
    for (let i = 1; i < 4; i++) {
      const x = (width / 4) * i;
      ctx.beginPath();
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
      ctx.stroke();
    }

  }, [histogramData, type]);

  // 计算直方图数据
  const calculateHistogram = (imageData: ImageData): HistogramData => {
    const data = imageData.data;
    const r = new Array(256).fill(0);
    const g = new Array(256).fill(0);
    const b = new Array(256).fill(0);
    const luminance = new Array(256).fill(0);

    for (let i = 0; i < data.length; i += 4) {
      const red = data[i];
      const green = data[i + 1];
      const blue = data[i + 2];

      r[red]++;
      g[green]++;
      b[blue]++;

      // 计算亮度 (Rec. 709)
      const lum = Math.round(0.2126 * red + 0.7152 * green + 0.0722 * blue);
      luminance[lum]++;
    }

    return { r, g, b, luminance };
  };

  // 生成模拟直方图数据
  const generateSimulatedHistogram = (): HistogramData => {
    const r = new Array(256).fill(0);
    const g = new Array(256).fill(0);
    const b = new Array(256).fill(0);
    const luminance = new Array(256).fill(0);

    // 生成类似真实照片的直方图分布
    for (let i = 0; i < 256; i++) {
      // 模拟高斯分布
      const gaussian = (x: number, mean: number, stdDev: number) => {
        return Math.exp(-0.5 * Math.pow((x - mean) / stdDev, 2));
      };

      // 红色通道 - 偏暖色调
      r[i] = Math.round(
        (gaussian(i, 140, 50) * 8000) +
        (gaussian(i, 200, 30) * 3000)
      );

      // 绿色通道 - 中间调
      g[i] = Math.round(
        (gaussian(i, 120, 45) * 7500) +
        (gaussian(i, 180, 35) * 2500)
      );

      // 蓝色通道 - 偏冷色调
      b[i] = Math.round(
        (gaussian(i, 100, 40) * 6000) +
        (gaussian(i, 160, 50) * 4000)
      );

      // 亮度 - 综合分布
      luminance[i] = Math.round(
        (gaussian(i, 128, 60) * 10000) +
        (gaussian(i, 180, 40) * 3000)
      );
    }

    return { r, g, b, luminance };
  };

  // 获取类型标签
  const getTypeLabel = () => {
    switch (type) {
      case 'rgb':
        return 'RGB 直方图';
      case 'luminance':
        return '亮度直方图';
      case 'combined':
        return '综合直方图';
    }
  };

  // 获取类型图例
  const renderLegend = () => {
    if (type === 'rgb') {
      return (
        <div className="flex items-center gap-3 mt-2">
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(255, 0, 0, 0.6)' }} />
            <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>R</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(0, 255, 0, 0.6)' }} />
            <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>G</span>
          </div>
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(0, 0, 255, 0.6)' }} />
            <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>B</span>
          </div>
        </div>
      );
    }

    if (type === 'luminance') {
      return (
        <div className="flex items-center gap-3 mt-2">
          <div className="flex items-center gap-1">
            <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(255, 255, 255, 0.6)' }} />
            <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>亮度</span>
          </div>
        </div>
      );
    }

    return (
      <div className="flex items-center gap-3 mt-2 flex-wrap">
        <div className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(255, 0, 0, 0.6)' }} />
          <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>R</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(0, 255, 0, 0.6)' }} />
          <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>G</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(0, 0, 255, 0.6)' }} />
          <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>B</span>
        </div>
        <div className="flex items-center gap-1">
          <div className="w-3 h-3 rounded-full" style={{ background: 'rgba(255, 255, 255, 0.6)' }} />
          <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.6)' }}>亮度</span>
        </div>
      </div>
    );
  };

  return (
    <div 
      className="rounded-xl p-3"
      style={{ background: 'rgba(255, 255, 255, 0.03)' }}
    >
      {/* 标题 */}
      <div 
        className="text-xs font-medium mb-2"
        style={{ color: '#FF6B35' }}
      >
        {getTypeLabel()}
      </div>

      {/* 直方图画布 */}
      <div className="relative">
        <canvas
          ref={canvasRef}
          width={320}
          height={120}
          className="w-full rounded-lg"
          style={{ 
            height: '120px',
            border: '1px solid rgba(255, 107, 53, 0.3)',
          }}
        />
      </div>

      {/* 图例 */}
      {renderLegend()}

      {/* 刻度标签 */}
      <div className="flex justify-between mt-1">
        <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.4)' }}>暗</span>
        <span className="text-xs" style={{ color: 'rgba(255, 255, 255, 0.4)' }}>亮</span>
      </div>
    </div>
  );
};

export default Histogram;