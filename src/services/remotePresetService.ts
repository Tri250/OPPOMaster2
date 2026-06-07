// 云同步预设服务 - 获取远程 JSON 数据

export interface PresetSection {
  title: string;
  items: Array<{
    label: string;
    value: string;
    span?: number;
  }>;
}

export interface RemotePreset {
  name: string;
  coverPath: string;
  galleryImages: string[];
  author: string;
  isNew?: boolean;
  sections: PresetSection[];
  tags: string[];
  description?: {
    title: string;
    content: string;
  };
}

export interface RemotePresetGroup {
  version: number;
  name: string;
  author: string;
  build: number;
  presets: RemotePreset[];
}

// 远程 JSON URL 配置
const REMOTE_PRESET_URLS = {
  oppo: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/oppo.json',
  realme: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/realme.json',
  honor: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/honor.json',
  vivo: 'https://cdn.jsdelivr.net/gh/fengyec2/OMaster-Community@main/presets/v2/vivo.json',
};

// 获取单个品牌的预设数据
export async function fetchRemotePresets(brand: keyof typeof REMOTE_PRESET_URLS): Promise<RemotePresetGroup | null> {
  try {
    const response = await fetch(REMOTE_PRESET_URLS[brand], {
      method: 'GET',
      headers: {
        'Accept': 'application/json',
      },
    });

    if (!response.ok) {
      console.error(`Failed to fetch ${brand} presets:`, response.status);
      return null;
    }

    const data = await response.json();
    return data as RemotePresetGroup;
  } catch (error) {
    console.error(`Error fetching ${brand} presets:`, error);
    return null;
  }
}

// 获取所有品牌的预设数据
export async function fetchAllRemotePresets(): Promise<Map<string, RemotePresetGroup>> {
  const results = new Map<string, RemotePresetGroup>();
  
  const fetchPromises = Object.entries(REMOTE_PRESET_URLS).map(async ([brand, url]) => {
    try {
      const response = await fetch(url);
      if (response.ok) {
        const data = await response.json();
        results.set(brand, data as RemotePresetGroup);
      }
    } catch (error) {
      console.error(`Failed to fetch ${brand} presets:`, error);
    }
  });

  await Promise.allSettled(fetchPromises);
  return results;
}

// 合并所有品牌的预设为一个列表
export async function fetchMergedPresets(): Promise<Array<RemotePreset & { brand: string; brandName: string }>> {
  const allPresets = await fetchAllRemotePresets();
  const merged: Array<RemotePreset & { brand: string; brandName: string }> = [];

  const brandNames: Record<string, string> = {
    oppo: 'OPPO',
    realme: 'realme',
    honor: '荣耀',
    vivo: 'vivo',
  };

  allPresets.forEach((group, brand) => {
    if (group && group.presets) {
      group.presets.forEach((preset) => {
        merged.push({
          ...preset,
          brand,
          brandName: brandNames[brand] || brand,
        });
      });
    }
  });

  return merged;
}

// 解析预设参数为键值对
export function parsePresetParams(preset: RemotePreset): Record<string, string> {
  const params: Record<string, string> = {};
  
  if (preset.sections) {
    preset.sections.forEach((section) => {
      section.items.forEach((item) => {
        params[item.label] = item.value;
      });
    });
  }

  return params;
}

// 获取完整的预设参数标签（中文）
export function getParamLabel(label: string): string {
  // 映射远程 JSON 中的标签到中文
  const labelMap: Record<string, string> = {
    '@string/param_filter': '滤镜',
    '@string/param_soft_light': '柔光',
    '@string/param_tone_curve': '色调曲线',
    '@string/param_saturation': '饱和度',
    '@string/param_warm_cool': '冷暖',
    '@string/param_cyan_magenta': '青品',
    '@string/param_sharpness': '锐度',
    '@string/param_vignette': '暗角',
    '@string/param_hue': '色相',
    '@string/param_contrast': '对比度',
    '@string/param_contrast_highlight': '高光对比',
    '@string/param_contrast_shadow': '阴影对比',
    '@string/param_brightness': '亮度',
    '@string/param_clarity': '清晰度',
    '@string/param_grain': '颗粒',
    '@string/param_grain_size': '颗粒大小',
    'ISO感光度': 'ISO',
    '快门速度': '快门',
    'AF对焦模式': '对焦',
    'WB白平衡': '白平衡',
    'M测光模式': '测光',
    '曝光': '曝光',
    '亮度': '亮度',
    '对比度': '对比度',
    '高光': '高光',
    '阴影': '阴影',
    '光感': '光感',
    '饱和度': '饱和度',
    '色温': '色温',
    '锐度': '锐度',
    'ISO': 'ISO',
    '快门': '快门',
    'EV': 'EV',
    '白平衡': '白平衡',
  };

  return labelMap[label] || label;
}

// 获取预设的所有参数（格式化后）
export function getFormattedParams(preset: RemotePreset): Array<{ label: string; value: string }> {
  const params: Array<{ label: string; value: string }> = [];

  if (preset.sections) {
    preset.sections.forEach((section) => {
      section.items.forEach((item) => {
        params.push({
          label: getParamLabel(item.label),
          value: item.value,
        });
      });
    });
  }

  return params;
}

// 获取拍摄建议
export function getShootingTips(preset: RemotePreset): {
  environment?: string;
  scenes?: string;
  tips?: string;
} {
  if (!preset.description?.content) {
    return {};
  }

  const content = preset.description.content;
  const environmentMatch = content.match(/【环境建议】([^【]+)/);
  const scenesMatch = content.match(/【场景推荐】([^【]+)/);
  const tipsMatch = content.match(/【拍摄要点】(.+)/);

  return {
    environment: environmentMatch?.[1]?.trim(),
    scenes: scenesMatch?.[1]?.trim(),
    tips: tipsMatch?.[1]?.trim(),
  };
}
