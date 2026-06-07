// 水印模板数据 - 参考2026年国内手机水印趋势
export interface WatermarkTemplate {
  id: string;
  name: string;
  description: string;
  previewUrl: string;
  category: 'brand' | 'functional' | 'free';
  features: string[];
  source?: string; // 来源品牌参考
}

// 使用图片生成API
const generateImageUrl = (prompt: string) => 
  `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${encodeURIComponent(prompt)}&image_size=landscape_4_3`;

export const watermarkTemplates: WatermarkTemplate[] = [
  // 品牌水印
  {
    id: 'hasselblad',
    name: '哈苏认证',
    description: 'OPPO哈苏HNCS官方认证水印',
    previewUrl: generateImageUrl('Hasselblad camera watermark, luxury brand logo, professional photography badge, orange and black theme, OPPO phone'),
    category: 'brand',
    features: ['HNCS认证', '官方授权', '专业风格'],
    source: 'OPPO'
  },
  {
    id: 'leica',
    name: '徕卡经典',
    description: '小米徕卡联名水印，红标设计',
    previewUrl: generateImageUrl('Leica camera watermark, red dot logo, minimalist design, black and white photography style, Xiaomi phone'),
    category: 'brand',
    features: ['徕卡红标', '经典双拼', '大师风格'],
    source: '小米'
  },
  {
    id: 'zeiss',
    name: '蔡司光学',
    description: 'vivo蔡司联名水印，T*镀膜标识',
    previewUrl: generateImageUrl('Zeiss camera watermark, blue accent, T coating mark, professional lens branding, vivo phone'),
    category: 'brand',
    features: ['蔡司T*', '光学认证', '多种背景色'],
    source: 'vivo'
  },
  {
    id: 'oppo-frame',
    name: 'OPPO相框',
    description: 'OPPO多样相框水印样式',
    previewUrl: generateImageUrl('OPPO photo frame watermark, elegant border design, camera parameters display, white background'),
    category: 'brand',
    features: ['相框样式', '参数展示', '节日限定'],
    source: 'OPPO'
  },
  // 功能水印
  {
    id: 'camera-info',
    name: '相机信息',
    description: '显示完整相机参数信息',
    previewUrl: generateImageUrl('camera parameters watermark, ISO shutter speed aperture, technical info overlay, photography metadata'),
    category: 'functional',
    features: ['ISO/快门', '光圈/焦距', '日期时间']
  },
  {
    id: 'timestamp',
    name: '时间戳',
    description: '简洁的时间日期水印',
    previewUrl: generateImageUrl('date timestamp watermark, calendar date display, minimal design, corner placement'),
    category: 'functional',
    features: ['日期显示', '时间记录', '自定义格式']
  },
  {
    id: 'location',
    name: '旅拍打卡',
    description: '旅行场景水印，显示城市名称',
    previewUrl: generateImageUrl('travel location watermark, city name display, GPS coordinates, travel photography style'),
    category: 'functional',
    features: ['城市名称', 'GPS定位', '旅拍风格'],
    source: 'vivo'
  },
  {
    id: 'live-photo',
    name: '动态照片',
    description: 'Live Photo动态水印效果',
    previewUrl: generateImageUrl('Live Photo watermark, dynamic effect indicator, motion photo badge, animated icon'),
    category: 'functional',
    features: ['动态效果', '实况标识', '趣味动画'],
    source: 'vivo/小米'
  },
  // 免费模板 - 参考2026年国内手机水印趋势
  {
    id: 'stamp',
    name: '邮票邮戳',
    description: 'vivo邮票邮戳风格，复古文艺',
    previewUrl: generateImageUrl('postage stamp watermark, vintage style, postal mark design, retro aesthetic, decorative border'),
    category: 'free',
    features: ['邮票边框', '邮戳效果', '复古文艺'],
    source: 'vivo'
  },
  {
    id: 'chinese-style',
    name: '国风印章',
    description: '国风传统印章水印，水墨风格',
    previewUrl: generateImageUrl('Chinese style watermark, traditional seal, ink painting style, red stamp mark, calligraphy'),
    category: 'free',
    features: ['国风设计', '水墨风格', '生肖定制'],
    source: 'vivo/荣耀'
  },
  {
    id: 'film-frame',
    name: '胶片相框',
    description: '小米胶片风格相框水印',
    previewUrl: generateImageUrl('film camera frame watermark, vintage film border, analog photography style, nostalgic look'),
    category: 'free',
    features: ['胶片质感', '复古边框', '怀旧风格'],
    source: '小米/荣耀'
  },
  {
    id: 'new-year',
    name: '新春舞狮',
    description: '小米新春舞狮水印，非遗文化',
    previewUrl: generateImageUrl('Chinese New Year watermark, lion dance design, festive red and gold, traditional pattern, Spring Festival'),
    category: 'free',
    features: ['舞狮元素', '新春限定', '非遗文化'],
    source: '小米'
  },
  {
    id: 'signature',
    name: '艺术签名',
    description: '手写艺术签名水印',
    previewUrl: generateImageUrl('handwritten signature watermark, script font style, personal signature, elegant cursive'),
    category: 'free',
    features: ['手写体', '个性签名', '艺术风格'],
    source: 'vivo'
  },
  {
    id: 'tile-pattern',
    name: '平铺防盗',
    description: '防盗用平铺水印',
    previewUrl: generateImageUrl('tiled watermark pattern, repeated text overlay, diagonal pattern, copyright protection'),
    category: 'free',
    features: ['全覆盖', '难去除', '防盗用']
  },
  {
    id: 'diagonal',
    name: '对角线',
    description: '对角线文字水印',
    previewUrl: generateImageUrl('diagonal text watermark, copyright text across image, bold typography, protection overlay'),
    category: 'free',
    features: ['对角布局', '版权保护', '视觉冲击']
  },
  {
    id: 'minimal',
    name: '极简白底',
    description: '小米白底水印潮流，简洁高级',
    previewUrl: generateImageUrl('minimal white background watermark, clean typography, simple text placement, elegant design'),
    category: 'free',
    features: ['白底设计', '极简风格', '高级质感'],
    source: '小米'
  }
];
