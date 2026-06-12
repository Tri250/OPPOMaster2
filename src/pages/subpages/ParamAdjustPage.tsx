import React, { useState, useCallback, useRef } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Aperture, Timer, Thermometer, Download, Upload, Share2, FileImage, Save } from 'lucide-react';

const ParamAdjustPage: React.FC = () => {
  const { cameraParams, setCameraParam, goBack } = useAppStore();
  
  // 导出状态
  const [showExportMenu, setShowExportMenu] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // 导出参数配置
  const handleExport = useCallback((format: 'json' | 'txt' | 'preset' = 'json') => {
    const paramsData = {
      iso: cameraParams.iso,
      shutter: cameraParams.shutter,
      aperture: cameraParams.aperture,
      wb: cameraParams.wb,
      timestamp: new Date().toISOString(),
      version: '3.2.0'
    };
    
    let content = '';
    let filename = '';
    
    if (format === 'json') {
      content = JSON.stringify(paramsData, null, 2);
      filename = `omaster_params_${Date.now()}.json`;
    } else if (format === 'txt') {
      content = `OMaster 参数配置\n==================\nISO: ${paramsData.iso}\n快门: ${paramsData.shutter >= 1000 ? `${paramsData.shutter/1000}s` : `1/${paramsData.shutter}s`}\n光圈: f/${paramsData.aperture.toFixed(1)}\n白平衡: ${paramsData.wb}K\n时间: ${paramsData.timestamp}\n版本: ${paramsData.version}`;
      filename = `omaster_params_${Date.now()}.txt`;
    } else if (format === 'preset') {
      content = JSON.stringify({
        name: '自定义参数预设',
        params: paramsData,
        created: paramsData.timestamp
      }, null, 2);
      filename = `omaster_preset_${Date.now()}.preset`;
    }
    
    // 创建 Blob 并下载
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
    
    setShowExportMenu(false);
    setShowSuccess(true);
    setTimeout(() => setShowSuccess(false), 1500);
  }, [cameraParams]);
  
  // 分享
  const handleShare = useCallback(async () => {
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'OMaster 参数配置',
          text: `ISO: ${cameraParams.iso}, 快门: ${cameraParams.shutter}, 光圈: f/${cameraParams.aperture.toFixed(1)}, 白平衡: ${cameraParams.wb}K`,
        });
      } catch (err) {
        console.log('分享失败:', err);
      }
    }
  }, [cameraParams]);
  
  // 保存到本地
  const handleSaveToLocal = useCallback(() => {
    localStorage.setItem('omaster_camera_params', JSON.stringify(cameraParams));
    setShowSuccess(true);
    setTimeout(() => setShowSuccess(false), 1500);
  }, [cameraParams]);

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
      format: (v: number) => v >= 1000 ? `${v/1000}s` : `1/${v}s`
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

  const quickPresets = [
    { name: '人像', iso: 200, shutter: 125, aperture: 2.8, wb: 5500 },
    { name: '风景', iso: 100, shutter: 60, aperture: 8, wb: 5600 },
    { name: '夜景', iso: 3200, shutter: 30, aperture: 2.8, wb: 4000 },
    { name: '运动', iso: 800, shutter: 500, aperture: 4, wb: 5500 },
  ];

  const applyPreset = (preset: typeof quickPresets[0]) => {
    setCameraParam('iso', preset.iso);
    setCameraParam('shutter', preset.shutter);
    setCameraParam('aperture', preset.aperture);
    setCameraParam('wb', preset.wb);
  };

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
        
        {/* 导出按钮 */}
        <div className="flex items-center gap-2 ml-auto">
          {/* 保存按钮 */}
          <button 
            onClick={handleSaveToLocal}
            className="p-2 rounded-full hover:bg-white/10"
            title="保存到本地"
          >
            <Save size={18} className="text-white/50" />
          </button>
          
          {/* 导出按钮 */}
          <div className="relative">
            <button 
              onClick={() => setShowExportMenu(!showExportMenu)}
              className="p-2 rounded-full hover:bg-white/10"
              title="导出参数"
            >
              <Download size={18} className="text-white/50" />
            </button>
            {/* 导出菜单 */}
            {showExportMenu && (
              <div className="absolute right-0 top-full mt-2 bg-[#1a1a1a] rounded-xl border border-white/10 shadow-lg z-50">
                <div className="p-2 space-y-1">
                  <button
                    onClick={() => handleExport('json')}
                    className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                  >
                    <FileImage size={14} />
                    JSON 格式
                  </button>
                  <button
                    onClick={() => handleExport('txt')}
                    className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                  >
                    <FileImage size={14} />
                    TXT 文档
                  </button>
                  <button
                    onClick={() => handleExport('preset')}
                    className="w-full px-3 py-2 rounded-lg hover:bg-white/10 text-white/70 text-sm flex items-center gap-2"
                  >
                    <FileImage size={14} />
                    预设文件
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
        </div>
      </div>
      
      {/* 成功提示 */}
      {showSuccess && (
        <div className="absolute top-16 left-1/2 -translate-x-1/2 px-4 py-2 rounded-full bg-[#FF6B35] text-white text-sm font-medium shadow-lg z-50">
          已保存
        </div>
      )}

      {/* Quick Presets */}
      <div className="px-4 py-4">
        <p className="text-white/50 text-xs mb-3">快捷档位</p>
        <div className="flex gap-2">
          {quickPresets.map((preset) => (
            <button
              key={preset.name}
              onClick={() => applyPreset(preset)}
              className="flex-1 py-2 rounded-xl bg-white/5 text-white text-sm font-medium transition-all hover:bg-white/10 active:scale-95"
            >
              {preset.name}
            </button>
          ))}
        </div>
      </div>

      {/* Param Controls */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        <div className="space-y-6">
          {params.map((param) => {
            const Icon = param.icon;
            const value = cameraParams[param.key as keyof typeof cameraParams];
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
                </div>

                {/* Slider */}
                <input
                  type="range"
                  min={param.min}
                  max={param.max}
                  step={param.step}
                  value={value}
                  onChange={(e) => setCameraParam(param.key, parseFloat(e.target.value))}
                  className="w-full h-3 bg-white/10 rounded-full appearance-none cursor-pointer accent-[#E91E63]"
                />

                {/* Marks */}
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
              </div>
            );
          })}
        </div>

        {/* Exposure Meter */}
        <div className="mt-6 p-4 bg-white/5 rounded-2xl">
          <p className="text-white/50 text-xs mb-3">曝光指示器</p>
          <div className="relative h-8 bg-gradient-to-r from-blue-900 via-green-900 to-red-900 rounded-full overflow-hidden">
            <div 
              className="absolute top-0 bottom-0 w-1 bg-white shadow-lg transition-all duration-300"
              style={{ 
                left: `${Math.min(100, Math.max(0, 50 + (cameraParams.iso - 100) / 100))}%`,
                transform: 'translateX(-50%)'
              }}
            />
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-white/50 text-xs font-mono">0</span>
            </div>
          </div>
          <div className="flex justify-between mt-1">
            <span className="text-white/30 text-xs">-3</span>
            <span className="text-white/30 text-xs">+3</span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ParamAdjustPage;
