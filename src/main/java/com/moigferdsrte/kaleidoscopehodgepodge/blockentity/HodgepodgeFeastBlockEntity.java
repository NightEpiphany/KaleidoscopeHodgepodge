package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.moigferdsrte.kaleidoscopehodgepodge.block.HodgepodgePlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HodgepodgeFeastBlockEntity extends BlockEntity {
    private static final String INGREDIENTS = "ingredients";
    private static final int MAX_SERIALIZED_INGREDIENTS = 40;
    private final List<PlacedIngredient> ingredients = new ArrayList<>(MAX_SERIALIZED_INGREDIENTS);
    private final List<PlacedIngredient> renderIngredients = Collections.unmodifiableList(ingredients);
    private int contentRevision;
    private int shapeRevision = -1;
    private VoxelShape ingredientShape = Shapes.empty();

    public HodgepodgeFeastBlockEntity(BlockPos pos, BlockState state) {
        super(KHBlockEntities.FEAST, pos, state);
    }

    public PlacementSpace.Result add(PackingIngredients ingredient, int hitX, int hitZ) {
        return add(ingredient, hitX, hitZ, 0);
    }

    public PlacementSpace.Result add(PackingIngredients ingredient, int hitX, int hitZ, int rotation) {
        return add(ingredient, hitX, hitZ, rotation, IngredientFoodData.EMPTY);
    }

    public PlacementSpace.Result add(PackingIngredients ingredient, int hitX, int hitZ, int rotation,
                                     IngredientFoodData food) {
        ContainerLimits limits = limits();
        PlacementSpace.Result result = PlacementSpace.place(ingredients, ingredient, hitX, hitZ,
                limits.capacity(), limits.baseHeight(), limits.maxHeight(), rotation, food);
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

    public VoxelShape ingredientShape() {
        if (shapeRevision == contentRevision) return ingredientShape;
        VoxelShape combined = Shapes.empty();
        for (PlacedIngredient ingredient : ingredients) {
            combined = Shapes.or(combined, IngredientHitTest.localShape(ingredient));
        }
        ingredientShape = combined.optimize();
        shapeRevision = contentRevision;
        return ingredientShape;
    }

    public void setIngredients(List<PlacedIngredient> values) {
        ingredients.clear();
        ContainerLimits limits = limits();
        values.stream()
                .filter(value -> PlacementSpace.within(value, limits.maxHeight()))
                .limit(limits.capacity())
                .forEach(ingredients::add);
        contentRevision++;
        refresh();
    }

    public CustomFeastData snapshot() {
        return new CustomFeastData(kind(), Direction.NORTH, ingredients);
    }

    public CustomFeastData.ContainerKind kind() {
        return getBlockState().getBlock() instanceof HodgepodgePlateBlock
                ? CustomFeastData.ContainerKind.DISH : CustomFeastData.ContainerKind.SOUP;
    }

    public ContainerLimits limits() {
        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        if (getBlockState().is(KHBlocks.WOODEN_PLATE)) {
            return new ContainerLimits(config.woodenPlateCapacity(), config.dishBaseHeight(),
                    config.woodenMaxModelHeight());
        }
        if (getBlockState().is(KHBlocks.PORCELAIN_PLATE)) {
            return new ContainerLimits(config.porcelainCapacity(), config.dishBaseHeight(),
                    config.porcelainMaxModelHeight());
        }
        return new ContainerLimits(config.soupCapacity(), config.soupBaseHeight(),
                config.porcelainMaxModelHeight());
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
        ContainerLimits limits = limits();
        for (PlacedIngredient ingredient : input.listOrEmpty(INGREDIENTS, PlacedIngredient.CODEC)) {
            if (ingredients.size() == Math.min(MAX_SERIALIZED_INGREDIENTS, limits.capacity())) break;
            if (PlacementSpace.within(ingredient, limits.maxHeight())) ingredients.add(ingredient);
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

    public record ContainerLimits(int capacity, int baseHeight, int maxHeight) {}
}
