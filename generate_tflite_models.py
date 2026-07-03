#!/usr/bin/env python3
"""
Generate minimal valid TFLite model files for OMaster Android project.

Constructs TFLite FlatBuffer binaries directly using the flatbuffers library,
without requiring TensorFlow or the TFLite schema Python bindings.

Model architectures:
  - Image models (scene_classifier, quality_analyzer):
      MEAN (global avg pool over spatial dims) + FULLY_CONNECTED
      [1, 224, 224, 3] --MEAN--> [1, 3] --FC--> [1, output_dim]

  - Param predictor:
      FULLY_CONNECTED only
      [1, 40] --FC--> [1, 18]
"""

import os
import struct
import numpy as np
import flatbuffers

OUTPUT_DIR = "/workspace/app/src/main/assets/models"

# ============================================================
# TFLite Schema Constants
# Source: tensorflow/lite/schema/schema.fbs
# ============================================================

# BuiltinOperator enum values
BUILTIN_MEAN = 40
BUILTIN_FULLY_CONNECTED = 9

# BuiltinOptions union member indices
OPTIONS_NONE = 0
OPTIONS_FULLY_CONNECTED = 8
OPTIONS_REDUCER = 27

# TensorType enum values
TENSOR_FLOAT32 = 0
TENSOR_INT32 = 2

# ActivationFunctionType enum values
ACT_NONE = 0


# ============================================================
# FlatBuffer Vector Helpers
# ============================================================

def make_int32_vector(builder, values):
    """Create a FlatBuffer vector of int32 values."""
    builder.StartVector(4, len(values), 4)
    for v in reversed(values):
        builder.PrependInt32(v)
    return builder.EndVector()


def make_uint8_vector(builder, data):
    """Create a FlatBuffer vector of uint8 values from bytes."""
    builder.StartVector(1, len(data), 1)
    for b in reversed(data):
        builder.PrependByte(b)
    return builder.EndVector()


def make_offset_vector(builder, offsets):
    """Create a FlatBuffer vector of table offsets."""
    builder.StartVector(4, len(offsets), 4)
    for off in reversed(offsets):
        builder.PrependUOffsetTRelative(off)
    return builder.EndVector()


# ============================================================
# TFLite Schema Table Builders
# Each builder follows the FlatBuffer convention:
#   1. Create child objects (vectors, strings, sub-tables) FIRST
#   2. Start the table with StartObject
#   3. Add fields with PrependXxxSlot
#   4. End the table with EndObject
# ============================================================

def build_buffer(builder, data_vec_offset=0):
    """
    Build a TFLite Buffer table.
    table Buffer { data: [ubyte]; }
    Slot 0 = data
    """
    builder.StartObject(1)
    if data_vec_offset:
        builder.PrependUOffsetTRelativeSlot(0, data_vec_offset, 0)
    return builder.EndObject()


def build_operator_code(builder, builtin_code, version=1):
    """
    Build a TFLite OperatorCode table.
    table OperatorCode {
        deprecated_builtin_code: byte;   // slot 0
        custom_code: string;             // slot 1
        version: int;                    // slot 2
        builtin_code: BuiltinOperator;   // slot 3 (added in v3a)
    }
    """
    builder.StartObject(4)
    builder.PrependByteSlot(0, builtin_code, 0)   # deprecated_builtin_code
    # custom_code (slot 1) not set - defaults to null
    builder.PrependInt32Slot(2, version, 0)         # version (force write)
    builder.PrependInt32Slot(3, builtin_code, 0)    # builtin_code (v3a+)
    return builder.EndObject()


def build_tensor(builder, shape, dtype, buffer_idx, name_offset):
    """
    Build a TFLite Tensor table.
    table Tensor {
        shape: [int];                        // slot 0
        type: TensorType;                    // slot 1
        buffer: int;                         // slot 2
        name: string;                        // slot 3
        quantization: QuantizationParameters; // slot 4
    }
    """
    shape_vec = make_int32_vector(builder, shape)
    builder.StartObject(5)
    builder.PrependUOffsetTRelativeSlot(0, shape_vec, 0)   # shape
    builder.PrependByteSlot(1, dtype, 0)                    # type
    builder.PrependInt32Slot(2, buffer_idx, 0)              # buffer
    builder.PrependUOffsetTRelativeSlot(3, name_offset, 0)  # name
    # quantization (slot 4) not set - defaults to null
    return builder.EndObject()


