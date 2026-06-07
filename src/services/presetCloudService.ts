// 云同步预设服务 - 模拟实时数据加载

export interface CloudPreset {
  id: string;
  name: string;
  coverPath: string;
  author: string;
  brand: string;
  tags: string[];
  isNew: boolean;
  isHncs: boolean;
  rating: number;
  downloadCount: number;
  // 影像参数
  cameraParams: {
    saturation: number;
    contrast: number;
    brightness: number;
    warmth: number;
    sharpness: number;
    clarity: number;
    highlights: number;
    shadows: number;
    hue: number;
    vibrance: number;
  };
  // 拍摄信息
  shotInfo?: {
    iso?: number;
    aperture?: string;
    shutter?: string;
    focalLength?: string;
    device?: string;
  };
  updatedAt: string;
}

// 模拟云数据
const cloudPresetsData: CloudPreset[] = [
  {
    id: 'cloud_1',
    name: '哈苏浓郁',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=500&fit=crop',
    author: '@Aurora',
    brand: 'Hasselblad',
    tags: ['风景', '浓郁', '专业'],
    isNew: true,
    isHncs: true,
    rating: 4.8,
    downloadCount: 12800,
    cameraParams: {
      saturation: 25,
      contrast: 15,
      brightness: 5,
      warmth: 10,
      sharpness: 20,
      clarity: 15,
      highlights: -10,
      shadows: 8,
      hue: 0,
      vibrance: 18,
    },
    shotInfo: {
      iso: 100,
      aperture: 'f/8',
      shutter: '1/250s',
      focalLength: '45mm',
      device: 'Hasselblad X2D 100C',
    },
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'cloud_2',
    name: '富士NC',
    coverPath: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&h=500&fit=crop',
    author: '@OPPO影像',
    brand: 'Fujifilm',
    tags: ['人像', '胶片', '柔和'],
    isNew: true,
    isHncs: false,
    rating: 4.9,
    downloadCount: 15600,
    cameraParams: {
      saturation: 8,
      contrast: 5,
      brightness: 0,
      warmth: 5,
      sharpness: 12,
      clarity: 8,
      highlights: -5,
      shadows: 5,
      hue: 2,
      vibrance: 10,
    },
    shotInfo: {
      iso: 200,
      aperture: 'f/2.8',
      shutter: '1/500s',
      focalLength: '56mm',
      device: 'FUJIFILM GFX 100S',
    },
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'cloud_3',
    name: '蓝调时刻',
    coverPath: 'https://images.unsplash.com/photo-1514565131-fce0801e5785?w=400&h=500&fit=crop',
    author: '@Find摄影',
    brand: 'OPPO',
    tags: ['夜景', '蓝调', '氛围'],
    isNew: false,
    isHncs: true,
    rating: 4.5,
    downloadCount: 8800,
    cameraParams: {
      saturation: 15,
      contrast: 20,
      brightness: -5,
      warmth: -15,
      sharpness: 18,
      clarity: 12,
      highlights: -20,
      shadows: 10,
      hue: -5,
      vibrance: 12,
    },
    shotInfo: {
      iso: 800,
      aperture: 'f/1.8',
      shutter: '1/60s',
      focalLength: '24mm',
      device: 'OPPO Find X7 Ultra',
    },
    updatedAt: new Date(Date.now() - 3600000).toISOString(),
  },
  {
    id: 'cloud_4',
    name: '徕卡黑白',
    coverPath: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=500&fit=crop',
    author: '@街拍大师',
    brand: 'Leica',
    tags: ['黑白', '街拍', '纪实'],
    isNew: false,
    isHncs: false,
    rating: 4.7,
    downloadCount: 9200,
    cameraParams: {
      saturation: -100,
      contrast: 25,
      brightness: 5,
      warmth: 0,
      sharpness: 22,
      clarity: 18,
      highlights: -15,
      shadows: 12,
      hue: 0,
      vibrance: 0,
    },
    shotInfo: {
      iso: 400,
      aperture: 'f/2.0',
      shutter: '1/125s',
      focalLength: '35mm',
      device: 'Leica M11',
    },
    updatedAt: new Date(Date.now() - 7200000).toISOString(),
  },
  {
    id: 'cloud_5',
    name: '美食暖调',
    coverPath: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=500&fit=crop',
    author: '@美食摄影',
    brand: 'OPPO',
    tags: ['美食', '暖调', '诱人'],
    isNew: true,
    isHncs: false,
    rating: 4.6,
    downloadCount: 7400,
    cameraParams: {
      saturation: 18,
      contrast: 12,
      brightness: 8,
      warmth: 22,
      sharpness: 15,
      clarity: 10,
      highlights: -8,
      shadows: 6,
      hue: 3,
      vibrance: 15,
    },
    shotInfo: {
      iso: 100,
      aperture: 'f/2.8',
      shutter: '1/200s',
      focalLength: '50mm',
      device: 'OPPO Find X6 Pro',
    },
    updatedAt: new Date().toISOString(),
  },
  {
    id: 'cloud_6',
    name: '清新人像',
    coverPath: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=500&fit=crop',
    author: '@人像摄影',
    brand: 'vivo',
    tags: ['人像', '清新', '通透'],
    isNew: false,
    isHncs: true,
    rating: 4.4,
    downloadCount: 6200,
    cameraParams: {
      saturation: 10,
      contrast: -5,
      brightness: 5,
      warmth: 8,
      sharpness: 12,
      clarity: 6,
      highlights: -10,
      shadows: 8,
      hue: 1,
      vibrance: 8,
    },
    shotInfo: {
      iso: 100,
      aperture: 'f/1.7',
      shutter: '1/320s',
      focalLength: '85mm',
      device: 'vivo X100 Pro',
    },
    updatedAt: new Date(Date.now() - 1800000).toISOString(),
  },
];

