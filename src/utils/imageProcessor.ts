// 真实图片处理核心模块 - Canvas API实现

export interface ImageAdjustParams {
  saturation: number; // -100 to 100
  contrast: number;   // -100 to 100
  brightness: number; // -100 to 100
  warmth: number;     // -100 to 100
  cyanMagenta: number; // -100 to 100
  sharpness: number;   // 0 to 100
  tone: number;       // -100 to 100
  softLight: number;  // 0 to 100
  vignette: boolean;
  filter?: string;    // 原图、胶片、黑白
}

export interface FrameStyle {
  id: string;
  name: string;
  type: 'solid' | 'gradient' | 'pattern' | 'literary' | 'collage';
  width: number;
  color?: string;
  gradient?: string[];
  pattern?: string;
  background?: string;
  decorations?: string[];
}

export interface WatermarkOptions {
  text: string;
  brand?: string;
  year?: string;
  position: 'top-left' | 'top-right' | 'bottom-left' | 'bottom-right' | 'center';
  fontSize: number;
  color: string;
  opacity: number;
  showYear: boolean;
  fontFamily?: string;
}

/**
 * 将参数应用到图片 - 真实算法实现
 */
export async function applyImageAdjustments(
  imageUrl: string,
  params: ImageAdjustParams
): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) {
          reject(new Error('Canvas not supported'));
          return;
        }
        ctx.drawImage(img, 0, 0);

        // 应用滤镜
        const filterStr = buildCSSFilter(params);
        ctx.filter = filterStr;

        // 重新绘制
        ctx.drawImage(img, 0, 0);
        ctx.filter = 'none';

        // 像素级处理 - 锐化、柔光、暗角
        if (params.sharpness > 0 || params.softLight > 0 || params.vignette) {
          applyPixelLevelEffects(ctx, canvas.width, canvas.height, params);
        }

        resolve(canvas.toDataURL('image/jpeg', 0.92));
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = imageUrl;
  });
}

/**
 * 构建CSS滤镜字符串
 */
function buildCSSFilter(params: ImageAdjustParams): string {
  const filters: string[] = [];
  
  // 饱和度
  filters.push(`saturate(${100 + params.saturation}%)`);
  
  // 对比度
  filters.push(`contrast(${100 + params.contrast}%)`);
  
  // 亮度
  filters.push(`brightness(${100 + params.brightness}%)`);
  
  // 色温（暖色为正）
  const sepiaAmount = Math.abs(params.warmth) / 200;
  if (params.warmth > 0) {
    filters.push(`sepia(${sepiaAmount})`);
    filters.push(`hue-rotate(${params.warmth / 10}deg)`);
  } else if (params.warmth < 0) {
    filters.push(`hue-rotate(${params.warmth / 5}deg)`);
  }
  
  // 黑白滤镜
  if (params.filter === '黑白') {
    filters.push('grayscale(100%)');
  } else if (params.filter === '胶片') {
    filters.push(`sepia(${0.3 + (params.tone / 200)})`);
  }
  
  return filters.join(' ');
}

/**
 * 像素级处理 - 锐化/柔光/暗角
 */
function applyPixelLevelEffects(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  params: ImageAdjustParams
): void {
  if (params.vignette) {
    applyVignette(ctx, width, height);
  }
  
  if (params.sharpness > 0) {
    applySharpen(ctx, width, height, params.sharpness / 100);
  }
  
  if (params.softLight > 0) {
    applySoftLight(ctx, width, height, params.softLight / 100);
  }
}

/**
 * 暗角效果
 */
function applyVignette(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number
): void {
  const centerX = width / 2;
  const centerY = height / 2;
  const outerRadius = Math.sqrt(centerX * centerX + centerY * centerY);
  
  const gradient = ctx.createRadialGradient(
    centerX, centerY, outerRadius * 0.5,
    centerX, centerY, outerRadius
  );
  gradient.addColorStop(0, 'rgba(0,0,0,0)');
  gradient.addColorStop(1, 'rgba(0,0,0,0.7)');
  
  ctx.globalCompositeOperation = 'multiply';
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, width, height);
  ctx.globalCompositeOperation = 'source-over';
}

/**
 * 锐化效果
 */
