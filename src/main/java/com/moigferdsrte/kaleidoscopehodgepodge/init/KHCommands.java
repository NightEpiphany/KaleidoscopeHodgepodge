package com.moigferdsrte.kaleidoscopehodgepodge.init;

import com.moigferdsrte.kaleidoscopehodgepodge.KaleidoscopeHodgepodge;
import com.moigferdsrte.kaleidoscopehodgepodge.config.GeneralConfig;
import com.moigferdsrte.kaleidoscopehodgepodge.core.*;
import com.moigferdsrte.kaleidoscopehodgepodge.item.CustomFeastBlockItem;
import com.moigferdsrte.kaleidoscopehodgepodge.util.CrashDiagnostics;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.datafixers.util.Unit;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.function.Supplier;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class KHCommands {

	@SuppressWarnings("unused")
	public static final int PERMISSION_IMPORT = 2;

	public static final String ARG_CODE = "code";

	static final String CMD_ROOT = "hodgepodge";

	private KHCommands() {
	}

	public static void init() {
		CommandRegistrationCallback.EVENT.register(KHCommands::register);
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
								 CommandBuildContext registryAccess, Commands.CommandSelection environment) {
		dispatcher.register(literal(CMD_ROOT)
				.then(literal("export")
						.executes(context -> exportDish(context.getSource())))
				.then(literal("import")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(literal("dish")
								.then(argument("targets", EntityArgument.entities())
										.then(argument(ARG_CODE, StringArgumentType.greedyString())
												.executes(context -> importDish(context.getSource(),
														EntityArgument.getEntities(context, "targets"),
														StringArgumentType.getString(context, ARG_CODE))))))
						.then(literal("recipe")
								.then(argument("targets", EntityArgument.entities())
										.then(argument(ARG_CODE, StringArgumentType.greedyString())
												.executes(context -> importRecipe(context.getSource(),
														EntityArgument.getEntities(context, "targets"),
														StringArgumentType.getString(context, ARG_CODE))))))
				));
	}

	private static int exportDish(CommandSourceStack source) {
		ServerPlayer player = asPlayer(source);
		if (player == null) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.export.no_player"));
			return 0;
		}
		ItemStack stack = player.getMainHandItem();
		Item item = stack.getItem();
		if (!(item instanceof CustomFeastBlockItem)) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.export.no_dish"));
			return 0;
		}
		CustomFeastData feast = stack.get(KHDataComponents.CUSTOM_FEAST);
		if (feast == null || feast.ingredients().isEmpty()) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.export.empty_dish"));
			return 0;
		}
		String containerPath = BuiltInRegistries.ITEM.getKey(item).toString();
		List<PlacedIngredient> sorted = new ArrayList<>(feast.ingredients());
		sorted.sort(Comparator.comparingInt(PlacedIngredient::y)
				.thenComparingInt(PlacedIngredient::x)
				.thenComparingInt(PlacedIngredient::z));
		String code = FeastCodec.encode(containerPath,
				new CustomFeastData(feast.kind(), feast.facing(), sorted));
		CrashDiagnostics.record("exported dish " + containerPath + " components=" + sorted.size());
		// 点击即可复制完整菜谱代码，不再把长串直接刷到聊天栏。
		Supplier<Component> success = () -> Component.translatable(
				"command.kaleidoscope_hodgepodge.export.success")
				.append(Component.translatable("command.kaleidoscope_hodgepodge.export.copy")
						.withStyle(ChatFormatting.GREEN, ChatFormatting.UNDERLINE, ChatFormatting.BOLD)
						.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.CopyToClipboard(code))));
		source.sendSuccess(success, false);
		return sorted.size();
	}

	private static List<ServerPlayer> scanPlayers(Collection<? extends Entity> targets) {
		return targets.stream()
				.filter(ServerPlayer.class::isInstance)
				.map(ServerPlayer.class::cast)
				.toList();
	}

	private static int importRecipe(CommandSourceStack source, Collection<? extends Entity> targets, String code) {
		List<ServerPlayer> players = scanPlayers(targets);
		if (players.isEmpty()) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.no_player"));
			return 0;
		}
		FeastCodec.Decoded decoded;
		try {
			decoded = FeastCodec.decode(code);
		} catch (FeastCodec.FormatException e) {
			source.sendFailure(e.asComponent());
			CrashDiagnostics.record("import rejected invalid format: " + e.getMessage());
			return 0;
		}
		Identifier containerId;
		try {
			containerId = Identifier.parse(decoded.containerPath());
		} catch (RuntimeException e) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.bad_container"));
			return 0;
		}
		Item container = BuiltInRegistries.ITEM.getValue(containerId);
		if (!(container instanceof CustomFeastBlockItem feastItem)) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.bad_container"));
			return 0;
		}
		CustomFeastData.ContainerKind expectedKind = feastItem.isSoup
				? CustomFeastData.ContainerKind.SOUP : CustomFeastData.ContainerKind.DISH;
		if (decoded.feast().kind() != expectedKind) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.kind_mismatch"));
			return 0;
		}
		Block block = ((BlockItem) container).getBlock();
		ContainerMetrics metrics = metrics(block);
		if (metrics == null || decoded.feast().ingredients().size() > metrics.capacity()) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.too_many",
					metrics == null ? 0 : metrics.capacity()));
			return 0;
		}
		List<PlacedIngredient> ingredients = new ArrayList<>(decoded.feast().ingredients().size());
		for (PlacedIngredient ingredient : decoded.feast().ingredients()) {
			PackingIngredients packing = PackingIngredientRegistry.byId(ingredient.id()).orElse(null);
			if (packing == null) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.unknown_ingredient", ingredient.id().toString()));
				return 0;
			}
			if (!isSuitable(packing, expectedKind)) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.unsuitable", ingredient.id().toString()));
				return 0;
			}
			if (!PlacementSpace.within(ingredient,
					new PlacementSpace.Bounds(0, metrics.width(), 0, metrics.depth(), metrics.maxHeight()))
					|| ingredient.y() < metrics.baseHeight()) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.out_of_bounds", ingredient.id().toString()));
				return 0;
			}
			ingredients.add(ingredient.withFood(
					IngredientFoodService.resolve(ingredient.id(), ingredient.food())));
		}
		for (ServerPlayer player : players) {
			ItemStack stack = KHItems.HODGEPODGE_RECIPE.getDefaultInstance();
			stack.set(KHDataComponents.HODGEPODGE_RECIPE,
					new HodgepodgeRecipeData(code, player.getUUID(), Optional.of(player.getGameProfile()), Optional.empty()));
			if (!player.addItem(stack)) player.drop(stack, false);
			CrashDiagnostics.record("imported recipe " + decoded.containerPath()
					+ " components=" + ingredients.size() + " by " + player.getName().getString());
		}
		if (GeneralConfig.snapshot().debugLogging())
			KaleidoscopeHodgepodge.LOGGER.info("Imported recipe {} components={} recipients={}",
					decoded.containerPath(), ingredients.size(), players.size());
		source.sendSuccess(() -> Component.translatable(
				"command.kaleidoscope_hodgepodge.import.success", ingredients.size()), false);
		return ingredients.size() * players.size();
	}

	private static int importDish(CommandSourceStack source, Collection<? extends Entity> targets, String code) {
		List<ServerPlayer> players = scanPlayers(targets);
		if (players.isEmpty()) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.no_player"));
			return 0;
		}
		FeastCodec.Decoded decoded;
		try {
			decoded = FeastCodec.decode(code);
		} catch (FeastCodec.FormatException e) {
			source.sendFailure(e.asComponent());
			CrashDiagnostics.record("import rejected invalid format: " + e.getMessage());
			return 0;
		}
		Identifier containerId;
		try {
			containerId = Identifier.parse(decoded.containerPath());
		} catch (RuntimeException e) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.bad_container"));
			return 0;
		}
		Item container = BuiltInRegistries.ITEM.getValue(containerId);
		if (!(container instanceof CustomFeastBlockItem feastItem)) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.bad_container"));
			return 0;
		}
		CustomFeastData.ContainerKind expectedKind = feastItem.isSoup
				? CustomFeastData.ContainerKind.SOUP : CustomFeastData.ContainerKind.DISH;
		if (decoded.feast().kind() != expectedKind) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.kind_mismatch"));
			return 0;
		}
		Block block = ((BlockItem) container).getBlock();
		ContainerMetrics metrics = metrics(block);
		if (metrics == null || decoded.feast().ingredients().size() > metrics.capacity()) {
			source.sendFailure(Component.translatable("command.kaleidoscope_hodgepodge.import.too_many",
					metrics == null ? 0 : metrics.capacity()));
			return 0;
		}
		List<PlacedIngredient> ingredients = new ArrayList<>(decoded.feast().ingredients().size());
		for (PlacedIngredient ingredient : decoded.feast().ingredients()) {
			PackingIngredients packing = PackingIngredientRegistry.byId(ingredient.id()).orElse(null);
			if (packing == null) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.unknown_ingredient", ingredient.id().toString()));
				return 0;
			}
			if (!isSuitable(packing, expectedKind)) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.unsuitable", ingredient.id().toString()));
				return 0;
			}
			if (!PlacementSpace.within(ingredient,
					new PlacementSpace.Bounds(0, metrics.width(), 0, metrics.depth(), metrics.maxHeight()))
					|| ingredient.y() < metrics.baseHeight()) {
				source.sendFailure(Component.translatable(
						"command.kaleidoscope_hodgepodge.import.out_of_bounds", ingredient.id().toString()));
				return 0;
			}
			ingredients.add(ingredient.withFood(
					IngredientFoodService.resolve(ingredient.id(), ingredient.food())));
		}
		for (ServerPlayer player : players) {
			ItemStack stack = new ItemStack(container);
			stack.set(KHDataComponents.CUSTOM_FEAST,
					new CustomFeastData(decoded.feast().kind(), decoded.feast().facing(), ingredients));
			if (feastItem.isSoup) stack.set(KHDataComponents.SOUP_BASE, Unit.INSTANCE);
			if (!player.addItem(stack)) player.drop(stack, false);
			CrashDiagnostics.record("imported dish " + decoded.containerPath()
					+ " components=" + ingredients.size() + " by " + player.getName().getString());
		}
		if (GeneralConfig.snapshot().debugLogging())
			KaleidoscopeHodgepodge.LOGGER.info("Imported dish {} components={} recipients={}",
					decoded.containerPath(), ingredients.size(), players.size());
		source.sendSuccess(() -> Component.translatable(
				"command.kaleidoscope_hodgepodge.import.success", ingredients.size()), false);
		return ingredients.size() * players.size();
	}

	@SuppressWarnings("all")
	private static boolean isSuitable(PackingIngredients ingredient, CustomFeastData.ContainerKind kind) {
		return ingredient.suitableFor() == PackingIngredients.SuitableFor.BOTH
				|| kind == CustomFeastData.ContainerKind.DISH
				&& ingredient.suitableFor() == PackingIngredients.SuitableFor.DISH
				|| kind == CustomFeastData.ContainerKind.SOUP
				&& ingredient.suitableFor() == PackingIngredients.SuitableFor.SOUP;
	}

	private static ContainerMetrics metrics(Block block) {
		GeneralConfig.Snapshot config = GeneralConfig.snapshot();
		if (block == KHBlocks.WOODEN_PLATE) {
			return new ContainerMetrics(config.woodenPlateCapacity(), 16, 16, config.dishBaseHeight(),
					config.woodenMaxModelHeight());
		}
		if (block == KHBlocks.BAMBOO_DISPLAY_TRAY) {
			return new ContainerMetrics(config.woodenPlateCapacity(), 16, 16, 6, 24);
		}
		if (block == KHBlocks.PORCELAIN_PLATE) {
			return new ContainerMetrics(config.porcelainCapacity(), 16, 16, config.dishBaseHeight(),
					config.porcelainMaxModelHeight());
		}
		if (block == KHBlocks.MEDIAN_PORCELAIN_PLATE) {
			return new ContainerMetrics(80, 32, 16, config.dishBaseHeight(), config.porcelainMaxModelHeight());
		}
		if (block == KHBlocks.LARGE_PORCELAIN_PLATE) {
			return new ContainerMetrics(360, 42, 42, config.dishBaseHeight(), config.porcelainMaxModelHeight());
		}
		if (block == KHBlocks.PORCELAIN_SOUP_BOWL) {
			return new ContainerMetrics(config.soupCapacity(), 16, 16, config.soupBaseHeight(),
					config.porcelainMaxModelHeight());
		}
		return null;
	}

	private static ServerPlayer asPlayer(CommandSourceStack source) {
		return source.getEntity() instanceof ServerPlayer player ? player : null;
	}

	private record ContainerMetrics(int capacity, int width, int depth, int baseHeight, int maxHeight) {
	}
}
