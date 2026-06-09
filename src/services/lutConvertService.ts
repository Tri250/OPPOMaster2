/**
 * LUT转换服务 (Web端)
 * 对应Android端 LUTToParamsConverter 和 PresetToLUTExporter
 * 
 * 功能：
 * 1. 从LUT文件反推近似参数
 * 2. 将预设参数导出为LUT文件
 */

import { LUTParams } from '../models/MasterLUT';

// ===== LUT数据结构 =====

interface LUTData {
  size: number;
  data: number[][]; // [r, g, b]
}

// ===== LUT解析 =====

/**
 * 解析CUBE文件内容
 */
export function parseCUBEFile(content: string): LUTData {
  const lines = content.split('\n');
  let size = 33;
  const data: number[][] = [];

  for (const line of lines) {
    const trimmed = line.trim();
    
    if (trimmed.startsWith('LUT_3D_SIZE')) {
      const match = trimmed.match(/LUT_3D_SIZE\s+(\d+)/);
      if (match) {
        size = parseInt(match[1], 10);
      }
    } else if (/^[\d.]+\s+[\d.]+\s+[\d.]+/.test(trimmed)) {
      const values = trimmed.split(/\s+/).map(parseFloat);
      if (values.length >= 3) {
        data.push([values[0], values[1], values[2]]);
      }
    }
  }

  return { size, data };
}

/**
 * 从LUT数据采样指定RGB值
 */
export function sampleLUT(lutData: LUTData, r: number, g: number, b: number): number[] {
  const { size, data } = lutData;
  
  // 将RGB映射到LUT网格索引
  const ri = Math.min(Math.floor(r * (size - 1)), size - 1);
  const gi = Math.min(Math.floor(g * (size - 1)), size - 1);
  const bi = Math.min(Math.floor(b * (size - 1)), size - 1);
  
  const index = ri + gi * size + bi * size * size;
  
  if (index < data.length) {
    return data[index];
  }
  
  return [r, g, b];
}

// ===== LUT → 参数反推 =====

/**
 * 从LUT文件反推近似参数
 * 策略：采样关键色彩点（肤色/天空/草地/中性灰），
 *       计算RGB偏移量，映射到预设参数空间
 */
export function approximateParams(lutFileContent: string): LUTParams {
  const lutData = parseCUBEFile(lutFileContent);
  
  // 采样关键色彩点
  const skinSample = sampleLUT(lutData, 0.7, 0.5, 0.4);    // 典型肤色
  const skySample = sampleLUT(lutData, 0.3, 0.5, 0.7);     // 天空蓝
  const neutralSample = sampleLUT(lutData, 0.5, 0.5, 0.5); // 中性灰
  
  return {
    saturation: calculateSaturationShift(neutralSample),
    contrast: calculateContrastShift(lutData),
    brightness: calculateBrightnessShift(neutralSample),
    colorTemperature: calculateTempShift(skinSample),
    tint: calculateTintShift(skinSample),
    highlightRolloff: calculateHighlightRolloff(lutData),
    shadowLift: calculateShadowLift(lutData),
    skinProtection: evaluateSkinProtection(skinSample),
  };
}

/**
 * 计算饱和度偏移
 */
function calculateSaturationShift(sample: number[]): number {
  const [r, g, b] = sample;
  
  // 计算输入饱和度（中性灰应为0）
  const inputSat = Math.sqrt(
    Math.pow(r - 0.5, 2) + 
    Math.pow(g - 0.5, 2) + 
    Math.pow(b - 0.5, 2)
  );
  
  // 输入是中性灰，输出偏离中性灰说明饱和度变化
  return Math.max(-1, Math.min(1, inputSat - 0));
}

/**
 * 计算对比度偏移
 */
function calculateContrastShift(lutData: LUTData): number {
  // 采样最亮和最暗点
  const brightest = sampleLUT(lutData, 1, 1, 1);
  const darkest = sampleLUT(lutData, 0, 0, 0);
  
  const brightLum = (brightest[0] + brightest[1] + brightest[2]) / 3;
  const darkLum = (darkest[0] + darkest[1] + darkest[2]) / 3;
  
  // 对比度 = 亮部亮度 - 暗部亮度
  const contrast = brightLum - darkLum;
  
  // 标准对比度约1，偏离表示对比度变化
  return Math.max(-1, Math.min(1, contrast - 1));
}

