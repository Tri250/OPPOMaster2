/**
 * 哈苏品牌色板
 * 基于 OPPO × 哈苏品牌体系
 */

export const HasselbladColors = {
  // ========== 主品牌色 ==========
  HasselbladOrange: '#FF6B35',      // 主品牌色
  HasselbladOrangeLight: '#FF8C42', // 浅哈苏橙
  
  // ========== 金色点缀 ==========
  Gold: '#D4AF37',                   // 哈苏大师系列分割线、徽章边框
  GoldLight: '#E5C76B',              // 浅金色
  GoldDark: '#B8962D',               // 深金色
  
  // ========== 文字色 ==========
  TextPrimary: '#FFFFFF',            // 默认水印文字色
  TextDark: '#1A1A1A',               // 高调画面自动切换
  TextSecondary: 'rgba(255, 255, 255, 0.7)',  // 70% 透明度白色
  TextTertiary: 'rgba(255, 255, 255, 0.4)',   // 40% 透明度白色
  
  // ========== 背景色 ==========
  BackgroundSemiTransparent: 'rgba(0, 0, 0, 0.4)',  // 40% 透明度黑色
  BackgroundLight: 'rgba(0, 0, 0, 0.2)',            // 20% 透明度黑色
  
  // ========== HNCS 认证色 ==========
  HncsGreen: '#4CAF50',              // HNCS 认证标识点缀
  HncsGreenLight: '#66BB6A',
  
  // ========== 分割线 ==========
  DividerGold: 'rgba(212, 175, 55, 0.3)',   // 金色分割线 30% 透明度
  DividerWhite: 'rgba(255, 255, 255, 0.2)', // 白色分割线 20% 透明度
} as const;

/**
 * 根据背景亮度选择文字颜色
 */
export function getTextColorForBackground(backgroundLuminance: number): string {
  return backgroundLuminance > 0.5 
    ? HasselbladColors.TextDark 
    : HasselbladColors.TextPrimary;
}

/**
 * 计算图片平均亮度
 */
export async function calculateImageLuminance(
  imageSource: string | HTMLImageElement
): Promise<number> {
  return new Promise((resolve) => {
    const img = typeof imageSource === 'string' ? new Image() : imageSource;
    
    if (typeof imageSource === 'string') {
      img.crossOrigin = 'anonymous';
      img.src = imageSource;
    }
    
    img.onload = () => {
      const canvas = document.createElement('canvas');
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        resolve(0.5);
        return;
      }
      
      // 缩小尺寸以提高性能
      const sampleSize = 50;
      canvas.width = sampleSize;
      canvas.height = sampleSize;
      
      ctx.drawImage(img, 0, 0, sampleSize, sampleSize);
      
      const imageData = ctx.getImageData(0, 0, sampleSize, sampleSize);
      const data = imageData.data;
      
      let totalLuminance = 0;
      const pixelCount = data.length / 4;
      
      for (let i = 0; i < data.length; i += 4) {
        const r = data[i];
        const g = data[i + 1];
        const b = data[i + 2];
        // 相对亮度公式
        totalLuminance += (0.299 * r + 0.587 * g + 0.114 * b) / 255;
      }
      
      resolve(totalLuminance / pixelCount);
    };
    
    img.onerror = () => resolve(0.5);
  });
}
