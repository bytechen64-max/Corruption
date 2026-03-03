package corruption.util.combat;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;

public class HurtTargetUtil {

    public static boolean TrueHurt(LivingEntity attacker, LivingEntity target,
                                   float ignoreInvulDamage, float commonDamage,
                                   int level, boolean hurtTick) {

        // 1. 检查无效目标
        if (isInvulnerableTarget(target)) {
            return false;
        }

        // 2. 检查盾牌防御（仅对等级1生效）
        if (level == 1 && isBlockingWithShield(target)) {
            handleShieldBlocking(target, ignoreInvulDamage);
            return false;
        }

        // 3. 检查不死图腾（仅对等级1和2生效）
        // 先检查最小伤害是否会触发图腾
        if (level <= 2 && willTriggerTotem(target, ignoreInvulDamage)) {
            triggerTotemEffect(target);
            return false;
        }

        // 4. 根据等级处理最小伤害
        boolean minDamageApplied = processMinDamageByLevel(attacker, target, ignoreInvulDamage, level);

        // 5. 处理普通伤害（与等级无关）
        boolean commonDamageApplied = false;
        if (commonDamage != 0) {
            commonDamageApplied = processCommonDamage(attacker, target, commonDamage, hurtTick);
        }

        return minDamageApplied || commonDamageApplied;
    }

    /**
     * 检查目标是否无敌（创造/旁观模式）
     */
    private static boolean isInvulnerableTarget(LivingEntity target) {
        if (target instanceof Player player) {
            return player.getAbilities().instabuild || player.isSpectator();
        }
        return false;
    }

    /**
     * 检查是否使用盾牌防御
     */
    private static boolean isBlockingWithShield(LivingEntity entity) {
        if (entity instanceof Player player) {
            ItemStack useItem = player.getUseItem();
            return useItem.getItem() instanceof ShieldItem && player.isUsingItem();
        }
        return entity.isBlocking() &&
                (entity.getMainHandItem().getItem() instanceof ShieldItem ||
                        entity.getOffhandItem().getItem() instanceof ShieldItem);
    }

    /**
     * 处理盾牌格挡
     */
    private static void handleShieldBlocking(LivingEntity target, float damage) {
        ItemStack shieldItem = ItemStack.EMPTY;

        if (target instanceof Player player && player.isUsingItem()) {
            shieldItem = player.getUseItem();
        } else {
            if (target.getMainHandItem().getItem() instanceof ShieldItem) {
                shieldItem = target.getMainHandItem();
            } else if (target.getOffhandItem().getItem() instanceof ShieldItem) {
                shieldItem = target.getOffhandItem();
            }
        }

        // 消耗盾牌耐久
        if (shieldItem.getItem() instanceof ShieldItem) {
            int durabilityCost = Math.max(1, (int) damage);
            shieldItem.hurtAndBreak(durabilityCost, target,
                    entity -> entity.broadcastBreakEvent(entity.getUsedItemHand()));
        }

        // 播放音效
        playShieldBlockSound(target);
    }

    /**
     * 根据等级处理最小伤害
     */
    private static boolean processMinDamageByLevel(LivingEntity attacker, LivingEntity target,
                                                   float ignoreInvulDamage, int level) {
        if (ignoreInvulDamage == 0) {
            return false;
        }

        switch (level) {
            case 1: // 普通物理伤害（无伤害来源的物理伤害，默认穿甲）
                return processLevel1MinDamage(attacker, target, ignoreInvulDamage);

            case 2: // 虚空伤害
                return processLevel2MinDamage(target, ignoreInvulDamage);

            case 3: // 原版setHealth
                return processLevel3MinDamage(target, ignoreInvulDamage);

            default: // 等级4及以上：使用setHealthUtils中的改血方法
                return processLevel4PlusMinDamage(target, ignoreInvulDamage, level);
        }
    }

    /**
     * 等级1：处理最小伤害（普通物理伤害）
     */
    private static boolean processLevel1MinDamage(LivingEntity attacker, LivingEntity target, float damage) {
        float originalHealth = target.getHealth();
        float absorption = target.getAbsorptionAmount();
        int savedInvulTime = target.invulnerableTime;

        // 临时移除无敌帧
        target.invulnerableTime = 0;

        boolean damageApplied = false;

        try {
            if (damage > 0) {
                // 正伤害
                if (absorption > 0) {
                    // 有吸收伤害，先消耗黄心
                    target.hurt(target.damageSources().mobAttack(attacker), damage);
                } else {
                    // 直接造成伤害
                    target.hurt(target.damageSources().mobAttack(attacker), damage);

                    // 确保伤害生效
                    if (target instanceof Player) {
                        target.setHealth(Math.max(0, originalHealth - damage));
                    } else {
                        target.setHealth(Math.max(0, originalHealth - damage));
                    }
                }
                damageApplied = true;
            } else if (damage < 0) {
                // 负伤害（治疗）
                target.hurt(target.damageSources().mobAttack(attacker), damage);
                damageApplied = true;
            }
        } finally {
            // 恢复无敌帧
            target.invulnerableTime = savedInvulTime;
        }

        // 播放音效
        if (damageApplied && damage > 0 && target.level().isClientSide) {
            playHurtSound(target);
        }

        return damageApplied;
    }

