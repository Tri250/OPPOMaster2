#!/usr/bin/env python3
"""
创建三个轻量级 TFLite 模型文件
1. scene_classifier.tflite - 场景分类模型
2. quality_analyzer.tflite - 图像质量分析模型
3. param_predictor.tflite - 参数预测模型
"""

import os
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers

# 设置随机种子以确保可重复性
tf.random.set_seed(42)
np.random.seed(42)

# 输出目录
OUTPUT_DIR = "/workspace/app/src/main/assets/models/"
os.makedirs(OUTPUT_DIR, exist_ok=True)


def representative_dataset_gen(input_shape, num_samples=100):
    """生成代表性数据集用于 INT8 量化"""
    for _ in range(num_samples):
        yield [np.random.uniform(-1, 1, input_shape).astype(np.float32)]


def convert_to_tflite(model, input_shape, output_path, representative_data_gen=None):
    """将 Keras 模型转换为 INT8 量化的 TFLite 模型"""
    converter = tf.lite.TFLiteConverter.from_keras_model(model)

    # 启用优化
    converter.optimizations = [tf.lite.Optimize.DEFAULT]

    # 启用 INT8 量化
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]

    # 设置输入输出类型
    converter.inference_input_type = tf.float32
    converter.inference_output_type = tf.float32

    # 提供代表性数据集
    if representative_data_gen is None:
        representative_data_gen = lambda: representative_dataset_gen(input_shape)

    converter.representative_dataset = representative_data_gen

    # 转换模型
    tflite_model = converter.convert()

    # 保存模型
    with open(output_path, 'wb') as f:
        f.write(tflite_model)

    file_size_kb = len(tflite_model) / 1024
    print(f"✓ 模型已保存: {output_path}")
    print(f"  文件大小: {file_size_kb:.2f} KB")

    return file_size_kb