function applySharpen(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  amount: number
): void {
  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;
  const kernel = [
    0, -amount, 0,
    -amount, 1 + 4 * amount, -amount,
    0, -amount, 0
  ];
  const side = Math.round(Math.sqrt(kernel.length));
  const halfSide = Math.floor(side / 2);
  const output = ctx.createImageData(width, height);
  const outputData = output.data;
  
  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const dstOff = (y * width + x) * 4;
      let r = 0, g = 0, b = 0;
      
      for (let cy = 0; cy < side; cy++) {
        for (let cx = 0; cx < side; cx++) {
          const scy = y + cy - halfSide;
          const scx = x + cx - halfSide;
          if (scy >= 0 && scy < height && scx >= 0 && scx < width) {
            const srcOff = (scy * width + scx) * 4;
            const weight = kernel[cy * side + cx];
            r += data[srcOff] * weight;
            g += data[srcOff + 1] * weight;
            b += data[srcOff + 2] * weight;
          }
        }
      }
      
      outputData[dstOff] = Math.max(0, Math.min(255, r));
      outputData[dstOff + 1] = Math.max(0, Math.min(255, g));
      outputData[dstOff + 2] = Math.max(0, Math.min(255, b));
      outputData[dstOff + 3] = data[dstOff + 3];
    }
  }
  
  ctx.putImageData(output, 0, 0);
}

/**
 * 柔光效果
 */
function applySoftLight(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  amount: number
): void {
  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;
  
  for (let i = 0; i < data.length; i += 4) {
    data[i] = data[i] + (255 - data[i]) * amount * 0.2;
    data[i + 1] = data[i + 1] + (255 - data[i + 1]) * amount * 0.2;
    data[i + 2] = data[i + 2] + (255 - data[i + 2]) * amount * 0.2;
  }
  
  ctx.putImageData(imageData, 0, 0);
}

/**
 * 添加水印到图片
 */
export async function addWatermarkToImage(
  imageUrl: string,
  options: WatermarkOptions
): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas not supported'));
          return;
        }
        
        ctx.drawImage(img, 0, 0);
        
        // 计算水印位置
        const padding = Math.max(20, img.width * 0.02);
        const fontSize = options.fontSize || Math.max(14, img.width * 0.02);
        const lineHeight = fontSize * 1.5;
        
        const texts: string[] = [];
        if (options.showYear && options.year) texts.push(options.year);
        if (options.brand) texts.push(options.brand);
        if (options.text) texts.push(options.text);
        
        const totalLines = texts.length;
        const totalHeight = totalLines * lineHeight;
        
        let x: number, y: number, textAlign: CanvasTextAlign;
        switch (options.position) {
          case 'top-left':
            x = padding; y = padding + fontSize; textAlign = 'left';
            break;
          case 'top-right':
            x = img.width - padding; y = padding + fontSize; textAlign = 'right';
            break;
          case 'bottom-left':
            x = padding; y = img.height - padding - totalHeight + fontSize; textAlign = 'left';
            break;
          case 'bottom-right':
            x = img.width - padding; y = img.height - padding - totalHeight + fontSize; textAlign = 'right';
            break;
          default:
            x = img.width / 2; y = img.height / 2; textAlign = 'center';
        }
        
        ctx.font = `bold ${fontSize}px ${options.fontFamily || 'sans-serif'}`;
        ctx.textAlign = textAlign;
        ctx.textBaseline = 'top';
        ctx.globalAlpha = options.opacity;
        
        texts.forEach((text, i) => {
          const textY = y + i * lineHeight;
          // 阴影背景
          ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
          const metrics = ctx.measureText(text);
          const bgWidth = metrics.width + padding;
          const bgHeight = lineHeight;
          let bgX = textAlign === 'left' ? x - padding / 2 : 
                    textAlign === 'right' ? x - metrics.width - padding / 2 :
                    x - metrics.width / 2 - padding / 2;
          ctx.fillRect(bgX, textY - padding / 2, bgWidth, bgHeight);
          
          // 文字
          ctx.fillStyle = options.color;
          ctx.fillText(text, x, textY);
        });
        
        ctx.globalAlpha = 1;
        resolve(canvas.toDataURL('image/jpeg', 0.92));
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = imageUrl;
  });
}

/**
 * 添加边框到图片 - 真实合成
 */
