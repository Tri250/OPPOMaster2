import React, { useState, useEffect } from 'react';

/**
 * Layer 3: 大师呈现层 - 哈苏光圈叶片分析动画
 * 
 * 「哈苏大师之眼睁开」设计
 * - 光圈叶片动画：从闭合→旋转→张开
 * - 哈苏橙渐变进度条
 * - 逐步揭示分析步骤
 * - 营造「大师正在观察你的画面」的专业仪式感
 */

interface AnalysisStep {
  id: string;
  name: string;
  status: 'pending' | 'processing' | 'completed';
}

interface HasselbladApertureAnimationProps {
  steps?: AnalysisStep[];
  progress?: number;
  onComplete?: () => void;
}

const DEFAULT_STEPS: AnalysisStep[] = [
  { id: 'color', name: '色彩分析', status: 'pending' },
  { id: 'light', name: '光影结构分析', status: 'pending' },
  { id: 'scene', name: '场景匹配', status: 'pending' },
  { id: 'film', name: '胶片推荐', status: 'pending' },
  { id: 'params', name: '哈苏参数优化', status: 'pending' },
];

export const HasselbladApertureAnimation: React.FC<HasselbladApertureAnimationProps> = ({
  steps = DEFAULT_STEPS,
  progress = 0,
  onComplete,
}) => {
  const [currentProgress, setCurrentProgress] = useState(0);
  const [apertureState, setApertureState] = useState<'closed' | 'rotating' | 'opening' | 'open'>('closed');
  const [currentSteps, setCurrentSteps] = useState<AnalysisStep[]>(steps);
  const [currentMessage, setCurrentMessage] = useState('正在读取光影信息...');

  // 模拟分析过程
  useEffect(() => {
    const runAnalysis = async () => {
      // Phase 1: 闭合状态 - 初始化
      setApertureState('closed');
      setCurrentMessage('正在读取光影信息...');
      await delay(500);

      // Phase 2: 旋转状态 - 开始分析
      setApertureState('rotating');
      setCurrentProgress(10);
      await delay(800);

      // Phase 3: 逐步张开 - 分析各步骤
      setApertureState('opening');

      for (let i = 0; i < steps.length; i++) {
        const stepProgress = 20 + (i * 16);
        setCurrentProgress(stepProgress);

        // 更新当前步骤状态
        setCurrentSteps(prev => prev.map((step, idx) => {
          if (idx < i) return { ...step, status: 'completed' };
          if (idx === i) return { ...step, status: 'processing' };
          return step;
        }));

        setCurrentMessage(`${steps[i].name}中...`);
        await delay(1200);

        // 标记完成
        setCurrentSteps(prev => prev.map((step, idx) => {
          if (idx <= i) return { ...step, status: 'completed' };
          return step;
        }));
      }

      // Phase 4: 完全张开 - 分析完成
      setApertureState('open');
      setCurrentProgress(100);
      setCurrentMessage('哈苏大师之眼已睁开');
      await delay(500);

      if (onComplete) onComplete();
    };

    runAnalysis();
  }, [steps, onComplete]);

  const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  // 计算光圈叶片角度
  const getBladeAngles = () => {
    const baseAngles = [0, 45, 90, 135, 180, 225, 270, 315];
    const rotationOffset = apertureState === 'rotating' ? 22.5 : 0;
    const openingFactor = apertureState === 'open' ? 0.3 : 
                          apertureState === 'opening' ? 0.5 : 1;

    return baseAngles.map(angle => ({
      angle: angle + rotationOffset,
      scale: openingFactor,
    }));
  };

  return (
    <div className="min-h-screen bg-[#0A0A0A] flex flex-col items-center justify-center px-6">
      {/* 光圈动画 */}
      <div className="relative w-32 h-32 mb-8">
        {/* 外圈 */}
        <div className="absolute inset-0 rounded-full border-2 border-[#FF6B35]/30" />

        {/* 光圈叶片 */}
        <div className="absolute inset-0 flex items-center justify-center">
          <svg width="128" height="128" viewBox="0 0 128 128" className="transition-transform duration-300">
            {getBladeAngles().map((blade, index) => (
              <g
                key={index}
                transform={`rotate(${blade.angle} 64 64)`}
                className="transition-all duration-500"
              >
                <path
                  d={`M64 64 L${64 + 40 * blade.scale} ${64 - 20} L${64 + 40 * blade.scale} ${64 + 20} Z`}
                  fill={`rgba(255, 107, 53, ${0.6 - index * 0.05})`}
                  className="transition-all duration-500"
                />
              </g>
            ))}
            {/* 中心点 */}
            <circle
              cx="64"
              cy="64"
              r={apertureState === 'open' ? 8 : 4}
              fill="#FF6B35"
              className="transition-all duration-500"
            />
          </svg>
        </div>

        {/* 旋转动画叠加 */}
        {apertureState === 'rotating' && (
          <div className="absolute inset-0 animate-spin-slow">
            <div className="w-full h-full rounded-full border border-[#FF6B35]/20" />
          </div>
        )}
      </div>

      {/* 当前状态文字 */}
      <p className="text-white/70 text-sm mb-6 text-center">
        {currentMessage}
      </p>

      {/* 哈苏橙渐变进度条 */}
      <div className="w-full max-w-xs mb-8">
        <div className="relative h-2 bg-white/10 rounded-full overflow-hidden">
          <div
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-[#FF6B35] via-[#FF8A50] to-[#FFB366] rounded-full transition-all duration-300"
            style={{ width: `${currentProgress}%` }}
          />
          {/* 进度条发光效果 */}
          <div
            className="absolute inset-y-0 left-0 bg-gradient-to-r from-[#FF6B35] to-transparent rounded-full opacity-50 blur-sm transition-all duration-300"
            style={{ width: `${currentProgress}%` }}
          />
        </div>
        <div className="flex justify-between mt-2">
          <span className="text-white/40 text-xs">分析进度</span>
          <span className="text-[#FF6B35] text-xs font-medium">{currentProgress}%</span>
        </div>
      </div>

      {/* 分析步骤列表 */}
      <div className="w-full max-w-xs space-y-2">
        {currentSteps.map((step) => (
          <div
            key={step.id}
            className={`flex items-center gap-3 py-2 px-3 rounded-lg transition-all duration-300
              ${step.status === 'completed' ? 'bg-[#FF6B35]/10' : 
                step.status === 'processing' ? 'bg-white/5' : 'bg-transparent'}`}
          >
            {/* 状态图标 */}
            <div className="w-5 h-5 flex items-center justify-center">
              {step.status === 'completed' && (
                <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
                  <circle cx="8" cy="8" r="7" fill="#FF6B35" fillOpacity="0.2" />
                  <path d="M4 8L7 11L12 5" stroke="#FF6B35" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                </svg>
              )}
              {step.status === 'processing' && (
                <div className="w-4 h-4 rounded-full border-2 border-[#FF6B35]/30 border-t-[#FF6B35] animate-spin" />
              )}
              {step.status === 'pending' && (
                <div className="w-4 h-4 rounded-full border border-white/20" />
              )}
            </div>

            {/* 步骤名称 */}
            <span className={`text-sm transition-colors duration-300
              ${step.status === 'completed' ? 'text-[#FF6B35]' : 
                step.status === 'processing' ? 'text-white/80' : 'text-white/40'}`}>
              {step.name}
            </span>

            {/* 完成标记 */}
            {step.status === 'completed' && (
              <span className="text-[#FF6B35]/60 text-xs">完成</span>
            )}
          </div>
        ))}
      </div>

      {/* 底部品牌标识 */}
      <div className="mt-12 text-center">
        <p className="text-white/30 text-xs tracking-widest">
          HNCS · HASSELBLAD NATURAL COLOR SOLUTION
        </p>
      </div>
    </div>
  );
};

export default HasselbladApertureAnimation;