# ==================== 1. 场景分类模型 ====================
def create_scene_classifier():
    """
    场景分类模型 - 轻量级 CNN (优化后 ~700KB)
    输入: [1, 224, 224, 3] FLOAT32
    输出: [1, 36] FLOAT32 (softmax 概率分布)
    """
    print("\n" + "="*60)
    print("创建场景分类模型 (scene_classifier)")
    print("="*60)

    inputs = layers.Input(shape=(224, 224, 3), name="input")

    # 初始卷积层
    x = layers.Conv2D(8, 3, strides=2, padding='same', use_bias=False)(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU(max_value=6)(x)

    # 轻量级倒置残差块 - 减少通道数和层数
    # Block 1: 8 -> 16, stride 2
    x = inverted_residual_block(x, 8, 16, 2, 4)
    x = inverted_residual_block(x, 16, 16, 1, 2)

    # Block 2: 16 -> 24, stride 2
    x = inverted_residual_block(x, 16, 24, 2, 4)
    x = inverted_residual_block(x, 24, 24, 1, 3)

    # Block 3: 24 -> 32, stride 2
    x = inverted_residual_block(x, 24, 32, 2, 3)
    x = inverted_residual_block(x, 32, 32, 1, 3)

    # Block 4: 32 -> 64, stride 2
    x = inverted_residual_block(x, 32, 64, 2, 4)
    x = inverted_residual_block(x, 64, 64, 1, 4)

    # Block 5: 64 -> 96, stride 1
    x = inverted_residual_block(x, 64, 96, 1, 4)
    x = inverted_residual_block(x, 96, 96, 1, 4)

    # Block 6: 96 -> 128, stride 2
    x = inverted_residual_block(x, 96, 128, 2, 4)

    # 最终卷积层 (减少通道数)
    x = layers.Conv2D(256, 1, padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU(max_value=6)(x)

    # 全局平均池化
    x = layers.GlobalAveragePooling2D()(x)

    # Dropout
    x = layers.Dropout(0.2)(x)

    # 输出层: 36 个场景类别
    outputs = layers.Dense(36, activation='softmax', name="output")(x)

    model = keras.Model(inputs, outputs, name="scene_classifier")

    # 打印模型摘要
    model.summary()

    return model


def inverted_residual_block(inputs, in_ch, out_ch, stride, expansion_factor):
    """轻量级倒置残差块"""
    hidden_dim = in_ch * expansion_factor
    use_residual = stride == 1 and in_ch == out_ch

    x = inputs

    # 扩展层
    if expansion_factor != 1:
        x = layers.Conv2D(hidden_dim, 1, padding='same', use_bias=False)(x)
        x = layers.BatchNormalization()(x)
        x = layers.ReLU(max_value=6)(x)

    # 深度可分离卷积
    x = layers.DepthwiseConv2D(3, strides=stride, padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)
    x = layers.ReLU(max_value=6)(x)

    # 投影层
    x = layers.Conv2D(out_ch, 1, padding='same', use_bias=False)(x)
    x = layers.BatchNormalization()(x)

    # 残差连接
    if use_residual:
        x = layers.Add()([inputs, x])

    return x


# ==================== 2. 图像质量分析模型 ====================
def create_quality_analyzer():
    """
    图像质量分析模型 - 轻量级 CNN
    输入: [1, 224, 224, 3] FLOAT32
    输出: [1, 4] FLOAT32 (亮度、对比度、噪点、清晰度评分)
    """
    print("\n" + "="*60)
    print("创建图像质量分析模型 (quality_analyzer)")
    print("="*60)

    inputs = layers.Input(shape=(224, 224, 3), name="input")

    # 特征提取层 (更轻量的架构)
    x = layers.Conv2D(16, 3, strides=2, padding='same', activation='relu')(inputs)
    x = layers.MaxPooling2D(2)(x)

    x = layers.Conv2D(32, 3, strides=1, padding='same', activation='relu')(x)
    x = layers.MaxPooling2D(2)(x)

    x = layers.Conv2D(64, 3, strides=1, padding='same', activation='relu')(x)
    x = layers.MaxPooling2D(2)(x)

    x = layers.Conv2D(128, 3, strides=1, padding='same', activation='relu')(x)
    x = layers.GlobalAveragePooling2D()(x)

    # 全连接层
    x = layers.Dense(64, activation='relu')(x)
    x = layers.Dropout(0.3)(x)

    # 输出层: 4 个质量指标 (亮度、对比度、噪点、清晰度)
    # 使用 sigmoid 输出 0-1 范围的评分
    outputs = layers.Dense(4, activation='sigmoid', name="output")(x)

    model = keras.Model(inputs, outputs, name="quality_analyzer")

    # 打印模型摘要
    model.summary()

    return model


# ==================== 3. 参数预测模型 ====================
def create_param_predictor():
    """
    参数预测模型 - MLP 架构
    输入: [1, 40] FLOAT32 (36维场景特征 + 4维质量特征)
    输出: [1, 18] FLOAT32 (18个哈苏调校参数)
    """
    print("\n" + "="*60)
    print("创建参数预测模型 (param_predictor)")
    print("="*60)

    inputs = layers.Input(shape=(40,), name="input")

    # 隐藏层 1: 40 -> 64
    x = layers.Dense(64, activation='relu')(inputs)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)

    # 隐藏层 2: 64 -> 32
    x = layers.Dense(32, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(0.2)(x)

    # 输出层: 18 个哈苏调校参数
    # 使用 tanh 输出 -1 到 1 范围，便于后续映射到实际参数值
    outputs = layers.Dense(18, activation='tanh', name="output")(x)

    model = keras.Model(inputs, outputs, name="param_predictor")

    # 打印模型摘要
    model.summary()

    return model


def main():
    """主函数：创建并保存所有模型"""
    print("开始创建 TFLite 模型...")
    print(f"输出目录: {OUTPUT_DIR}")

    results = {}

    # 1. 创建场景分类模型
    scene_model = create_scene_classifier()
    scene_path = os.path.join(OUTPUT_DIR, "scene_classifier.tflite")
    results['scene_classifier'] = convert_to_tflite(
        scene_model,
        input_shape=(1, 224, 224, 3),
        output_path=scene_path
    )

    # 2. 创建图像质量分析模型
    quality_model = create_quality_analyzer()
    quality_path = os.path.join(OUTPUT_DIR, "quality_analyzer.tflite")
    results['quality_analyzer'] = convert_to_tflite(
        quality_model,
        input_shape=(1, 224, 224, 3),
        output_path=quality_path
    )

    # 3. 创建参数预测模型
    param_model = create_param_predictor()
    param_path = os.path.join(OUTPUT_DIR, "param_predictor.tflite")

    # 为参数预测模型创建特定的代表性数据集生成器
    def param_representative_dataset():
        for _ in range(100):
            yield [np.random.uniform(-1, 1, (1, 40)).astype(np.float32)]

    results['param_predictor'] = convert_to_tflite(
        param_model,
        input_shape=(1, 40),
        output_path=param_path,
        representative_data_gen=param_representative_dataset
    )

    # 打印汇总信息
    print("\n" + "="*60)
    print("模型创建完成！")
    print("="*60)
    for name, size_kb in results.items():
        print(f"  {name}: {size_kb:.2f} KB")
    print("="*60)

    # 验证模型
    print("\n验证模型...")
    verify_models()


def verify_models():
    """验证创建的 TFLite 模型"""
    model_files = [
        ("scene_classifier.tflite", (1, 224, 224, 3), (1, 36)),
        ("quality_analyzer.tflite", (1, 224, 224, 3), (1, 4)),
        ("param_predictor.tflite", (1, 40), (1, 18)),
    ]

    for filename, input_shape, output_shape in model_files:
        path = os.path.join(OUTPUT_DIR, filename)
        if not os.path.exists(path):
            print(f"✗ {filename} 不存在!")
            continue

        # 加载模型
        interpreter = tf.lite.Interpreter(model_path=path)
        interpreter.allocate_tensors()

        # 获取输入输出信息
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()

        print(f"\n{filename}:")
        print(f"  输入形状: {input_details[0]['shape']} (期望: {input_shape})")
        print(f"  输出形状: {output_details[0]['shape']} (期望: {output_shape})")
        print(f"  输入类型: {input_details[0]['dtype']}")
        print(f"  输出类型: {output_details[0]['dtype']}")

        # 测试推理
        test_input = np.random.uniform(-1, 1, input_shape).astype(np.float32)
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()
        test_output = interpreter.get_tensor(output_details[0]['index'])

        print(f"  测试输出形状: {test_output.shape}")
        print(f"  ✓ 模型验证通过")


if __name__ == "__main__":
    main()
