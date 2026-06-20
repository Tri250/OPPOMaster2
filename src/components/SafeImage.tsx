import React, { useState, useCallback } from 'react';
import { tokens } from '../styles/designTokens';
import { ImageOff } from 'lucide-react';

interface SafeImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallbackIconSize?: number;
}

/**
 * 安全图片组件
 * - 处理加载失败，显示占位图
 * - 支持 srcSet / sizes 以适配不同分辨率
 * - 提升 Android 端图片稳定性与兼容性
 */
export const SafeImage: React.FC<SafeImageProps> = ({
  fallbackIconSize = 24,
  className = '',
  style,
  src,
  alt = '',
  srcSet,
  sizes,
  loading,
  onError,
  ...rest
}) => {
  const [failed, setFailed] = useState(false);

  const handleError = useCallback(
    (e: React.SyntheticEvent<HTMLImageElement, Event>) => {
      setFailed(true);
      onError?.(e);
    },
    [onError]
  );

  if (failed || !src) {
    return (
      <div
        className={`flex items-center justify-center ${className}`}
        style={{
          background: tokens.colors.glass,
          ...style,
        }}
        {...rest}
      >
        <ImageOff size={fallbackIconSize} style={{ color: tokens.colors.textTertiary }} />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      srcSet={srcSet}
      sizes={sizes}
      loading={loading}
      onError={handleError}
      className={className}
      style={style}
      {...rest}
    />
  );
};

export default SafeImage;
