package com.mocharealm.compound.ui.util

import android.graphics.RuntimeShader
import org.intellij.lang.annotations.Language

object SpoilerShader {
    @Language("AGSL")
    const val SHADER_CODE = """
uniform float2 resolution;
uniform float time;
uniform half4 particleColor;

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

float2 getFlow(float2 p, float t) {
    float freq = 0.01;
    float speed = 0.5;
    float n1 = sin(p.y * freq + t * speed);
    float n2 = cos(p.x * freq + t * speed);
    return float2(n1, n2);
}

half4 main(float2 coord) {
    float spacing = 15.0;
    float2 currentGrid = floor(coord / spacing);
    
    float intensity = 0.0;
    
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            float2 gridId = currentGrid + float2(x, y);
            
            float seed = hash(gridId);
            
            float t = fract(time * 0.2 + seed);
            float alpha_pulse = sin(t * 3.14159); 
            
            float2 origin = (gridId + 0.5) * spacing;
            float2 randOffset = (float2(hash(gridId + 4.0), hash(gridId + 9.0)) - 0.5) * spacing;
            
            float2 flow = getFlow(origin, time);
            float2 position = origin + randOffset + flow * 40.0 * t; 
            
            float dist = distance(coord, position);
            float r = 1.2 + seed * 1.8;
            
            intensity += smoothstep(r, r - 1.0, dist) * alpha_pulse * (0.3 + 0.7 * seed);
        }
    }

    intensity = clamp(intensity, 0.0, 1.2);
    return half4(particleColor.rgb * intensity, intensity);
}
"""

    fun getShader(): RuntimeShader {
        return RuntimeShader(SHADER_CODE)
    }
}
