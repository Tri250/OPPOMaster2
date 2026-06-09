# TensorFlow Lite 模型训练指南

本指南详细说明如何训练、导出和量化 OMaster 应用所需的三个 TFLite 模型。

---

## 📋 目录

- [环境准备](#环境准备)
- [场景分类模型训练](#场景分类模型训练)
- [质量分析模型训练](#质量分析模型训练)
- [参数预测模型训练](#参数预测模型训练)
- [模型导出与量化](#模型导出与量化)
- [模型验证与测试](#模型验证与测试)
- [常见问题](#常见问题)

---

## 环境准备

### 系统要求

- **操作系统**: Ubuntu 20.04+ / macOS 12+ / Windows 10+ (WSL2)
- **Python**: 3.9 - 3.11
- **GPU**: NVIDIA GPU (推荐，用于加速训练)
- **内存**: 至少 16GB RAM
- **存储**: 至少 100GB 可用空间

### 安装依赖

```bash
# 创建虚拟环境
python -m venv omaster-training
source omaster-training/bin/activate  # Linux/macOS
# 或 omaster-training\Scripts\activate  # Windows

# 安装 TensorFlow
pip install tensorflow==2.14.0

# 安装其他依赖
pip install numpy==1.24.3
pip install pillow==10.0.0
pip install matplotlib==3.7.2
pip install scikit-learn==1.3.0
pip install pandas==2.0.3
pip install tqdm==4.65.0
pip install albumentations==1.3.1

# 安装 TensorFlow Lite 工具
pip install tensorflow-model-optimization==0.7.5
```

### 验证安装

```python
import tensorflow as tf
print(f"TensorFlow 版本: {tf.__version__}")
print(f"GPU 可用: {tf.config.list_physical_devices('GPU')}")
```

---

## 场景分类模型训练

### 数据集准备

#### 目录结构

```
datasets/
└── scene_classification/
    ├── train/
    │   ├── landscape/
    │   │   ├── img001.jpg
    │   │   ├── img002.jpg
    │   │   └── ...
    │   ├── portrait/
    │   ├── night/
    │   ├── food/
    │   └── ... (共36个类别)
    ├── validation/
    │   ├── landscape/
    │   ├── portrait/
    │   └── ...
    └── test/
        ├── landscape/
        ├── portrait/
        └── ...
```

#### 数据集统计

| 类别 | 训练集 | 验证集 | 测试集 |
|------|--------|--------|--------|
| landscape | 1500 | 150 | 150 |
| portrait | 1500 | 150 | 150 |
| ... | ... | ... | ... |
| **总计** | **50000** | **5000** | **5000** |

### 训练脚本

```python
# train_scene_classifier.py

import tensorflow as tf
from tensorflow.keras import layers, models, optimizers
from tensorflow.keras.applications import MobileNetV3Small
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import numpy as np
import os

# 配置
CONFIG = {
    'input_size': 224,
    'batch_size': 32,
    'epochs': 50,
    'learning_rate': 0.001,
    'num_classes': 36,
    'train_dir': 'datasets/scene_classification/train',
    'val_dir': 'datasets/scene_classification/validation',
    'test_dir': 'datasets/scene_classification/test',
    'checkpoint_path': 'checkpoints/scene_classifier_best.h5',
    'output_path': 'models/scene_classifier.h5'
}

# 数据增强
train_datagen = ImageDataGenerator(
    rescale=1./255,
    rotation_range=15,
    width_shift_range=0.1,
    height_shift_range=0.1,
    shear_range=0.1,
    zoom_range=0.1,
    horizontal_flip=True,
    fill_mode='reflect'
)

val_datagen = ImageDataGenerator(rescale=1./255)

# 加载数据
train_generator = train_datagen.flow_from_directory(
    CONFIG['train_dir'],
    target_size=(CONFIG['input_size'], CONFIG['input_size']),
    batch_size=CONFIG['batch_size'],
    class_mode='categorical'
)

val_generator = val_datagen.flow_from_directory(
    CONFIG['val_dir'],
    target_size=(CONFIG['input_size'], CONFIG['input_size']),
    batch_size=CONFIG['batch_size'],
    class_mode='categorical'
)

# 构建模型
def build_model():
    # 加载预训练的 MobileNetV3-Small
    base_model = MobileNetV3Small(
        input_shape=(CONFIG['input_size'], CONFIG['input_size'], 3),
        include_top=False,
        weights='imagenet',
        minimalistic=False
    )
    
    # 冻结基础模型的前几层
    base_model.trainable = True
    for layer in base_model.layers[:50]:
        layer.trainable = False
    
    # 构建完整模型
    inputs = tf.keras.Input(shape=(CONFIG['input_size'], CONFIG['input_size'], 3))
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)
    x = layers.Dense(256, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(CONFIG['num_classes'], activation='softmax')(x)
    
    model = models.Model(inputs, outputs)
    return model

model = build_model()

# 编译模型
model.compile(
    optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='categorical_crossentropy',
    metrics=['accuracy', tf.keras.metrics.TopKCategoricalAccuracy(k=5)]
)

# 回调函数
callbacks = [
    tf.keras.callbacks.ModelCheckpoint(
        CONFIG['checkpoint_path'],
        monitor='val_accuracy',
        save_best_only=True,
        mode='max',
        verbose=1
    ),
    tf.keras.callbacks.EarlyStopping(
        monitor='val_accuracy',
        patience=10,
        restore_best_weights=True,
        verbose=1
    ),
    tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=1e-7,
        verbose=1
    ),
    tf.keras.callbacks.TensorBoard(
        log_dir='logs/scene_classifier',
        histogram_freq=1
    )
]

# 训练模型
history = model.fit(
    train_generator,
    epochs=CONFIG['epochs'],
    validation_data=val_generator,
    callbacks=callbacks,
    verbose=1
)

# 保存最终模型
model.save(CONFIG['output_path'])

print(f"模型已保存到: {CONFIG['output_path']}")
print(f"最佳验证准确率: {max(history.history['val_accuracy']):.4f}")
```

### 运行训练

```bash
python train_scene_classifier.py
```

---

## 质量分析模型训练

### 数据集准备

#### 目录结构

```
datasets/
└── quality_assessment/
    ├── train/
    │   ├── images/
    │   │   ├── img001.jpg
    │   │   ├── img002.jpg
    │   │   └── ...
    │   └── labels.csv  # 包含图片名和质量评分
    ├── validation/
    │   ├── images/
    │   └── labels.csv
    └── test/
        ├── images/
        └── labels.csv
```

#### 标签格式 (labels.csv)

```csv
filename,brightness,contrast,noise,sharpness
img001.jpg,0.65,0.72,0.81,0.58
img002.jpg,0.48,0.55,0.42,0.67
...
```

### 训练脚本

```python
# train_quality_analyzer.py

import tensorflow as tf
from tensorflow.keras import layers, models, optimizers
from tensorflow.keras.applications import MobileNetV2
import pandas as pd
import numpy as np
from PIL import Image
import os

# 配置
CONFIG = {
    'input_size': 224,
    'batch_size': 32,
    'epochs': 40,
    'learning_rate': 0.0005,
    'output_dims': 4,
    'train_dir': 'datasets/quality_assessment/train',
    'val_dir': 'datasets/quality_assessment/validation',
    'checkpoint_path': 'checkpoints/quality_analyzer_best.h5',
    'output_path': 'models/quality_analyzer.h5'
}

# 自定义数据生成器
class QualityDataGenerator(tf.keras.utils.Sequence):
    def __init__(self, data_dir, batch_size=32, input_size=224, shuffle=True):
        self.data_dir = data_dir
        self.batch_size = batch_size
        self.input_size = input_size
        self.shuffle = shuffle
        
        # 加载标签
        self.labels_df = pd.read_csv(os.path.join(data_dir, 'labels.csv'))
        self.image_dir = os.path.join(data_dir, 'images')
        self.indices = np.arange(len(self.labels_df))
        
        if self.shuffle:
            np.random.shuffle(self.indices)
    
    def __len__(self):
        return int(np.ceil(len(self.labels_df) / self.batch_size))
    
    def __getitem__(self, idx):
        batch_indices = self.indices[idx * self.batch_size:(idx + 1) * self.batch_size]
        
        batch_images = []
        batch_labels = []
        
        for i in batch_indices:
            row = self.labels_df.iloc[i]
            
            # 加载图像
            img_path = os.path.join(self.image_dir, row['filename'])
            img = Image.open(img_path).convert('RGB')
            img = img.resize((self.input_size, self.input_size))
            img = np.array(img) / 255.0
            
            # 获取标签
            labels = [
                row['brightness'],
                row['contrast'],
                row['noise'],
                row['sharpness']
            ]
            
            batch_images.append(img)
            batch_labels.append(labels)
        
        return np.array(batch_images), np.array(batch_labels)
    
    def on_epoch_end(self):
        if self.shuffle:
            np.random.shuffle(self.indices)

# 构建模型
def build_model():
    # 加载预训练的 MobileNetV2
    base_model = MobileNetV2(
        input_shape=(CONFIG['input_size'], CONFIG['input_size'], 3),
        include_top=False,
        weights='imagenet'
    )
    
    # 冻结部分层
    base_model.trainable = True
    for layer in base_model.layers[:100]:
        layer.trainable = False
    
    # 构建完整模型 (NIMA 风格)
    inputs = tf.keras.Input(shape=(CONFIG['input_size'], CONFIG['input_size'], 3))
    x = base_model(inputs, training=False)
    x = layers.GlobalAveragePooling2D()(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.3)(x)
    x = layers.Dense(512, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.3)(x)
    x = layers.Dense(128, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)
    outputs = layers.Dense(CONFIG['output_dims'], activation='sigmoid')(x)
    
    model = models.Model(inputs, outputs)
    return model

model = build_model()

# 编译模型
model.compile(
    optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='mse',
    metrics=['mae', 'mse']
)

# 数据生成器
train_generator = QualityDataGenerator(
    CONFIG['train_dir'],
    batch_size=CONFIG['batch_size'],
    input_size=CONFIG['input_size']
)

val_generator = QualityDataGenerator(
    CONFIG['val_dir'],
    batch_size=CONFIG['batch_size'],
    input_size=CONFIG['input_size'],
    shuffle=False
)

# 回调函数
callbacks = [
    tf.keras.callbacks.ModelCheckpoint(
        CONFIG['checkpoint_path'],
        monitor='val_loss',
        save_best_only=True,
        mode='min',
        verbose=1
    ),
    tf.keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=10,
        restore_best_weights=True,
        verbose=1
    ),
    tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=1e-7,
        verbose=1
    ),
    tf.keras.callbacks.TensorBoard(
        log_dir='logs/quality_analyzer',
        histogram_freq=1
    )
]

# 训练模型
history = model.fit(
    train_generator,
    epochs=CONFIG['epochs'],
    validation_data=val_generator,
    callbacks=callbacks,
    verbose=1
)

# 保存最终模型
model.save(CONFIG['output_path'])

print(f"模型已保存到: {CONFIG['output_path']}")
print(f"最佳验证 MSE: {min(history.history['val_loss']):.4f}")
```

### 运行训练

```bash
python train_quality_analyzer.py
```

---

## 参数预测模型训练

### 数据集准备

#### 目录结构

```
datasets/
└── parameter_prediction/
    ├── train/
    │   ├── features.csv  # 场景特征 + 质量特征
    │   └── params.csv    # 专家调校参数
    ├── validation/
    │   ├── features.csv
    │   └── params.csv
    └── test/
        ├── features.csv
        └── params.csv
```

#### 特征格式 (features.csv)

```csv
sample_id,scene_0,scene_1,...,scene_35,quality_0,quality_1,quality_2,quality_3
1,0.05,0.85,...,0.02,0.65,0.72,0.81,0.58
2,0.90,0.02,...,0.01,0.48,0.55,0.42,0.67
...
```

#### 参数格式 (params.csv)

```csv
sample_id,exposure,contrast,saturation,highlights,shadows,whites,blacks,clarity,vibrance,warmth,tint,sharpness,noise_reduction,vignette,grain,fade,split_tone_highlights,split_tone_shadows
1,0.55,0.62,0.58,0.45,0.52,0.48,0.55,0.35,0.42,0.38,0.45,0.28,0.32,0.15,0.08,0.12,0.25,0.30
...
```

### 训练脚本

```python
# train_param_predictor.py

import tensorflow as tf
from tensorflow.keras import layers, models, optimizers
import pandas as pd
import numpy as np
import os

# 配置
CONFIG = {
    'input_dims': 40,  # 36 (scene) + 4 (quality)
    'output_dims': 18,
    'batch_size': 64,
    'epochs': 100,
    'learning_rate': 0.001,
    'train_dir': 'datasets/parameter_prediction/train',
    'val_dir': 'datasets/parameter_prediction/validation',
    'checkpoint_path': 'checkpoints/param_predictor_best.h5',
    'output_path': 'models/param_predictor.h5'
}

# 加载数据
def load_data(data_dir):
    features_df = pd.read_csv(os.path.join(data_dir, 'features.csv'))
    params_df = pd.read_csv(os.path.join(data_dir, 'params.csv'))
    
    # 提取特征和标签
    feature_cols = [f'scene_{i}' for i in range(36)] + \
                   [f'quality_{i}' for i in range(4)]
    param_cols = ['exposure', 'contrast', 'saturation', 'highlights', 
                  'shadows', 'whites', 'blacks', 'clarity', 'vibrance',
                  'warmth', 'tint', 'sharpness', 'noise_reduction',
                  'vignette', 'grain', 'fade', 
                  'split_tone_highlights', 'split_tone_shadows']
    
    X = features_df[feature_cols].values
    y = params_df[param_cols].values
    
    return X, y

X_train, y_train = load_data(CONFIG['train_dir'])
X_val, y_val = load_data(CONFIG['val_dir'])

# 构建模型
def build_model():
    model = models.Sequential([
        layers.Input(shape=(CONFIG['input_dims'],)),
        
        layers.Dense(128, activation='relu'),
        layers.BatchNormalization(),
        layers.Dropout(0.2),
        
        layers.Dense(64, activation='relu'),
        layers.BatchNormalization(),
        layers.Dropout(0.2),
        
        layers.Dense(32, activation='relu'),
        layers.BatchNormalization(),
        layers.Dropout(0.1),
        
        layers.Dense(CONFIG['output_dims'], activation='sigmoid')
    ])
    
    return model

model = build_model()

# 编译模型
model.compile(
    optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='mse',
    metrics=['mae', 'mse']
)

# 回调函数
callbacks = [
    tf.keras.callbacks.ModelCheckpoint(
        CONFIG['checkpoint_path'],
        monitor='val_loss',
        save_best_only=True,
        mode='min',
        verbose=1
    ),
    tf.keras.callbacks.EarlyStopping(
        monitor='val_loss',
        patience=10,
        restore_best_weights=True,
        verbose=1
    ),
    tf.keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=1e-7,
        verbose=1
    ),
    tf.keras.callbacks.TensorBoard(
        log_dir='logs/param_predictor',
        histogram_freq=1
    )
]

# 训练模型
history = model.fit(
    X_train, y_train,
    epochs=CONFIG['epochs'],
    batch_size=CONFIG['batch_size'],
    validation_data=(X_val, y_val),
    callbacks=callbacks,
    verbose=1
)

# 保存最终模型
model.save(CONFIG['output_path'])

print(f"模型已保存到: {CONFIG['output_path']}")
print(f"最佳验证 MSE: {min(history.history['val_loss']):.4f}")
```

### 运行训练

```bash
python train_param_predictor.py
```

---

## 模型导出与量化

### 导出为 TFLite 格式

```python
# export_to_tflite.py

import tensorflow as tf
import numpy as np
import os

def export_to_tflite(keras_model_path, tflite_path, quantize=True):
    """
    将 Keras 模型导出为 TFLite 格式
    
    Args:
        keras_model_path: Keras 模型路径
        tflite_path: 输出 TFLite 模型路径
        quantize: 是否进行 INT8 量化
    """
    # 加载模型
    model = tf.keras.models.load_model(keras_model_path)
    
    # 创建转换器
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    if quantize:
        # INT8 全整数量化
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # 提供代表性数据集用于量化
        def representative_dataset():
            # 这里需要提供实际的代表性数据
            # 示例：生成随机数据（实际使用时需要替换为真实数据）
            for _ in range(500):
                data = np.random.rand(1, 224, 224, 3).astype(np.float32)
                yield [data]
        
        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.uint8
        converter.inference_output_type = tf.uint8
    
    # 转换模型
    tflite_model = converter.convert()
    
    # 保存模型
    os.makedirs(os.path.dirname(tflite_path), exist_ok=True)
    with open(tflite_path, 'wb') as f:
        f.write(tflite_model)
    
    print(f"模型已导出到: {tflite_path}")
    print(f"模型大小: {len(tflite_model) / 1024:.2f} KB")

# 导出场景分类模型
export_to_tflite(
    'models/scene_classifier.h5',
    'tflite/scene_classifier.tflite',
    quantize=True
)

# 导出质量分析模型
export_to_tflite(
    'models/quality_analyzer.h5',
    'tflite/quality_analyzer.tflite',
    quantize=True
)

# 导出参数预测模型
def export_mlp_to_tflite():
    """导出 MLP 模型（参数预测器）"""
    model = tf.keras.models.load_model('models/param_predictor.h5')
    
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # INT8 量化
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    def representative_dataset():
        for _ in range(500):
            # 40 维特征向量
            data = np.random.rand(1, 40).astype(np.float32)
            yield [data]
    
    converter.representative_dataset = representative_dataset
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    converter.inference_input_type = tf.uint8
    converter.inference_output_type = tf.uint8
    
    tflite_model = converter.convert()
    
    with open('tflite/param_predictor.tflite', 'wb') as f:
        f.write(tflite_model)
    
    print(f"参数预测模型已导出")
    print(f"模型大小: {len(tflite_model) / 1024:.2f} KB")

export_mlp_to_tflite()
```

### 运行导出

```bash
python export_to_tflite.py
```

---

## 模型验证与测试

### 验证 TFLite 模型

```python
# validate_tflite.py

import tensorflow as tf
import numpy as np
from PIL import Image
import os

def validate_tflite_model(tflite_path, test_image_path=None):
    """
    验证 TFLite 模型是否正常工作
    """
    # 加载 TFLite 模型
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    
    # 获取输入输出详情
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"模型: {os.path.basename(tflite_path)}")
    print(f"输入详情: {input_details}")
    print(f"输出详情: {output_details}")
    
    # 测试推理
    if test_image_path and os.path.exists(test_image_path):
        # 加载测试图像
        img = Image.open(test_image_path).convert('RGB')
        img = img.resize((224, 224))
        img_array = np.array(img, dtype=np.float32) / 255.0
        img_array = np.expand_dims(img_array, axis=0)
        
        # 设置输入
        interpreter.set_tensor(input_details[0]['index'], img_array)
        
        # 执行推理
        interpreter.invoke()
        
        # 获取输出
        output = interpreter.get_tensor(output_details[0]['index'])
        print(f"输出形状: {output.shape}")
        print(f"输出值: {output[0][:10]}...")  # 只显示前10个值
    else:
        # 使用随机数据测试
        input_shape = input_details[0]['shape']
        random_input = np.random.rand(*input_shape).astype(np.float32)
        
        interpreter.set_tensor(input_details[0]['index'], random_input)
        interpreter.invoke()
        output = interpreter.get_tensor(output_details[0]['index'])
        
        print(f"输出形状: {output.shape}")
        print(f"输出范围: [{output.min():.4f}, {output.max():.4f}]")
    
    print("-" * 50)

# 验证所有模型
validate_tflite_model('tflite/scene_classifier.tflite')
validate_tflite_model('tflite/quality_analyzer.tflite')
validate_tflite_model('tflite/param_predictor.tflite')
```

### 性能基准测试

```python
# benchmark_tflite.py

import tensorflow as tf
import numpy as np
import time
import os

def benchmark_tflite_model(tflite_path, num_runs=100):
    """
    测试 TFLite 模型推理性能
    """
    # 加载模型
    interpreter = tf.lite.Interpreter(model_path=tflite_path)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    # 准备输入数据
    input_shape = input_details[0]['shape']
    test_input = np.random.rand(*input_shape).astype(np.float32)
    
    # 预热
    for _ in range(10):
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()
    
    # 基准测试
    times = []
    for _ in range(num_runs):
        start = time.perf_counter()
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()
        end = time.perf_counter()
        times.append((end - start) * 1000)  # 转换为毫秒
    
    print(f"模型: {os.path.basename(tflite_path)}")
    print(f"  平均推理时间: {np.mean(times):.2f} ms")
    print(f"  最小推理时间: {np.min(times):.2f} ms")
    print(f"  最大推理时间: {np.max(times):.2f} ms")
    print(f"  标准差: {np.std(times):.2f} ms")
    print(f"  模型大小: {os.path.getsize(tflite_path) / 1024:.2f} KB")
    print("-" * 50)

# 运行基准测试
benchmark_tflite_model('tflite/scene_classifier.tflite')
benchmark_tflite_model('tflite/quality_analyzer.tflite')
benchmark_tflite_model('tflite/param_predictor.tflite')
```

---

## 常见问题

### Q1: 训练时 GPU 内存不足

**解决方案:**
```python
# 减小 batch_size
CONFIG['batch_size'] = 16  # 从 32 减小到 16

# 或使用混合精度训练
from tensorflow.keras import mixed_precision
mixed_precision.set_global_policy('mixed_float16')
```

### Q2: 量化后精度下降严重

**解决方案:**
```python
# 增加代表性数据集大小
def representative_dataset():
    # 使用更多真实数据
    for i in range(1000):  # 从 500 增加到 1000
        # 加载真实训练数据
        data = load_real_training_sample(i)
        yield [data]

# 或使用更宽松的量化设置
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
    tf.lite.OpsSet.TFLITE_BUILTINS  # 允许回退到浮点
]
```

### Q3: TFLite 模型在 Android 上加载失败

**解决方案:**
```kotlin
// 检查模型文件是否正确放置
val modelFile = File(context.filesDir, "models/scene_classifier.tflite")
if (!modelFile.exists()) {
    // 从 assets 复制模型
    copyModelFromAssets(context, "models/scene_classifier.tflite")
}

// 使用正确的 TFLite 版本
// build.gradle.kts
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
```

### Q4: 如何更新模型而不重新训练

**解决方案:**
```python
# 微调现有模型
model = tf.keras.models.load_model('models/scene_classifier.h5')

# 解冻部分层进行微调
for layer in model.layers[-20:]:
    layer.trainable = True

# 使用较小的学习率
model.compile(
    optimizer=tf.keras.optimizers.Adam(learning_rate=0.0001),
    loss='categorical_crossentropy',
    metrics=['accuracy']
)

# 使用新数据微调
model.fit(new_train_data, epochs=10)
```

### Q5: 如何处理不平衡的数据集

**解决方案:**
```python
from sklearn.utils import class_weight
import numpy as np

# 计算类别权重
class_weights = class_weight.compute_class_weight(
    class_weight='balanced',
    classes=np.unique(train_generator.classes),
    y=train_generator.classes
)
class_weights_dict = dict(enumerate(class_weights))

# 在训练中使用类别权重
model.fit(
    train_generator,
    epochs=CONFIG['epochs'],
    validation_data=val_generator,
    class_weight=class_weights_dict
)
```

---

## 附录

### A. 数据增强策略

```python
import albumentations as A

# 场景分类数据增强
scene_augmentation = A.Compose([
    A.RandomResizedCrop(224, 224, scale=(0.8, 1.0)),
    A.HorizontalFlip(p=0.5),
    A.Rotate(limit=15, p=0.5),
    A.ColorJitter(brightness=0.2, contrast=0.2, saturation=0.2, hue=0.1, p=0.5),
    A.GaussianBlur(blur_limit=(3, 7), p=0.3),
    A.GaussNoise(var_limit=(10.0, 50.0), p=0.3),
    A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

# 质量分析数据增强（保守）
quality_augmentation = A.Compose([
    A.Resize(224, 224),
    A.HorizontalFlip(p=0.5),
    A.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])
```

### B. 模型架构可视化

```python
# 安装依赖
# pip install pydot graphviz

from tensorflow.keras.utils import plot_model

# 可视化模型架构
model = build_model()
plot_model(
    model,
    to_file='model_architecture.png',
    show_shapes=True,
    show_layer_names=True,
    rankdir='TB',
    expand_nested=True,
    dpi=96
)
```

### C. TensorBoard 监控

```bash
# 启动 TensorBoard
tensorboard --logdir=logs/

# 在浏览器中打开
# http://localhost:6006
```

---

## 联系方式

如有问题或建议，请联系：
- 项目地址: https://github.com/silas/omaster
- 问题反馈: https://github.com/silas/omaster/issues