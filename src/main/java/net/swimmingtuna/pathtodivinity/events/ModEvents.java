package net.swimmingtuna.pathtodivinity.events;

import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.client.Configs;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.entity.Mobs.PlayerMobEntity;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.combat.CombatTag;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import net.swimmingtuna.pathtodivinity.PTDTags;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import net.swimmingtuna.pathtodivinity.behavior.ProfileBehaviors;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfile;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderProfiles;
import net.swimmingtuna.pathtodivinity.beyonders.BeyonderRegistrations;
import net.swimmingtuna.pathtodivinity.compat.BornInChaosCompat;
import net.swimmingtuna.pathtodivinity.compat.CataclysmCompat;
import net.swimmingtuna.pathtodivinity.compat.EEEABMobsCompat;
import net.swimmingtuna.pathtodivinity.compat.ModCompat;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import net.swimmingtuna.pathtodivinity.compat.PTDItems;
import net.swimmingtuna.pathtodivinity.compat.SoulsWeaponryCompat;
import net.swimmingtuna.pathtodivinity.compat.TerramityCompat;
import net.swimmingtuna.pathtodivinity.profile.AbilityDamageTracker;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import net.swimmingtuna.pathtodivinity.config.PTDBalance;


@Mod.EventBusSubscriber(modid = PTD.MOD_ID)
public class ModEvents {