/**
 * 计算亮度偏移
 */
function calculateBrightnessShift(sample: number[]): number {
  const lum = (sample[0] + sample[1] + sample[2]) / 3;
  return Math.max(-1, Math.min(1, lum - 0.5));
}

/**
 * 计算色温偏移
 */
function calculateTempShift(sample: number[]): number {
  // 肤色偏暖表示正色温偏移
  const [r, b] = sample;
  return Math.max(-1, Math.min(1, r - b));
}

/**
 * 计算色调偏移
 */
function calculateTintShift(sample: number[]): number {
  const [g, r] = sample;
  return Math.max(-1, Math.min(1, g - r));
}

/**
 * 计算高光衰减
 */
function calculateHighlightRolloff(lutData: LUTData): number {
  const bright = sampleLUT(lutData, 0.9, 0.9, 0.9);
  const expected = 0.9;
  
  const diff = (bright[0] + bright[1] + bright[2]) / 3 - expected;
  return Math.max(-1, Math.min(1, diff));
}

/**
 * 计算阴影提升
 */
function calculateShadowLift(lutData: LUTData): number {
  const dark = sampleLUT(lutData, 0.1, 0.1, 0.1);
  const expected = 0.1;
  
  const diff = (dark[0] + dark[1] + dark[2]) / 3 - expected;
  return Math.max(-1, Math.min(1, diff));
}

/**
 * 评估肤色保护
 */
function evaluateSkinProtection(sample: number[]): boolean {
  // 肤色偏离较小表示保护开启
  const [r, g, b] = sample;
  
  const deviation = Math.sqrt(
    Math.pow(r - 0.7, 2) + 
    Math.pow(g - 0.5, 2) + 
    Math.pow(b - 0.4, 2)
  );
  
  return deviation < 0.1;
}

// ===== 参数 → LUT导出 =====

/**
 * 应用参数变换到RGB值
 */
function applyParams(r: number, g: number, b: number, params: LUTParams): number[] {
  let outR = r;
  let outG = g;
  let outB = b;
  
  // 1. 亮度调整
  const brightness = params.brightness;
  outR = Math.max(0, Math.min(1, outR + brightness));
  outG = Math.max(0, Math.min(1, outG + brightness));
  outB = Math.max(0, Math.min(1, outB + brightness));
  
  // 2. 对比度调整
  const contrast = params.contrast + 1;
  outR = Math.max(0, Math.min(1, (outR - 0.5) * contrast + 0.5));
  outG = Math.max(0, Math.min(1, (outG - 0.5) * contrast + 0.5));
  outB = Math.max(0, Math.min(1, (outB - 0.5) * contrast + 0.5));
  
  // 3. 饱和度调整
  const saturation = params.saturation + 1;
  const gray = (outR + outG + outB) / 3;
  outR = Math.max(0, Math.min(1, gray + (outR - gray) * saturation));
  outG = Math.max(0, Math.min(1, gray + (outG - gray) * saturation));
  outB = Math.max(0, Math.min(1, gray + (outB - gray) * saturation));
  
  // 4. 色温调整
  const tempShift = params.colorTemperature;
  if (tempShift > 0) {
    // 暖调：增加红色，减少蓝色
    outR = Math.max(0, Math.min(1, outR + tempShift * 0.1));
    outB = Math.max(0, Math.min(1, outB - tempShift * 0.1));
  } else {
    // 冷调：减少红色，增加蓝色
    outR = Math.max(0, Math.min(1, outR + tempShift * 0.1));
    outB = Math.max(0, Math.min(1, outB - tempShift * 0.1));
  }
  
  // 5. 色调调整
  const tintShift = params.tint;
  outG = Math.max(0, Math.min(1, outG + tintShift * 0.1));
  
  // 6. 高光衰减
  const highlightRolloff = params.highlightRolloff;
  if (highlightRolloff > 0) {
    if (outR > 0.8) {
      const rolloff = 1 - highlightRolloff * (outR - 0.8) / 0.2;
      outR = outR * Math.max(0, Math.min(1, rolloff));
    }
    if (outG > 0.8) {
      const rolloff = 1 - highlightRolloff * (outG - 0.8) / 0.2;
      outG = outG * Math.max(0, Math.min(1, rolloff));
    }
    if (outB > 0.8) {
      const rolloff = 1 - highlightRolloff * (outB - 0.8) / 0.2;
      outB = outB * Math.max(0, Math.min(1, rolloff));
    }
  }
  
  // 7. 阴影提升
  const shadowLift = params.shadowLift;
  if (shadowLift > 0) {
    if (outR < 0.2) {
      outR = Math.max(0, Math.min(1, outR + shadowLift * (0.2 - outR) / 0.2));
    }
    if (outG < 0.2) {
      outG = Math.max(0, Math.min(1, outG + shadowLift * (0.2 - outG) / 0.2));
    }
    if (outB < 0.2) {
      outB = Math.max(0, Math.min(1, outB + shadowLift * (0.2 - outB) / 0.2));
    }
  }
  
  // 8. 肤色保护（简化实现）
  if (params.skinProtection) {
    // 检测是否接近肤色范围
    const isSkinTone = outR > 0.5 && outR < 0.9 &&
                      outG > 0.3 && outG < 0.7 &&
                      outB > 0.2 && outB < 0.5 &&
                      outR > outG && outG > outB;
    
    if (isSkinTone) {
      // 保护肤色，减少饱和度变化
      const skinGray = (outR + outG + outB) / 3;
      outR = Math.max(0, Math.min(1, skinGray + (outR - skinGray) * 0.8));
      outG = Math.max(0, Math.min(1, skinGray + (outG - skinGray) * 0.8));
      outB = Math.max(0, Math.min(1, skinGray + (outB - skinGray) * 0.8));
    }
  }
  
  return [outR, outG, outB];
}

