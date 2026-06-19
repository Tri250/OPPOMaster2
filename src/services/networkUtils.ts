/**
 * 网络请求工具
 * 提供带超时配置的fetch请求
 */

// ============================================
// 超时配置
// ============================================

/**
 * 默认超时时间（毫秒）
 */
export const DEFAULT_TIMEOUT_MS = 30000; // 30秒

/**
 * 不同请求类型的超时配置
 */
export const TIMEOUT_CONFIG = {
  // 快速请求（如状态检查）
  quick: 5000,
  // 标准请求（如预设获取）
  standard: 15000,
  // 长请求（如AI推理）
  long: 30000,
  // 文件下载（如LUT下载）
  download: 60000,
  // 图片加载
  image: 20000,
} as const;

// ============================================
// fetch超时工具
// ============================================

/**
 * 带超时的fetch请求
 * 
 * @param url 请求URL
 * @param options fetch选项
 * @param timeoutMs 超时时间（毫秒）
 * @returns Promise<Response>
 */
export async function fetchWithTimeout(
  url: string,
  options: RequestInit = {},
  timeoutMs: number = DEFAULT_TIMEOUT_MS
): Promise<Response> {
  // 创建AbortController用于超时控制
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
  
  try {
    // 合并signal到options
    const response = await fetch(url, {
      ...options,
      signal: controller.signal,
    });
    
    // 清除超时定时器
    clearTimeout(timeoutId);
    
    return response;
    
  } catch (error) {
    // 清除超时定时器
    clearTimeout(timeoutId);
    
    // 判断是否为超时错误
    if (error instanceof Error && error.name === 'AbortError') {
      throw new TimeoutError(url, timeoutMs);
    }
    
    throw error;
  }
}

/**
 * 安全解析 JSON 响应
 */
export async function safeParseJson<T>(response: Response, url: string): Promise<T> {
  const text = await response.text();
  if (!text || text.trim() === '') {
    throw new NetworkError(url, response.status, 'Empty response body');
  }
  try {
    return JSON.parse(text) as T;
  } catch (parseError) {
    const message = parseError instanceof Error ? parseError.message : 'JSON parse failed';
    throw new NetworkError(url, response.status, `Invalid JSON: ${message}`);
  }
}

/**
 * 带超时的GET请求
 */
export async function fetchGet<T>(
  url: string,
  timeoutMs: number = TIMEOUT_CONFIG.standard
): Promise<T> {
  const response = await fetchWithTimeout(url, { method: 'GET' }, timeoutMs);

  if (!response.ok) {
    throw new NetworkError(url, response.status, response.statusText);
  }

  return safeParseJson<T>(response, url);
}

/**
 * 带超时的POST请求
 */
export async function fetchPost<T>(
  url: string,
  data: unknown,
  timeoutMs: number = TIMEOUT_CONFIG.standard
): Promise<T> {
  const response = await fetchWithTimeout(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  }, timeoutMs);

  if (!response.ok) {
    throw new NetworkError(url, response.status, response.statusText);
  }

  return safeParseJson<T>(response, url);
}

/**
 * 带超时的文件下载
 */
export async function fetchBlob(
  url: string,
  timeoutMs: number = TIMEOUT_CONFIG.download
): Promise<Blob> {
  const response = await fetchWithTimeout(url, { method: 'GET' }, timeoutMs);
  
  if (!response.ok) {
    throw new NetworkError(url, response.status, response.statusText);
  }
  
  return response.blob();
}

/**
 * 带超时的图片加载
 */
export async function fetchImage(
  url: string,
  timeoutMs: number = TIMEOUT_CONFIG.image
): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => {
      controller.abort();
      reject(new TimeoutError(url, timeoutMs));
    }, timeoutMs);
    
    const img = new Image();
    img.crossOrigin = 'anonymous';
    
    img.onload = () => {
      clearTimeout(timeoutId);
      resolve(img);
    };
    
    img.onerror = () => {
      clearTimeout(timeoutId);
      reject(new NetworkError(url, 0, 'Image load failed'));
    };
    
    img.src = url;
  });
}

// ============================================
// 错误类型
// ============================================

/**
 * 超时错误
 */
export class TimeoutError extends Error {
  constructor(
    public readonly url: string,
    public readonly timeoutMs: number
  ) {
    super(`请求超时: ${url} (${timeoutMs}ms)`);
    this.name = 'TimeoutError';
  }
}

/**
 * 网络错误
 */
export class NetworkError extends Error {
  constructor(
    public readonly url: string,
    public readonly status: number,
    public readonly statusText: string
  ) {
    super(`网络错误: ${url} (${status} ${statusText})`);
    this.name = 'NetworkError';
  }
}

// ============================================
// 重试机制
// ============================================

/**
 * 带重试的fetch请求
 * 
 * @param url 请求URL
 * @param options fetch选项
 * @param timeoutMs 超时时间
 * @param maxRetries 最大重试次数
 * @param retryDelay 重试延迟（毫秒）
 */
export async function fetchWithRetry(
  url: string,
  options: RequestInit = {},
  timeoutMs: number = DEFAULT_TIMEOUT_MS,
  maxRetries: number = 3,
  retryDelay: number = 1000
): Promise<Response> {
  let lastError: Error | null = null;
  
  for (let attempt = 0; attempt < maxRetries; attempt++) {
    try {
      const response = await fetchWithTimeout(url, options, timeoutMs);
      return response;
    } catch (error) {
      lastError = error instanceof Error ? error : new Error('Unknown error');
      
      // 超时错误可以重试
      if (error instanceof TimeoutError) {
        console.warn(`请求超时，重试 ${attempt + 1}/${maxRetries}: ${url}`);
        
        // 等待一段时间后重试
        if (attempt < maxRetries - 1) {
          await new Promise(resolve => setTimeout(resolve, retryDelay * (attempt + 1)));
        }
        continue;
      }
      
      // 其他错误直接抛出
      throw error;
    }
  }
  
  throw lastError;
}

// ============================================
// 网络状态检测
// ============================================

/**
 * 检测网络是否可用
 */
export function isNetworkAvailable(): boolean {
  return navigator.onLine;
}

/**
 * 监听网络状态变化
 */
export function listenNetworkStatus(
  onOnline: () => void,
  onOffline: () => void
): () => void {
  window.addEventListener('online', onOnline);
  window.addEventListener('offline', onOffline);
  
  // 返回取消监听函数
  return () => {
    window.removeEventListener('online', onOnline);
    window.removeEventListener('offline', onOffline);
  };
}