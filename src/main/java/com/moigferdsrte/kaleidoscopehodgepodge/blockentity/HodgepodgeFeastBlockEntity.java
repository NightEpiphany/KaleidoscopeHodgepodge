package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
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
        return addAgainst(ingredients, ingredient, hitX, hitZ, rotation, food);
    }

    public PlacementSpace.Result addAgainst(List<PlacedIngredient> existing, PackingIngredients ingredient,
                                             int hitX, int hitZ, int rotation, IngredientFoodData food) {
        ContainerLimits limits = limits();
        if (ingredients.size() >= limits.capacity()) {
            return PlacementSpace.Result.failure(PlacementSpace.Failure.CAPACITY);
        }
        PlacementSpace.Result result = PlacementSpace.place(existing, ingredient, hitX, hitZ,
                Math.max(limits.capacity(), existing.size() + 1), limits.baseHeight(),
                placementBounds(), rotation, food);
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
        PlacementSpace.Bounds bounds = placementBounds();
        values.stream()
                .filter(value -> PlacementSpace.within(value, bounds))
                .limit(limits.capacity())
                .forEach(ingredients::add);
        contentRevision++;
        refresh();
    }

    public CustomFeastData snapshot() {
        return new CustomFeastData(kind(), Direction.NORTH, ingredients);
    }

    public CustomFeastData.ContainerKind kind() {
        return getBlockState().is(KHBlocks.PORCELAIN_SOUP_BOWL)
                ? CustomFeastData.ContainerKind.SOUP : CustomFeastData.ContainerKind.DISH;
    }

    public ContainerLimits limits() {
        GeneralConfig.Snapshot config = GeneralConfig.snapshot();
        if (getBlockState().is(KHBlocks.WOODEN_PLATE)) {
            return new ContainerLimits(config.woodenPlateCapacity(), config.dishBaseHeight(),
                    config.woodenMaxModelHeight());
        }
        if (getBlockState().is(KHBlocks.PORCELAIN_PLATE)
                || getBlockState().is(KHBlocks.MEDIAN_PORCELAIN_PLATE)
                || getBlockState().is(KHBlocks.LARGE_PORCELAIN_PLATE)) {
            return new ContainerLimits(config.porcelainCapacity(), config.dishBaseHeight(),
                    config.porcelainMaxModelHeight());
        }
        return new ContainerLimits(config.soupCapacity(), config.soupBaseHeight(),
                config.porcelainMaxModelHeight());
    }

    public PlacementSpace.Bounds placementBounds() {
        int maxHeight = limits().maxHeight();
        return getBlockState().getBlock() instanceof IHodgepodge hodgepodge
                ? hodgepodge.placementBounds(getBlockState(), maxHeight)
                : PlacementSpace.Bounds.full(maxHeight);
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
        PlacementSpace.Bounds bounds = placementBounds();
        for (PlacedIngredient ingredient : input.listOrEmpty(INGREDIENTS, PlacedIngredient.CODEC)) {
            if (ingredients.size() == Math.min(MAX_SERIALIZED_INGREDIENTS, limits.capacity())) break;
            if (PlacementSpace.within(ingredient, bounds)) ingredients.add(ingredient);
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