/**
 * 将预设参数导出为CUBE文件内容
 */
export function exportToCUBE(params: LUTParams, size: number = 33): string {
  const lines: string[] = [];
  
  // CUBE文件头
  lines.push('TITLE "OMaster Exported LUT"');
  lines.push('# Exported from OMaster Hasselblad Master System');
  lines.push(`# Generated: ${new Date().toISOString()}`);
  lines.push('');
  lines.push(`LUT_3D_SIZE ${size}`);
  lines.push('');
  lines.push('DOMAIN_MIN 0.0 0.0 0.0');
  lines.push('DOMAIN_MAX 1.0 1.0 1.0');
  lines.push('');
  
  // 生成LUT数据
  for (let b = 0; b < size; b++) {
    for (let g = 0; g < size; g++) {
      for (let r = 0; r < size; r++) {
        const inputR = r / (size - 1);
        const inputG = g / (size - 1);
        const inputB = b / (size - 1);
        
        // 应用参数变换
        const [outR, outG, outB] = applyParams(inputR, inputG, inputB, params);
        
        // 格式化输出（保留6位小数）
        lines.push(`${outR.toFixed(6)} ${outG.toFixed(6)} ${outB.toFixed(6)}`);
      }
    }
  }
  
  return lines.join('\n');
}

/**
 * 下载LUT文件
 */
export function downloadLUTFile(params: LUTParams, filename: string = 'omaster-lut.cube'): void {
  const content = exportToCUBE(params);
  const blob = new Blob([content], { type: 'text/plain' });
  const url = URL.createObjectURL(blob);
  
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  
  URL.revokeObjectURL(url);
}

/**
 * 读取本地LUT文件并反推参数
 */
export function readLUTFileAndApproximate(file: File): Promise<LUTParams> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    
    reader.onload = (e) => {
      try {
        const content = e.target?.result as string;
        const params = approximateParams(content);
        resolve(params);
      } catch (error) {
        reject(new Error('解析LUT文件失败'));
      }
    };
    
    reader.onerror = () => {
      reject(new Error('读取文件失败'));
    };
    
    reader.readAsText(file);
  });
}

// ===== 导出便捷方法 =====

export default {
  parseCUBEFile,
  sampleLUT,
  approximateParams,
  exportToCUBE,
  downloadLUTFile,
  readLUTFileAndApproximate,
};