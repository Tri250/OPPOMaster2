import React, { useState, useRef, useCallback, useEffect } from 'react';
import { useAppStore } from '../../store/appStore';
import {
  ArrowLeft, Aperture, Palette, Sun, Moon, Contrast, Sparkles,
  Check, RefreshCw, Camera, User, Info, Download,
  Circle, Layers, Target, Eye, SlidersHorizontal, RotateCcw,
  MoveHorizontal, FileJson
} from 'lucide-react';
import ImageUploader from '../../components/ImageUploader';
import { applyImageAdjustments, downloadImage, ImageAdjustParams } from '../../utils/imageProcessor';

// ==================== 类型定义 ====================

/** 色彩曲线控制点 (x: 0-255 输入, y: 0-255 输出) */
interface CurvePoint { x: number; y: number; }

/** 色彩曲线数据，R/G/B 三个通道 */
interface ColorCurves {
  r: CurvePoint[];
  g: CurvePoint[];
  b: CurvePoint[];
}

/** HSL 微调参数 */
interface HSLAdjustment {
  hue: number;        // -180 ~ 180
  saturation: number; // -100 ~ 100
  lightness: number;  // -100 ~ 100
}

/** LUT 预设 */
interface LUTPreset {
  id: string;
  name: string;
  icon: React.ElementType;
  color: string;
  desc: string;
  /** 像素级颜色映射函数 */
  mapPixel: (r: number, g: number, b: number) => [number, number, number];
}

/** 所有调色参数（用于导出） */
interface AllColorParams {
  style: string;
  baseParams: ImageAdjustParams;
  curves: ColorCurves;
  hsl: HSLAdjustment;
  lut: string;
}

// ==================== 默认曲线（5个控制点，线性） ====================

const defaultCurvePoints: CurvePoint[] = [
  { x: 0, y: 0 },
  { x: 64, y: 64 },
  { x: 128, y: 128 },
  { x: 192, y: 192 },
  { x: 255, y: 255 },
];

const defaultCurves: ColorCurves = {
  r: [...defaultCurvePoints.map(p => ({ ...p }))],
  g: [...defaultCurvePoints.map(p => ({ ...p }))],
  b: [...defaultCurvePoints.map(p => ({ ...p }))],
};

const defaultHSL: HSLAdjustment = { hue: 0, saturation: 0, lightness: 0 };

// ==================== LUT 预设定义（像素级映射） ====================

const lutPresets: LUTPreset[] = [
  {
    id: 'warm-sun',
    name: '暖阳',
    icon: Sun,
    color: '#FF9800',
    desc: '温暖阳光色调',
    mapPixel: (r, g, b) => {
      // 暖阳：提升红/黄通道，降低蓝通道
      const nr = Math.min(255, r + 25 + (r / 255) * 15);
      const ng = Math.min(255, g + 12 + (g / 255) * 8);
      const nb = Math.max(0, b - 20 - (b / 255) * 15);
      return [nr, ng, nb];
    },
  },
  {
    id: 'cool-tone',
    name: '冷调',
    icon: Moon,
    color: '#2196F3',
    desc: '清冷蓝色调',
    mapPixel: (r, g, b) => {
      // 冷调：提升蓝通道，降低红/黄通道
      const nr = Math.max(0, r - 15 - (r / 255) * 10);
      const ng = Math.min(255, g + 5 + (g / 255) * 5);
      const nb = Math.min(255, b + 30 + (b / 255) * 20);
      return [nr, ng, nb];
    },
  },
  {
    id: 'film',
    name: '胶片',
    icon: Camera,
    color: '#8D6E63',
    desc: '经典胶片质感',
    mapPixel: (r, g, b) => {
      // 胶片：S型曲线 + 轻微褪色 + 偏暖
      const fade = 15; // 黑场提升
      const sCurve = (v: number) => {
        const normalized = (v - fade) / (255 - fade * 2);
        // S型曲线
        const curved = normalized < 0.5
          ? 2 * normalized * normalized
          : 1 - 2 * (1 - normalized) * (1 - normalized);
        return Math.max(0, Math.min(255, curved * (255 - fade * 2) + fade));
      };
      const nr = sCurve(r) + 8;
      const ng = sCurve(g) + 3;
      const nb = sCurve(b) - 5;
      return [Math.min(255, nr), Math.min(255, ng), Math.max(0, nb)];
    },
  },
  {
    id: 'bw',
    name: '黑白',
    icon: Contrast,
    color: '#9E9E9E',
    desc: '高对比黑白',
    mapPixel: (r, g, b) => {
      // 黑白：加权灰度 + 高对比
      const gray = r * 0.299 + g * 0.587 + b * 0.114;
      // 增加对比度
      const contrast = 1.3;
      const adjusted = ((gray / 255 - 0.5) * contrast + 0.5) * 255;
      const val = Math.max(0, Math.min(255, adjusted));
      return [val, val, val];
    },
  },
  {
    id: 'vintage',
    name: '复古',
    icon: Palette,
    color: '#795548',
    desc: '怀旧复古色调',
    mapPixel: (r, g, b) => {
      // 复古：降低饱和度 + 偏黄/棕 + 褪色
      const fade = 20;
      const gray = r * 0.299 + g * 0.587 + b * 0.114;
      // 降低饱和度
      const desatR = r * 0.6 + gray * 0.4;
      const desatG = g * 0.6 + gray * 0.4;
      const desatB = b * 0.6 + gray * 0.4;
      // 偏黄/棕
      const nr = Math.min(255, desatR + 20 + fade);
      const ng = Math.min(255, desatG + 8 + fade);
      const nb = Math.max(0, desatB - 15 + fade);
      return [nr, ng, nb];
    },
  },
  {
    id: 'vivid',
    name: '鲜艳',
    icon: Sparkles,
    color: '#E91E63',
    desc: '高饱和鲜艳',
    mapPixel: (r, g, b) => {
      // 鲜艳：大幅提升饱和度
      const gray = r * 0.299 + g * 0.587 + b * 0.114;
      const boost = 1.6;
      const nr = Math.min(255, Math.max(0, gray + (r - gray) * boost + 10));
      const ng = Math.min(255, Math.max(0, gray + (g - gray) * boost + 5));
      const nb = Math.min(255, Math.max(0, gray + (b - gray) * boost + 5));
      return [nr, ng, nb];
    },
  },
];