export async function addFrameToImage(
  imageUrl: string,
  frame: FrameStyle,
  customWidth?: number
): Promise<string> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        const frameWidth = customWidth ?? frame.width;
        const literaryPadding = frame.type === 'literary' ? frameWidth : 0;
        canvas.width = img.width + 2 * (frameWidth + literaryPadding);
        canvas.height = img.height + 2 * (frameWidth + literaryPadding);
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          reject(new Error('Canvas not supported'));
          return;
        }
        
        // 绘制边框背景
        if (frame.gradient && frame.gradient.length > 0) {
          const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
          frame.gradient.forEach((color, i) => {
            gradient.addColorStop(i / (frame.gradient!.length - 1), color);
          });
          ctx.fillStyle = gradient;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
        } else if (frame.background) {
          ctx.fillStyle = frame.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
        } else {
          ctx.fillStyle = frame.color || '#FFFFFF';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
        }
        
        // 绘制装饰
        if (frame.decorations) {
          frame.decorations.forEach((dec, i) => {
            drawDecoration(ctx, dec, canvas.width, canvas.height, i);
          });
        }
        
        // 绘制文学边框文字
        if (frame.type === 'literary' && frame.decorations) {
          drawLiteraryDecorations(ctx, canvas.width, canvas.height, frame);
        }
        
        // 绘制图片
        ctx.drawImage(img, frameWidth + literaryPadding, frameWidth + literaryPadding);
        
        resolve(canvas.toDataURL('image/jpeg', 0.92));
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = imageUrl;
  });
}

/**
 * 绘制装饰
 */
function drawDecoration(
  ctx: CanvasRenderingContext2D,
  type: string,
  width: number,
  height: number,
  index: number
): void {
  ctx.save();
  ctx.fillStyle = 'rgba(255, 215, 0, 0.3)';
  ctx.strokeStyle = 'rgba(255, 215, 0, 0.5)';
  ctx.lineWidth = 2;
  
  const size = 30;
  const positions: [number, number][] = [
    [10, 10], [width - 10, 10], [10, height - 10], [width - 10, height - 10]
  ];
  
  const [x, y] = positions[index % 4];
  if (type === 'corner') {
    ctx.beginPath();
    if (index === 0) {
      ctx.moveTo(x, y + size);
      ctx.lineTo(x, y);
      ctx.lineTo(x + size, y);
    } else if (index === 1) {
      ctx.moveTo(x - size, y);
      ctx.lineTo(x, y);
      ctx.lineTo(x, y + size);
    } else if (index === 2) {
      ctx.moveTo(x, y - size);
      ctx.lineTo(x, y);
      ctx.lineTo(x + size, y);
    } else {
      ctx.moveTo(x - size, y);
      ctx.lineTo(x, y);
      ctx.lineTo(x, y - size);
    }
    ctx.stroke();
  }
  
  ctx.restore();
}

/**
 * 绘制文学装饰
 */
function drawLiteraryDecorations(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  frame: FrameStyle
): void {
  ctx.save();
  ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
  ctx.font = 'italic 14px serif';
  ctx.textAlign = 'center';
  
  // 顶部诗句
  const topText = '· 2026 · 美好瞬间 ·';
  ctx.fillText(topText, width / 2, 25);
  
  // 底部诗句
  const bottomText = '· 时光留影 · 记忆永存 ·';
  ctx.fillText(bottomText, width / 2, height - 15);
  
  ctx.restore();
}

/**
 * 创建拼图 - 真实合成
 */
