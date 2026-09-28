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
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.swimmingtuna.lotm.LOTM;
import net.swimmingtuna.lotm.beyonder.api.BeyonderClass;
import net.swimmingtuna.lotm.entity.Mobs.PlayerMobEntity;
import net.swimmingtuna.lotm.init.BeyonderClassInit;
import net.swimmingtuna.lotm.util.BeyonderUtil;
import net.swimmingtuna.pathtodivinity.PTD;
import net.swimmingtuna.pathtodivinity.PTDConfig;
import net.swimmingtuna.pathtodivinity.PTDUtil;
import net.swimmingtuna.pathtodivinity.compat.BornInChaosCompat;
import net.swimmingtuna.pathtodivinity.compat.CataclysmCompat;
import net.swimmingtuna.pathtodivinity.compat.EEEABMobsCompat;
import net.swimmingtuna.pathtodivinity.compat.ModCompat;
import net.swimmingtuna.pathtodivinity.compat.PTDEntities;
import net.swimmingtuna.pathtodivinity.compat.PTDItems;
import net.swimmingtuna.pathtodivinity.compat.SoulsWeaponryCompat;
import net.swimmingtuna.pathtodivinity.compat.TerramityCompat;

import org.jetbrains.annotations.Nullable;

import java.util.Map;


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
            if (fullCommand.startsWith("ftbteams party create")
                    || fullCommand.startsWith("ftbteams party join")
                    || fullCommand.startsWith("ftbteams party invite")) {
                event.setCanceled(true);
                player.sendSystemMessage(Component.literal(
                        "Teams are disabled on this pack — each player must progress their own pathway.")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            CompoundTag tag = player.getPersistentData();
            if (tag.getInt("PTDCombatTimer") > 0) {
                String commandName = event.getParseResults().getReader().getString().split(" ")[0].toLowerCase();
                if (commandName.equals("/home") || commandName.equals("/spawn")) {
                    event.setCanceled(true);
                    player.sendSystemMessage(Component.literal("You are in combat and cannot use /home or /spawn!"));
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
            CompoundTag tag = living.getPersistentData();
            int combatTimer = tag.getInt("PTDCombatTimer");
            if (combatTimer >= 1) {
                tag.putInt("PTDCombatTimer", combatTimer - 1);
            }

            int tickCount = living.tickCount;
            if (tickCount % 200 == 0 && living instanceof Player player) {
                ItemStack mainHand = living.getMainHandItem();
                if (mainHand.isEnchanted() && mainHand.getEnchantmentLevel(Enchantments.PIERCING) > 0) {
                    Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(mainHand);
                    enchantments.remove(Enchantments.PIERCING);
                    living.sendSystemMessage(Component.literal("Piercing is banned").withStyle(ChatFormatting.RED));
                    EnchantmentHelper.setEnchantments(enchantments, mainHand);
                }
                Item razor = PTDItems.get(PTDItems.MEHRUNES_RAZOR);
                if (razor != null && player.getInventory().contains(razor.getDefaultInstance())) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        if (stack.is(razor)) {
                            player.getInventory().setItem(i, ItemStack.EMPTY);
                        }
                    }
                    player.containerMenu.broadcastChanges();
                    player.sendSystemMessage(Component.literal("Mehrunes Razor cannot be used.").withStyle(ChatFormatting.RED));
                }
                PTDUtil.removeBannedItem(living);
            }

            if (living.tickCount % 40 == 0) {
                if (typeId.equals(PTDEntities.CHAOS_MONARCH)) {


                } else if (typeId.equals(PTDEntities.DRAUGR_BOSS)) {


                } else if (typeId.equals(PTDEntities.NIGHT_SHADE)) {


                } else if (typeId.equals(PTDEntities.CLOUD_GOLEM)) {


                } else if (typeId.equals(PTDEntities.THE_LEVIATHAN)) {


                } else if (living.getName().getString().equalsIgnoreCase("horseman")) {


                } else if (typeId.equals(PTDEntities.NAMELESS_GUARDIAN)) {


                } else if (typeId.equals(PTDEntities.MOONKNIGHT)) {


                } else if (typeId.equals(PTDEntities.LORD_PUMPKINHEAD)) {


                } else if (typeId.equals(PTDEntities.TRIAL_GUARDIAN)) {


                } else if (typeId.equals(PTDEntities.DAY_STALKER)) {
                    if (living.tickCount % 4 == 0) {
                        for (Mob mob : living.level().getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(25))) {
                            if (PTDEntities.NIGHT_PROWLER.equals(PTDEntities.idOf(mob))) {
                                BeyonderUtil.forceAlly(mob, living);
                            }
                        }
                    }
                    if (SoulsWeaponryCompat.isPhaseTwo(living)) {
                        boolean x = tag.getBoolean("isPhaseTwo");
                        tag.putBoolean("isPhaseTwo", false);
                        if (!x) {
                            multiplyMaxHealth(living, 2);
                        }
                        multiplyDamage(living, 2.6);
                    }


                } else if (typeId.equals(PTDEntities.NIGHT_PROWLER)) {
                    if (living.tickCount % 4 == 0) {
                        for (Mob mob : living.level().getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(25))) {
                            if (PTDEntities.DAY_STALKER.equals(PTDEntities.idOf(mob))) {
                                BeyonderUtil.forceAlly(mob, living);
                            }
                        }
                    }
                    if (SoulsWeaponryCompat.isPhaseTwo(living)) {
                        boolean x = tag.getBoolean("isPhaseTwo");
                        multiplyDamage(living, 0.75);
                        if (!x) {
                            tag.putBoolean("isPhaseTwo", false);
                            multiplyMaxHealth(living, 2);
                        }
                    }


                } else if (typeId.equals(PTDEntities.ULTRA_SNIFFER)) {
                    if (BeyonderUtil.getPathway(living) == null) {
                        BeyonderClass[] pathways = {BeyonderClassInit.MONSTER.get(), BeyonderClassInit.WARRIOR.get(), BeyonderClassInit.SPECTATOR.get(), BeyonderClassInit.SAILOR.get()};
                    }
                } else if (typeId.equals(PTDEntities.WARPED_FUNGUSSUS)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.4);
                } else if (typeId.equals(PTDEntities.KOBOLEDIATOR)) {
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.MAW)) {
                    multiplyDamage(living, 1.5);
                } else if (typeId.equals(PTDEntities.WROUGHTNAUT)) {
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.DIRE_HOUND_LEADER)) {
                    multiplyDamage(living, 1.5);

                    // Sequence 8
                } else if (typeId.equals(PTDEntities.DUSKROK)) {
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.MUTANT_SKELETON)) {
                    multiplyDamage(living, 1.2);
                } else if (living.getName().getString().toLowerCase().contains("terrible") || living.getName().getString().toLowerCase().contains("puny")) {
                    multiplyDamage(living, 1.3);
                } else if (type == EntityType.ELDER_GUARDIAN) {
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.MUTANT_ENDERMAN)) {
                    multiplyDamage(living, 1.2);
                } else if (living.getName().getString().toLowerCase().contains("aero_guardian")) {
                    multiplyDamage(living, 1.6);
                } else if (typeId.equals(PTDEntities.MOTHER_SPIDER)) {
                    multiplyDamage(living, 2.0);
                } else if (typeId.equals(PTDEntities.HELLROK)) {
                    multiplyDamage(living, 1.3);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1, 40, false, false));
                }

                // Sequence 7
                else if (living.getName().getString().toLowerCase().contains("doomharbor")) {
                    multiplyDamage(living, 1.75);
                } else if (typeId.equals(PTDEntities.FROSTMAW)) {
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.MAZE_MOTHER)) {
                    multiplyDamage(living, 1.6);
                } else if (living.getName().getString().toLowerCase().contains("plague_bringer")) {
                    multiplyDamage(living, 1.4);
                }else if (living.getClass().getSimpleName().equals("LichEntity")) {
                    multiplyDamage(living, 1.1);
                } else if (living.getClass().getSimpleName().equals("GauntletEntity")) {
                    multiplyDamage(living, 2.0);
                }else if (living.getClass().getSimpleName().equals("ObsidilithEntity")) {
                    multiplyDamage(living, 1.1);

                    //Sequence 6
                } else if (type == EntityType.WITHER) {
                    multiplyDamage(living, 1.1);
                } else if (living.getClass().getSimpleName().equals("VoidBlossomEntity")) {
                    multiplyDamage(living, 1.3);
                }else if (typeId.equals(PTDEntities.LIFESTEALER)) {
                    multiplyDamage(living, 2.0);
                    multiplyMaxHealth(living, 1.4);
                }else if (typeId.equals(PTDEntities.SIR_PUMPKINHEAD)) {
                    multiplyDamage(living, 2.8);
                } else if (living.getName().getString().toLowerCase().contains("dyrolian")) {
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.ENDERSENT)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 4.0);

                    // Sequence 5
                } else if (typeId.equals(PTDEntities.CHAOS_MONARCH)) {
                    multiplyDamage(living, 4.5);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.SHADOWLESS_DEMONIC_WOLF.get()) {
                    multiplyMaxHealth(living, 3.0);
                    multiplyDamage(living, 1.1);
                } else if (typeId.equals(PTDEntities.CAPTAIN_CORNELIA)) {
                    multiplyDamage(living, 1.6);
                    multiplyMaxHealth(living, 1.3);
                } else if (typeId.equals(PTDEntities.DRAUGR_BOSS)) {
                    multiplyDamage(living, 1.2);


                } else if (typeId.equals(PTDEntities.NIGHT_SHADE)) {
                    multiplyDamage(living, 1.5);


                } else if (typeId.equals(PTDEntities.ACCURSED_LORD_BOSS)) {
                    multiplyDamage(living, 2.0);
                } else if (typeId.equals(PTDEntities.RETURNING_KNIGHT)) {
                    multiplyDamage(living, 1.6);
                } else if (typeId.equals(PTDEntities.ENDER_GUARDIAN)) {
                    multiplyDamage(living, 1.1);
                    multiplyMaxHealth(living, 0.75);
                } else if (typeId.equals(PTDEntities.NETHERITE_MONSTROSITY)) {
                    multiplyDamage(living, 1.5);
                    multiplyMaxHealth(living, 1.2);
                }else if (typeId.equals(PTDEntities.POSESSED_PALADIN)) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 1.2);


                    // Sequence 4
                } else if (typeId.equals(PTDEntities.CLOUD_GOLEM)) {


                } else if (typeId.equals(PTDEntities.THE_LEVIATHAN)) {


                } else if (typeId.equals(PTDEntities.SCYLLA)) {
                    multiplyDamage(living, 1);


                } else if (typeId.equals(PTDEntities.GOB)) {
                    multiplyDamage(living, 1.8);
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
                }else if (living.getName().getString().equalsIgnoreCase("horseman")) {


                } else if (type == net.swimmingtuna.lotm.init.EntityInit.DRAGON.get()) {
                    multiplyDamage(living, 0.75f);
                } else if (living.getName().getString().toLowerCase().contains("vessel")) {


                    multiplyDamage(living, 2.5);
                } else if (typeId.equals(PTDEntities.MOONKNIGHT)) {
                    multiplyDamage(living, 2.0);


                }else if (typeId.equals(PTDEntities.LORD_PUMPKINHEAD)) {
                    multiplyDamage(living, 1.2);


                } else if (typeId.equals(PTDEntities.TRIAL_GUARDIAN)) {



                    // Sequence 2
                } else if (typeId.equals(PTDEntities.SUPER_SNIFFER)) {
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.DAY_STALKER)) {


                } else if (typeId.equals(PTDEntities.NIGHT_PROWLER)) {


                } else if (typeId.equals(PTDEntities.GUNDALF)) {
                    multiplyDamage(living, 1.5);
                }

            }


            else if (living.getName().getString().equalsIgnoreCase("horseman")) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            } else if (typeId.equals(PTDEntities.HEROBRINE)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            } else if (typeId.equals(PTDEntities.UMVUTHI)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false));
            } else if (living.getName().getString().toLowerCase().contains("terrible_ten")) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            } else if (typeId.equals(PTDEntities.SPIRITOF_CHAOS)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
            } else if (typeId.equals(PTDEntities.MOONKNIGHT)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
            } else if (typeId.equals(PTDEntities.LORD_PUMPKINHEAD)) {
                if (living.tickCount % 3 == 0) {
                    for (Mob mob : living.level().getEntitiesOfClass(Mob.class, living.getBoundingBox().inflate(25))) {
                        if (!(mob instanceof PlayerMobEntity)) {
                            BeyonderUtil.forceAlly(mob, living);
                        }
                    }
                }
            } else if (typeId.equals(PTDEntities.DAY_STALKER)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
            } else if (typeId.equals(PTDEntities.NIGHT_PROWLER)) {
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false));
            } else if (typeId.equals(PTDEntities.MALEDICTUS)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 2, false, false));
            } else if (typeId.equals(PTDEntities.SCYLLA)) {
                living.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 2, false, false));
            }
            if (living instanceof Mob ultraSniffer && typeId.equals(PTDEntities.ULTRA_SNIFFER)) {
                if (ultraSniffer.getTarget() == null) {
                    for (Player player : ultraSniffer.level().getEntitiesOfClass(Player.class, ultraSniffer.getBoundingBox().inflate(50))) {
                        if (!player.isCreative() && !player.isSpectator()) {
                            ultraSniffer.setTarget(player);
                        }
                    }
                    float health = ultraSniffer.getHealth();
                    if (Float.isNaN(health) || health < 0.0F) {
                        ultraSniffer.setHealth(0.0F);
                    }
                    boolean isPhaseTwo = TerramityCompat.isUltraSnifferPhaseTwo(ultraSniffer);
                    if (isPhaseTwo) {
                        int fullHeal = ultraSniffer.getPersistentData().getInt("PtDFullHeal");
                        if (fullHeal <= 100) {
                            ultraSniffer.getPersistentData().putInt("PtDFullHeal", fullHeal + 1);
                            ultraSniffer.setHealth(ultraSniffer.getMaxHealth());
                            ultraSniffer.getPersistentData().putInt("age", 0);
                        }
                    } else if (ultraSniffer.tickCount <= 100) {
                        ultraSniffer.setHealth(ultraSniffer.getMaxHealth());
                        ultraSniffer.getPersistentData().putInt("age", 0);
                    }
                }
                if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.SPECTATOR.get())) {
                    multiplyDamage(living, 0.8);
                } else if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.SAILOR.get())) {
                    multiplyDamage(living, 0.9);
                } else if (BeyonderUtil.currentPathwayMatchesNoException(living, BeyonderClassInit.MONSTER.get())) {
                    if (!ultraSniffer.getPersistentData().getBoolean("PtDGaveLuck")) {
                        ultraSniffer.getPersistentData().putDouble("luck", 5000);
                        ultraSniffer.getPersistentData().putBoolean("PtDGaveLuck", true);
                    }
                }
            }
            if (living instanceof Mob superSnifferEntity && typeId.equals(PTDEntities.SUPER_SNIFFER) && superSnifferEntity.getTarget() == null) {
                for (Player player : superSnifferEntity.level().getEntitiesOfClass(Player.class, superSnifferEntity.getBoundingBox().inflate(50))) {
                    if (!player.isCreative() && !player.isSpectator()) {
                        superSnifferEntity.setTarget(player);
                    }
                }
            }
            if (living instanceof Mob mob && PTDUtil.isBeyonderEntity(mob) && tickCount % 10 == 0) {
                if (mob.getTarget() == null && combatTimer == 0 && mob.getHealth() < mob.getMaxHealth() && mob.isAlive() && !Float.isNaN(mob.getHealth())) {
                    mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + (mob.getMaxHealth() * 0.02f)));
                }
                if (mob.getTarget() != null && mob.getTarget() instanceof Player) {
                    Level level = mob.level();
                    AABB box = mob.getBoundingBox().inflate(1.0);
                    BlockPos.betweenClosedStream(box).forEach(pos -> {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && state.getDestroySpeed(level, pos) >= 0 && pos.getY() >= mob.getY() + 1 && mob.getTarget().getY() > mob.getEyeY() && state != Blocks.WATER.defaultBlockState() && state != Blocks.LAVA.defaultBlockState()) {
                            level.destroyBlock(pos, true, mob);
                        }
                    });
                }
            }
        }
    }


    @SubscribeEvent
    public static void onEntityChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity living = event.getEntity();
        CompoundTag tag = living.getPersistentData();
        BeyonderClass pathway = BeyonderUtil.getPathway(living);
        if (!living.level().isClientSide() && (event.getOriginalTarget() instanceof Player || event.getNewTarget() instanceof Player) && event.getOriginalTarget() != null && event.getNewTarget() != null) {
            if (PTDUtil.isBeyonderEntity(living) && living instanceof Mob mob) {
                float newTargetHealth = event.getNewTarget().getHealth();
                float originalTargetHealth = event.getOriginalTarget().getHealth();
                if (newTargetHealth > originalTargetHealth) {
                    event.setCanceled(true);
                } else if (event.getNewTarget().distanceTo(living) > event.getOriginalTarget().distanceTo(living) && event.getNewTarget().getHealth() > event.getOriginalTarget().getHealth()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void livingDeathEvent(LivingDeathEvent event) {
        Entity entity = event.getEntity();
        if (!event.getEntity().level().isClientSide()) {
            if (entity.getName().getString().contains("vessel")) {
                dropAt(entity, PTDItems.get(PTDItems.POCKET_UNIVERSE));
            } else if (PTDEntities.DUSKROK.equals(PTDEntities.idOf(entity))) {
                dropAt(entity, Items.NETHERITE_SCRAP);
            } else if (entity.getName().getString().equalsIgnoreCase("wither")) {
                dropAt(entity, PTDItems.get(PTDItems.LORD_SOUL_DARK));
                dropAt(entity, PTDItems.get(PTDItems.SHARD_OF_UNCERTAINTY));
            }
        }
    }

    /** Drops a never-despawning item at the entity. Skipped when the item's mod is not installed. */
    private static void dropAt(Entity entity, @Nullable Item item) {
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        ItemEntity itemEntity = new ItemEntity(entity.level(), entity.getX(), entity.getY(), entity.getZ(), stack);
        itemEntity.setNoPickUpDelay();
        itemEntity.teleportTo(entity.getX(), entity.getY(), entity.getZ());
        itemEntity.setUnlimitedLifetime();
        entity.level().addFreshEntity(itemEntity);
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


            tag.putInt("PTDCombatTimer", 200);
            if (entitySource instanceof LivingEntity livingEntity) {
                if (PTDUtil.isBeyonderEntity(livingEntity) && directSource instanceof Projectile) {
                    event.setAmount(event.getAmount() * 0.6f);
                }
            }


            if (damageDealer != null) {
                if (damageDealer.getPersistentData().contains("PTDDamageMultiplier")) {
                    event.setAmount((float) (event.getAmount() * damageDealer.getPersistentData().getDouble("PTDDamageMultiplier")));
                }
            }
        }
    }


    /** Damage multiplier for weapon projectiles/effects from other mods, 1.0 when none applies. */
    private static float projectileDamageMultiplier(Entity directSource) {
        ResourceLocation sourceId = PTDEntities.idOf(directSource);
        if (sourceId.equals(PTDEntities.SOLARIS_BOMB)) {
            return 5.0f;
        } else if (sourceId.equals(PTDEntities.CRESCENTIA_DRAGON)) {
            return 2.0f;
        } else if (sourceId.equals(PTDEntities.FROSTBOUND_SHARD)) {
            return 6.5f;
        } else if (sourceId.equals(PTDEntities.PUMPKIN_PISTOL_PROJECTILE)) {
            if (directSource instanceof Projectile projectile && projectile.getOwner() != null && projectile.getOwner() instanceof Player) {
                return 7.0f;
            }
        } else if (sourceId.equals(PTDEntities.FREYR_SWORD)) {
            return 1.8f;
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
                ResourceLocation typeId = EntityType.getKey(type);
                if (typeId.equals(PTDEntities.KRAMPUS)) {
                    event.setCanceled(true);
                }
                if (typeId.equals(PTDEntities.KRAMPUS_HENCHMAN)) {
                    event.setCanceled(true);
                }
                // Sequence 9
                if (typeId.equals(PTDEntities.OVERGROWN_COLOSSUS)) {
                    multiplyMaxHealth(living, 1.5);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.GEM_DEVOURING_WORM.get()) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 1.3);
                }
                else if (typeId.equals(PTDEntities.WARPED_FUNGUSSUS)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.4);
                } else if (typeId.equals(PTDEntities.KOBOLEDIATOR)) {
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.UMVUTHI)) {
                    multiplyMaxHealth(living, 1.0);
                } else if (typeId.equals(PTDEntities.MAW)) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 1.5);
                } else if (typeId.equals(PTDEntities.SKELETOSAURUS)) {
                    multiplyMaxHealth(living, 1.2);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.NIGHTMARE_STALKER)) {
                    multiplyMaxHealth(living, 1.2);
                } else if (typeId.equals(PTDEntities.WROUGHTNAUT)) {
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.DIRE_HOUND_LEADER)) {
                    multiplyMaxHealth(living, 1.25);
                    multiplyDamage(living, 1.5);

                    // Sequence 8
                } else if (typeId.equals(PTDEntities.BLAST_CANNON)) {
                    multiplyMaxHealth(living, 1.25);
                } else if (typeId.equals(PTDEntities.FROSTBITTEN_GOLEM)) {
                    multiplyMaxHealth(living, 1.3);
                } else if (typeId.equals(PTDEntities.DUSKROK)) {
                    multiplyMaxHealth(living, 1.3);
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.MUTANT_SKELETON)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.2);
                } else if (living.getName().getString().toLowerCase().contains("terrible") || living.getName().getString().toLowerCase().contains("puny")) { //Terrible Ten
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.3);
                    //} else if (type == AMEntityRegistry.WARPED_MOSCO.get()) {
                    //    multiplyMaxHealth(living, 1.0);
                    //    multiplyDamage(living, 1.3);
                } else if (type == EntityType.ELDER_GUARDIAN) {
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.MUTANT_ENDERMAN)) {
                    multiplyMaxHealth(living, 1.4);
                    multiplyDamage(living, 1.2);
                } else if (living.getName().getString().toLowerCase().contains("aero_guardian")) { //Aero Guardian
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.6);
                } else if (typeId.equals(PTDEntities.SPIRITOF_CHAOS)) {
                    multiplyMaxHealth(living, 4.0);
                } else if (typeId.equals(PTDEntities.MOTHER_SPIDER)) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 2.0);
                }else if (typeId.equals(PTDEntities.HELLROK)) {
                    multiplyMaxHealth(living, 1.3);
                    multiplyDamage(living, 1.4);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.SPIRIT_EATER.get()) {
                    multiplyMaxHealth(living, 2.5);
                    multiplyDamage(living, 1.2);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.DEEP_SEA_MARLIN.get()) {
                    multiplyMaxHealth(living, 1.3);
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.MUTANT_ZOMBIE)) {
                    multiplyMaxHealth(living, 1.5);

                    // Sequence 7
                }else if (typeId.equals(PTDEntities.WARPED_FUNGUSSUS)) {
                    multiplyMaxHealth(living, 1.8);
                }
                else if (typeId.equals(PTDEntities.ANCIENT_GUARDIAN)) {
                    multiplyMaxHealth(living, 1.3);
                } else if (living.getName().getString().toLowerCase().contains("doomharbor")) { //Doomharbor Lich
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.75);
                } else if (typeId.equals(PTDEntities.FROSTMAW)) {
                    multiplyMaxHealth(living, 1.2);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.MAZE_MOTHER)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.6);
                } else if (living.getName().getString().toLowerCase().contains("plague_bringer")) { //Plague Bringer
                    multiplyMaxHealth(living, 0.7);
                    multiplyDamage(living, 1.7);
                } else if (entity.getClass().getSimpleName().equals("LichEntity")) { //Lich (Bosses of Mass Destruction)
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.1);
                } else if (typeId.equals(PTDEntities.WITHERED_ABOMINATION)) {
                    multiplyMaxHealth(living, 1.2);
                } else if (entity.getClass().getSimpleName().equals("GauntletEntity")) { //Nether Gauntlet
                    multiplyMaxHealth(living, 1.1);
                    multiplyDamage(living, 2.0);
                }else if (entity.getClass().getSimpleName().equals("ObsidilithEntity")) { //Obsidilith
                    multiplyMaxHealth(living, 1.8);
                    multiplyDamage(living, 1.2);

                    //Sequence 6
                } else if (type == EntityType.WITHER) {
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.1);
                } else if (entity.getClass().getSimpleName().equals("VoidBlossomEntity")) { //Void Blossom
                    multiplyMaxHealth(living, 1.2);
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.HEROBRINE)) {
                    multiplyMaxHealth(living, 1.0);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.LIFESTEALER)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.5);
                } else if (typeId.equals(PTDEntities.LAVA_EATER)) {
                    multiplyMaxHealth(living, 1.7);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.SIR_PUMPKINHEAD)) {
                    multiplyMaxHealth(living, 2.2);
                    multiplyDamage(living, 2.8);
                } else if (typeId.equals(PTDEntities.ENDERSENT)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 4.0);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.SHADOWLESS_DEMONIC_WOLF.get()) {
                    multiplyMaxHealth(living, 1.5);
                }
                else if (living.getName().getString().toLowerCase().contains("dyrolian")) {
                    multiplyDamage(living, 1.3);
                    multiplyMaxHealth(living, 0.5);

                    // Sequence 5
                } else if (typeId.equals(PTDEntities.CHAOS_MONARCH)) {
                    multiplyMaxHealth(living, 1.8);
                    multiplyDamage(living, 4.5);


                    living.getPersistentData().putDouble("luck", 500);
                } else if (typeId.equals(PTDEntities.NETHERITE_MONSTROSITY)) {
                    multiplyDamage(living, 1.5);
                    multiplyMaxHealth(living, 1.2);
                } else if (typeId.equals(PTDEntities.THE_HARBINGER)) {
                    multiplyMaxHealth(living, 1.6);
                    multiplyDamage(living, 1.8);
                }  else if (typeId.equals(PTDEntities.CAPTAIN_CORNELIA)) {
                    multiplyMaxHealth(living, 1.3);
                    multiplyDamage(living, 1.3);
                } else if (typeId.equals(PTDEntities.DRAUGR_BOSS)) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living, 1.2);


                } else if (typeId.equals(PTDEntities.NIGHT_SHADE)) {
                    multiplyMaxHealth(living, 2.5);
                    multiplyDamage(living, 1.5);


                } else if (typeId.equals(PTDEntities.ACCURSED_LORD_BOSS)) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 1.7);
                } else if (typeId.equals(PTDEntities.RETURNING_KNIGHT)) {
                    multiplyMaxHealth(living, 1.6);
                    multiplyDamage(living, 1.6);
                } else if (typeId.equals(PTDEntities.ENDER_GUARDIAN)) {
                    multiplyMaxHealth(living, 1.7);
                    multiplyDamage(living, 1.6);
                } else if (typeId.equals(PTDEntities.POSESSED_PALADIN)) {
                    multiplyMaxHealth(living, 2.0);
                    multiplyDamage(living, 1.2);

                    // Sequence 4
                } else if (typeId.equals(PTDEntities.IGNIS)) {
                    multiplyMaxHealth(living, 1.2);
                    multiplyDamage(living, 0.9);
                } else if (typeId.equals(PTDEntities.CLOUD_GOLEM)) {
                    multiplyMaxHealth(living, 2.0);


                } else if (typeId.equals(PTDEntities.THE_LEVIATHAN)) {
                    multiplyMaxHealth(living, 1.0);


                } else if (typeId.equals(PTDEntities.SCYLLA)) {
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 2.0);


                } else if (typeId.equals(PTDEntities.GOB)) {
                    multiplyMaxHealth(living, 3.0);
                    multiplyDamage(living, 1.8);
                } else if (living.getName().getString().equalsIgnoreCase("horseman")) { //Pumpkin Horseman
                    multiplyMaxHealth(living, 1.5);


                }else if (typeId.equals(PTDEntities.NAMELESS_GUARDIAN)) {
                    multiplyMaxHealth(living, 1.7);
                    multiplyDamage(living, 2.2);


                } else if (typeId.equals(PTDEntities.MALEDICTUS)) {
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 2.5);
                } else if (typeId.equals(PTDEntities.ANCIENT_REMNANT)) {
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 1.1);
                }


                // Sequence 3
                else if (living.getName().getString().toLowerCase().contains("vessel")) { //Vessel of Calamity
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 2.5);
                } else if (typeId.equals(PTDEntities.MOONKNIGHT)) {
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 2.0);


                }  else if (typeId.equals(PTDEntities.LORD_PUMPKINHEAD)) {
                    multiplyMaxHealth(living, 5.0);
                    multiplyDamage(living, 1.2);


                } else if (typeId.equals(PTDEntities.TRIAL_GUARDIAN)) {
                    multiplyMaxHealth(living, 4.0);


                    // Sequence 2
                } else if (typeId.equals(PTDEntities.SUPER_SNIFFER)) {
                    multiplyMaxHealth(living, 6.0);
                    multiplyDamage(living, 1.2);
                } else if (typeId.equals(PTDEntities.DAY_STALKER)) {
                    multiplyMaxHealth(living, 4.0);
                    multiplyDamage(living, 1.1);

                } else if (typeId.equals(PTDEntities.NIGHT_PROWLER)) {
                    multiplyMaxHealth(living, 3.0);
                    multiplyDamage(living, 0.4);

                } else if (typeId.equals(PTDEntities.GUNDALF)) {
                    multiplyMaxHealth(living, 6.0);
                    multiplyDamage(living, 1.5);

                    // Sequence 1
                } else if (typeId.equals(PTDEntities.ULTRA_SNIFFER)) {
                    multiplyMaxHealthUltraSniffer(living, 10.0);
                } else if (type == net.swimmingtuna.lotm.init.EntityInit.INTERDIMENSIONAL_HUNTER.get()) {
                    multiplyMaxHealth(living, 2.5);
                    multiplyDamage(living, 1);
                }  else if (type == net.swimmingtuna.lotm.init.EntityInit.WANDERING.get()) {
                    multiplyMaxHealth(living, 1.5);
                    multiplyDamage(living,0.8);
                }
            } else if (entity instanceof ItemEntity item) {
                if (PTDItems.is(item.getItem(), PTDItems.MUSIC_SHEET_OF_UNTIMELY_DEATH)) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void serverStartEvent(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        if (ModCompat.isLoaded(ModCompat.BORN_IN_CHAOS)) {
            BornInChaosCompat.disableKrampusSpawns(server);
        }
        Commands commands = server.getCommands();
        CommandSourceStack commandSource = server.createCommandSourceStack();
        try {
            addBeyonderEntity(commands, commandSource, "soulsweapons:chaos_monarch", "lotm:monster 7");
            addBeyonderEntity(commands, commandSource, "cataclysm:ender_guardian", "lotm:apprentice 7"); //11111111
            addBeyonderEntity(commands, commandSource, "legendary_monsters:cloud_golem", "lotm:sailor 6");
            addBeyonderEntity(commands, commandSource, "cataclysm:ancient_ancient_remnant", "lotm:apprentice 6");
            addBeyonderEntity(commands, commandSource, "soulsweapons:returning_knight", "lotm:sailor 7"); //11111111
            addBeyonderEntity(commands, commandSource, "aquamirae:captain_cornelia", "lotm:warrior 7"); //11111111
            addBeyonderEntity(commands, commandSource, "cataclysm:the_leviathan", "lotm:monster 6"); //11111111
            addBeyonderEntity(commands, commandSource, "sleepy_hollows:horseman", "lotm:spectator 6"); //11111111
            addBeyonderEntity(commands, commandSource, "born_in_chaos_v1:lord_pumpkinhead", "lotm:warrior 5"); //11111111
            addBeyonderEntity(commands, commandSource, "soulsweapons:moonknight", "lotm:spectator 5");  //11111111
            addBeyonderEntity(commands, commandSource, "cataclysm:the_harbinger", "lotm:warrior 7");  //11111111
            addBeyonderEntity(commands, commandSource, "terramity:gob", "lotm:monster 6");
            addBeyonderEntity(commands, commandSource, "soulsweapons:draugr_boss", "lotm:warrior 8");//11111111
            addBeyonderEntity(commands, commandSource, "legendary_monsters:posessed_paladin", "lotm:apprentice 7");  //11111111
            addBeyonderEntity(commands, commandSource, "monsterexpansion:leivekilth", "lotm:sailor 5");  //11111111
            addBeyonderEntity(commands, commandSource, "eeeabsmobs:realm_warden", "lotm:spectator 7");  //11111111
            int random = (int) BeyonderUtil.getPositiveRandomInRange(5);
            if (random == 0) {
                addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:spectator 3");
            } else if (random == 1) {
                addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:warrior 3");
            } else if (random == 2) {
                addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:sailor 3");
            } else if (random == 3) {
                addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:apprentice 3");
            } else {
                addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:monster 3");
            }

        } catch (Exception e) {
            PTD.LOGGER.info("Failed to execute beyonderrecipe load command: {}", e.getMessage());
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 1200 == 0) {
            MinecraftServer server = event.getServer();
            float foundSniffers = 0;
            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (PTDEntities.ULTRA_SNIFFER.equals(PTDEntities.idOf(entity))) {
                        foundSniffers++;
                    }
                }
            }
            Commands commands = server.getCommands();
            CommandSourceStack commandSource = server.createCommandSourceStack();
            addBeyonderEntity(commands, commandSource, "soulsweapons:chaos_monarch", "lotm:monster 7");
            addBeyonderEntity(commands, commandSource, "cataclysm:ender_guardian", "lotm:apprentice 7"); //11111111
            addBeyonderEntity(commands, commandSource, "legendary_monsters:cloud_golem", "lotm:sailor 6");
            addBeyonderEntity(commands, commandSource, "cataclysm:ancient_ancient_remnant", "lotm:apprentice 6");
            addBeyonderEntity(commands, commandSource, "soulsweapons:returning_knight", "lotm:sailor 7"); //11111111
            addBeyonderEntity(commands, commandSource, "aquamirae:captain_cornelia", "lotm:warrior 7"); //11111111
            addBeyonderEntity(commands, commandSource, "cataclysm:the_leviathan", "lotm:monster 6"); //11111111
            addBeyonderEntity(commands, commandSource, "sleepy_hollows:horseman", "lotm:spectator 6"); //11111111
            addBeyonderEntity(commands, commandSource, "born_in_chaos_v1:lord_pumpkinhead", "lotm:warrior 5"); //11111111
            addBeyonderEntity(commands, commandSource, "soulsweapons:moonknight", "lotm:spectator 5");  //11111111
            addBeyonderEntity(commands, commandSource, "cataclysm:the_harbinger", "lotm:warrior 7");  //11111111
            addBeyonderEntity(commands, commandSource, "terramity:gob", "lotm:monster 6");
            addBeyonderEntity(commands, commandSource, "soulsweapons:draugr_boss", "lotm:warrior 8");//11111111
            addBeyonderEntity(commands, commandSource, "legendary_monsters:posessed_paladin", "lotm:apprentice 7");  //11111111
            addBeyonderEntity(commands, commandSource, "monsterexpansion:leivekilth", "lotm:sailor 5");  //11111111
            addBeyonderEntity(commands, commandSource, "eeeabsmobs:realm_warden", "lotm:spectator 7");  //11111111

            if (foundSniffers == 0) {
                int random = (int) BeyonderUtil.getPositiveRandomInRange(4);
                if (random == 0) {
                    addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:spectator 3");
                } else if (random == 1) {
                    addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:warrior 3");
                } else if (random == 2) {
                    addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:sailor 3");
                } else {
                    addBeyonderEntity(commands, commandSource, "terramity:ultra_sniffer", "lotm:monster 3");
                }
            }
        }
    }

    /** Runs {@code /beyonderentity add}, skipping mobs whose mod is not installed. */
    private static void addBeyonderEntity(Commands commands, CommandSourceStack commandSource, String entityId, String pathwayAndSequence) {
        if (ModCompat.isLoaded(new ResourceLocation(entityId))) {
            commands.performPrefixedCommand(commandSource, "beyonderentity add " + entityId + " " + pathwayAndSequence);
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

    public static void multiplyMaxHealth(LivingEntity living, double multiplier) {
        if (!living.getPersistentData().getBoolean("maxHealthMultiplied")) {
            float multiplierAmount = (float) (multiplier * PTDConfig.COMMON.healthMultiplier.get());
            float maxHealth = living.getMaxHealth();
            AttributeInstance maxHealthAttribute = living.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                if (maxHealth < 10000) {
                    maxHealthAttribute.setBaseValue(maxHealth * multiplierAmount);
                }
            }
            living.setHealth(maxHealth * multiplierAmount);
            living.getPersistentData().putBoolean("maxHealthMultiplied", true);
            LOTM.LOGGER.info("Multiplied {}'s health by {}", living.getName().getString(), multiplier);
        }
    }

    public static void multiplyMaxHealthUltraSniffer(LivingEntity living, double multiplier) {
        if (!living.getPersistentData().getBoolean("maxHealthMultiplied")) {
            float multiplierAmount = (float) (multiplier * PTDConfig.COMMON.healthMultiplier.get());
            float maxHealth = living.getMaxHealth();
            AttributeInstance maxHealthAttribute = living.getAttribute(Attributes.MAX_HEALTH);
            if (maxHealthAttribute != null) {
                maxHealthAttribute.setBaseValue(maxHealth * multiplierAmount + 1);
            }
            living.setHealth(maxHealth * multiplierAmount);
            living.getPersistentData().putBoolean("maxHealthMultiplied", true);
            LOTM.LOGGER.info("Multiplied {}'s health by {}", living.getName().getString(), multiplier);
        }
    }

    private static void multiplyDamage(LivingEntity entity, double multiplier) {
        if (!entity.getPersistentData().getBoolean("damageMultiplied")) {
            CompoundTag tag = entity.getPersistentData();
            tag.putDouble("PTDDamageMultiplier", multiplier * PTDConfig.COMMON.damageMultiplier.get());
            entity.getPersistentData().putBoolean("damageMultiplied", true);
            LOTM.LOGGER.info("Multiplied {}'s damage by {}", entity.getName().getString(), multiplier);
        }
    }
}