// ==================== 像素级处理函数 ====================

/** 三次样条插值：根据控制点生成256级查找表 */
function buildCurveLUT(points: CurvePoint[]): Uint8Array {
  const lut = new Uint8Array(256);
  const sorted = [...points].sort((a, b) => a.x - b.x);

  for (let i = 0; i < 256; i++) {
    // 找到i所在的区间
    let segIdx = 0;
    for (let j = 0; j < sorted.length - 1; j++) {
      if (i >= sorted[j].x && i <= sorted[j + 1].x) {
        segIdx = j;
        break;
      }
      if (j === sorted.length - 2) segIdx = j;
    }

    const p0 = sorted[Math.max(0, segIdx - 1)];
    const p1 = sorted[segIdx];
    const p2 = sorted[Math.min(sorted.length - 1, segIdx + 1)];
    const p3 = sorted[Math.min(sorted.length - 1, segIdx + 2)];

    const dx = p2.x - p1.x;
    const t = dx === 0 ? 0 : (i - p1.x) / dx;

    // Catmull-Rom 样条
    const t2 = t * t;
    const t3 = t2 * t;

    const val =
      0.5 * (
        (2 * p1.y) +
        (-p0.y + p2.y) * t +
        (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
        (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3
      );

    lut[i] = Math.max(0, Math.min(255, Math.round(val)));
  }
  return lut;
}

/** RGB -> HSL */
function rgbToHsl(r: number, g: number, b: number): [number, number, number] {
  r /= 255; g /= 255; b /= 255;
  const max = Math.max(r, g, b);
  const min = Math.min(r, g, b);
  const l = (max + min) / 2;
  let h = 0, s = 0;

  if (max !== min) {
    const d = max - min;
    s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
    switch (max) {
      case r: h = ((g - b) / d + (g < b ? 6 : 0)) / 6; break;
      case g: h = ((b - r) / d + 2) / 6; break;
      case b: h = ((r - g) / d + 4) / 6; break;
    }
  }
  return [h * 360, s * 100, l * 100];
}

/** HSL -> RGB */
function hslToRgb(h: number, s: number, l: number): [number, number, number] {
  h /= 360; s /= 100; l /= 100;
  let r: number, g: number, b: number;

  if (s === 0) {
    r = g = b = l;
  } else {
    const hue2rgb = (p: number, q: number, t: number) => {
      if (t < 0) t += 1;
      if (t > 1) t -= 1;
      if (t < 1 / 6) return p + (q - p) * 6 * t;
      if (t < 1 / 2) return q;
      if (t < 2 / 3) return p + (q - p) * (2 / 3 - t) * 6;
      return p;
    };
    const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
    const p = 2 * l - q;
    r = hue2rgb(p, q, h + 1 / 3);
    g = hue2rgb(p, q, h);
    b = hue2rgb(p, q, h - 1 / 3);
  }
  return [Math.round(r * 255), Math.round(g * 255), Math.round(b * 255)];
}

/** 应用色彩曲线到 Canvas（像素级处理） */
function applyCurvesToCanvas(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  curves: ColorCurves
): void {
  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;

  const rLUT = buildCurveLUT(curves.r);
  const gLUT = buildCurveLUT(curves.g);
  const bLUT = buildCurveLUT(curves.b);

  for (let i = 0; i < data.length; i += 4) {
    data[i] = rLUT[data[i]];
    data[i + 1] = gLUT[data[i + 1]];
    data[i + 2] = bLUT[data[i + 2]];
  }

  ctx.putImageData(imageData, 0, 0);
}

/** 应用HSL微调到 Canvas（像素级处理） */
function applyHSLToCanvas(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  hsl: HSLAdjustment
): void {
  if (hsl.hue === 0 && hsl.saturation === 0 && hsl.lightness === 0) return;

  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;

  for (let i = 0; i < data.length; i += 4) {
    const [h, s, l] = rgbToHsl(data[i], data[i + 1], data[i + 2]);
    const newH = ((h + hsl.hue) % 360 + 360) % 360;
    const newS = Math.max(0, Math.min(100, s + hsl.saturation));
    const newL = Math.max(0, Math.min(100, l + hsl.lightness));
    const [nr, ng, nb] = hslToRgb(newH, newS, newL);
    data[i] = nr;
    data[i + 1] = ng;
    data[i + 2] = nb;
  }

  ctx.putImageData(imageData, 0, 0);
}

/** 应用LUT预设到 Canvas（像素级颜色映射） */
function applyLUTToCanvas(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  lut: LUTPreset
): void {
  const imageData = ctx.getImageData(0, 0, width, height);
  const data = imageData.data;

  for (let i = 0; i < data.length; i += 4) {
    const [nr, ng, nb] = lut.mapPixel(data[i], data[i + 1], data[i + 2]);
    data[i] = nr;
    data[i + 1] = ng;
    data[i + 2] = nb;
  }

  ctx.putImageData(imageData, 0, 0);
}

/** 完整的图片处理流程：基础参数 → 曲线 → HSL → LUT */
async function processImageFull(
  imageUrl: string,
  baseParams: ImageAdjustParams,
  curves: ColorCurves,
  hsl: HSLAdjustment,
  lutId: string
): Promise<string> {
  // 第1步：应用基础参数（使用原有的 applyImageAdjustments）
  let currentImage = await applyImageAdjustments(imageUrl, baseParams);

  // 第2步：应用色彩曲线
  const curvesChanged = JSON.stringify(curves) !== JSON.stringify(defaultCurves);
  if (curvesChanged) {
    currentImage = await new Promise<string>((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) { reject(new Error('Canvas not supported')); return; }
        ctx.drawImage(img, 0, 0);
        applyCurvesToCanvas(ctx, canvas.width, canvas.height, curves);
        resolve(canvas.toDataURL('image/jpeg', 0.92));
      };
      img.onerror = reject;
      img.src = currentImage;
    });
  }

  // 第3步：应用HSL微调
  if (hsl.hue !== 0 || hsl.saturation !== 0 || hsl.lightness !== 0) {
    currentImage = await new Promise<string>((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = img.width;
        canvas.height = img.height;
        const ctx = canvas.getContext('2d', { willReadFrequently: true });
        if (!ctx) { reject(new Error('Canvas not supported')); return; }
        ctx.drawImage(img, 0, 0);
        applyHSLToCanvas(ctx, canvas.width, canvas.height, hsl);
        resolve(canvas.toDataURL('image/jpeg', 0.92));
      };
      img.onerror = reject;
      img.src = currentImage;
    });
  }

  // 第4步：应用LUT预设
  if (lutId && lutId !== 'none') {
    const lut = lutPresets.find(l => l.id === lutId);
    if (lut) {
      currentImage = await new Promise<string>((resolve, reject) => {
        const img = new Image();
        img.crossOrigin = 'anonymous';
        img.onload = () => {
          const canvas = document.createElement('canvas');
          canvas.width = img.width;
          canvas.height = img.height;
          const ctx = canvas.getContext('2d', { willReadFrequently: true });
          if (!ctx) { reject(new Error('Canvas not supported')); return; }
          ctx.drawImage(img, 0, 0);
          applyLUTToCanvas(ctx, canvas.width, canvas.height, lut);
          resolve(canvas.toDataURL('image/jpeg', 0.92));
        };
        img.onerror = reject;
        img.src = currentImage;
      });
    }
  }

  return currentImage;
}