def build_operator(builder, opcode_idx, inputs, outputs,
                   options_type=OPTIONS_NONE, options_offset=0):
    """
    Build a TFLite Operator table.
    table Operator {
        opcode_index: int;                // slot 0
        inputs: [int];                    // slot 1
        outputs: [int];                   // slot 2
        builtin_options_type: BuiltinOptions; // slot 3
        builtin_options: union;           // slot 4
    }
    """
    inputs_vec = make_int32_vector(builder, inputs)
    outputs_vec = make_int32_vector(builder, outputs)
    builder.StartObject(5)
    builder.PrependInt32Slot(0, opcode_idx, 0)                   # opcode_index
    builder.PrependUOffsetTRelativeSlot(1, inputs_vec, 0)        # inputs
    builder.PrependUOffsetTRelativeSlot(2, outputs_vec, 0)       # outputs
    builder.PrependByteSlot(3, options_type, 0)                  # builtin_options_type
    if options_offset:
        builder.PrependUOffsetTRelativeSlot(4, options_offset, 0)  # builtin_options
    return builder.EndObject()


def build_reducer_options(builder, keep_dims=False):
    """
    Build TFLite ReducerOptions table.
    table ReducerOptions { keep_dims: bool; }  // slot 0
    """
    builder.StartObject(1)
    builder.PrependBoolSlot(0, keep_dims, False)
    return builder.EndObject()


def build_fully_connected_options(builder, activation=ACT_NONE,
                                  keep_num_dims=False):
    """
    Build TFLite FullyConnectedOptions table.
    table FullyConnectedOptions {
        fused_activation_function: ActivationFunctionType; // slot 0
        weights_format: FullyConnectedOptionsWeightsFormat; // slot 1
        keep_num_dims: bool;                               // slot 2
        asymmetric_quantize_inputs: bool;                  // slot 3
    }
    """
    builder.StartObject(4)
    builder.PrependByteSlot(0, activation, 0)         # fused_activation_function
    # weights_format (slot 1) defaults to 0 (DEFAULT)
    builder.PrependBoolSlot(2, keep_num_dims, False)   # keep_num_dims
    # asymmetric_quantize_inputs (slot 3) defaults to false
    return builder.EndObject()


def build_subgraph(builder, tensors_vec, inputs_vec, outputs_vec,
                   operators_vec, name_offset=0):
    """
    Build a TFLite SubGraph table.
    table SubGraph {
        tensors: [Tensor];   // slot 0
        inputs: [int];       // slot 1
        outputs: [int];      // slot 2
        operators: [Operator]; // slot 3
        name: string;        // slot 4
    }
    """
    builder.StartObject(5)
    builder.PrependUOffsetTRelativeSlot(0, tensors_vec, 0)    # tensors
    builder.PrependUOffsetTRelativeSlot(1, inputs_vec, 0)     # inputs
    builder.PrependUOffsetTRelativeSlot(2, outputs_vec, 0)    # outputs
    builder.PrependUOffsetTRelativeSlot(3, operators_vec, 0)  # operators
    if name_offset:
        builder.PrependUOffsetTRelativeSlot(4, name_offset, 0)  # name
    return builder.EndObject()


def build_model(builder, opcodes_vec, subgraphs_vec, desc_offset,
                buffers_vec, version=3):
    """
    Build a TFLite Model table (root table).
    table Model {
        version: int = 3;              // slot 0
        operator_codes: [OperatorCode]; // slot 1
        subgraphs: [SubGraph];         // slot 2
        description: string;           // slot 3
        buffers: [Buffer];            // slot 4
    }
    """
    builder.StartObject(5)
    # Use default=0 to force writing version (TFLite runtime may not
    # correctly fall back to the schema default of 3 when field is absent)
    builder.PrependInt32Slot(0, version, 0)                        # version
    builder.PrependUOffsetTRelativeSlot(1, opcodes_vec, 0)        # operator_codes
    builder.PrependUOffsetTRelativeSlot(2, subgraphs_vec, 0)      # subgraphs
    builder.PrependUOffsetTRelativeSlot(3, desc_offset, 0)        # description
    builder.PrependUOffsetTRelativeSlot(4, buffers_vec, 0)        # buffers
    return builder.EndObject()


# ============================================================
# Model Generation Functions
# ============================================================