    /**
     * 等级2：处理最小伤害（虚空伤害）
     */
    private static boolean processLevel2MinDamage(LivingEntity target, float damage) {
        if (damage == 0) {
            return false;
        }

        float currentHealth = target.getHealth();
        float newHealth = currentHealth - damage;
        newHealth = Math.max(0, newHealth);
        target.setHealth(newHealth);


        // 播放音效
        if (damage > 0 && target.level().isClientSide) {
            target.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5f, 0.1f);
        }

        return true;
    }

    /**
     * 等级3：处理最小伤害（原版setHealth）
     */
    private static boolean processLevel3MinDamage(LivingEntity target, float damage) {
        if (damage == 0) {
            return false;
        }

        float currentHealth = target.getHealth();
        float newHealth = currentHealth - damage;
        newHealth = Math.max(0, Math.min(newHealth, target.getMaxHealth()));

        target.setHealth(newHealth);

        // 播放音效
        if (damage > 0 && target.level().isClientSide) {
            playHurtSound(target);
        }

        return true;
    }

    /**
     * 等级4及以上：处理最小伤害（自定义改血方法）
     */
    private static boolean processLevel4PlusMinDamage(LivingEntity target, float damage, int level) {
        if (damage == 0) {
            return false;
        }

        float currentHealth = target.getHealth();
        float newHealth = currentHealth - damage;

        if (level == 4) {
            target.setHealth(newHealth);
        } else {
            target.setHealth(newHealth);
        }

        // 播放音效
        if (damage > 0 && target.level().isClientSide) {
            playHurtSound(target);
        }

        return true;
    }

    /**
     * 处理普通伤害（与等级无关）
     */
    private static boolean processCommonDamage(LivingEntity attacker, LivingEntity target,
                                               float damage, boolean hurtTick) {
        if (damage == 0) {
            return false;
        }

        float absorption = target.getAbsorptionAmount();
        int savedInvulTime = target.invulnerableTime;

        // 如果hurtTick为true，重置无敌帧
        if (hurtTick) {
            target.invulnerableTime = 0;
        }

        boolean damageApplied = false;

        try {
            if (damage > 0) {
                // 正伤害
                if (absorption > 0) {
                    // 有吸收伤害，先消耗黄心
                    target.hurt(target.damageSources().mobAttack(attacker), damage);
                    damageApplied = true;
                } else {
                    // 只有在无敌帧为0时才能造成伤害
                    if (target.invulnerableTime == 0) {
                        target.hurt(target.damageSources().mobAttack(attacker), damage);
                        damageApplied = true;

                        // 播放受伤音效
                        if (hurtTick && target instanceof Player && !target.level().isClientSide) {
                            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                                    SoundEvents.PLAYER_HURT, SoundSource.HOSTILE, 1.0F, 1.0F);
                        }
                    }
                }
            } else if (damage < 0) {
                // 负伤害（治疗）
                target.hurt(target.damageSources().mobAttack(attacker), damage);
                damageApplied = true;
            }
        } finally {
            // 恢复无敌帧
            if (hurtTick) {
                target.invulnerableTime = savedInvulTime;
            }
        }

        // 播放音效
        if (damageApplied && damage > 0 && target.level().isClientSide) {
            playHurtSound(target);
        }

        return damageApplied;
    }

    /**
     * 检查是否会触发不死图腾
     */
    private static boolean willTriggerTotem(LivingEntity target, float damage) {
        if (!(target instanceof Player player)) {
            return false;
        }

        if (player.getHealth() - damage > 0) {
            return false;
        }

        return hasTotemOfUndying(player);
    }

    /**
     * 检查是否拥有不死图腾
     */
    private static boolean hasTotemOfUndying(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                return true;
            }
        }

        return player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
    }

    /**
     * 触发不死图腾效果
     */
    private static void triggerTotemEffect(LivingEntity target) {
        if (!(target instanceof Player player)) {
            return;
        }

        ItemStack totem = findAndConsumeTotem(player);
        if (totem.isEmpty()) {
            return;
        }

        player.setHealth(1.0F);
        player.removeAllEffects();
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));

        playTotemSound(player);
    }

    /**
     * 找到并消耗一个图腾
     */
    private static ItemStack findAndConsumeTotem(Player player) {
        if (player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            ItemStack totem = player.getOffhandItem();
            totem.shrink(1);
            return totem;
        }

        if (player.getMainHandItem().getItem() == Items.TOTEM_OF_UNDYING) {
            ItemStack totem = player.getMainHandItem();
            totem.shrink(1);
            return totem;
        }

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                stack.shrink(1);
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 播放盾牌格挡音效
     */
    private static void playShieldBlockSound(LivingEntity target) {
        if (!target.level().isClientSide) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            target.playSound(SoundEvents.SHIELD_BLOCK, 1.0f, 1.0f);
        }
    }

    /**
     * 播放受伤音效
     */
    private static void playHurtSound(LivingEntity target) {
        if (target instanceof Player) {
            target.playSound(SoundEvents.PLAYER_HURT, 1.0f, 1.0f);
        } else {
            target.playSound(SoundEvents.GENERIC_HURT, 1.0f, 1.0f);
        }
    }

    /**
     * 播放图腾使用音效
     */
    private static void playTotemSound(Player player) {
        if (!player.level().isClientSide) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        } else {
            player.playSound(SoundEvents.TOTEM_USE, 1.0F, 1.0F);
        }
    }
}