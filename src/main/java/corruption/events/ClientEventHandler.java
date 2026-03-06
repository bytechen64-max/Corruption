package corruption.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import corruption.CorruptionMod;
import corruption.client.ModShaders;
import corruption.init.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = CorruptionMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    /** 后处理着色器资源路径：assets/corruption/shaders/post/instant_death.json */
    private static final ResourceLocation SCREEN_EFFECT =
            new ResourceLocation(CorruptionMod.MODID, "instant_death");

    private static boolean shaderLoaded = false;

    // =========================================================================
    // 1. 玩家屏幕后处理着色器（波纹透镜 + 红色晕影）
    // =========================================================================

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {

        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean hasEffect = mc.player.hasEffect(ModEffects.INSTANT_DEATH.get());

        if (hasEffect && !shaderLoaded) {
            // 加载屏幕后处理链
            mc.gameRenderer.loadEffect(SCREEN_EFFECT);
            shaderLoaded = true;
        } else if (!hasEffect && shaderLoaded) {
            // 卸载后处理链
            mc.gameRenderer.shutdownEffect();
            shaderLoaded = false;
        }

        // 每帧更新 Time 制服（uniform）供 GLSL 使用
        if (hasEffect && shaderLoaded) {
            PostChain chain = mc.gameRenderer.currentEffect();
            if (chain != null) {
                float time = (float)(mc.level.getGameTime() % 1_000_000L) / 20.0f;
                setPostChainUniform(chain, "Time", time);
            }
        }
        // 在 ClientEventHandler.onClientTick 开头加入

    }

    /**
     * 通过反射访问 PostChain 的 passes 列表，逐 Pass 更新 uniform。
     * Forge AT 方式（推荐）：在 META-INF/accesstransformer.cfg 中添加：
     *   public net.minecraft.client.renderer.PostChain f_110021_ # passes
     * 添加后可直接访问 chain.passes，无需反射。
     */
    @SuppressWarnings("unchecked")
    private static void setPostChainUniform(PostChain chain, String name, float value) {
        try {
            for (Field f : PostChain.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    List<?> list = (List<?>) f.get(chain);
                    if (!list.isEmpty() && list.get(0) instanceof PostPass) {
                        for (PostPass pass : (List<PostPass>) list) {
                            pass.getEffect().safeGetUniform(name).set(value);
                        }
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
            // 如果反射失败，着色器仍可正常运行（Time=0 静态效果）
        }
    }

    // =========================================================================
    // 2. 实体周围扭曲视觉效果（自定义核心着色器渲染层）
    // =========================================================================

    /**
     * 在实体渲染完成后，用扭曲着色器再渲染一遍该实体的模型。
     * 着色器会放大顶点 + 正弦波位移，并叠加红色半透明效果，
     * 营造"能量扭曲光晕"的视觉感受。
     */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();

        // 仅对携带效果的生物处理
        if (!entity.hasEffect(ModEffects.INSTANT_DEATH.get())) return;
        // 跳过本地玩家（其屏幕已有后处理效果）
        Minecraft mc = Minecraft.getInstance();
        if (entity == mc.player) return;

        if (ModShaders.entityDistortShader == null) return;

        LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>> renderer =
                (LivingEntityRenderer<LivingEntity, EntityModel<LivingEntity>>) event.getRenderer();

        ResourceLocation texture = renderer.getTextureLocation(entity);
        if (texture == null) return;

        PoseStack poseStack     = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();

        // 计算脉冲透明度（0.35 ~ 0.65，随时间平滑变化）
        float time  = (entity.tickCount + event.getPartialTick()) / 20.0f;
        float alpha = 0.35f + 0.3f * (float) Math.sin(time * 4.0);

        poseStack.pushPose();

        // 获取扭曲 RenderType（含自定义 entity_distort 着色器）
        VertexConsumer vc = buffer.getBuffer(
                ModShaders.getEntityDistortRenderType(texture));

        // 用同一模型再渲染一遍——着色器会在 GPU 上位移顶点
        renderer.getModel().renderToBuffer(
                poseStack, vc,
                event.getPackedLight(),
                OverlayTexture.NO_OVERLAY,
                1.0f, 0.1f, 0.1f, alpha   // 红色调，半透明
        );

        poseStack.popPose();
    }
}