export async function createCollage(
  imageUrls: string[],
  layout: 'grid2' | 'grid3' | 'grid4' | 'film' | 'story' | 'free',
  options: { gap: number; background: string }
): Promise<string> {
  return new Promise(async (resolve, reject) => {
    try {
      // 加载所有图片
      const images: HTMLImageElement[] = [];
      for (const url of imageUrls) {
        const img = await loadImage(url);
        images.push(img);
      }
      
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('Canvas not supported'));
        return;
      }
      
      const gap = options.gap;
      
      switch (layout) {
        case 'grid2':
          canvas.width = images[0].width * 2 + gap;
          canvas.height = images[0].height + gap;
          ctx.fillStyle = options.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          images.slice(0, 2).forEach((img, i) => {
            const x = i * (images[0].width + gap);
            drawImageCover(ctx, img, x, gap / 2, images[0].width, images[0].height - gap);
          });
          break;
          
        case 'grid3':
          canvas.width = images[0].width * 3 + gap * 2;
          canvas.height = images[0].height + gap;
          ctx.fillStyle = options.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          images.slice(0, 3).forEach((img, i) => {
            const x = i * (images[0].width + gap);
            drawImageCover(ctx, img, x, gap / 2, images[0].width, images[0].height - gap);
          });
          break;
          
        case 'grid4':
          canvas.width = images[0].width * 2 + gap;
          canvas.height = images[0].height * 2 + gap * 2;
          ctx.fillStyle = options.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          images.slice(0, 4).forEach((img, i) => {
            const col = i % 2;
            const row = Math.floor(i / 2);
            const x = col * (images[0].width + gap);
            const y = row * (images[0].height + gap);
            drawImageCover(ctx, img, x, y, images[0].width, images[0].height);
          });
          break;
          
        case 'film':
          canvas.width = images[0].width + gap;
          canvas.height = images[0].height * images.length + gap * (images.length - 1);
          ctx.fillStyle = '#1a1a1a';
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          // 胶片孔
          ctx.fillStyle = '#000';
          for (let i = 0; i < images.length; i++) {
            for (let j = 0; j < 8; j++) {
              const y = i * (images[0].height + gap) + j * 30;
              ctx.fillRect(0, y, 8, 16);
              ctx.fillRect(canvas.width - 8, y, 8, 16);
            }
          }
          images.forEach((img, i) => {
            const y = i * (images[0].height + gap);
            ctx.drawImage(img, 12, y, images[0].width - 24, images[0].height);
          });
          break;
          
        case 'story':
          canvas.width = images[0].width + gap;
          canvas.height = images[0].height * images.length + gap * (images.length - 1);
          ctx.fillStyle = options.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          images.forEach((img, i) => {
            const y = i * (images[0].height + gap);
            ctx.drawImage(img, 0, y, images[0].width, images[0].height);
          });
          break;
          
        case 'free':
          // 自由拼图
          canvas.width = 1080;
          canvas.height = 1080;
          ctx.fillStyle = options.background;
          ctx.fillRect(0, 0, canvas.width, canvas.height);
          const size = (canvas.width - gap * 3) / 2;
          images.slice(0, 4).forEach((img, i) => {
            const col = i % 2;
            const row = Math.floor(i / 2);
            const x = col * (size + gap) + gap;
            const y = row * (size + gap) + gap;
            drawImageCover(ctx, img, x, y, size, size);
          });
          break;
      }
      
      resolve(canvas.toDataURL('image/jpeg', 0.92));
    } catch (e) {
      reject(e);
    }
  });
}

/**
 * 加载图片
 */
function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = url;
  });
}

/**
 * 绘制图片（cover模式）
 */
function drawImageCover(
  ctx: CanvasRenderingContext2D,
  img: HTMLImageElement,
  x: number, y: number, w: number, h: number
): void {
  const scale = Math.max(w / img.width, h / img.height);
  const sw = w / scale;
  const sh = h / scale;
  const sx = (img.width - sw) / 2;
  const sy = (img.height - sh) / 2;
  ctx.drawImage(img, sx, sy, sw, sh, x, y, w, h);
}

/**
 * 真实场景识别 - 基于像素分析
 */
export interface SceneAnalysisResult {
  scene: string;
  confidence: number;
  hasselbladStyle: string;
  description: string;
  suggestedParams: ImageAdjustParams;
  topScenes: { name: string; confidence: number }[];
}