def create_image_model(input_shape, output_dim, output_path, model_name):
    """
    Create a minimal TFLite model for image inputs.
    Architecture: MEAN (global avg pool) + FULLY_CONNECTED

    Graph:
        input [1, H, W, C]
          |
        MEAN (axes=[1,2], keep_dims=false)
          |
        mean_output [1, C]
          |
        FULLY_CONNECTED (weights=[output_dim, C], bias=[output_dim])
          |
        output [1, output_dim]

    Tensors:
        0: input        [1, H, W, C]     FLOAT32  buf=0 (runtime)
        1: axes         [2]              INT32    buf=1 (data: [1, 2])
        2: mean_output  [1, C]           FLOAT32  buf=0 (runtime)
        3: fc_weight    [output_dim, C]  FLOAT32  buf=2 (zeros)
        4: fc_bias      [output_dim]     FLOAT32  buf=3 (zeros)
        5: output       [1, output_dim]  FLOAT32  buf=0 (runtime)

    Buffers:
        0: empty (runtime tensors)
        1: axes data [1, 2] as int32
        2: fc_weight data (output_dim * C float32 zeros)
        3: fc_bias data (output_dim float32 zeros)

    Operators:
        0: MEAN             opcode=0  inputs=[0,1]  outputs=[2]
        1: FULLY_CONNECTED  opcode=1  inputs=[2,3,4] outputs=[5]
    """
    _, H, W, C = input_shape
    builder = flatbuffers.Builder(8192)

    # --- Phase 1: Create ALL strings (must precede tables that reference them) ---
    s_input = builder.CreateString("input")
    s_axes = builder.CreateString("reduction_indices")
    s_mean_out = builder.CreateString("mean_output")
    s_fc_w = builder.CreateString("fc_weight")
    s_fc_b = builder.CreateString("fc_bias")
    s_output = builder.CreateString("output")
    s_subgraph = builder.CreateString("main")
    s_desc = builder.CreateString(f"OMaster {model_name} placeholder")

    # --- Phase 2: Create buffer data vectors ---
    axes_bytes = np.array([1, 2], dtype=np.int32).tobytes()
    fc_weight_bytes = np.zeros(output_dim * C, dtype=np.float32).tobytes()
    fc_bias_bytes = np.zeros(output_dim, dtype=np.float32).tobytes()

    bv_axes = make_uint8_vector(builder, axes_bytes)
    bv_fc_weight = make_uint8_vector(builder, fc_weight_bytes)
    bv_fc_bias = make_uint8_vector(builder, fc_bias_bytes)

    # --- Phase 3: Build inner tables (bottom-up: children before parents) ---

    # Buffers
    buf0 = build_buffer(builder)                    # empty (runtime)
    buf1 = build_buffer(builder, bv_axes)          # axes data
    buf2 = build_buffer(builder, bv_fc_weight)     # FC weights
    buf3 = build_buffer(builder, bv_fc_bias)       # FC bias

    # BuiltinOptions tables
    reducer_opts = build_reducer_options(builder, keep_dims=False)
    fc_opts = build_fully_connected_options(builder, keep_num_dims=False)

    # OperatorCodes
    oc_mean = build_operator_code(builder, BUILTIN_MEAN, version=1)
    oc_fc = build_operator_code(builder, BUILTIN_FULLY_CONNECTED, version=1)

    # Tensors (built in any order; collected into offset vector later)
    t5 = build_tensor(builder, [1, output_dim], TENSOR_FLOAT32, 0, s_output)
    t4 = build_tensor(builder, [output_dim], TENSOR_FLOAT32, 3, s_fc_b)
    t3 = build_tensor(builder, [output_dim, C], TENSOR_FLOAT32, 2, s_fc_w)
    t2 = build_tensor(builder, [1, C], TENSOR_FLOAT32, 0, s_mean_out)
    t1 = build_tensor(builder, [2], TENSOR_INT32, 1, s_axes)
    t0 = build_tensor(builder, [1, H, W, C], TENSOR_FLOAT32, 0, s_input)

    # Operators
    op1 = build_operator(builder, 1, [2, 3, 4], [5],
                         OPTIONS_FULLY_CONNECTED, fc_opts)
    op0 = build_operator(builder, 0, [0, 1], [2],
                         OPTIONS_REDUCER, reducer_opts)

    # --- Phase 4: Build vectors of offsets ---
    operators_vec = make_offset_vector(builder, [op0, op1])
    tensors_vec = make_offset_vector(builder, [t0, t1, t2, t3, t4, t5])
    opcodes_vec = make_offset_vector(builder, [oc_mean, oc_fc])
    buffers_vec = make_offset_vector(builder, [buf0, buf1, buf2, buf3])

    # SubGraph scalar vectors (inputs/outputs tensor indices)
    sg_inputs = make_int32_vector(builder, [0])
    sg_outputs = make_int32_vector(builder, [5])

    # --- Phase 5: Build outer tables ---
    subgraph = build_subgraph(builder, tensors_vec, sg_inputs,
                              sg_outputs, operators_vec, s_subgraph)
    subgraphs_vec = make_offset_vector(builder, [subgraph])

    model = build_model(builder, opcodes_vec, subgraphs_vec,
                        s_desc, buffers_vec, version=3)

    # --- Phase 6: Finish the FlatBuffer ---
    builder.Finish(model, b'TFL3')
    buf = builder.Output()

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'wb') as f:
        f.write(bytes(buf))

    print(f"  Created: {output_path} ({len(buf)} bytes)")


