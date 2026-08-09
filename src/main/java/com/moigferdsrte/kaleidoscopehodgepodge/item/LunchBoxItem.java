package com.moigferdsrte.kaleidoscopehodgepodge.item;

import com.moigferdsrte.kaleidoscopehodgepodge.inventory.LunchBoxMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

/** 可保存九个已填充打包纸袋的便携容器。 */
public final class LunchBoxItem extends Item {
    public LunchBoxItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.CONTAINER,
                ItemContainerContents.EMPTY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new LunchBoxMenu(containerId, inventory, stack, hand),
                    stack.getHoverName()));
        }
        return InteractionResult.SUCCESS;
    }
}
