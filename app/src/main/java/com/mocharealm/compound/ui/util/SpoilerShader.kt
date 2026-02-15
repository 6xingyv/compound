package com.mocharealm.compound.ui.util

import android.graphics.RuntimeShader
import org.intellij.lang.annotations.Language

object SpoilerShader {
    @Language("AGSL")
    const val SHADER_CODE = """
    uniform float2 resolution;
    uniform float time;
    uniform half4 particleColor; // 新增：从 Compose 传入的颜色
    
    float hash(float2 p) {
        p = fract(p * float2(123.34, 456.21));
        p += dot(p, p + 45.32);
        return fract(p.x * p.y);
    }

    float noise(float2 p) {
        float2 i = floor(p);
        float2 f = fract(p);
        float2 u = f * f * (3.0 - 2.0 * f);
        float a = hash(i + float2(0.0, 0.0));
        float b = hash(i + float2(1.0, 0.0));
        float c = hash(i + float2(0.0, 1.0));
        float d = hash(i + float2(1.0, 1.0));
        return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
    }

    half4 main(float2 coord) {
        float2 pixelBlock = floor(coord / 2.5); 
        float n1 = noise(coord * 0.01 + float2(time * 0.5, time * 0.2));
        float dust = smoothstep(0.5, 1.0, n1) * 0.4; 
        
        float timeStep = floor(time * 12.0);
        float n2 = hash(pixelBlock + float2(timeStep, -timeStep));
        float sparkle = step(0.97, n2) * n1;
        
        // 计算粒子的绝对密度 (Alpha)
        float alpha = clamp(dust + sparkle * 1.5, 0.0, 1.0);
        
        // 【关键】：Compose 要求预乘 Alpha (Premultiplied Alpha)
        // RGB 通道必须乘以计算出的 Alpha，否则会出现黑色或者奇怪的光晕
        return half4(particleColor.rgb * alpha, alpha);
    }
    """

    fun getShader(): RuntimeShader {
        return RuntimeShader(SHADER_CODE)
    }
}
