// Vitest 测试环境全局 mock
// 在 Node 环境下提供浏览器 API 的轻量替代

class MockImageData implements ImageData {
  width: number;
  height: number;
  data: Uint8ClampedArray;
  colorSpace: PredefinedColorSpace;

  constructor(width: number, height: number);
  constructor(data: Uint8ClampedArray, width: number, height?: number);
  constructor(
    arg1: number | Uint8ClampedArray,
    arg2: number,
    arg3?: number,
  ) {
    if (typeof arg1 === 'number') {
      this.width = arg1;
      this.height = arg2;
      this.data = new Uint8ClampedArray(arg1 * arg2 * 4);
    } else {
      this.data = arg1;
      this.width = arg2;
      this.height = arg3 ?? Math.floor(arg1.length / 4 / arg2);
    }
    this.colorSpace = 'srgb';
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
(globalThis as any).ImageData = MockImageData;
