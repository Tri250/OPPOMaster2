package com.silas.omaster.renderer

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * 着色器程序管理类
 * 负责GLSL着色器的编译、链接和管理
 * 
 * 使用OpenGL ES 3.0特性
 */
class ShaderProgram private constructor(
    val programId: Int,
    val vertexShaderId: Int,
    val fragmentShaderId: Int
) {
    companion object {
        private const val TAG = "ShaderProgram"
        
        /**
         * 从assets创建着色器程序
         * @param context Android上下文
         * @param vertexShaderFile 顶点着色器文件名（相对于assets/shaders/）
         * @param fragmentShaderFile 片段着色器文件名（相对于assets/shaders/）
         * @return 着色器程序实例，失败返回null
         */
        fun createFromAssets(
            context: Context,
            vertexShaderFile: String,
            fragmentShaderFile: String
        ): ShaderProgram? {
            val vertexShaderSource = loadShaderFromAssets(context, "shaders/$vertexShaderFile")
            val fragmentShaderSource = loadShaderFromAssets(context, "shaders/$fragmentShaderFile")
            
            if (vertexShaderSource == null || fragmentShaderSource == null) {
                Log.e(TAG, "Failed to load shader sources")
                return null
            }
            
            return create(vertexShaderSource, fragmentShaderSource)
        }
        
        /**
         * 从源码创建着色器程序
         * @param vertexShaderSource 顶点着色器源码
         * @param fragmentShaderSource 片段着色器源码
         * @return 着色器程序实例，失败返回null
         */
        fun create(
            vertexShaderSource: String,
            fragmentShaderSource: String
        ): ShaderProgram? {
            // 编译顶点着色器
            val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderSource)
            if (vertexShader == 0) {
                Log.e(TAG, "Failed to compile vertex shader")
                return null
            }
            
            // 编译片段着色器
            val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderSource)
            if (fragmentShader == 0) {
                Log.e(TAG, "Failed to compile fragment shader")
                GLES30.glDeleteShader(vertexShader)
                return null
            }
            
            // 链接程序
            val program = linkProgram(vertexShader, fragmentShader)
            if (program == 0) {
                Log.e(TAG, "Failed to link program")
                GLES30.glDeleteShader(vertexShader)
                GLES30.glDeleteShader(fragmentShader)
                return null
            }
            
            return ShaderProgram(program, vertexShader, fragmentShader)
        }
        
        /**
         * 从assets加载着色器源码
         */
        private fun loadShaderFromAssets(context: Context, path: String): String? {
            return try {
                val inputStream = context.assets.open(path)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                
                reader.close()
                inputStream.close()
                
                stringBuilder.toString()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load shader from assets: $path", e)
                null
            }
        }
        
        /**
         * 编译着色器
         * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
         * @param source 着色器源码
         * @return 着色器ID，失败返回0
         */
        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES30.glCreateShader(type)
            if (shader == 0) {
                Log.e(TAG, "Failed to create shader object")
                return 0
            }
            
            // 加载着色器源码
            GLES30.glShaderSource(shader, source)
            
            // 编译着色器
            GLES30.glCompileShader(shader)
            
            // 检查编译状态
            val compileStatus = IntArray(1)
            GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
            
            if (compileStatus[0] == GLES30.GL_FALSE) {
                val log = GLES30.glGetShaderInfoLog(shader)
                Log.e(TAG, "Shader compilation failed: $log")
                GLES30.glDeleteShader(shader)
                return 0
            }
            
            return shader
        }
        
        /**
         * 链接着色器程序
         * @param vertexShader 顶点着色器ID
         * @param fragmentShader 片段着色器ID
         * @return 程序ID，失败返回0
         */
        private fun linkProgram(vertexShader: Int, fragmentShader: Int): Int {
            val program = GLES30.glCreateProgram()
            if (program == 0) {
                Log.e(TAG, "Failed to create program object")
                return 0
            }
            
            // 附加着色器
            GLES30.glAttachShader(program, vertexShader)
            GLES30.glAttachShader(program, fragmentShader)
            
            // 链接程序
            GLES30.glLinkProgram(program)
            
            // 检查链接状态
            val linkStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
            
            if (linkStatus[0] == GLES30.GL_FALSE) {
                val log = GLES30.glGetProgramInfoLog(program)
                Log.e(TAG, "Program linking failed: $log")
                GLES30.glDeleteProgram(program)
                return 0
            }
            
            // 验证程序
            GLES30.glValidateProgram(program)
            val validateStatus = IntArray(1)
            GLES30.glGetProgramiv(program, GLES30.GL_VALIDATE_STATUS, validateStatus, 0)
            
            if (validateStatus[0] == GLES30.GL_FALSE) {
                val log = GLES30.glGetProgramInfoLog(program)
                Log.w(TAG, "Program validation warning: $log")
            }
            
            return program
        }
    }
    
    // Uniform位置缓存（实例级别，避免多实例共享冲突）
    private val uniformLocationCache = mutableMapOf<String, Int>()
    
    /**
     * 使用此着色器程序
     */
    fun use() {
        GLES30.glUseProgram(programId)
    }
    
    /**
     * 获取uniform位置（带缓存）
     */
    fun getUniformLocation(name: String): Int {
        return uniformLocationCache.getOrPut(name) {
            val location = GLES30.glGetUniformLocation(programId, name)
            if (location == -1) {
                Log.w(TAG, "Uniform not found: $name")
            }
            location
        }
    }
    
    /**
     * 获取attribute位置
     */
    fun getAttribLocation(name: String): Int {
        val location = GLES30.glGetAttribLocation(programId, name)
        if (location == -1) {
            Log.w(TAG, "Attribute not found: $name")
        }
        return location
    }
    
    /**
     * 设置uniform值 - float
     */
    fun setUniform1f(name: String, value: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform1f(location, value)
        }
    }
    
    /**
     * 设置uniform值 - int
     */
    fun setUniform1i(name: String, value: Int) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform1i(location, value)
        }
    }
    
    /**
     * 设置uniform值 - vec2
     */
    fun setUniform2f(name: String, x: Float, y: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform2f(location, x, y)
        }
    }
    
    /**
     * 设置uniform值 - vec3
     */
    fun setUniform3f(name: String, x: Float, y: Float, z: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform3f(location, x, y, z)
        }
    }
    
    /**
     * 设置uniform值 - vec4
     */
    fun setUniform4f(name: String, x: Float, y: Float, z: Float, w: Float) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform4f(location, x, y, z, w)
        }
    }
    
    /**
     * 设置uniform值 - float数组
     */
    fun setUniform1fv(name: String, values: FloatArray) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniform1fv(location, values.size, values, 0)
        }
    }
    
    /**
     * 设置uniform值 - mat4
     */
    fun setUniformMatrix4fv(name: String, matrix: FloatArray, transpose: Boolean = false) {
        val location = getUniformLocation(name)
        if (location != -1) {
            GLES30.glUniformMatrix4fv(location, 1, transpose, matrix, 0)
        }
    }
    
    /**
     * 设置渲染参数到着色器
     */
    fun setRenderParameters(params: RenderParameters) {
        // 设置各个参数
        setUniform1f("uSaturation", params.saturation / 100f)
        setUniform1f("uContrast", params.contrast / 100f)
        setUniform1f("uBrightness", params.brightness / 100f)
        setUniform1f("uWarmth", params.warmth / 100f)
        setUniform1f("uSharpness", params.sharpness / 100f)
        setUniform1f("uClarity", params.clarity / 100f)
        setUniform1f("uVibrance", params.vibrance / 100f)
        setUniform1f("uHighlights", params.highlights / 100f)
        setUniform1f("uShadows", params.shadows / 100f)
        setUniform1f("uWhites", params.whites / 100f)
        setUniform1f("uBlacks", params.blacks / 100f)
        setUniform1f("uGrain", params.grain / 100f)
        setUniform1f("uFade", params.fade / 100f)
        setUniform1f("uDehaze", params.dehaze / 100f)
        setUniform1f("uDenoise", params.denoise / 100f)
        setUniform1f("uSkinSmooth", params.skinSmooth / 100f)
        setUniform1f("uExposure", params.exposure / 100f)
        setUniform1f("uTexture", params.texture / 100f)
    }
    
    /**
     * 清除uniform缓存
     */
    fun clearUniformCache() {
        uniformLocationCache.clear()
    }
    
    /**
     * 释放着色器程序资源
     */
    fun release() {
        clearUniformCache()
        GLES30.glDetachShader(programId, vertexShaderId)
        GLES30.glDetachShader(programId, fragmentShaderId)
        GLES30.glDeleteShader(vertexShaderId)
        GLES30.glDeleteShader(fragmentShaderId)
        GLES30.glDeleteProgram(programId)
    }
}