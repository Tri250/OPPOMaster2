import {
  Preset,
  PresetSource,
  AndroidPresetJson,
  convertAndroidPresetToReact,
} from '../store/appStore';

export interface PresetLoadResult {
  presets: Preset[];
  failedSources: string[];
  successCount: number;
  fromFallback: boolean;
}

// 本地演示预设（当所有网络源失败时作为兜底）
export const DEMO_PRESETS: AndroidPresetJson[] = [
  {
    name: 'OPPO 清新人像',
    coverPath: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=500&fit=crop',
    author: '@OPPO影像',
    tags: ['人像', '清新'],
    saturation: 10,
    contrast: 5,
    warmth: 8,
    sharpness: 15,
  },
  {
    name: 'realme 街拍胶片',
    coverPath: 'https://images.unsplash.com/photo-1476973422084-e0fa66ff9456?w=400&h=300&fit=crop',
    author: '@realme摄影',
    tags: ['街拍', '胶片'],
    saturation: 5,
    contrast: 18,
    warmth: 2,
    sharpness: 20,
  },
  {
    name: 'vivo 风景通透',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    author: '@vivo影像',
    tags: ['风景', '通透'],
    saturation: 20,
    contrast: 10,
    warmth: -5,
    sharpness: 25,
  },
  {
    name: '荣耀夜景霓虹',
    coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    author: '@荣耀影像',
    tags: ['夜景', '霓虹'],
    saturation: 30,
    contrast: 20,
    warmth: -10,
    sharpness: 25,
  },
];

/**
 * 从单个预设源加载原始数据并归一化为 Preset
 */
export async function fetchPresetsFromSource(
  source: PresetSource
): Promise<{ presets: Preset[]; success: boolean }> {
  try {
    const response = await fetch(source.url, {
      headers: { Accept: 'application/json' },
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const data = await response.json();
    const rawPresets = Array.isArray(data.presets)
      ? data.presets
      : Array.isArray(data)
        ? data
        : [];

    const presets = rawPresets
      .filter((p: unknown): p is AndroidPresetJson => p !== null && typeof p === 'object')
      .map((p: AndroidPresetJson, index: number) => {
        const converted = convertAndroidPresetToReact(p, index);
        return {
          ...converted,
          id: `${source.id}-${converted.id}`,
        };
      });

    return { presets, success: true };
  } catch (err) {
    console.error(`Failed to fetch from ${source.name}:`, err);
    return { presets: [], success: false };
  }
}

/**
 * 从多个预设源加载预设，失败时使用本地演示数据兜底
 */
export async function fetchPresetsFromSources(
  sources: PresetSource[]
): Promise<PresetLoadResult> {
  const allPresets: Preset[] = [];
  const failedSources: string[] = [];
  let successCount = 0;

  for (const source of sources) {
    if (!source.enabled) continue;

    const { presets, success } = await fetchPresetsFromSource(source);
    if (success) {
      allPresets.push(...presets);
      successCount += 1;
    } else {
      failedSources.push(source.name);
    }
  }

  const fromFallback = allPresets.length === 0;
  if (fromFallback) {
    const demo = DEMO_PRESETS.map((p, i) => convertAndroidPresetToReact(p, i));
    allPresets.push(...demo);
  }

  return { presets: allPresets, failedSources, successCount, fromFallback };
}

/**
 * 获取加载结果的用户友好提示文案
 */
export function getLoadMessage(result: PresetLoadResult): string | null {
  if (result.fromFallback) {
    return result.failedSources.length > 0
      ? `网络源加载失败（${result.failedSources.join('、')}），已切换为本地演示预设`
      : '暂无可用预设源，已加载本地演示预设';
  }
  if (result.failedSources.length > 0) {
    return `部分源加载失败：${result.failedSources.join('、')}`;
  }
  return null;
}