// ==================== HNCS 3.0 哈苏自然色彩解决方案 - 5种风格预设 ====================

const hncsStyles = [
  {
    id: 'natural',
    name: 'HNCS 自然',
    fullName: 'Hasselblad Natural Color Solution',
    desc: '真实还原，细节丰富，自然色彩',
    color: '#4CAF50',
    icon: Sun,
    technical: '16-bit色深处理，宽色域覆盖，中性色调映射',
    params: {
      saturation: 12, contrast: 8, brightness: 5, warmth: 0,
      cyanMagenta: 0, sharpness: 18, tone: 8, softLight: 15,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400&h=300&fit=crop',
    characteristics: ['中性色调', '高动态范围', '细节保留', '真实色彩']
  },
  {
    id: 'portrait',
    name: 'HNCS 人像',
    fullName: 'Hasselblad Portrait Color',
    desc: '柔美肤色，自然光影，专业人像',
    color: '#E91E63',
    icon: User,
    technical: '肤色优化算法，柔和色调过渡，面部细节增强',
    params: {
      saturation: 8, contrast: 5, brightness: 8, warmth: 12,
      cyanMagenta: 0, sharpness: 12, tone: 5, softLight: 35,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=400&h=300&fit=crop',
    characteristics: ['肤色优化', '柔和光影', '面部增强', '自然过渡']
  },
  {
    id: 'cinematic',
    name: 'HNCS 电影',
    fullName: 'Hasselblad Cinematic Color',
    desc: '电影质感，氛围感强，叙事风格',
    color: '#FF9800',
    icon: Moon,
    technical: '电影级色调映射，暗部细节保留，高对比度处理',
    params: {
      saturation: 18, contrast: 22, brightness: 0, warmth: -5,
      cyanMagenta: 5, sharpness: 25, tone: 18, softLight: 20,
      vignette: true, filter: '胶片'
    },
    sampleImage: 'https://images.unsplash.com/photo-1519608487953-e999c86e7455?w=400&h=350&fit=crop',
    characteristics: ['电影色调', '暗部细节', '高对比度', '氛围营造']
  },
  {
    id: 'vintage',
    name: 'HNCS 复古',
    fullName: 'Hasselblad Vintage Color',
    desc: '经典胶片，怀旧质感， timeless风格',
    color: '#795548',
    icon: Palette,
    technical: '胶片颗粒模拟，复古色调映射，暖色偏移处理',
    params: {
      saturation: -5, contrast: 15, brightness: 0, warmth: 25,
      cyanMagenta: 0, sharpness: 15, tone: 20, softLight: 25,
      vignette: true, filter: '胶片'
    },
    sampleImage: 'https://images.unsplash.com/photo-1495616811223-4d98d6e944aa?w=400&h=300&fit=crop',
    characteristics: ['胶片质感', '复古色调', '暖色偏移', '经典风格']
  },
  {
    id: 'vivid',
    name: 'HNCS 鲜艳',
    fullName: 'Hasselblad Vivid Color',
    desc: '色彩鲜艳，视觉冲击，活力风格',
    color: '#FF5722',
    icon: Sparkles,
    technical: '高饱和度处理，色彩增强算法，动态范围扩展',
    params: {
      saturation: 30, contrast: 18, brightness: 5, warmth: 8,
      cyanMagenta: 0, sharpness: 22, tone: 12, softLight: 10,
      vignette: false, filter: '原图'
    },
    sampleImage: 'https://images.unsplash.com/photo-1490750967868-5aa43378c200?w=400&h=300&fit=crop',
    characteristics: ['高饱和度', '色彩增强', '视觉冲击', '活力风格']
  },
];

// HNCS技术参数说明
const hncsTechSpecs = [
  { name: '色深处理', value: '16-bit', desc: '专业级色深，保留更多细节' },
  { name: '色域覆盖', value: 'P3/Rec.2020', desc: '宽色域支持，色彩更丰富' },
  { name: '动态范围', value: '14EV', desc: '高动态范围，暗部细节清晰' },
  { name: '色调映射', value: 'HNCS 3.0', desc: '哈苏自然色调映射算法' },
  { name: '肤色优化', value: 'Skin Tone+', desc: '专业肤色处理算法' },
  { name: '降噪算法', value: 'Multi-scale', desc: '多尺度降噪，细节保留' },
];

// ==================== 曲线编辑器组件 ====================

interface CurveEditorProps {
  curves: ColorCurves;
  onChange: (curves: ColorCurves) => void;
}

const channelColors = { r: '#FF4444', g: '#44FF44', b: '#4488FF' };
const channelNames = { r: '红', g: '绿', b: '蓝' };

const CurveEditor: React.FC<CurveEditorProps> = ({ curves, onChange }) => {
  const [activeChannel, setActiveChannel] = useState<'r' | 'g' | 'b'>('r');
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const draggingIdx = useRef<number | null>(null);

  // 绘制曲线
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const w = canvas.width;
    const h = canvas.height;
    const padding = 0;

    ctx.clearRect(0, 0, w, h);

    // 背景
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(0, 0, w, h);

    // 网格
    ctx.strokeStyle = 'rgba(255,255,255,0.08)';
    ctx.lineWidth = 1;
    for (let i = 1; i < 4; i++) {
      ctx.beginPath();
      ctx.moveTo((w * i) / 4, padding);
      ctx.lineTo((w * i) / 4, h - padding);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(padding, (h * i) / 4);
      ctx.lineTo(w - padding, (h * i) / 4);
      ctx.stroke();
    }

    // 对角线（线性参考）
    ctx.strokeStyle = 'rgba(255,255,255,0.15)';
    ctx.setLineDash([4, 4]);
    ctx.beginPath();
    ctx.moveTo(padding, h - padding);
    ctx.lineTo(w - padding, padding);
    ctx.stroke();
    ctx.setLineDash([]);

    // 绘制非活动通道的曲线（半透明）
    for (const ch of ['r', 'g', 'b'] as const) {
      if (ch === activeChannel) continue;
      const points = curves[ch];
      const lut = buildCurveLUT(points);
      ctx.strokeStyle = channelColors[ch] + '40';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      for (let i = 0; i < 256; i++) {
        const x = padding + (i / 255) * (w - 2 * padding);
        const y = h - padding - (lut[i] / 255) * (h - 2 * padding);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.stroke();
    }

    // 绘制活动通道的曲线
    const points = curves[activeChannel];
    const lut = buildCurveLUT(points);
    ctx.strokeStyle = channelColors[activeChannel];
    ctx.lineWidth = 2;
    ctx.beginPath();
    for (let i = 0; i < 256; i++) {
      const x = padding + (i / 255) * (w - 2 * padding);
      const y = h - padding - (lut[i] / 255) * (h - 2 * padding);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // 绘制控制点
    points.forEach((pt) => {
      const x = padding + (pt.x / 255) * (w - 2 * padding);
      const y = h - padding - (pt.y / 255) * (h - 2 * padding);

      ctx.fillStyle = channelColors[activeChannel];
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, Math.PI * 2);
      ctx.fill();

      ctx.strokeStyle = '#fff';
      ctx.lineWidth = 1.5;
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, Math.PI * 2);
      ctx.stroke();
    });
  }, [curves, activeChannel]);

  // 拖拽控制点
  const handlePointerDown = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const mx = (e.clientX - rect.left) * scaleX;
    const my = (e.clientY - rect.top) * scaleY;
    const w = canvas.width;
    const h = canvas.height;

    const points = curves[activeChannel];
    for (let i = 0; i < points.length; i++) {
      const px = (points[i].x / 255) * w;
      const py = h - (points[i].y / 255) * h;
      if (Math.hypot(mx - px, my - py) < 12) {
        draggingIdx.current = i;
        canvas.setPointerCapture(e.pointerId);
        return;
      }
    }
  }, [curves, activeChannel]);

  const handlePointerMove = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (draggingIdx.current === null) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const mx = (e.clientX - rect.left) * scaleX;
    const my = (e.clientY - rect.top) * scaleY;
    const w = canvas.width;
    const h = canvas.height;

    const idx = draggingIdx.current;
    const newPoints = curves[activeChannel].map((pt, i) => {
      if (i !== idx) return { ...pt };
      // 第一个和最后一个点只能上下移动
      if (i === 0) return { x: 0, y: Math.max(0, Math.min(255, Math.round((1 - my / h) * 255))) };
      if (i === curves[activeChannel].length - 1) return { x: 255, y: Math.max(0, Math.min(255, Math.round((1 - my / h) * 255))) };
      return {
        x: Math.max(0, Math.min(255, Math.round((mx / w) * 255))),
        y: Math.max(0, Math.min(255, Math.round((1 - my / h) * 255))),
      };
    });

    onChange({ ...curves, [activeChannel]: newPoints });
  }, [curves, activeChannel, onChange]);

  const handlePointerUp = useCallback(() => {
    draggingIdx.current = null;
  }, []);

  return (
    <div className="space-y-3">
      {/* 通道选择 */}
      <div className="flex gap-2">
        {(['r', 'g', 'b'] as const).map((ch) => (
          <button
            key={ch}
            onClick={() => setActiveChannel(ch)}
            className={`flex-1 py-1.5 rounded-lg text-xs font-medium transition-all ${
              activeChannel === ch ? 'text-white' : 'text-white/50 bg-white/5'
            }`}
            style={activeChannel === ch ? { backgroundColor: channelColors[ch] + '40', color: channelColors[ch] } : {}}
          >
            {channelNames[ch]}通道
          </button>
        ))}
      </div>

      {/* 曲线画布 */}
      <canvas
        ref={canvasRef}
        width={256}
        height={256}
        className="w-full aspect-square rounded-xl cursor-crosshair touch-none"
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
      />

      {/* 重置按钮 */}
      <button
        onClick={() => onChange({
          r: defaultCurvePoints.map(p => ({ ...p })),
          g: defaultCurvePoints.map(p => ({ ...p })),
          b: defaultCurvePoints.map(p => ({ ...p })),
        })}
        className="w-full py-2 rounded-lg bg-white/5 text-white/50 text-xs flex items-center justify-center gap-1 hover:bg-white/10 transition-colors"
      >
        <RotateCcw size={12} />
        重置曲线
      </button>
    </div>
  );
};

