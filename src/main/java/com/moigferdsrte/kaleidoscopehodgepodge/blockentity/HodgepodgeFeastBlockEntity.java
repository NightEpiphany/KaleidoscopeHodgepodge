package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HodgepodgeFeastBlockEntity extends BlockEntity {
    private static final String INGREDIENTS = "ingredients";
    private final List<PlacedIngredient> ingredients = new ArrayList<>(12);
    private final List<PlacedIngredient> renderIngredients = Collections.unmodifiableList(ingredients);
    private int contentRevision;

    public HodgepodgeFeastBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.FEAST, pos, state);
    }

    public PlacementSpace.Result add(PackingIngredients ingredient, int hitX, int hitZ) {
        PlacementSpace.Result result = PlacementSpace.place(ingredients, ingredient, hitX, hitZ, kind());
        result.placement().ifPresent(value -> {
            ingredients.add(value);
            contentRevision++;
            refresh();
        });
        return result;
    }

    public List<PlacedIngredient> ingredients() {
        return List.copyOf(ingredients);
    }

    public Optional<PlacedIngredient> removeIngredient(int index) {
        if (index < 0 || index >= ingredients.size()) return Optional.empty();
        PlacedIngredient removed = ingredients.remove(index);
        contentRevision++;
        refresh();
        return Optional.of(removed);
    }

    public List<PlacedIngredient> renderIngredients() {
        return renderIngredients;
    }

    public int contentRevision() {
        return contentRevision;
    }

    public void setIngredients(List<PlacedIngredient> values) {
        ingredients.clear();
        ingredients.addAll(values.subList(0, Math.min(12, values.size())));
        contentRevision++;
        refresh();
    }

    public CustomFeastData snapshot() {
        return new CustomFeastData(kind(), Direction.NORTH, ingredients);
    }

    private CustomFeastData.ContainerKind kind() {
        return getBlockState().getBlock() instanceof HodgepodgePlateBlock
                ? CustomFeastData.ContainerKind.DISH : CustomFeastData.ContainerKind.SOUP;
    }

    private void refresh() {
        setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(@NonNull ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<PlacedIngredient> list = output.list(INGREDIENTS, PlacedIngredient.CODEC);
        ingredients.forEach(list::add);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        ingredients.clear();
        for (PlacedIngredient ingredient : input.listOrEmpty(INGREDIENTS, PlacedIngredient.CODEC)) {
            if (ingredients.size() == 12) break;
            ingredients.add(ingredient);
        }
        contentRevision++;
    }

    @Override
    public @NonNull CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