def create_fc_model(input_dim, output_dim, output_path, model_name):
    """
    Create a minimal TFLite model with a single FULLY_CONNECTED operator.
    Architecture: FULLY_CONNECTED only

    Graph:
        input [1, input_dim]
          |
        FULLY_CONNECTED (weights=[output_dim, input_dim], bias=[output_dim])
          |
        output [1, output_dim]

    Tensors:
        0: input      [1, input_dim]       FLOAT32  buf=0 (runtime)
        1: fc_weight  [output_dim, input_dim] FLOAT32  buf=1 (zeros)
        2: fc_bias    [output_dim]         FLOAT32  buf=2 (zeros)
        3: output     [1, output_dim]      FLOAT32  buf=0 (runtime)

    Buffers:
        0: empty (runtime tensors)
        1: fc_weight data (output_dim * input_dim float32 zeros)
        2: fc_bias data (output_dim float32 zeros)

    Operators:
        0: FULLY_CONNECTED  opcode=0  inputs=[0,1,2]  outputs=[3]
    """
    builder = flatbuffers.Builder(8192)

    # --- Phase 1: Strings ---
    s_input = builder.CreateString("input")
    s_fc_w = builder.CreateString("fc_weight")
    s_fc_b = builder.CreateString("fc_bias")
    s_output = builder.CreateString("output")
    s_subgraph = builder.CreateString("main")
    s_desc = builder.CreateString(f"OMaster {model_name} placeholder")

    # --- Phase 2: Buffer data ---
    fc_weight_bytes = np.zeros(output_dim * input_dim, dtype=np.float32).tobytes()
    fc_bias_bytes = np.zeros(output_dim, dtype=np.float32).tobytes()

    bv_fc_weight = make_uint8_vector(builder, fc_weight_bytes)
    bv_fc_bias = make_uint8_vector(builder, fc_bias_bytes)

    # --- Phase 3: Inner tables ---
    buf0 = build_buffer(builder)
    buf1 = build_buffer(builder, bv_fc_weight)
    buf2 = build_buffer(builder, bv_fc_bias)

    fc_opts = build_fully_connected_options(builder, keep_num_dims=False)

    oc_fc = build_operator_code(builder, BUILTIN_FULLY_CONNECTED, version=1)

    t3 = build_tensor(builder, [1, output_dim], TENSOR_FLOAT32, 0, s_output)
    t2 = build_tensor(builder, [output_dim], TENSOR_FLOAT32, 2, s_fc_b)
    t1 = build_tensor(builder, [output_dim, input_dim], TENSOR_FLOAT32, 1, s_fc_w)
    t0 = build_tensor(builder, [1, input_dim], TENSOR_FLOAT32, 0, s_input)

    op0 = build_operator(builder, 0, [0, 1, 2], [3],
                         OPTIONS_FULLY_CONNECTED, fc_opts)

    # --- Phase 4: Offset vectors ---
    operators_vec = make_offset_vector(builder, [op0])
    tensors_vec = make_offset_vector(builder, [t0, t1, t2, t3])
    opcodes_vec = make_offset_vector(builder, [oc_fc])
    buffers_vec = make_offset_vector(builder, [buf0, buf1, buf2])

    sg_inputs = make_int32_vector(builder, [0])
    sg_outputs = make_int32_vector(builder, [3])

    # --- Phase 5: Outer tables ---
    subgraph = build_subgraph(builder, tensors_vec, sg_inputs,
                              sg_outputs, operators_vec, s_subgraph)
    subgraphs_vec = make_offset_vector(builder, [subgraph])

    model = build_model(builder, opcodes_vec, subgraphs_vec,
                        s_desc, buffers_vec, version=3)

    # --- Phase 6: Finish ---
    builder.Finish(model, b'TFL3')
    buf = builder.Output()

    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'wb') as f:
        f.write(bytes(buf))

    print(f"  Created: {output_path} ({len(buf)} bytes)")


