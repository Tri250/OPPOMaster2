/**
 * 用户图片数据管理
 * 全局共享用户上传的图片
 */

export interface UserImage {
  dataUrl: string;        // base64 dataURL
  width: number;
  height: number;
  size: number;           // bytes
  fileName: string;
  uploadedAt: number;
}

type Listener = (image: UserImage | null) => void;

class UserImageStore {
  private current: UserImage | null = null;
  private listeners: Set<Listener> = new Set();

  set(image: UserImage) {
    this.current = image;
    this.listeners.forEach(l => l(image));
  }

  clear() {
    this.current = null;
    this.listeners.forEach(l => l(null));
  }

  get(): UserImage | null {
    return this.current;
  }

  subscribe(listener: Listener) {
    this.listeners.add(listener);
    // 立即触发一次当前状态
    listener(this.current);
    return () => this.listeners.delete(listener);
  }
}

export const userImageStore = new UserImageStore();
