package com.mocharealm.compound.ui.composable.experimental

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val METABALL = """
uniform float2 iResolution;
uniform float iTime;

// 平滑并集函数，产生熔岩灯的粘稠融合感
float smin(float a, float b, float k) {
    float h = max(k - abs(a - b), 0.0) / k;
    return min(a, b) - h * h * k * 0.25;
}

// 简单的伪随机函数
float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution.xy;
    float aspect = iResolution.x / iResolution.y;
    float2 p = uv * float2(aspect, 1.0);
    float dist = 1e10;
    
    for (int i = 0; i < 10; i++) {
        float index = float(i);
        float radius = 0.05 + 0.05 * hash(index + 134.418);
        float xOffset = 0.2 + 0.6 * hash(index + 567.89);
        float xPos = xOffset * aspect + 0.1 * sin(iTime * 0.5 + index);
        float yPos = fract(iTime * (0.2 + 0.2 * hash(index * 0.1)) + hash(index)) * 1.3 - 0.15;
        dist = smin(dist, distance(p, float2(xPos, yPos)) - radius, 0.05);
    }
    
    float data = clamp(-dist / 0.15, 0.0, 1.0);
    return half4(data, 0.0, 0.0, 1.0); 
}
    """

@Language("AGSL")
private const val GLASS = """
uniform shader content;
uniform shader sdfTexture;

uniform float2 size;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float chromaticAberration;

uniform float lightAngle;
uniform float lightFalloff;
uniform float lightIntensity;

float2 getGradient(float2 coord) {
float e = 1.0;
float f0 = sdfTexture.eval(coord).r;
float fx = sdfTexture.eval(coord + float2(e, 0)).r;
float fy = sdfTexture.eval(coord + float2(0, e)).r;
return normalize(float2(f0 - fx, f0 - fy));
}
float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float sd = sdfTexture.eval(coord).r;
    float2 halfSize = size * 0.5;
    if (sd < 0.01) {
        return content.eval(coord);
    }
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);
    
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float2 grad = normalize(getGradient(coord) + 1.0 * normalize(coord));
    
    float2 refractedCoord = coord + d * grad;
    float dispersionIntensity = chromaticAberration * ((coord.x * coord.y) / (halfSize.x * halfSize.y));
    float2 dispersedCoord = d * grad * dispersionIntensity;
    
    half4 color = half4(0.0);
    
    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;
    
    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;
    
    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;
    
    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;
    
    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;
    
    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;
    
    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;
    
    float2 lightDir = float2(cos(lightAngle), sin(lightAngle));
    float highlightDot = dot(grad, lightDir);
    float highlightAlpha = pow(abs(highlightDot), lightFalloff) * lightIntensity;
    
    color.rgb += half3(highlightAlpha);
    return color;
}
    """



@Composable
fun Glassifier(
    modifier: Modifier = Modifier,
    refractionHeight: Float = 10f,
    refractionAmount: Float = 20f,
    chromaticAberration: Float = 0f,
    lightAngle: Float = 0.785f,
    lightFalloff: Float = 8f,
    lightIntensity: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val time by produceState(0f) {
        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                value = (frameTime - startTime) / 1_000_000_0000f
            }
        }
    }

    val metaball = remember { RuntimeShader(METABALL) }
    val glass = remember { RuntimeShader(GLASS) }

    Box(
        modifier = modifier.graphicsLayer {
            metaball.setFloatUniform("iResolution", size.width, size.height)
            metaball.setFloatUniform("iTime", time)
            glass.setFloatUniform("size", size.width, size.height)
            glass.setFloatUniform("refractionHeight", refractionHeight)
            glass.setFloatUniform("refractionAmount", refractionAmount)
            glass.setFloatUniform("chromaticAberration", chromaticAberration)
            glass.setFloatUniform("lightAngle", lightAngle)
            glass.setFloatUniform("lightFalloff", lightFalloff)
            glass.setFloatUniform("lightIntensity", lightIntensity)
            glass.setInputShader("sdfTexture", metaball)
            renderEffect = RenderEffect.createRuntimeShaderEffect(
                glass,
                "content"
            ).asComposeRenderEffect()
            clip = true
            compositingStrategy = CompositingStrategy.Offscreen
        }
    ) {
        content()
    }
}