// ==================== 对比滑块组件 ====================

interface ComparisonSliderProps {
  beforeSrc: string;
  afterSrc: string;
}

const ComparisonSlider: React.FC<ComparisonSliderProps> = ({ beforeSrc, afterSrc }) => {
  const [position, setPosition] = useState(50);
  const containerRef = useRef<HTMLDivElement>(null);
  const isDragging = useRef(false);

  const handleMove = useCallback((clientX: number) => {
    const container = containerRef.current;
    if (!container) return;
    const rect = container.getBoundingClientRect();
    const x = clientX - rect.left;
    const pct = Math.max(0, Math.min(100, (x / rect.width) * 100));
    setPosition(pct);
  }, []);

  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    isDragging.current = true;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    handleMove(e.clientX);
  }, [handleMove]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!isDragging.current) return;
    handleMove(e.clientX);
  }, [handleMove]);

  const handlePointerUp = useCallback(() => {
    isDragging.current = false;
  }, []);

  return (
    <div
      ref={containerRef}
      className="relative w-full aspect-video rounded-xl overflow-hidden cursor-col-resize select-none touch-none"
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
    >
      {/* 处理后图片（底层） */}
      <img src={afterSrc} alt="处理后" className="absolute inset-0 w-full h-full object-cover" />

      {/* 原图（裁切覆盖） */}
      <div
        className="absolute inset-0 overflow-hidden"
        style={{ width: `${position}%` }}
      >
        <img
          src={beforeSrc}
          alt="原图"
          className="absolute inset-0 w-full h-full object-cover"
          style={{ width: `${containerRef.current ? (containerRef.current.offsetWidth / (position / 100)) : 100}%`, maxWidth: 'none' }}
        />
      </div>

      {/* 分割线 */}
      <div
        className="absolute top-0 bottom-0 w-0.5 bg-white shadow-lg"
        style={{ left: `${position}%` }}
      >
        {/* 拖动手柄 */}
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-8 h-8 rounded-full bg-white shadow-lg flex items-center justify-center">
          <MoveHorizontal size={16} className="text-gray-800" />
        </div>
      </div>

      {/* 标签 */}
      <div className="absolute top-2 left-2 px-2 py-1 rounded bg-black/60 text-white text-xs">原图</div>
      <div className="absolute top-2 right-2 px-2 py-1 rounded bg-[#FF6B35]/80 text-white text-xs">处理后</div>
    </div>
  );
};

