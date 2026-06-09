import React, { useState, useEffect } from 'react';
import { Camera, Film, SlidersHorizontal, Lightbulb, Share2, Sparkles, CheckCircle } from 'lucide-react';

/**
 * Layer 4: 大师洞察层 - 端到端大师工作流
 * 
 * 场景→胶片→参数的端到端工作流
 * 这是「哈苏大师之眼」区别于普通场景识别的最核心差异
 * 
 * 流程：
 * 1. 哈苏之眼（颜色直方图 + EXIF + 人脸检测）
 * 2. 智能胶片推荐（场景→胶片映射表）
 * 3. 哈苏参数优化（HasselbladParams 映射）
 * 4. 大师拍摄建议（构图、光线、焦段建议）
 * 5. 配方保存/分享（场景+胶片+参数 = 可分享配方）
 */

interface WorkflowStep {
  id: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  status: 'pending' | 'processing' | 'completed';
  result?: string;
}

interface MasterWorkflowProps {
  imageUrl?: string;
  onComplete?: (result: WorkflowResult) => void;
}

interface WorkflowResult {
  sceneId: string;
  sceneName: string;
  confidence: number;
  recommendedFilm: string;
  hasselbladParams: Record<string, number>;
  masterTips: string[];
}

const DEFAULT_WORKFLOW_STEPS: WorkflowStep[] = [
  {
    id: 'scene',
    icon: <Camera size={20} />,
    title: '哈苏之眼',
    description: '颜色直方图 + EXIF + 人脸检测',
    status: 'pending',
  },
  {
    id: 'film',
    icon: <Film size={20} />,
    title: '智能胶片推荐',
    description: '场景→胶片映射表',
    status: 'pending',
  },
  {
    id: 'params',
    icon: <SlidersHorizontal size={20} />,
    title: '哈苏参数优化',
    description: 'HNCS 色彩科学',
    status: 'pending',
  },
  {
    id: 'tips',
    icon: <Lightbulb size={20} />,
    title: '大师拍摄建议',
    description: '哈苏大师赛级别指导',
    status: 'pending',
  },
  {
    id: 'save',
    icon: <Share2 size={20} />,
    title: '配方保存/分享',
    description: '可分享的胶片配方',
    status: 'pending',
  },
];