export async function analyzeImageScene(imageUrl: string): Promise<SceneAnalysisResult> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = 'anonymous';
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas');
        const sampleSize = 100;
        canvas.width = sampleSize;
        canvas.height = sampleSize;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) {
          reject(new Error('Canvas not supported'));
          return;
        }
        ctx.drawImage(img, 0, 0, sampleSize, sampleSize);
        const imageData = ctx.getImageData(0, 0, sampleSize, sampleSize);
        const data = imageData.data;
        
        // 分析图片特征
        let totalR = 0, totalG = 0, totalB = 0;
        let brightness = 0;
        let saturation = 0;
        let warmPixels = 0;
        let coolPixels = 0;
        let darkPixels = 0;
        let brightPixels = 0;
        let skyPixels = 0;
        let skinPixels = 0;
        let greenPixels = 0;
        let bluePixels = 0;
        
        const pixelCount = sampleSize * sampleSize;
        
        for (let i = 0; i < data.length; i += 4) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];
          
          totalR += r;
          totalG += g;
          totalB += b;
          
          const max = Math.max(r, g, b);
          const min = Math.min(r, g, b);
          const lum = (r * 0.299 + g * 0.587 + b * 0.114) / 255;
          const sat = max === 0 ? 0 : (max - min) / max;
          
          brightness += lum;
          saturation += sat;
          
          if (lum < 0.3) darkPixels++;
          if (lum > 0.7) brightPixels++;
          
          if (r > b + 30 && r > g) warmPixels++;
          if (b > r + 30 && b > g) coolPixels++;
          
          // 天空（上方区域偏蓝）
          const pixelIndex = (i / 4);
          const y = Math.floor(pixelIndex / sampleSize);
          if (y < sampleSize * 0.4 && b > 150 && b > r) skyPixels++;
          
          // 肤色
          if (r > 95 && g > 40 && b > 20 && r > g && r > b && Math.abs(r - g) > 15) skinPixels++;
          
          // 绿色植被
          if (g > r && g > b && g > 80) greenPixels++;
          
          // 蓝色水域
          if (b > r && b > g && b > 100) bluePixels++;
        }
        
        const avgBrightness = brightness / pixelCount;
        const avgSaturation = (saturation / pixelCount) * 100;
        const avgR = totalR / pixelCount;
        const avgG = totalG / pixelCount;
        const avgB = totalB / pixelCount;
        
        // 基于特征推断场景
        const scores: { name: string; score: number; hasselblad: string; params: ImageAdjustParams }[] = [
          {
            name: '人像',
            score: skinPixels / pixelCount * 100 * 3,
            hasselblad: 'portrait',
            params: { saturation: 10, contrast: 5, brightness: 5, warmth: 8, cyanMagenta: 0, sharpness: 15, tone: 5, softLight: 25, vignette: false, filter: '原图' }
          },
          {
            name: '风景',
            score: (greenPixels + skyPixels) / pixelCount * 100 * 2,
            hasselblad: 'natural',
            params: { saturation: 20, contrast: 15, brightness: 10, warmth: -5, cyanMagenta: -5, sharpness: 25, tone: 15, softLight: 10, vignette: false, filter: '原图' }
          },
          {
            name: '夜景',
            score: (darkPixels / pixelCount) * 100 * 2.5 + (avgBrightness < 0.3 ? 30 : 0),
            hasselblad: 'cinematic',
            params: { saturation: 25, contrast: 20, brightness: 0, warmth: -10, cyanMagenta: 5, sharpness: 30, tone: 20, softLight: 20, vignette: true, filter: '原图' }
          },
          {
            name: '日落黄昏',
            score: (warmPixels / pixelCount) * 100 * 2.5 + (avgBrightness > 0.4 && avgBrightness < 0.7 ? 20 : 0),
            hasselblad: 'cinematic',
            params: { saturation: 30, contrast: 15, brightness: 5, warmth: 20, cyanMagenta: 0, sharpness: 20, tone: 10, softLight: 30, vignette: true, filter: '胶片' }
          },
          {
            name: '美食',
            score: (warmPixels / pixelCount) * 100 * 1.5 + (avgSaturation > 40 ? 20 : 0),
            hasselblad: 'natural',
            params: { saturation: 15, contrast: 10, brightness: 5, warmth: 20, cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 40, vignette: false, filter: '原图' }
          },
          {
            name: '建筑',
            score: (avgSaturation < 30 ? 30 : 0) + (brightPixels / pixelCount) * 50,
            hasselblad: 'natural',
            params: { saturation: 8, contrast: 15, brightness: 0, warmth: 0, cyanMagenta: 0, sharpness: 20, tone: 15, softLight: 15, vignette: false, filter: '原图' }
          },
          {
            name: '街拍',
            score: (avgSaturation > 30 && avgSaturation < 60 ? 30 : 0) + (brightPixels / pixelCount) * 30,
            hasselblad: 'cinematic',
            params: { saturation: 12, contrast: 18, brightness: 0, warmth: 10, cyanMagenta: 0, sharpness: 18, tone: 12, softLight: 20, vignette: true, filter: '胶片' }
          },
          {
            name: '黑白街拍',
            score: (avgSaturation < 20 ? 40 : 0),
            hasselblad: 'cinematic',
            params: { saturation: -100, contrast: 25, brightness: 0, warmth: 0, cyanMagenta: 0, sharpness: 20, tone: 25, softLight: 15, vignette: true, filter: '黑白' }
          },
          {
            name: '花卉',
            score: (saturation > 50 ? 40 : 0) + (greenPixels / pixelCount) * 30,
            hasselblad: 'natural',
            params: { saturation: 25, contrast: 10, brightness: 5, warmth: 5, cyanMagenta: 0, sharpness: 20, tone: 5, softLight: 30, vignette: false, filter: '原图' }
          },
          {
            name: '海景水域',
            score: (bluePixels / pixelCount) * 100 * 2,
            hasselblad: 'cinematic',
            params: { saturation: 15, contrast: 8, brightness: 10, warmth: -8, cyanMagenta: 0, sharpness: 15, tone: 8, softLight: 15, vignette: false, filter: '原图' }
          },
          {
            name: '雪景',
            score: (brightPixels / pixelCount) * 50 + (avgBrightness > 0.7 ? 30 : 0),
            hasselblad: 'natural',
            params: { saturation: 5, contrast: 5, brightness: 10, warmth: -10, cyanMagenta: 0, sharpness: 15, tone: 5, softLight: 10, vignette: false, filter: '原图' }
          },
          {
            name: '宠物',
            score: (skinPixels > 0 ? 15 : 0) + (avgSaturation > 30 ? 20 : 0),
            hasselblad: 'natural',
            params: { saturation: 12, contrast: 8, brightness: 5, warmth: 8, cyanMagenta: 0, sharpness: 25, tone: 8, softLight: 20, vignette: false, filter: '原图' }
          }
        ];
        
        // 排序获取Top 3
        scores.sort((a, b) => b.score - a.score);
        const topScene = scores[0];
        const topScenes = scores.slice(0, 3).map(s => ({
          name: s.name,
          confidence: Math.min(95, Math.max(60, Math.round(s.score + 50)))
        }));
        
        resolve({
          scene: topScene.name,
          confidence: topScenes[0].confidence,
          hasselbladStyle: topScene.hasselblad,
          description: `识别为${topScene.name}，推荐${topScene.hasselblad === 'natural' ? '哈苏自然' : topScene.hasselblad === 'portrait' ? '哈苏人像' : topScene.hasselblad === 'cinematic' ? '哈苏电影' : '哈苏复古'}风格`,
          suggestedParams: topScene.params,
          topScenes
        });
      } catch (e) {
        reject(e);
      }
    };
    img.onerror = reject;
    img.src = imageUrl;
  });
}