    @SubscribeEvent
    public static void commandEvent(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer player) {
            // Disable FTB Teams party formation. FTB Quests shares quest progress across
            // every member of a party, which lets one player's Sequence potion complete the
            // quest for the whole team. Blocking party create/join/invite keeps every player
            // as their own single-member team, so pathway progression stays per-player.
            String fullCommand = event.getParseResults().getReader().getString()
                    .toLowerCase().replaceAll("\\s+", " ").trim();
            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }
            for (String prefix : PTDServerConfig.BLOCKED_COMMAND_PREFIXES.get()) {
                String normalizedPrefix = prefix.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
                if (normalizedPrefix.startsWith("/")) {
                    normalizedPrefix = normalizedPrefix.substring(1);
                }
                if (!normalizedPrefix.isEmpty() && fullCommand.startsWith(normalizedPrefix)) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal(PTDServerConfig.BLOCKED_COMMAND_MESSAGE.get())
                            .withStyle(ChatFormatting.RED));
                    return;
                }
            }

            if (CombatTag.isInCombat(player)) {
                // fullCommand already has the leading "/" stripped (commands typed in chat arrive without it)
                String blocked = CombatTag.blockedCommand(fullCommand);
                if (blocked != null) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal(PTDServerConfig.COMBAT_MESSAGE.get().replace("%s", blocked)));
                }
            }
        }
    }


    @SubscribeEvent
    public static void handleLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (!living.level().isClientSide()) {
            EntityType<?> type = living.getType();
            ResourceLocation typeId = EntityType.getKey(type);
            CombatTag.tick(living);

            int tickCount = living.tickCount;
            if (tickCount % PTDServerConfig.ITEM_SCAN_INTERVAL_TICKS.get() == 0 && living instanceof Player player) {
                removeBannedEnchantments(player, living.getMainHandItem());
                // Banned items (Mehrunes Razor included) come from #pathtodivinity:banned.
                PTDUtil.removeBannedItem(living);
            }

            BeyonderProfile profile = BeyonderProfiles.find(living);
            if (profile != null) {
                ProfileBehaviors.onTick(living, profile);
            }
            boolean regenTick = tickCount % PTDServerConfig.BOSS_REGEN_INTERVAL_TICKS.get() == 0;
            boolean blockBreakTick = tickCount % PTDServerConfig.BOSS_BLOCK_BREAKING_INTERVAL_TICKS.get() == 0;
            if ((regenTick || blockBreakTick) && living instanceof Mob mob && PTDUtil.isBeyonderEntity(mob)) {
                // Out-of-combat regeneration, see [bosses] regen_enabled. Gated on "no player anywhere
                // near" rather than mob.getTarget(): LOTM's own mobs drop their target constantly.
                if (regenTick && PTDServerConfig.BOSS_REGEN_ENABLED.get() && !CombatTag.isRegenLocked(mob)
                        && mob.getHealth() < mob.getMaxHealth() && mob.isAlive() && !Float.isNaN(mob.getHealth())
                        && !isPlayerNearby(mob, PTDServerConfig.BOSS_REGEN_PLAYER_RADIUS.get())) {
                    // heal() rather than setHealth(): it clamps to max health and fires LivingHealEvent,
                    // so the heal can be cancelled and anti-heal effects apply.
                    mob.heal((float) (mob.getMaxHealth() * PTDServerConfig.BOSS_REGEN_PERCENT.get() / 100.0));
                }
                // Follows LOTM's own "Mobs Destroy Blocks" option, and LOTM's per-block rules
                // (hardness limit, faction claims) via BeyonderUtil.canDestroyBlock.
                if (blockBreakTick && Configs.COMMON.shouldMobsDestroyBlocks.get()
                        && mob.getTarget() instanceof Player target && target.getY() > mob.getEyeY()) {
                    Level level = mob.level();
                    boolean drops = PTDServerConfig.BOSS_BLOCK_BREAKING_DROPS.get();
                    AABB box = mob.getBoundingBox().inflate(1.0);
                    BlockPos.betweenClosedStream(box).forEach(pos -> {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && pos.getY() >= mob.getY() + 1 && state.getFluidState().isEmpty()
                                && !state.is(PTDTags.BOSS_UNBREAKABLE) && BeyonderUtil.canDestroyBlock(mob, pos)) {
                            level.destroyBlock(pos, drops, mob);
                        }
                    });
                }
            }
        }
    }


    private static boolean isPlayerNearby(Mob mob, double radius) {
        return !mob.level().getEntitiesOfClass(Player.class, mob.getBoundingBox().inflate(radius),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()).isEmpty();
    }

    /** Strips the [items] banned_enchantments from a player's held item. */
    private static void removeBannedEnchantments(Player player, ItemStack stack) {
        if (!stack.isEnchanted()) {
            return;
        }
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
        boolean changed = false;
        for (String id : PTDServerConfig.BANNED_ENCHANTMENTS.get()) {
            ResourceLocation enchantmentId = ResourceLocation.tryParse(id);
            Enchantment enchantment = enchantmentId == null ? null : ForgeRegistries.ENCHANTMENTS.getValue(enchantmentId);
            if (enchantment != null && enchantments.remove(enchantment) != null) {
                changed = true;
                player.sendSystemMessage(Component.translatable(enchantment.getDescriptionId())
                        .append(" is banned").withStyle(ChatFormatting.RED));
            }
        }
        if (changed) {
            EnchantmentHelper.setEnchantments(enchantments, stack);
        }
    }

    @SubscribeEvent
    public static void onEntityChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag tag = living.getPersistentData();
        BeyonderClass pathway = BeyonderUtil.getPathway(living);
        if (!living.level().isClientSide() && (event.getOriginalTarget() instanceof Player || event.getNewTarget() instanceof Player) && event.getOriginalTarget() != null && event.getNewTarget() != null) {
            if (PTDServerConfig.BOSS_TARGET_LOCK.get() && PTDUtil.isBeyonderEntity(living) && living instanceof Mob mob) {
                float newTargetHealth = event.getNewTarget().getHealth();
                float originalTargetHealth = event.getOriginalTarget().getHealth();
                // Redirect back to the original target rather than cancelling. Cancelling leaves
                // mob.target untouched while the AI goal that asked for the switch believes it
                // succeeded; the goal then fails canContinueToUse and calls setTarget(null), which
                // this handler allows through. On a multiplayer server that thrash left bosses
                // permanently targetless - they stopped fighting and started regenerating.
                if (newTargetHealth > originalTargetHealth) {
                    event.setNewTarget(event.getOriginalTarget());
                } else if (event.getNewTarget().distanceTo(living) > event.getOriginalTarget().distanceTo(living) && event.getNewTarget().getHealth() > event.getOriginalTarget().getHealth()) {
                    event.setNewTarget(event.getOriginalTarget());
                }
            }
        }
    }

    @SubscribeEvent
    public static void livingDeathEvent(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!event.getEntity().level().isClientSide()) {
            if (entity instanceof LivingEntity living) {
                BeyonderProfile profile = BeyonderProfiles.find(living);
                if (profile != null) {
                    ProfileBehaviors.onDeath(living, profile);
                }
            }
        }
    }

    @SubscribeEvent
    public static void hurtEvent(LivingHurtEvent event) {
        Entity entity = event.getEntity();
        CompoundTag tag = entity.getPersistentData();
        DamageSource source = event.getSource();
        Entity entitySource = source.getEntity();
        Entity directSource = source.getDirectEntity();
        Entity damageDealer = source.getEntity();
        if (damageDealer == null && source.getDirectEntity() != null) {
            damageDealer = source.getDirectEntity();
        }
        if (!event.getEntity().level().isClientSide()) {
            if (directSource != null) {
                event.setAmount(event.getAmount() * projectileDamageMultiplier(directSource));
            }


            CombatTag.onHurt(event.getEntity(), source);
            if (entitySource instanceof LivingEntity livingEntity) {
                if (PTDUtil.isBeyonderEntity(livingEntity) && directSource instanceof Projectile) {
                    event.setAmount((float) (event.getAmount() * PTDServerConfig.BOSS_PROJECTILE_DAMAGE_MULTIPLIER.get()));
                }
            }


            if (damageDealer != null) {
                if (damageDealer.getPersistentData().contains("PTDDamageMultiplier")) {
                    event.setAmount((float) (event.getAmount() * damageDealer.getPersistentData().getDouble("PTDDamageMultiplier")));
                }
            }

            // Safemode profiles trade offensive power against other players for immunity to death regression.
            // SafemodeDamageSourceMixin tags the damage source as it is built, so this only fires for genuine
            // Beyonder ability damage - melee, arrows and damage dealt to mobs are all untouched.
            if (entity instanceof Player && AbilityDamageTracker.isSafemodeAbilityDamage(source)) {
                event.setAmount((float) (event.getAmount() * PTDServerConfig.SAFEMODE_ABILITY_DAMAGE.get()));
            }
        }
    }


    /** Damage multiplier for weapon projectiles/effects from other mods, 1.0 when none applies. */
    private static float projectileDamageMultiplier(Entity directSource) {
        ResourceLocation sourceId = PTDEntities.idOf(directSource);
        if (sourceId.equals(PTDEntities.SOLARIS_BOMB)) {
            return PTDBalance.SOLARIS_BOMB_HIT_MULTIPLIER.scale(1.0f);
        } else if (sourceId.equals(PTDEntities.CRESCENTIA_DRAGON)) {
            return PTDBalance.CRESCENTIA_DRAGON_HIT_MULTIPLIER.scale(1.0f);
        } else if (sourceId.equals(PTDEntities.FROSTBOUND_SHARD)) {
            return PTDBalance.FROSTBOUND_SHARD_HIT_MULTIPLIER.scale(1.0f);
        } else if (sourceId.equals(PTDEntities.PUMPKIN_PISTOL_PROJECTILE)) {
            if (directSource instanceof Projectile projectile && projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return PTDBalance.PUMPKIN_PISTOL_HIT_MULTIPLIER.scale(1.0f);
            }
        } else if (sourceId.equals(PTDEntities.FREYR_SWORD)) {
            return PTDBalance.FREYR_SWORD_HIT_MULTIPLIER.scale(1.0f);
        } else if (ModCompat.CATACLYSM.equals(sourceId.getNamespace())) {
            return CataclysmCompat.projectileDamageMultiplier(directSource);
        } else if (ModCompat.EEEABS_MOBS.equals(sourceId.getNamespace())) {
            return EEEABMobsCompat.projectileDamageMultiplier(directSource);
        }
        return 1.0f;
    }


    @SubscribeEvent
    public static void entityJoinEvent(EntityJoinLevelEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            Entity entity = event.getEntity();
            if (entity instanceof LivingEntity living) {
                EntityType<?> type = living.getType();
                BeyonderProfile profile = BeyonderProfiles.find(living);
                if (profile != null) {
                    ProfileBehaviors.onJoin(living, profile);
                }
            } else if (entity instanceof ItemEntity item) {
                if (item.getItem().is(PTDTags.DESTROYED)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void serverStartEvent(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        // Only ever switches the gamerule off; with disable_krampus = false the gamerule is left to the admin.
        if (PTDServerConfig.DISABLE_KRAMPUS.get() && ModCompat.isLoaded(ModCompat.BORN_IN_CHAOS)) {
            BornInChaosCompat.disableKrampusSpawns(server);
        }
    }

    /** Registers bosses' pathways with LOTM from their profiles; levels exist by now, unlike at ServerStarting. */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        BeyonderRegistrations.apply(event.getServer(), true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        int interval = PTDServerConfig.BEYONDER_REFRESH_INTERVAL_TICKS.get();
        if (event.phase == TickEvent.Phase.END && interval > 0 && event.getServer().getTickCount() % interval == 0) {
            BeyonderRegistrations.apply(event.getServer(), false);
        }
    }

    public static boolean isEntityFromMod(Entity entity, String modId) {
        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (entityId != null && entityId.getNamespace().equals(modId)) {
            return true;
        }
        String packageName = entity.getClass().getPackage().getName();
        return packageName.toLowerCase().contains(modId.toLowerCase());
    }

}