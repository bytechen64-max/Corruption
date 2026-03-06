package corruption.client;

import net.minecraft.client.renderer.RenderStateShard;

/**
 * 继承 RenderStateShard 以访问其 protected 静态字段。
 * 注意：仅用作访问入口，不要实例化。
 */
public final class ModRenderStateShard extends RenderStateShard {
    // 私有构造防止意外实例化
    private ModRenderStateShard(String name, Runnable setup, Runnable cleanup) {
        super(name, setup, cleanup);
    }

    // 重新暴露为 public 静态字段，方便外部引用
    public static final RenderStateShard.TransparencyStateShard TRANSLUCENT_TRANSPARENCY = 
            RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    public static final RenderStateShard.CullStateShard NO_CULL = 
            RenderStateShard.NO_CULL;
    public static final RenderStateShard.LightmapStateShard LIGHTMAP = 
            RenderStateShard.LIGHTMAP;
    public static final RenderStateShard.OverlayStateShard OVERLAY = 
            RenderStateShard.OVERLAY;
}