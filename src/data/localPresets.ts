import { Preset } from '../store/appStore';

/**
 * 本地兜底预设数据
 * 当外部预设源加载失败时展示，确保用户始终能看到预设内容
 */
export const LOCAL_FALLBACK_PRESETS: Preset[] = [
  {
    id: 'local_1',
    name: '清新人像',
    coverPath: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
      'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&h=300&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['人像', '清新', 'hncs'],
    isNew: true,
    isHncs: true,
    mode: 'auto',
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外或充足自然光【场景推荐】街拍、人像、风景、建筑【拍摄要点】适合追求自然清新的风格'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '清新 80%', span: 2 },
          { label: '饱和度', value: '+10', span: 1 },
          { label: '对比度', value: '+5', span: 1 },
          { label: '锐度', value: '+15', span: 1 },
          { label: '清晰度', value: '+10', span: 1 }
        ]
      }
    ],
    downloads: 15230,
    rating: 4.7,
    ratingCount: 328,
    comments: [],
    saturation: 10,
    contrast: 5,
    warmth: 8,
    sharpness: 15,
    clarity: 10
  },
  {
    id: 'local_2',
    name: '夜景霓虹',
    coverPath: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['夜景', '霓虹', 'hncs'],
    isNew: false,
    isHncs: true,
    mode: 'pro',
    description: {
      title: '拍摄建议',
      content: '【环境建议】城市夜晚霓虹灯环境【场景推荐】夜景、街拍、建筑【拍摄要点】适合色彩丰富的夜晚场景'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '霓虹 100%', span: 2 },
          { label: '饱和度', value: '+35', span: 1 },
          { label: '对比度', value: '+20', span: 1 },
          { label: '锐度', value: '+25', span: 1 },
          { label: '色温', value: '-10', span: 1 }
        ]
      }
    ],
    downloads: 23100,
    rating: 4.8,
    ratingCount: 512,
    comments: [],
    saturation: 35,
    contrast: 20,
    warmth: -10,
    sharpness: 25,
    clarity: 15
  },
  {
    id: 'local_3',
    name: '美食暖调',
    coverPath: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=280&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['美食', '暖调'],
    isNew: true,
    isHncs: false,
    mode: 'auto',
    description: {
      title: '拍摄建议',
      content: '【环境建议】餐厅或自然光充足的环境【场景推荐】美食、静物【拍摄要点】暖色调增强食欲感'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '暖调 90%', span: 2 },
          { label: '饱和度', value: '+20', span: 1 },
          { label: '对比度', value: '+10', span: 1 },
          { label: '锐度', value: '+18', span: 1 },
          { label: '色温', value: '+15', span: 1 }
        ]
      }
    ],
    downloads: 18900,
    rating: 4.6,
    ratingCount: 276,
    comments: [],
    saturation: 20,
    contrast: 10,
    warmth: 15,
    sharpness: 18,
    clarity: 12
  },
  {
    id: 'local_4',
    name: '街拍黑白',
    coverPath: 'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1444723121867-c61267198d6c?w=400&h=320&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['街拍', '黑白'],
    isNew: false,
    isHncs: false,
    mode: 'pro',
    description: {
      title: '拍摄建议',
      content: '【环境建议】强烈光影对比场景【场景推荐】街拍、建筑、纪实【拍摄要点】利用明暗对比突出主体'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '黑白 100%', span: 2 },
          { label: '饱和度', value: '0', span: 1 },
          { label: '对比度', value: '+25', span: 1 },
          { label: '锐度', value: '+20', span: 1 },
          { label: '颗粒', value: '+12', span: 1 }
        ]
      }
    ],
    downloads: 12400,
    rating: 4.5,
    ratingCount: 198,
    comments: [],
    saturation: -100,
    contrast: 25,
    warmth: 0,
    sharpness: 20,
    clarity: 18
  },
  {
    id: 'local_5',
    name: '风景通透',
    coverPath: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['风景', '通透', 'hncs'],
    isNew: true,
    isHncs: true,
    mode: 'auto',
    description: {
      title: '拍摄建议',
      content: '【环境建议】日间户外风景【场景推荐】风光、自然、旅行【拍摄要点】增强通透感和色彩层次'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '通透 85%', span: 2 },
          { label: '饱和度', value: '+18', span: 1 },
          { label: '对比度', value: '+12', span: 1 },
          { label: '锐度', value: '+22', span: 1 },
          { label: '去雾', value: '+15', span: 1 }
        ]
      }
    ],
    downloads: 26700,
    rating: 4.8,
    ratingCount: 645,
    comments: [],
    saturation: 18,
    contrast: 12,
    warmth: -5,
    sharpness: 22,
    clarity: 20
  },
  {
    id: 'local_6',
    name: '建筑几何',
    coverPath: 'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=400&h=340&fit=crop',
    galleryImages: [
      'https://images.unsplash.com/photo-1486325212027-8081e485255e?w=400&h=340&fit=crop'
    ],
    author: '@OMaster',
    brand: 'OMaster',
    tags: ['建筑', '几何'],
    isNew: false,
    isHncs: false,
    mode: 'pro',
    description: {
      title: '拍摄建议',
      content: '【环境建议】城市建筑环境【场景推荐】建筑、几何、极简【拍摄要点】强调线条和结构感'
    },
    sections: [
      {
        title: '🎨 大师调色参数',
        items: [
          { label: '滤镜', value: '结构 100%', span: 2 },
          { label: '饱和度', value: '+8', span: 1 },
          { label: '对比度', value: '+18', span: 1 },
          { label: '锐度', value: '+30', span: 1 },
          { label: '清晰度', value: '+25', span: 1 }
        ]
      }
    ],
    downloads: 9800,
    rating: 4.4,
    ratingCount: 156,
    comments: [],
    saturation: 8,
    contrast: 18,
    warmth: 0,
    sharpness: 30,
    clarity: 25
  }
];
