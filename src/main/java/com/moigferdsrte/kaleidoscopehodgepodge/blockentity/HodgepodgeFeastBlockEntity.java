package com.moigferdsrte.kaleidoscopehodgepodge.blockentity;

import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.api.IHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.block.AbstractMultiBlockPlateBlock;
import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientHitTest;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientCollisionHeightMap;
import com.moigferdsrte.kaleidoscopehodgepodge.core.IngredientFoodData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacedIngredient;
import com.moigferdsrte.kaleidoscopehodgepodge.core.PlacementSpace;
import com.moigferdsrte.kaleidoscopehodgepodge.core.HodgepodgeRecipeData;
import com.moigferdsrte.kaleidoscopehodgepodge.core.FeastCodec;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlockEntities;
import com.moigferdsrte.kaleidoscopehodgepodge.init.KHBlocks;
import com.moigferdsrte.kaleidoscopehodgepodge.init.PackingIngredients;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
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
    private static final int MAX_SERIALIZED_INGREDIENTS = 360;
    private static final int PLACE_ANIMATION_EVENT = 1;
    private final List<PlacedIngredient> ingredients = new ArrayList<>(MAX_SERIALIZED_INGREDIENTS);
    private final List<PlacedIngredient> renderIngredients = Collections.unmodifiableList(ingredients);
    private int contentRevision;
    private int shapeRevision = -1;
    private VoxelShape ingredientShape = Shapes.empty();
    private int ingredientCollisionRevision = -1;
    private VoxelShape ingredientCollisionShape = Shapes.empty();
    private int collisionShapeRevision = -1;
    private int collisionShapeStateHash;
    private VoxelShape collisionShape = Shapes.empty();
    private int outlineShapeRevision = -1;
    private int outlineShapeStateHash;
    private VoxelShape outlineShape = Shapes.empty();
    private long structureShapeKey = Long.MIN_VALUE;
    private VoxelShape structureShape = Shapes.empty();
    private long structureOutlineShapeKey = Long.MIN_VALUE;
    private VoxelShape structureOutlineShape = Shapes.empty();
    private long structureCollisionShapeKey = Long.MIN_VALUE;
    private VoxelShape structureCollisionShape = Shapes.empty();
    private int placementAnimationRevision;
    private int placementAnimationIndex = -1;
    private long placementAnimationStartedAt;
    private HodgepodgeRecipeData lockedRecipe;
    private Optional<Component> dishName = Optional.empty();
    private int recipeIndex;
    /** Sorted recipe targets cached while a container is locked. */
    private List<PlacedIngredient> lockedTargets = List.of();

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
        HodgepodgeFeastBlockEntity controller = recipeController();
        PlacedIngredient expected = controller.nextRecipePlacement()
                .map(value -> getBlockState().getBlock() instanceof IHodgepodge surface
                        ? surface.recipePlacementTarget(worldPosition, getBlockState(), value) : value)
                .orElse(null);
        if (controller.lockedRecipe != null) {
            if (expected == null) {
                return PlacementSpace.Result.failure(PlacementSpace.Failure.OUT_OF_BOUNDS);
            }
            if (!expected.id().equals(ingredient.getId())) {
                return PlacementSpace.Result.failure(PlacementSpace.Failure.OUT_OF_BOUNDS);
            }
            // Recipes own orientation; callers do not need to pre-rotate the held model.
            rotation = expected.rotation();
        }
        if (ingredients.size() >= limits.capacity()) {
            return PlacementSpace.Result.failure(PlacementSpace.Failure.CAPACITY);
        }
        PlacementSpace.Result result = PlacementSpace.place(existing, ingredient, hitX, hitZ,
                Math.max(limits.capacity(), existing.size() + 1), limits.baseHeight(),
                placementBounds(), rotation, food);
        if (result.placement().isPresent() && controller.lockedRecipe != null
                && !samePlacement(result.placement().get(), expected)) {
            return PlacementSpace.Result.failure(PlacementSpace.Failure.OUT_OF_BOUNDS);
        }
        result.placement().ifPresent(value -> {
            ingredients.add(value);
            contentRevision++;
            if (controller.lockedRecipe != null) {
                controller.recipeIndex++;
                if (controller.recipeIndex >= controller.lockedTargets.size()) {
                    controller.lockedRecipe.dishName().ifPresent(name -> controller.dishName = Optional.of(name.copy()));
                    controller.lockedRecipe = null;
                    controller.lockedTargets = List.of();
                    controller.recipeIndex = 0;
                }
                if (controller != this) controller.refresh();
            }
            // Send the ingredient and the updated lock state in one block update.
            refresh();
            if (level != null && !level.isClientSide()) {
                level.blockEvent(worldPosition, getBlockState().getBlock(),
                        PLACE_ANIMATION_EVENT, ingredients.size() - 1);
            }
        });
        return result;
    }

    private HodgepodgeFeastBlockEntity recipeController() {
        if (level != null && getBlockState().getBlock() instanceof IHodgepodge surface) {
            BlockPos controllerPos = surface.recipeControllerPos(worldPosition, getBlockState());
            if (!controllerPos.equals(worldPosition)
                    && level.getBlockEntity(controllerPos) instanceof HodgepodgeFeastBlockEntity controller) {
                return controller;
            }
        }
        return this;
    }

    public HodgepodgeRecipeData lockedRecipe() { return recipeController().lockedRecipe; }
    public Optional<Component> dishName() {
        HodgepodgeFeastBlockEntity controller = recipeController();
        Optional<Component> inherited = controller.lockedRecipe == null
                ? Optional.empty() : controller.lockedRecipe.dishName();
        return inherited.or(() -> controller.dishName).map(Component::copy);
    }

    public void setDishName(Optional<Component> name) {
        HodgepodgeFeastBlockEntity controller = recipeController();
        controller.dishName = name.filter(value -> !value.getString().isBlank()).map(Component::copy);
        controller.refresh();
    }
    public boolean isRecipeLocked() { return lockedRecipe() != null; }
    public void setLockedRecipe(HodgepodgeRecipeData recipe) {
        HodgepodgeFeastBlockEntity controller = recipeController();
        if (controller != this) {
            controller.setLockedRecipe(recipe);
            return;
        }
        lockedRecipe = recipe;
        lockedTargets = decodeTargets(recipe);
        recipeIndex = Math.min(recipeSnapshot().ingredients().size(), lockedTargets.size());
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public void clearLockedRecipe() {
        HodgepodgeFeastBlockEntity controller = recipeController();
        if (controller != this) {
            controller.clearLockedRecipe();
            return;
        }
        lockedRecipe = null;
        lockedTargets = List.of();
        recipeIndex = 0;
        setChanged();
        if (level != null && !level.isClientSide()) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    public Optional<PlacedIngredient> nextRecipePlacement() {
        HodgepodgeFeastBlockEntity controller = recipeController();
        if (controller != this) return controller.nextRecipePlacement();
        if (lockedRecipe == null || recipeIndex < 0 || recipeIndex >= lockedTargets.size()) return Optional.empty();
        return Optional.of(lockedTargets.get(recipeIndex));
    }

    private static boolean samePlacement(PlacedIngredient actual, PlacedIngredient expected) {
        return actual.id().equals(expected.id())
                && actual.x() == expected.x() && actual.y() == expected.y() && actual.z() == expected.z()
                && actual.sizeX() == expected.sizeX() && actual.sizeY() == expected.sizeY()
                && actual.sizeZ() == expected.sizeZ()
                && Math.floorMod(actual.rotation(), 4) == Math.floorMod(expected.rotation(), 4);
    }

    private static List<PlacedIngredient> decodeTargets(HodgepodgeRecipeData recipe) {
        if (recipe == null) return List.of();
        try {
            return recipe == null ? List.of() : FeastCodec.decode(recipe.feastCode()).feast().ingredients().stream()
                    .sorted(java.util.Comparator.comparingInt(PlacedIngredient::y)).toList();
        } catch (FeastCodec.FormatException ignored) {
            return List.of();
        }
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

    public int placementAnimationRevision() {
        return placementAnimationRevision;
    }

    public int placementAnimationIndex() {
        return placementAnimationIndex;
    }

    public long placementAnimationStartedAt() {
        return placementAnimationStartedAt;
    }

    @Override
    public boolean triggerEvent(int id, int data) {
        if (id != PLACE_ANIMATION_EVENT) return super.triggerEvent(id, data);
        if (level != null && level.isClientSide()) {
            placementAnimationIndex = data;
            placementAnimationStartedAt = System.nanoTime();
            placementAnimationRevision++;
        }
        return true;
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

    /** 尺度缓存map */
    public VoxelShape ingredientCollisionShape() {
        if (ingredientCollisionRevision == contentRevision) return ingredientCollisionShape;
        ingredientCollisionShape = IngredientCollisionHeightMap.fromIngredients(ingredients);
        ingredientCollisionRevision = contentRevision;
        return ingredientCollisionShape;
    }

    public boolean hasCollisionShape(int stateHash) {
        return collisionShapeRevision == contentRevision && collisionShapeStateHash == stateHash;
    }

    public VoxelShape collisionShape() {
        return collisionShape;
    }

    public void cacheCollisionShape(int stateHash, VoxelShape shape) {
        collisionShapeRevision = contentRevision;
        collisionShapeStateHash = stateHash;
        collisionShape = shape;
    }

    /** 四分之一格检测 */
    public boolean hasOutlineShape(int stateHash) {
        return outlineShapeRevision == contentRevision && outlineShapeStateHash == stateHash;
    }

    public VoxelShape outlineShape() {
        return outlineShape;
    }

    public void cacheOutlineShape(int stateHash, VoxelShape shape) {
        outlineShapeRevision = contentRevision;
        outlineShapeStateHash = stateHash;
        outlineShape = shape;
    }

    public long structureShapeKey() {
        return structureShapeKey;
    }

    public VoxelShape structureShape() {
        return structureShape;
    }

    public void cacheStructureShape(long key, VoxelShape shape) {
        structureShapeKey = key;
        structureShape = shape;
    }

    /** 缓存方块外形数据 */
    public boolean hasStructureOutlineShape(long key) {
        return structureOutlineShapeKey == key;
    }

    public VoxelShape structureOutlineShape() {
        return structureOutlineShape;
    }

    public void cacheStructureOutlineShape(long key, VoxelShape shape) {
        structureOutlineShapeKey = key;
        structureOutlineShape = shape;
    }

    public boolean hasStructureCollisionShape(long key) {
        return structureCollisionShapeKey == key;
    }

    public VoxelShape structureCollisionShape() {
        return structureCollisionShape;
    }

    public void cacheStructureCollisionShape(long key, VoxelShape shape) {
        structureCollisionShapeKey = key;
        structureCollisionShape = shape;
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

    public CustomFeastData recipeSnapshot() {
        return level != null && getBlockState().getBlock() instanceof AbstractMultiBlockPlateBlock plate
                ? plate.recipeSnapshot(level, worldPosition, getBlockState()) : snapshot();
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
        if (getBlockState().is(KHBlocks.BAMBOO_DISPLAY_TRAY)) {
            return new ContainerLimits(config.woodenPlateCapacity(), 6, 24);
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
        dishName.ifPresent(name -> output.store("dish_name", ComponentSerialization.CODEC, name));
        if (lockedRecipe != null) output.store("locked_recipe", HodgepodgeRecipeData.CODEC, lockedRecipe);
        output.putInt("recipe_index", recipeIndex);
    }

    @Override
    protected void loadAdditional(@NonNull ValueInput input) {
        super.loadAdditional(input);
        ingredients.clear();
        lockedRecipe = input.read("locked_recipe", HodgepodgeRecipeData.CODEC).orElse(null);
        dishName = input.read("dish_name", ComponentSerialization.CODEC)
                .filter(value -> !value.getString().isBlank());
        lockedTargets = decodeTargets(lockedRecipe);
        recipeIndex = input.getInt("recipe_index").orElse(0);
        if (lockedRecipe != null && (lockedTargets.isEmpty() || recipeIndex < 0 || recipeIndex > lockedTargets.size())) {
            lockedRecipe = null;
            lockedTargets = List.of();
            recipeIndex = 0;
        }
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