// ==================== 主组件 ====================

const HasselbladColorPage: React.FC = () => {
  const { goBack, setAiParam } = useAppStore();
  const [uploadedImage, setUploadedImage] = useState<string>('');
  const [selectedStyle, setSelectedStyle] = useState<string>('natural');
  const [processedImage, setProcessedImage] = useState<string>('');
  const [isProcessing, setIsProcessing] = useState(false);
  const [showTechSpecs, setShowTechSpecs] = useState(false);
  const [showComparison, setShowComparison] = useState(false);

  // 新增状态
  const [activeTab, setActiveTab] = useState<'style' | 'curves' | 'hsl' | 'lut'>('style');
  const [curves, setCurves] = useState<ColorCurves>(JSON.parse(JSON.stringify(defaultCurves)));
  const [hsl, setHsl] = useState<HSLAdjustment>({ ...defaultHSL });
  const [selectedLUT, setSelectedLUT] = useState<string>('none');

  // 应用完整处理流程
  const applyFullProcess = useCallback(async () => {
    if (!uploadedImage) return;
    setIsProcessing(true);
    try {
      const style = hncsStyles.find(s => s.id === selectedStyle);
      const baseParams = style?.params ?? hncsStyles[0].params;
      const result = await processImageFull(uploadedImage, baseParams, curves, hsl, selectedLUT);
      setProcessedImage(result);

      // 同步参数到全局状态
      setAiParam('saturation', baseParams.saturation);
      setAiParam('contrast', baseParams.contrast);
      setAiParam('warmth', baseParams.warmth);
      setAiParam('sharpness', baseParams.sharpness);
    } catch (e) {
      console.error('处理失败:', e);
    } finally {
      setIsProcessing(false);
    }
  }, [uploadedImage, selectedStyle, curves, hsl, selectedLUT, setAiParam]);

  // 参数变化时自动重新处理
  useEffect(() => {
    if (uploadedImage) {
      applyFullProcess();
    }
  }, [uploadedImage, selectedStyle, curves, hsl, selectedLUT, applyFullProcess]);

  // 应用HNCS风格
  const applyHncsStyle = (styleId: string) => {
    setSelectedStyle(styleId);
  };

  // 导出参数为JSON
  const exportParams = () => {
    const style = hncsStyles.find(s => s.id === selectedStyle);
    const allParams: AllColorParams = {
      style: selectedStyle,
      baseParams: style?.params ?? hncsStyles[0].params,
      curves,
      hsl,
      lut: selectedLUT,
    };
    const json = JSON.stringify(allParams, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.download = `OMaster_HNCS_Params_${Date.now()}.json`;
    link.href = url;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  // 重置所有参数
  const resetAll = () => {
    setCurves(JSON.parse(JSON.stringify(defaultCurves)));
    setHsl({ ...defaultHSL });
    setSelectedLUT('none');
    setSelectedStyle('natural');
  };

  const currentStyle = hncsStyles.find(s => s.id === selectedStyle);

  return (
    <div className="h-full flex flex-col bg-[#0a0a0a]">
      {/* Header */}
      <div className="px-4 py-3 flex items-center justify-between bg-gradient-to-b from-[#1a1a1a] to-transparent">
        <button onClick={goBack} className="p-2 rounded-full bg-white/10">
          <ArrowLeft size={20} className="text-white" />
        </button>
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-full bg-[#FF6B35]/20 flex items-center justify-center">
            <Aperture size={16} className="text-[#FF6B35]" />
          </div>
          <span className="text-white font-bold">哈苏色彩科学</span>
        </div>
        <button
          onClick={() => setShowTechSpecs(true)}
          className="p-2 rounded-full bg-white/10"
        >
          <Info size={18} className="text-white/70" />
        </button>
      </div>

      {/* HNCS介绍 */}
      <div className="px-4 py-2">
        <div className="p-3 rounded-2xl bg-gradient-to-r from-[#FF6B35]/20 to-[#FF8C42]/10 border border-[#FF6B35]/30">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 rounded-lg bg-[#FF6B35]/30 flex items-center justify-center">
              <Layers size={16} className="text-[#FF6B35]" />
            </div>
            <div>
              <p className="text-[#FF6B35] text-xs font-bold">HNCS 3.0 · 专业色彩调节</p>
              <p className="text-white/50 text-xs">色彩曲线 · HSL微调 · LUT预设 · 实时对比</p>
            </div>
          </div>
        </div>
      </div>

      {/* 图片上传 */}
      <div className="px-4 pb-2">
        <ImageUploader
          onImageSelect={setUploadedImage}
          currentImage={uploadedImage}
          title="上传照片体验HNCS"
          description="选择照片应用哈苏色彩科学"
        />
      </div>

      {/* 功能Tab切换 */}
      <div className="px-4 pb-2">
        <div className="flex gap-1 p-1 rounded-xl bg-white/5">
          {([
            { id: 'style' as const, label: '风格', icon: Palette },
            { id: 'curves' as const, label: '曲线', icon: SlidersHorizontal },
            { id: 'hsl' as const, label: 'HSL', icon: Circle },
            { id: 'lut' as const, label: 'LUT', icon: Layers },
          ]).map((tab) => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`flex-1 py-2 rounded-lg text-xs font-medium flex items-center justify-center gap-1 transition-all ${
                  activeTab === tab.id
                    ? 'bg-[#FF6B35] text-white'
                    : 'text-white/50 hover:text-white/70'
                }`}
              >
                <Icon size={12} />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Tab内容区域 - 可滚动 */}
      <div className="flex-1 overflow-y-auto px-4 pb-4">
        {/* ===== 风格选择 ===== */}
        {activeTab === 'style' && (
          <div className="space-y-3">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <Palette size={12} />
              5种HNCS色彩风格
            </p>
            <div className="grid grid-cols-5 gap-2">
              {hncsStyles.map((style) => {
                const Icon = style.icon;
                const isSelected = selectedStyle === style.id;
                return (
                  <button
                    key={style.id}
                    onClick={() => applyHncsStyle(style.id)}
                    disabled={!uploadedImage || isProcessing}
                    className={`p-3 rounded-xl transition-all ${
                      isSelected ? 'bg-[#FF6B35]/30 border border-[#FF6B35]' :
                      'bg-white/5 hover:bg-white/10'
                    } ${!uploadedImage ? 'opacity-50' : ''}`}
                  >
                    <div className="flex flex-col items-center gap-1">
                      <div className="w-8 h-8 rounded-lg flex items-center justify-center"
                        style={{ backgroundColor: `${style.color}20` }}>
                        <Icon size={16} style={{ color: style.color }} />
                      </div>
                      <span className={`text-xs ${isSelected ? 'text-[#FF6B35]' : 'text-white/70'}`}>
                        {style.name.replace('HNCS ', '')}
                      </span>
                    </div>
                  </button>
                );
              })}
            </div>

            {/* 当前风格详情 */}
            {currentStyle && (
              <div className="p-4 rounded-2xl bg-white/5">
                <div className="flex items-center gap-3 mb-2">
                  <div className="w-10 h-10 rounded-xl flex items-center justify-center"
                    style={{ backgroundColor: `${currentStyle.color}20` }}>
                    {React.createElement(currentStyle.icon, { size: 20, style: { color: currentStyle.color } })}
                  </div>
                  <div>
                    <p className="text-white font-bold text-sm">{currentStyle.name}</p>
                    <p className="text-white/40 text-xs">{currentStyle.technical}</p>
                  </div>
                </div>
                <div className="flex gap-2 flex-wrap">
                  {currentStyle.characteristics.map((char, i) => (
                    <span key={i} className="px-2 py-0.5 rounded-full bg-[#FF6B35]/10 text-[#FF6B35] text-xs">
                      {char}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* 样张展示 */}
            <p className="text-white/50 text-xs flex items-center gap-2">
              <Target size={12} />
              HNCS大师样张
            </p>
            <div className="grid grid-cols-2 gap-2">
              {hncsStyles.map((style) => (
                <div key={style.id} className="rounded-xl overflow-hidden">
                  <img src={style.sampleImage} alt={style.name} className="w-full aspect-video object-cover" />
                  <div className="p-1.5 bg-white/5">
                    <div className="flex items-center gap-1.5">
                      <div className="w-5 h-5 rounded flex items-center justify-center"
                        style={{ backgroundColor: `${style.color}20` }}>
                        {React.createElement(style.icon, { size: 10, style: { color: style.color } })}
                      </div>
                      <span className="text-white text-xs">{style.name.replace('HNCS ', '')}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* ===== 色彩曲线 ===== */}
        {activeTab === 'curves' && (
          <div className="space-y-3">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <SlidersHorizontal size={12} />
              色彩曲线调节 · 拖动控制点调整曲线
            </p>
            <div className="p-3 rounded-2xl bg-white/5">
              <CurveEditor curves={curves} onChange={setCurves} />
            </div>
            {/* 曲线数值显示 */}
            <div className="grid grid-cols-3 gap-2">
              {(['r', 'g', 'b'] as const).map((ch) => {
                const channelColors = { r: '#FF4444', g: '#44FF44', b: '#4488FF' };
                const channelNames = { r: '红', g: '绿', b: '蓝' };
                return (
                  <div key={ch} className="p-2 rounded-lg bg-white/5 text-center">
                    <p className="text-xs" style={{ color: channelColors[ch] }}>{channelNames[ch]}通道</p>
                    <p className="text-white/40 text-xs mt-1">
                      {curves[ch].map(p => p.y).join(' → ')}
                    </p>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* ===== HSL微调 ===== */}
        {activeTab === 'hsl' && (
          <div className="space-y-4">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <Circle size={12} />
              HSL色轮微调 · 实时调整色相/饱和度/明度
            </p>

            {/* 色相 */}
            <div className="p-3 rounded-2xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white/70 text-xs font-medium">色相 (Hue)</span>
                <span className="text-white text-xs">{hsl.hue > 0 ? '+' : ''}{hsl.hue}°</span>
              </div>
              <input
                type="range"
                min={-180}
                max={180}
                value={hsl.hue}
                onChange={(e) => setHsl({ ...hsl, hue: parseInt(e.target.value) })}
                className="w-full h-2 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, #ff0000, #ffff00, #00ff00, #00ffff, #0000ff, #ff00ff, #ff0000)`,
                }}
              />
              <div className="flex justify-between text-white/30 text-xs mt-1">
                <span>-180°</span><span>0°</span><span>+180°</span>
              </div>
            </div>

            {/* 饱和度 */}
            <div className="p-3 rounded-2xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white/70 text-xs font-medium">饱和度 (Saturation)</span>
                <span className="text-white text-xs">{hsl.saturation > 0 ? '+' : ''}{hsl.saturation}</span>
              </div>
              <input
                type="range"
                min={-100}
                max={100}
                value={hsl.saturation}
                onChange={(e) => setHsl({ ...hsl, saturation: parseInt(e.target.value) })}
                className="w-full h-2 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, #808080, #FF6B35)`,
                }}
              />
              <div className="flex justify-between text-white/30 text-xs mt-1">
                <span>-100</span><span>0</span><span>+100</span>
              </div>
            </div>

            {/* 明度 */}
            <div className="p-3 rounded-2xl bg-white/5">
              <div className="flex items-center justify-between mb-2">
                <span className="text-white/70 text-xs font-medium">明度 (Lightness)</span>
                <span className="text-white text-xs">{hsl.lightness > 0 ? '+' : ''}{hsl.lightness}</span>
              </div>
              <input
                type="range"
                min={-100}
                max={100}
                value={hsl.lightness}
                onChange={(e) => setHsl({ ...hsl, lightness: parseInt(e.target.value) })}
                className="w-full h-2 rounded-full appearance-none cursor-pointer"
                style={{
                  background: `linear-gradient(to right, #000000, #808080, #ffffff)`,
                }}
              />
              <div className="flex justify-between text-white/30 text-xs mt-1">
                <span>-100</span><span>0</span><span>+100</span>
              </div>
            </div>

            {/* HSL重置 */}
            <button
              onClick={() => setHsl({ ...defaultHSL })}
              className="w-full py-2 rounded-lg bg-white/5 text-white/50 text-xs flex items-center justify-center gap-1 hover:bg-white/10 transition-colors"
            >
              <RotateCcw size={12} />
              重置HSL
            </button>
          </div>
        )}

        {/* ===== LUT预设 ===== */}
        {activeTab === 'lut' && (
          <div className="space-y-3">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <Layers size={12} />
              LUT色彩预设 · 像素级颜色映射
            </p>

            {/* 无LUT选项 */}
            <button
              onClick={() => setSelectedLUT('none')}
              className={`w-full p-3 rounded-xl transition-all flex items-center gap-3 ${
                selectedLUT === 'none' ? 'bg-white/10 border border-white/30' : 'bg-white/5'
              }`}
            >
              <div className="w-10 h-10 rounded-lg bg-white/10 flex items-center justify-center">
                <RotateCcw size={18} className="text-white/50" />
              </div>
              <div className="text-left">
                <p className={`text-sm font-medium ${selectedLUT === 'none' ? 'text-white' : 'text-white/70'}`}>无LUT</p>
                <p className="text-white/40 text-xs">不应用LUT预设</p>
              </div>
              {selectedLUT === 'none' && <Check size={16} className="text-[#FF6B35] ml-auto" />}
            </button>

            {/* LUT预设列表 */}
            <div className="space-y-2">
              {lutPresets.map((lut) => {
                const Icon = lut.icon;
                const isSelected = selectedLUT === lut.id;
                return (
                  <button
                    key={lut.id}
                    onClick={() => setSelectedLUT(lut.id)}
                    className={`w-full p-3 rounded-xl transition-all flex items-center gap-3 ${
                      isSelected ? 'bg-white/10 border' : 'bg-white/5'
                    }`}
                    style={isSelected ? { borderColor: lut.color + '60' } : {}}
                  >
                    <div className="w-10 h-10 rounded-lg flex items-center justify-center"
                      style={{ backgroundColor: lut.color + '20' }}>
                      <Icon size={18} style={{ color: lut.color }} />
                    </div>
                    <div className="text-left">
                      <p className={`text-sm font-medium ${isSelected ? 'text-white' : 'text-white/70'}`}>{lut.name}</p>
                      <p className="text-white/40 text-xs">{lut.desc}</p>
                    </div>
                    {isSelected && <Check size={16} className="ml-auto" style={{ color: lut.color }} />}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* ===== 处理结果 ===== */}
        {processedImage && (
          <div className="mt-4 space-y-3">
            <p className="text-white/50 text-xs flex items-center gap-2">
              <Eye size={12} />
              处理结果
            </p>

            {/* 对比模式切换 */}
            <div className="flex gap-2">
              <button
                onClick={() => setShowComparison(false)}
                className={`flex-1 py-2 rounded-xl text-xs ${
                  !showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/70'
                }`}
              >
                单图预览
              </button>
              <button
                onClick={() => setShowComparison(true)}
                className={`flex-1 py-2 rounded-xl text-xs ${
                  showComparison ? 'bg-[#FF6B35] text-white' : 'bg-white/10 text-white/70'
                }`}
              >
                对比预览
              </button>
            </div>

            {showComparison ? (
              <ComparisonSlider beforeSrc={uploadedImage} afterSrc={processedImage} />
            ) : (
              <div className="rounded-2xl overflow-hidden relative">
                <img src={processedImage} alt="处理后" className="w-full aspect-video object-cover" />
                <div className="absolute bottom-2 left-2 px-3 py-1.5 rounded-lg bg-[#FF6B35]/80 backdrop-blur-sm">
                  <span className="text-white text-xs font-medium">{currentStyle?.name}</span>
                </div>
              </div>
            )}

            {/* 操作按钮 */}
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => downloadImage(processedImage, `OMaster_HNCS_${currentStyle?.id}_${Date.now()}.jpg`)}
                className="py-3 rounded-xl bg-gradient-to-r from-[#FF6B35] to-[#FF8C42] flex items-center justify-center gap-2 text-white font-medium text-sm"
              >
                <Download size={16} />
                保存出片
              </button>
              <button
                onClick={exportParams}
                className="py-3 rounded-xl bg-white/10 flex items-center justify-center gap-2 text-white font-medium text-sm hover:bg-white/15 transition-colors"
              >
                <FileJson size={16} />
                导出参数
              </button>
            </div>

            {/* 重置所有参数 */}
            <button
              onClick={resetAll}
              className="w-full py-2 rounded-xl bg-white/5 text-white/40 text-xs flex items-center justify-center gap-1 hover:bg-white/10 transition-colors"
            >
              <RefreshCw size={12} />
              重置所有参数
            </button>
          </div>
        )}
      </div>

      {/* 技术规格弹窗 */}
      {showTechSpecs && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm flex items-end">
          <div className="w-full bg-[#1a1a1a] rounded-t-3xl p-6 animate-slide-up max-h-[80vh] overflow-y-auto">
            <button
              onClick={() => setShowTechSpecs(false)}
              className="absolute top-4 right-4 p-2 rounded-full bg-white/10"
            >
              <ArrowLeft size={20} className="text-white" />
            </button>

            <div className="mb-4">
              <div className="flex items-center gap-3 mb-4">
                <div className="w-12 h-12 rounded-xl bg-[#FF6B35]/20 flex items-center justify-center">
                  <Aperture size={24} className="text-[#FF6B35]" />
                </div>
                <div>
                  <p className="text-white font-bold">HNCS 3.0 技术规格</p>
                  <p className="text-white/50 text-sm">哈苏自然色彩解决方案</p>
                </div>
              </div>
            </div>

            {/* 技术参数列表 */}
            <div className="space-y-3 mb-6">
              {hncsTechSpecs.map((spec, i) => (
                <div key={i} className="p-4 rounded-xl bg-white/5">
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-white/70 text-sm">{spec.name}</span>
                    <span className="text-[#FF6B35] text-sm font-bold">{spec.value}</span>
                  </div>
                  <p className="text-white/40 text-xs">{spec.desc}</p>
                </div>
              ))}
            </div>

            {/* 哈苏历史 */}
            <div className="p-4 rounded-xl bg-[#FF6B35]/10 border border-[#FF6B35]/30">
              <p className="text-[#FF6B35] text-sm font-bold mb-2">哈苏80年影像传承</p>
              <p className="text-white/60 text-sm leading-relaxed">
                1941年创立于瑞典哥德堡，哈苏相机曾伴随NASA登月任务，
                记录人类首次踏上月球的历史瞬间。HNCS色彩科学源自数十年
                专业中画幅相机研发经验，为OPPO Find系列注入大师级影像基因。
              </p>
            </div>

            <button
              onClick={() => setShowTechSpecs(false)}
              className="w-full mt-4 py-3 rounded-xl bg-white/10 text-white font-medium"
            >
              了解更多
            </button>
          </div>
        </div>
      )}

      {/* 处理中动画 */}
      {isProcessing && (
        <div className="absolute inset-0 z-30 bg-black/60 flex items-center justify-center">
          <div className="text-center">
            <div className="w-16 h-16 rounded-full bg-[#FF6B35]/20 flex items-center justify-center mb-4 animate-pulse">
              <Aperture size={32} className="text-[#FF6B35] animate-spin" style={{ animationDuration: '2s' }} />
            </div>
            <p className="text-white text-sm">HNCS处理中...</p>
            <p className="text-white/50 text-xs mt-1">16-bit色深 · 宽色域处理 · 像素级调色</p>
          </div>
        </div>
      )}
    </div>
  );
};

export default HasselbladColorPage;