/**
 * 下载图片
 */
export function downloadImage(dataUrl: string, filename: string): void {
  const link = document.createElement('a');
  link.download = filename;
  link.href = dataUrl;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

/**
 * 多图合成拼图 - 真实实现
 */
export async function createMultiCollage(
  images: { url: string; x?: number; y?: number; scale?: number; rotation?: number }[],
  canvasWidth: number,
  canvasHeight: number,
  background: string = '#000000'
): Promise<string> {
  return new Promise(async (resolve, reject) => {
    try {
      const canvas = document.createElement('canvas');
      canvas.width = canvasWidth;
      canvas.height = canvasHeight;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        reject(new Error('Canvas not supported'));
        return;
      }
      
      ctx.fillStyle = background;
      ctx.fillRect(0, 0, canvasWidth, canvasHeight);
      
      for (const imgData of images) {
        const img = await loadImage(imgData.url);
        ctx.save();
        const x = imgData.x ?? 0;
        const y = imgData.y ?? 0;
        const scale = imgData.scale ?? 1;
        const rotation = imgData.rotation ?? 0;
        
        ctx.translate(x, y);
        ctx.rotate((rotation * Math.PI) / 180);
        ctx.scale(scale, scale);
        ctx.drawImage(img, 0, 0);
        ctx.restore();
      }
      
      resolve(canvas.toDataURL('image/jpeg', 0.92));
    } catch (e) {
      reject(e);
    }
  });
}