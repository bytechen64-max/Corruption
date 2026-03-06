#version 150

/*
 * corruption:ripple_lens
 * 屏幕后处理着色器
 *
 * 效果：
 *  1. 从屏幕中心向外扩散的多层波纹透镜畸变
 *  2. 屏幕边缘向中心扩散的脉动红色晕影
 */

uniform sampler2D DiffuseSampler;
uniform float     Time;        // 由 ClientEventHandler 每帧更新（秒）

in  vec2 texCoord;
in  vec2 oneTexel;
out vec4 fragColor;

// --------------------------------------------------------
// 平滑阶梯：在 [lo, hi] 区间内从 0 过渡到 1
// --------------------------------------------------------
float ease(float lo, float hi, float x) {
    float t = clamp((x - lo) / (hi - lo), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

void main() {
    vec2 uv     = texCoord;
    vec2 center = vec2(0.5, 0.5);
    vec2 delta  = uv - center;
    float dist  = length(delta);

    // ----------------------------------------------------------
    // 波纹透镜畸变
    //   wave1：高频快速外扩波  wave2：低频慢速外扩波
    //   两者叠加后除以距离衰减，使中心扰动更强
    // ----------------------------------------------------------
    float wave1   = sin(dist * 40.0 - Time * 5.0) * 0.010;
    float wave2   = sin(dist * 22.0 - Time * 3.2 + 1.2) * 0.006;
    float wave3   = sin(dist * 12.0 - Time * 1.8 + 2.7) * 0.004;
    float ripple  = (wave1 + wave2 + wave3) / (dist * 6.0 + 0.8);

    vec2 dir         = normalize(delta + vec2(0.0001)); // 防止除零
    vec2 distortedUV = uv + dir * ripple;
    distortedUV      = clamp(distortedUV, vec2(0.0), vec2(1.0));

    // 采样畸变后的场景颜色
    vec4 color = texture(DiffuseSampler, distortedUV);

    // ----------------------------------------------------------
    // 红色晕影（边缘向中心扩散）
    //   vignette  = 0 (中心) → 1 (边缘)
    //   pulse     = 缓慢脉动强度调制
    //   redAmount = 最终叠加的红色量
    // ----------------------------------------------------------
    float vignette  = ease(0.25, 0.9, dist * 2.0);
    float pulse     = 0.55 + 0.45 * sin(Time * 2.8);      // 0.1 ~ 1.0
    float redAmount = vignette * pulse * 0.75;

    // 向 R 通道叠加红色，压低 G/B 通道
    color.r  = min(1.0, color.r + redAmount * 0.9);
    color.g  = max(0.0, color.g - redAmount * 0.45);
    color.b  = max(0.0, color.b - redAmount * 0.45);

    // 深度晕影：极边缘几乎完全变成暗红
    float darkEdge = ease(0.55, 1.0, dist * 2.0);
    color.rgb *= (1.0 - darkEdge * 0.5);
    color.r   = min(1.0, color.r + darkEdge * 0.3);

    fragColor = color;
}