export const MasterWorkflow: React.FC<MasterWorkflowProps> = ({
  imageUrl,
  onComplete,
}) => {
  const [steps, setSteps] = useState<WorkflowStep[]>(DEFAULT_WORKFLOW_STEPS);
  const [currentStepIndex, setCurrentStepIndex] = useState(-1);
  const [isComplete, setIsComplete] = useState(false);
  const [workflowResult, setWorkflowResult] = useState<WorkflowResult | null>(null);

  // 模拟工作流执行
  useEffect(() => {
    const runWorkflow = async () => {
      for (let i = 0; i < steps.length; i++) {
        setCurrentStepIndex(i);

        // 更新当前步骤为处理中
        setSteps(prev => prev.map((step, idx) => {
          if (idx === i) return { ...step, status: 'processing' };
          return step;
        }));

        // 模拟处理时间
        await delay(800 + Math.random() * 400);

        // 更新步骤结果
        const result = getStepResult(i);
        setSteps(prev => prev.map((step, idx) => {
          if (idx === i) return { ...step, status: 'completed', result };
          return step;
        }));
      }

      // 工作流完成
      setCurrentStepIndex(-1);
      setIsComplete(true);

      // 构建最终结果
      const finalResult: WorkflowResult = {
        sceneId: 'landscape-sunset',
        sceneName: '日落',
        confidence: 0.92,
        recommendedFilm: 'RDP3 正片',
        hasselbladParams: {
          tone: -5,
          saturation: 25,
          contrast: 10,
          colorTemp: 20,
          sharpness: 12,
          vignette: 0,
          cyanMagenta: 5,
        },
        masterTips: [
          '黄金时刻出片率最高',
          '利用前景增加层次感',
          '试试 XPAN 宽幅模式',
          '浓郁胶片让色彩更鲜活',
        ],
      };

      setWorkflowResult(finalResult);
      if (onComplete) onComplete(finalResult);
    };

    runWorkflow();
  }, [imageUrl, onComplete, steps]);

  const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

  const getStepResult = (stepIndex: number): string => {
    switch (stepIndex) {
      case 0: return '日落 · 置信度 92%';
      case 1: return 'RDP3 正片 (93%匹配)';
      case 2: return '影调-5 饱和度+25 色温+20';
      case 3: return '4 条大师建议';
      case 4: return '配方已生成';
      default: return '';
    }
  };

  return (
    <div className="min-h-screen bg-[#0A0A0A] flex flex-col items-center justify-center px-6">
      {/* 顶部标题 */}
      <div className="text-center mb-8">
        <div className="flex items-center justify-center gap-2 mb-2">
          <Sparkles size={20} className="text-[#FF6B35]" />
          <span className="text-white font-semibold">哈苏大师工作流</span>
        </div>
        <p className="text-white/50 text-xs">场景 → 胶片 → 参数 → 建议 → 配方</p>
      </div>

      {/* 工作流步骤 */}
      <div className="w-full max-w-sm space-y-3">
        {steps.map((step, index) => (
          <div
            key={step.id}
            className={`relative flex items-center gap-4 p-4 rounded-xl transition-all duration-300
              ${step.status === 'completed' ? 'bg-[#FF6B35]/10 border border-[#FF6B35]/30' :
                step.status === 'processing' ? 'bg-white/5 border border-[#FF6B35]/50' :
                'bg-white/5 border border-white/5'}`}
          >
            {/* 步骤序号 */}
            <div className={`w-8 h-8 rounded-full flex items-center justify-center transition-all
              ${step.status === 'completed' ? 'bg-[#FF6B35]' :
                step.status === 'processing' ? 'bg-[#FF6B35]/30 border-2 border-[#FF6B35]' :
                'bg-white/10'}`}
            >
              {step.status === 'completed' ? (
                <CheckCircle size={16} className="text-white" />
              ) : step.status === 'processing' ? (
                <div className="w-4 h-4 rounded-full border-2 border-[#FF6B35]/30 border-t-[#FF6B35] animate-spin" />
              ) : (
                <span className="text-white/40 text-xs font-medium">{index + 1}</span>
              )}
            </div>

            {/* 步骤内容 */}
            <div className="flex-1">
              <div className="flex items-center gap-2">
                <span className={`transition-colors
                  ${step.status === 'completed' ? 'text-[#FF6B35]' :
                    step.status === 'processing' ? 'text-white' :
                    'text-white/50'}`}>
                  {step.icon}
                </span>
                <span className={`text-sm font-medium transition-colors
                  ${step.status === 'completed' ? 'text-[#FF6B35]' :
                    step.status === 'processing' ? 'text-white' :
                    'text-white/50'}`}>
                  {step.title}
                </span>
              </div>
              <p className="text-white/40 text-xs mt-1">{step.description}</p>

              {/* 步骤结果 */}
              {step.result && (
                <p className="text-[#FF6B35] text-xs mt-2 font-medium">{step.result}</p>
              )}
            </div>

            {/* 连接线 */}
            {index < steps.length - 1 && (
              <div className={`absolute left-4 top-full h-3 w-0.5 transition-colors
                ${step.status === 'completed' ? 'bg-[#FF6B35]' : 'bg-white/10'}`}
                style={{ transform: 'translateX(12px)' }}
              />
            )}
          </div>
        ))}
      </div>

      {/* 完成状态 */}
      {isComplete && workflowResult && (
        <div className="mt-8 w-full max-w-sm">
          <div className="bg-[#FF6B35]/10 rounded-2xl p-4 border border-[#FF6B35]/30">
            <div className="flex items-center gap-2 mb-3">
              <CheckCircle size={18} className="text-[#FF6B35]" />
              <span className="text-[#FF6B35] font-medium">工作流完成</span>
            </div>

            {/* 结果摘要 */}
            <div className="space-y-2 text-sm">
              <div className="flex items-center gap-2">
                <Camera size={14} className="text-white/50" />
                <span className="text-white/70">场景：</span>
                <span className="text-white">{workflowResult.sceneName}</span>
                <span className="text-[#FF6B35] text-xs">({Math.round(workflowResult.confidence * 100)}%)</span>
              </div>
              <div className="flex items-center gap-2">
                <Film size={14} className="text-white/50" />
                <span className="text-white/70">胶片：</span>
                <span className="text-white">{workflowResult.recommendedFilm}</span>
              </div>
              <div className="flex items-center gap-2">
                <SlidersHorizontal size={14} className="text-white/50" />
                <span className="text-white/70">参数：</span>
                <span className="text-white text-xs">
                  影调{workflowResult.hasselbladParams.tone} 饱和度+{workflowResult.hasselbladParams.saturation}
                </span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 底部品牌标识 */}
      <div className="mt-12 text-center">
        <p className="text-white/30 text-xs tracking-widest">
          HNCS · HASSELBLAD NATURAL COLOR SOLUTION
        </p>
      </div>
    </div>
  );
};

export default MasterWorkflow;