// 模拟网络延迟
const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// 获取云同步预设列表
export async function fetchCloudPresets(): Promise<CloudPreset[]> {
  await delay(800 + Math.random() * 400); // 模拟网络延迟
  
  // 模拟实时更新：随机更新一个预设的下载量
  const randomIndex = Math.floor(Math.random() * cloudPresetsData.length);
  cloudPresetsData[randomIndex].downloadCount += Math.floor(Math.random() * 100);
  cloudPresetsData[randomIndex].updatedAt = new Date().toISOString();
  
  return [...cloudPresetsData];
}

// 获取单个预设详情
export async function fetchPresetDetail(id: string): Promise<CloudPreset | null> {
  await delay(300);
  return cloudPresetsData.find(p => p.id === id) || null;
}

// 模拟实时订阅
export function subscribeToPresets(callback: (presets: CloudPreset[]) => void): () => void {
  let intervalId: number;
  
  const fetchAndUpdate = async () => {
    const presets = await fetchCloudPresets();
    callback(presets);
  };
  
  // 初始加载
  fetchAndUpdate();
  
  // 每30秒更新一次
  intervalId = window.setInterval(fetchAndUpdate, 30000);
  
  // 返回取消订阅函数
  return () => {
    clearInterval(intervalId);
  };
}

// ========== 自定义预设管理 ==========

// 添加自定义预设
export async function addCustomPreset(preset: Omit<CloudPreset, 'id' | 'updatedAt'>): Promise<CloudPreset> {
  await delay(500);
  
  const newPreset: CloudPreset = {
    ...preset,
    id: `custom_${Date.now()}`,
    updatedAt: new Date().toISOString(),
  };
  
  cloudPresetsData.push(newPreset);
  return newPreset;
}

// 删除预设
export async function deletePreset(id: string): Promise<boolean> {
  await delay(300);
  
  const index = cloudPresetsData.findIndex(p => p.id === id);
  if (index !== -1) {
    cloudPresetsData.splice(index, 1);
    return true;
  }
  return false;
}

// 更新预设
export async function updatePreset(id: string, updates: Partial<CloudPreset>): Promise<CloudPreset | null> {
  await delay(400);
  
  const index = cloudPresetsData.findIndex(p => p.id === id);
  if (index !== -1) {
    cloudPresetsData[index] = {
      ...cloudPresetsData[index],
      ...updates,
      updatedAt: new Date().toISOString(),
    };
    return cloudPresetsData[index];
  }
  return null;
}

// 批量删除预设
export async function batchDeletePresets(ids: string[]): Promise<number> {
  await delay(500);
  
  let deletedCount = 0;
  ids.forEach(id => {
    const index = cloudPresetsData.findIndex(p => p.id === id);
    if (index !== -1) {
      cloudPresetsData.splice(index, 1);
      deletedCount++;
    }
  });
  
  return deletedCount;
}

// 导出预设数据（用于分享）
export async function exportPreset(id: string): Promise<string | null> {
  const preset = await fetchPresetDetail(id);
  if (preset) {
    return JSON.stringify(preset, null, 2);
  }
  return null;
}

// 导入预设数据
export async function importPreset(jsonData: string): Promise<CloudPreset | null> {
  await delay(500);
  
  try {
    const data = JSON.parse(jsonData);
    const newPreset = await addCustomPreset(data);
    return newPreset;
  } catch (error) {
    console.error('Import preset failed:', error);
    return null;
  }
}
