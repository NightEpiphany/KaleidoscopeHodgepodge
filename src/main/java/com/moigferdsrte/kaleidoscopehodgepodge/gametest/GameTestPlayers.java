package com.moigferdsrte.kaleidoscopehodgepodge.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

final class GameTestPlayers {
    static Player create(GameTestHelper helper, GameType mode) {
        Player player = helper.makeMockPlayer(mode);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        // Drops must stay in the loaded test area, not request chunks at the world origin during shutdown.
        player.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        return player;
    }

    private GameTestPlayers() {}
}
