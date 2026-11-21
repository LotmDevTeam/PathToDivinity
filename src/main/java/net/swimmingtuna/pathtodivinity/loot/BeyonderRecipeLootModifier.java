package net.swimmingtuna.pathtodivinity.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.swimmingtuna.pathtodivinity.config.PTDServerConfig;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Adds Beyonder recipe items (LOTM's {@code lotm:recipes/*}) of one Sequence to loot, like LOTM's own
 * {@code lotm:add_item} modifier, but takes the chance and the loot tables it applies to from
 * {@code [beyonder_recipes.sequence_N]} in the server config instead of from the JSON.
 *
 * <p>Path to Divinity overrides LOTM's {@code lotm:seq_N_recipes_from_chest} modifiers with this one, so
 * the config replaces LOTM's hardcoded values. The items themselves stay in the JSON, so a datapack can
 * still change which recipes drop.
 */
public class BeyonderRecipeLootModifier extends LootModifier {

    public static final MapCodec<BeyonderRecipeLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst)
            .and(Codec.intRange(0, 9).fieldOf("sequence").forGetter(m -> m.sequence))
            .and(BuiltInRegistries.ITEM.byNameCodec().listOf().fieldOf("item").forGetter(m -> m.items))
            .apply(inst, BeyonderRecipeLootModifier::new));

    private final int sequence;
    private final List<Item> items;

    public BeyonderRecipeLootModifier(LootItemCondition[] conditions, int sequence, List<Item> items) {
        super(conditions);
        this.sequence = sequence;
        this.items = items;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (items.isEmpty()
                || !PTDServerConfig.beyonderRecipeSpawnsIn(sequence, context.getQueriedLootTableId())
                || context.getRandom().nextFloat() >= PTDServerConfig.beyonderRecipeChance(sequence)) {
            return generatedLoot;
        }

        // Same number of recipes as LOTM's lotm:add_item: 1 (50%), 2 (35%) or 3+ (15%).
        RandomSource random = context.getRandom();
        int numToAdd;
        float roll = random.nextFloat();
        if (roll < 0.5f) {
            numToAdd = 1;
        } else if (roll < 0.85f) {
            numToAdd = 2;
        } else {
            numToAdd = 3 + random.nextInt(Math.max(1, items.size() - 3));
        }

        List<Item> shuffled = new ArrayList<>(items);
        for (int i = shuffled.size() - 1; i > 0; i--) {
            Collections.swap(shuffled, i, random.nextInt(i + 1));
        }
        for (int i = 0; i < Math.min(numToAdd, shuffled.size()); i++) {
            generatedLoot.add(new ItemStack(shuffled.get(i)));
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
