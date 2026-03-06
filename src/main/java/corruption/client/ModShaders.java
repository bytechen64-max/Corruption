package corruption.client;

import corruption.CorruptionMod;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModShaders {

    /** 实体扭曲着色器实例（由 RegisterShadersEvent 注入） */
    public static ShaderInstance entityDistortShader;

    /** 缓存：每个纹理对应一个扭曲 RenderType，避免重复创建 */
    private static final Map<ResourceLocation, RenderType> DISTORT_RENDER_TYPES = new HashMap<>();

    // -------------------------------------------------------------------------
    // 着色器注册（MOD 总线，仅客户端）
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
        // 注册实体扭曲核心着色器
        // 着色器文件位于：assets/corruption/shaders/core/entity_distort.json
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        new ResourceLocation(CorruptionMod.MODID, "entity_distort"),
                        DefaultVertexFormat.NEW_ENTITY          // Position|Color|UV0|UV2|Normal
                ),
                shader -> entityDistortShader = shader
        );
    }

    // -------------------------------------------------------------------------
    // 动态 RenderType（每个实体纹理缓存一份）
    // -------------------------------------------------------------------------

    /**
     * 返回/创建带有自定义扭曲着色器的 RenderType。
     * 使用 NEW_ENTITY 顶点格式，开启半透明混合，不裁剪背面，以显示整个扭曲层。
     */
    public static RenderType getEntityDistortRenderType(ResourceLocation texture) {
        return DISTORT_RENDER_TYPES.computeIfAbsent(texture, tex -> {
            RenderType.CompositeState state = RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            () -> ModShaders.entityDistortShader))
                    .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                    .setTransparencyState(ModRenderStateShard.TRANSLUCENT_TRANSPARENCY)   // 替换
                    .setCullState(ModRenderStateShard.NO_CULL)                            // 替换
                    .setLightmapState(ModRenderStateShard.LIGHTMAP)                       // 替换
                    .setOverlayState(ModRenderStateShard.OVERLAY)                         // 替换
                    .createCompositeState(false);
            return RenderType.create(
                    CorruptionMod.MODID + ":entity_distort",
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    true,   // sortOnUpload
                    true,   // affectsCrumbling
                    state
            );
        });
    }
}