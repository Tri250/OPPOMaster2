import React, { useCallback } from 'react';
import { useAppStore } from '../../store/appStore';
import { ArrowLeft, Users, Mountain, Moon, Utensils, Building, Leaf, type LucideIcon } from 'lucide-react';

interface AiParams {
  saturation: number;
  contrast: number;
  brightness: number;
  warmth: number;
  sharpness: number;
}

interface SceneItem {
  id: string;
  name: string;
  icon: LucideIcon;
  color: string;
  params: Partial<AiParams>;
  desc: string;
}

const SCENES: SceneItem[] = [
  {
    id: 'portrait',
    name: '人像',
    icon: Users,
    color: '#FF6B9D',
    params: { saturation: 8, contrast: 10, brightness: 5, warmth: 3, sharpness: 18 },
    desc: '柔美肤色，自然美化',
  },
  {
    id: 'landscape',
    name: '风景',
    icon: Mountain,
    color: '#4ECDC4',
    params: { saturation: 15, contrast: 12, brightness: 5, warmth: 5, sharpness: 22 },
    desc: '色彩鲜明，层次丰富',
  },
  {
    id: 'night',
    name: '夜景',
    icon: Moon,
    color: '#7C6EF6',
    params: { saturation: 5, contrast: 20, brightness: 10, warmth: 8, sharpness: 25 },
    desc: '降噪提亮，氛围感强',
  },
  {
    id: 'food',
    name: '美食',
    icon: Utensils,
    color: '#FF6B35',
    params: { saturation: 25, contrast: 8, brightness: 5, warmth: 12, sharpness: 30 },
    desc: '鲜艳暖调，食欲满满',
  },
  {
    id: 'architecture',
    name: '建筑',
    icon: Building,
    color: '#607D8B',
    params: { saturation: 8, contrast: 15, brightness: 0, warmth: 0, sharpness: 30 },
    desc: '清晰锐利，几何质感',
  },
  {
    id: 'nature',
    name: '自然',
    icon: Leaf,
    color: '#66BB6A',
    params: { saturation: 12, contrast: 10, brightness: 5, warmth: 5, sharpness: 20 },
    desc: '清新通透，色彩自然',
  },
];

const AISceneRecognitionPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();

  const handleSceneSelect = useCallback(
    (scene: SceneItem) => {
      const entries = Object.entries(scene.params) as [keyof AiParams, number][];
      entries.forEach(([key, value]) => {
        setAiParam(key, value);
      });
    },
    [setAiParam],
  );

  return (
    <div
      className="min-h-screen flex flex-col"
      style={{ background: 'var(--color-bg-primary)', color: 'var(--color-text-primary)' }}
    >
      {/* 顶部标题栏 */}
      <div
        className="sticky top-0 z-50 backdrop-blur-md"
        style={{
          background: 'rgba(10,10,10,0.92)',
          borderBottom: '1px solid var(--color-border-light)',
        }}
      >
        <div className="flex items-center gap-3 px-4 py-3">
          <button
            onClick={goBack}
            aria-label="返回上一页"
            className="p-2 -ml-2 rounded-full transition-colors"
            style={{ color: 'var(--color-text-primary)' }}
          >
            <ArrowLeft size={20} />
          </button>
          <div className="flex-1">
            <h1 className="text-lg font-bold">AI 场景识别</h1>
            <p className="text-xs" style={{ color: 'var(--color-text-tertiary)' }}>
              智能识别场景，自动推荐最佳参数
            </p>
          </div>
        </div>
      </div>

      {/* 内容区 */}
      <div className="flex-1 overflow-y-auto px-4 py-4 animate-liquid-fade">
        <div className="grid grid-cols-2 gap-3">
          {SCENES.map((scene, index) => {
            const Icon = scene.icon;
            return (
              <button
                key={scene.id}
                onClick={() => handleSceneSelect(scene)}
                aria-label={`选择${scene.name}场景`}
                className="rounded-2xl p-4 text-left transition-liquid animate-liquid-slide-up"
                style={{
                  background: 'var(--color-bg-secondary)',
                  border: '1px solid var(--color-border-light)',
                  animationDelay: `${index * 60}ms`,
                }}
              >
                <div
                  className="w-12 h-12 rounded-xl flex items-center justify-center mb-3"
                  style={{ background: `${scene.color}20` }}
                >
                  <Icon size={24} style={{ color: scene.color }} />
                </div>
                <h3 className="text-sm font-bold mb-1">{scene.name}</h3>
                <p className="text-xs mb-2" style={{ color: 'var(--color-text-tertiary)' }}>
                  {scene.desc}
                </p>
                <div className="flex flex-wrap gap-1">
                  {Object.entries(scene.params).map(([key, value]) => (
                    <span
                      key={key}
                      className="text-[10px] px-1.5 py-0.5 rounded"
                      style={{ background: 'var(--color-bg-tertiary)', color: 'var(--color-text-tertiary)' }}
                    >
                      {key === 'saturation'
                        ? '饱和'
                        : key === 'contrast'
                          ? '对比'
                          : key === 'brightness'
                            ? '亮度'
                            : key === 'warmth'
                              ? '色温'
                              : '锐度'}
                      :{value > 0 ? '+' : ''}
                      {value}
                    </span>
                  ))}
                </div>
              </button>
            );
          })}
        </div>

        <div
          className="mt-4 p-4 rounded-2xl animate-liquid-slide-up"
          style={{
            background: 'var(--color-accent-primary-muted)',
            border: '1px solid var(--color-border-accent)',
            animationDelay: '360ms',
          }}
        >
          <p className="text-xs" style={{ color: 'var(--color-text-secondary)' }}>
            点击场景卡片将自动设置对应的 AI 微调参数，可在「AI 微调」页面进一步调整。
          </p>
        </div>
      </div>
    </div>
  );
};

export default React.memo(AISceneRecognitionPage);
