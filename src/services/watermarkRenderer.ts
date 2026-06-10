/**
 * 水印渲染器
 * Web端 - 使用Canvas渲染水印并导出图片
 */

import { MasterWatermarkTemplate, ExifWatermarkData, WatermarkPosition } from '../types/watermark';
import { HasselbladColors, calculateImageLuminance, getTextColorForBackground } from './hasselbladColors';

// ========== 导出格式 ==========
export type ExportFormat = 'jpeg' | 'png' | 'webp';

export interface ExportOptions {
  format: ExportFormat;
  quality: number;  // 0-100
  filename?: string;
}

// ========== 水印渲染器类 ==========
export class WatermarkRenderer {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;

  constructor() {
    this.canvas = document.createElement('canvas');
    const ctx = this.canvas.getContext('2d');
    if (!ctx) throw new Error('Cannot get canvas context');
    this.ctx = ctx;
  }

  /**
   * 导出带水印的图片
   */
  async exportWatermarkedImage(
    imageSource: string | HTMLImageElement,
    template: MasterWatermarkTemplate,
    exifData: Partial<ExifWatermarkData>,
    options: ExportOptions = { format: 'jpeg', quality: 95 }
  ): Promise<Blob> {
    // 1. 加载图片
    const img = await this.loadImage(imageSource);
    
    // 2. 设置画布尺寸
    this.canvas.width = img.width;
    this.canvas.height = img.height;
    
    // 3. 绘制原图
    this.ctx.drawImage(img, 0, 0);
    
    // 4. 渲染水印
    this.render(template, exifData);
    
    // 5. 导出为 Blob
    return new Promise((resolve, reject) => {
      this.canvas.toBlob(
        (blob) => {
          if (blob) resolve(blob);
          else reject(new Error('Failed to create blob'));
        },
        `image/${options.format}`,
        options.quality / 100
      );
    });
  }