# ============================================================
# Verification
# ============================================================

def verify_tflite_file(filepath, expected_input_shape, expected_output_shape):
    """Basic verification of a generated TFLite file."""
    if not os.path.exists(filepath):
        print(f"  FAIL: {filepath} does not exist")
        return False

    with open(filepath, 'rb') as f:
        data = f.read()

    # Check minimum size (at least the FlatBuffer header)
    if len(data) < 8:
        print(f"  FAIL: {filepath} too small ({len(data)} bytes)")
        return False

    # Check file identifier "TFL3" at offset 4-7
    file_id = data[4:8]
    if file_id != b'TFL3':
        print(f"  FAIL: {filepath} has wrong file identifier: {file_id!r}")
        return False

    # Check root table offset (first 4 bytes, uint32 LE)
    root_offset = struct.unpack('<I', data[0:4])[0]
    if root_offset == 0 or root_offset >= len(data):
        print(f"  FAIL: {filepath} has invalid root offset: {root_offset}")
        return False

    print(f"  OK: {filepath} ({len(data)} bytes, TFL3 identifier valid)")
    return True


# ============================================================
# Main
# ============================================================

def main():
    print("Generating minimal TFLite model files for OMaster...")
    print(f"Output directory: {OUTPUT_DIR}")
    print()

    # 1. Scene classifier: [1, 224, 224, 3] -> [1, 11]
    print("[1/3] scene_classifier (MEAN + FC): "
          "[1,224,224,3] -> [1,11]")
    create_image_model(
        input_shape=(1, 224, 224, 3),
        output_dim=11,
        output_path=os.path.join(OUTPUT_DIR, "scene_classifier.tflite"),
        model_name="scene_classifier",
    )

    # 2. Quality analyzer: [1, 224, 224, 3] -> [1, 4]
    print("[2/3] quality_analyzer (MEAN + FC): "
          "[1,224,224,3] -> [1,4]")
    create_image_model(
        input_shape=(1, 224, 224, 3),
        output_dim=4,
        output_path=os.path.join(OUTPUT_DIR, "quality_analyzer.tflite"),
        model_name="quality_analyzer",
    )

    # 3. Param predictor: [1, 40] -> [1, 18]
    print("[3/3] param_predictor (FC): "
          "[1,40] -> [1,18]")
    create_fc_model(
        input_dim=40,
        output_dim=18,
        output_path=os.path.join(OUTPUT_DIR, "param_predictor.tflite"),
        model_name="param_predictor",
    )

    # Verify
    print()
    print("Verifying generated models...")
    all_ok = True
    all_ok &= verify_tflite_file(
        os.path.join(OUTPUT_DIR, "scene_classifier.tflite"),
        [1, 224, 224, 3], [1, 11])
    all_ok &= verify_tflite_file(
        os.path.join(OUTPUT_DIR, "quality_analyzer.tflite"),
        [1, 224, 224, 3], [1, 4])
    all_ok &= verify_tflite_file(
        os.path.join(OUTPUT_DIR, "param_predictor.tflite"),
        [1, 40], [1, 18])

    if all_ok:
        print()
        print("All model files generated and verified successfully!")
    else:
        print()
        print("WARNING: Some model files failed verification!")

    # Print summary
    print()
    print("Summary:")
    for name in ["scene_classifier.tflite", "quality_analyzer.tflite",
                 "param_predictor.tflite"]:
        path = os.path.join(OUTPUT_DIR, name)
        if os.path.exists(path):
            size = os.path.getsize(path)
            print(f"  {name}: {size} bytes")
        else:
            print(f"  {name}: MISSING!")


if __name__ == "__main__":
    main()