  /**
   * 导出并下载
   */
  async exportAndDownload(
    imageSource: string | HTMLImageElement,
    template: MasterWatermarkTemplate,
    exifData: Partial<ExifWatermarkData>,
    options: ExportOptions = { format: 'jpeg', quality: 95 }
  ): Promise<void> {
    const blob = await this.exportWatermarkedImage(imageSource, template, exifData, options);
    
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = options.filename || `OMaster_${Date.now()}.${options.format}`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * 渲染水印
   */
  render(
    template: MasterWatermarkTemplate,
    exifData: Partial<ExifWatermarkData>
  ): void {
    const { width, height } = this.canvas;

    // 根据模板 ID 选择渲染方式
    switch (template.id) {
      case 'hasselblad-master':
        this.renderHasselbladMaster(exifData);
        break;
      case 'hasselblad-hncs':
        this.renderHasselbladHncs(exifData);
        break;
      case 'hasselblad-xpan':
        this.renderHasselbladXpan(exifData);
        break;
      default:
        this.renderGeneric(template, exifData);
    }
  }

  /**
   * 渲染哈苏大师印记
   */
  private renderHasselbladMaster(exifData: Partial<ExifWatermarkData>): void {
    const { width, height } = this.canvas;
    const ctx = this.ctx;

    // 底部信息栏高度 (8% 画面高度)
    const barHeight = height * 0.08;
    const barTop = height - barHeight;

    // 绘制半透明背景
    ctx.fillStyle = HasselbladColors.BackgroundSemiTransparent;
    ctx.fillRect(0, barTop, width, barHeight);

    // 金色分割线
    ctx.strokeStyle = HasselbladColors.Gold;
    ctx.globalAlpha = 0.3;
    ctx.lineWidth = 1;
    const dividerY = barTop + barHeight * 0.15;
    ctx.beginPath();
    ctx.moveTo(width * 0.2, dividerY);
    ctx.lineTo(width * 0.8, dividerY);
    ctx.stroke();
    ctx.globalAlpha = 1;

    // HASSELBLAD 品牌
    ctx.fillStyle = HasselbladColors.Gold;
    ctx.font = `bold ${barHeight * 0.25}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.letterSpacing = '2px';
    const brandY = barTop + barHeight * 0.4;
    ctx.fillText('HASSELBLAD', width / 2, brandY);

    // HNCS 认证标识
    ctx.fillStyle = HasselbladColors.TextSecondary;
    ctx.font = `${barHeight * 0.12}px sans-serif`;
    ctx.fillText('HNCS', width / 2, brandY + barHeight * 0.2);

    // 设备型号
    const deviceInfo = this.getDeviceInfo(exifData);
    ctx.fillStyle = HasselbladColors.TextSecondary;
    ctx.font = `${barHeight * 0.15}px sans-serif`;
    ctx.fillText(deviceInfo, width / 2, brandY + barHeight * 0.4);

    // 拍摄参数
    const paramsInfo = this.getParamsInfo(exifData);
    ctx.fillStyle = HasselbladColors.TextTertiary;
    ctx.font = `${barHeight * 0.12}px monospace`;
    ctx.fillText(paramsInfo, width / 2, brandY + barHeight * 0.6);

    // 日期
    const dateStr = exifData.dateTaken || this.getCurrentDate();
    ctx.fillStyle = HasselbladColors.TextTertiary;
    ctx.font = `${barHeight * 0.1}px sans-serif`;
    ctx.fillText(dateStr, width / 2, barTop + barHeight * 0.85);

    // 底部品牌联合标识
    ctx.fillStyle = HasselbladColors.TextTertiary;
    ctx.font = `${barHeight * 0.08}px sans-serif`;
    ctx.fillText('OPPO × Hasselblad | Master Edition', width / 2, barTop + barHeight * 0.95);
  }

  /**
   * 渲染 HNCS 认证标识
   */
  private renderHasselbladHncs(exifData: Partial<ExifWatermarkData>): void {
    const { width, height } = this.canvas;
    const ctx = this.ctx;

    // 徽章位置 (左上角)
    const badgeWidth = width * 0.15;
    const badgeHeight = height * 0.08;
    const badgeLeft = width * 0.05;
    const badgeTop = height * 0.05;
    const cornerRadius = badgeWidth * 0.1;

    // 绘制徽章背景
    ctx.fillStyle = HasselbladColors.BackgroundSemiTransparent;
    this.roundRect(badgeLeft, badgeTop, badgeWidth, badgeHeight, cornerRadius);
    ctx.fill();

    // 绘制金色边框
    ctx.strokeStyle = HasselbladColors.Gold;
    ctx.lineWidth = 2;
    this.roundRect(badgeLeft, badgeTop, badgeWidth, badgeHeight, cornerRadius);
    ctx.stroke();

    // HNCS 文字
    ctx.fillStyle = HasselbladColors.Gold;
    ctx.font = `bold ${badgeHeight * 0.3}px sans-serif`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('HNCS', badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.35);

    // 哈苏自然色彩认证
    ctx.fillStyle = HasselbladColors.TextSecondary;
    ctx.font = `${badgeHeight * 0.2}px sans-serif`;
    ctx.fillText('哈苏自然', badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.6);
    ctx.fillText('色彩认证', badgeLeft + badgeWidth / 2, badgeTop + badgeHeight * 0.8);
  }

  /**
   * 渲染 XPAN 宽幅印记
   */
  private renderHasselbladXpan(exifData: Partial<ExifWatermarkData>): void {
    const { width, height } = this.canvas;
    const ctx = this.ctx;

    // 底部信息条高度
    const barHeight = height * 0.05;
    const barTop = height - barHeight;

    // 绘制背景
    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    ctx.fillRect(0, barTop, width, barHeight);

    // XPAN 65:24
    ctx.fillStyle = HasselbladColors.TextPrimary;
    ctx.font = `bold ${barHeight * 0.5}px sans-serif`;
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    ctx.letterSpacing = '3px';
    const leftMargin = width * 0.05;
    ctx.fillText('XPAN  65:24', leftMargin, barTop + barHeight * 0.5);

    // HASSELBLAD · 设备型号
    const deviceInfo = `HASSELBLAD · ${this.getDeviceInfo(exifData)}`;
    ctx.fillStyle = HasselbladColors.TextSecondary;
    ctx.font = `${barHeight * 0.35}px sans-serif`;
    ctx.textAlign = 'right';
    ctx.letterSpacing = '0px';
    const rightMargin = width * 0.95;
    ctx.fillText(deviceInfo, rightMargin, barTop + barHeight * 0.5);
  }

  /**
   * 渲染通用水印
   */
  private renderGeneric(
    template: MasterWatermarkTemplate,
    exifData: Partial<ExifWatermarkData>
  ): void {
    const { width, height } = this.canvas;
    const ctx = this.ctx;

    const layers = template.layers.sort((a, b) => a.sortOrder - b.sortOrder);
    const position = template.defaultPosition;
    const { x, y, align } = this.getPositionCoords(position, width, height);

    ctx.textAlign = align;
    ctx.textBaseline = 'top';

    let currentY = y;
    for (const layer of layers) {
      if (!layer.isEnabled) continue;

      const content = this.getLayerContent(layer, exifData);
      if (!content) continue;

      const fontSize = layer.defaultStyle.fontSize * (height / 1000);
      ctx.font = `${layer.defaultStyle.fontWeight >= 700 ? 'bold' : 'normal'} ${fontSize}px ${layer.defaultStyle.fontFamily}`;
      ctx.fillStyle = layer.defaultStyle.color;
      ctx.globalAlpha = layer.defaultStyle.opacity;

      ctx.fillText(content, x, currentY);
      currentY += fontSize * 1.5;
    }

    ctx.globalAlpha = 1;
  }

  // ========== 辅助方法 ==========

  private async loadImage(source: string | HTMLImageElement): Promise<HTMLImageElement> {
    if (source instanceof HTMLImageElement) {
      return source;
    }

    return new Promise((resolve, reject) => {
      const img = new Image();
      img.crossOrigin = 'anonymous';
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error('Failed to load image'));
      img.src = source;
    });
  }

  private roundRect(
    x: number,
    y: number,
    w: number,
    h: number,
    r: number
  ): void {
    const ctx = this.ctx;
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.lineTo(x + w - r, y);
    ctx.quadraticCurveTo(x + w, y, x + w, y + r);
    ctx.lineTo(x + w, y + h - r);
    ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
    ctx.lineTo(x + r, y + h);
    ctx.quadraticCurveTo(x, y + h, x, y + h - r);
    ctx.lineTo(x, y + r);
    ctx.quadraticCurveTo(x, y, x + r, y);
    ctx.closePath();
  }

  private getDeviceInfo(exif: Partial<ExifWatermarkData>): string {
    if (exif.make && exif.model) return `${exif.make} ${exif.model}`;
    if (exif.model) return exif.model;
    return 'Unknown Device';
  }

  private getParamsInfo(exif: Partial<ExifWatermarkData>): string {
    const parts: string[] = [];
    if (exif.aperture) parts.push(exif.aperture);
    if (exif.shutterSpeed) parts.push(exif.shutterSpeed);
    if (exif.iso) parts.push(exif.iso);
    if (exif.focalLength) parts.push(exif.focalLength);
    return parts.join('  ');
  }

  private getCurrentDate(): string {
    const now = new Date();
    return `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')}`;
  }

  private getLayerContent(
    layer: { type: string; defaultContent: string; contentSource: string },
    exif: Partial<ExifWatermarkData>
  ): string {
    switch (layer.contentSource) {
      case 'manual':
        return layer.defaultContent;
      case 'exif':
        if (layer.type === 'params') return this.getParamsInfo(exif);
        if (layer.type === 'timestamp') return exif.dateTaken || '';
        return layer.defaultContent;
      case 'device_info':
        return this.getDeviceInfo(exif);
      case 'system':
        return this.getCurrentDate();
      case 'gps':
        return exif.locationName || '';
      default:
        return layer.defaultContent;
    }
  }

  private getPositionCoords(
    position: WatermarkPosition,
    width: number,
    height: number
  ): { x: number; y: number; align: CanvasTextAlign } {
    const padding = width * 0.05;
    switch (position) {
      case 'top-left':
        return { x: padding, y: padding * 2, align: 'left' };
      case 'top-center':
        return { x: width / 2, y: padding * 2, align: 'center' };
      case 'top-right':
        return { x: width - padding, y: padding * 2, align: 'right' };
      case 'center-left':
        return { x: padding, y: height / 2, align: 'left' };
      case 'center':
        return { x: width / 2, y: height / 2, align: 'center' };
      case 'center-right':
        return { x: width - padding, y: height / 2, align: 'right' };
      case 'bottom-left':
        return { x: padding, y: height - padding, align: 'left' };
      case 'bottom-center':
        return { x: width / 2, y: height - padding, align: 'center' };
      case 'bottom-right':
        return { x: width - padding, y: height - padding, align: 'right' };
    }
  }
}

// ========== 单例导出 ==========
export const watermarkRenderer = new WatermarkRenderer();

// ========== 便捷方法 ==========
export async function exportWatermarkedImage(
  imageSource: string | HTMLImageElement,
  template: MasterWatermarkTemplate,
  exifData: Partial<ExifWatermarkData>,
  options?: ExportOptions
): Promise<Blob> {
  return watermarkRenderer.exportWatermarkedImage(imageSource, template, exifData, options);
}

export async function exportAndDownload(
  imageSource: string | HTMLImageElement,
  template: MasterWatermarkTemplate,
  exifData: Partial<ExifWatermarkData>,
  options?: ExportOptions
): Promise<void> {
  return watermarkRenderer.exportAndDownload(imageSource, template, exifData, options);
}
