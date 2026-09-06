package com.moigferdsrte.kaleidoscopehodgepodge.core;

import com.moigferdsrte.kaleidoscopehodgepodge.core.CustomFeastData.ContainerKind;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/*自定义菜品数据的纯文本编解码器。*/
public final class FeastCodec {

    /** 魔数 + 当前格式版本。 */
    public static final String MAGIC = "KHP";
    public static final int VERSION = 1;

    /** 单份菜品允许的最大材料数，与 {@code HodgepodgeFeastBlockEntity.MAX_SERIALIZED_INGREDIENTS} 保持一致。 */
    public static final int MAX_INGREDIENTS = 360;

    private static final String HEADER_1 = MAGIC + ":" + VERSION;

    private FeastCodec() {
    }

    /**
     * 编码一份菜品为纯文本。
     *
     * @param containerPath 容器物品的注册键字符串（如 {@code kaleidoscope_hodgepodge:porcelain_plate}）
     * @param data          菜品数据
     * @return 单行纯文本代码
     */
    public static String encode(String containerPath, CustomFeastData data) {
        StringBuilder builder = new StringBuilder(256);
        builder.append(HEADER_1).append('|');
        builder.append(containerPath).append('|');
        builder.append(kindToken(data.kind())).append('|');
        builder.append(facingToken(data.facing())).append('|');
        builder.append(data.ingredients().size());
        for (PlacedIngredient ingredient : data.ingredients()) {
            builder.append('|');
            appendIngredient(builder, ingredient);
        }
        return builder.toString();
    }

    private static void appendIngredient(StringBuilder builder, PlacedIngredient ingredient) {
        builder.append(ingredient.id()).append(';');
        builder.append(ingredient.x()).append(';');
        builder.append(ingredient.y()).append(';');
        builder.append(ingredient.z()).append(';');
        builder.append(ingredient.sizeX()).append(';');
        builder.append(ingredient.sizeY()).append(';');
        builder.append(ingredient.sizeZ()).append(';');
        builder.append(ingredient.rotation()).append(';');
        IngredientFoodData food = ingredient.food();
        builder.append(food.nutrition()).append(';');
        builder.append(food.saturation());
        for (IngredientEffectGroup group : food.effects()) {
            builder.append(';');
            builder.append(group.probability()).append('!');
            builder.append(group.effects().size()).append(':');
            boolean first = true;
            for (IngredientStatusEffect effect : group.effects()) {
                if (!first) builder.append(',');
                first = false;
                appendEffect(builder, effect);
            }
        }
        // 无效果时也补足效果组字段，使每个材料行固定为 11 个字段（与解析端一致）。
        builder.append(';');
    }

    private static void appendEffect(StringBuilder builder, IngredientStatusEffect effect) {
        builder.append(effect.id()).append('@');
        builder.append(effect.duration()).append('@');
        builder.append(effect.amplifier()).append('@');
        builder.append(effect.ambient()).append('@');
        builder.append(effect.visible()).append('@');
        builder.append(effect.showIcon());
    }

    /** 解码结果：容器注册键 + 菜品数据。 */
    public record Decoded(String containerPath, CustomFeastData feast) {
    }

    /**
     * 解码纯文本为菜品数据。
     *
     * @throws FormatException 格式非法或版本不支持
     */
    public static Decoded decode(String text) throws FormatException {
        if (text == null) throw new FormatException("empty");
        String[] parts = text.split("\\|", -1);
        if (parts.length < 5) throw new FormatException("missing header");
        if (!HEADER_1.equals(parts[0])) {
            throw new FormatException("unsupported version");
        }
        String containerPath = parts[1];
        if (containerPath.isEmpty()) throw new FormatException("missing container");
        ContainerKind kind = parseKind(parts[2]);
        Direction facing = parseFacing(parts[3]);
        int count;
        try {
            count = Integer.parseInt(parts[4]);
        } catch (NumberFormatException e) {
            throw new FormatException("invalid count");
        }
        if (count < 0 || count > MAX_INGREDIENTS) throw new FormatException("invalid count");
        if (parts.length != 5 + count) throw new FormatException("ingredient count mismatch");

        List<PlacedIngredient> ingredients = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ingredients.add(parseIngredient(parts[5 + i]));
        }
        return new Decoded(containerPath, new CustomFeastData(kind, facing, ingredients));
    }

    private static PlacedIngredient parseIngredient(String token) throws FormatException {
        String[] fields = token.split(";", 11);
        if (fields.length != 11) throw new FormatException("bad ingredient field");
        Identifier id;
        try {
            id = Identifier.parse(fields[0]);
        } catch (RuntimeException e) {
            throw new FormatException("bad ingredient id");
        }
        int x = parseInt(fields[1], "x");
        int y = parseInt(fields[2], "y");
        int z = parseInt(fields[3], "z");
        int sizeX = parseInt(fields[4], "sizeX");
        int sizeY = parseInt(fields[5], "sizeY");
        int sizeZ = parseInt(fields[6], "sizeZ");
        int rotation = parseInt(fields[7], "rotation");
        int nutrition = parseInt(fields[8], "nutrition");
        float saturation;
        try {
            saturation = Float.parseFloat(fields[9]);
        } catch (NumberFormatException e) {
            throw new FormatException("bad saturation");
        }
        String groupsToken = fields[10];
        IngredientFoodData food = new IngredientFoodData(nutrition, saturation, parseEffects(groupsToken));
        return new PlacedIngredient(id, x, y, z, sizeX, sizeY, sizeZ, rotation, food);
    }

    private static List<IngredientEffectGroup> parseEffects(String groupsToken) {
        if (groupsToken.isEmpty()) return List.of();
        List<IngredientEffectGroup> groups = new ArrayList<>();
        for (String groupToken : groupsToken.split(";")) {
            String[] parts = groupToken.split("!", 2);
            if (parts.length != 2) continue;
            float probability;
            try {
                probability = Float.parseFloat(parts[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            String[] tail = parts[1].split(":", 2);
            if (tail.length != 2) continue;
            int count;
            try {
                count = Integer.parseInt(tail[0]);
            } catch (NumberFormatException e) {
                continue;
            }
            List<IngredientStatusEffect> effects = new ArrayList<>(count);
            if (count > 0) {
                for (String effectToken : tail[1].split(",")) {
                    IngredientStatusEffect effect = parseEffect(effectToken);
                    if (effect != null) effects.add(effect);
                }
            }
            groups.add(new IngredientEffectGroup(probability, effects));
        }
        return groups;
    }

    private static IngredientStatusEffect parseEffect(String token) {
        String[] parts = token.split("@", 6);
        if (parts.length != 6) return null;
        Identifier id;
        try {
            id = Identifier.parse(parts[0]);
        } catch (RuntimeException e) {
            return null;
        }
        int duration;
        int amplifier;
        try {
            duration = Integer.parseInt(parts[1]);
            amplifier = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
        boolean ambient = Boolean.parseBoolean(parts[3]);
        boolean visible = Boolean.parseBoolean(parts[4]);
        boolean showIcon = Boolean.parseBoolean(parts[5]);
        return new IngredientStatusEffect(id, duration, amplifier, ambient, visible, showIcon);
    }

    private static int parseInt(String value, String field) throws FormatException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FormatException("bad " + field);
        }
    }

    private static String kindToken(CustomFeastData.ContainerKind kind) {
        return kind == CustomFeastData.ContainerKind.SOUP ? "s" : "d";
    }

    private static CustomFeastData.ContainerKind parseKind(String token) throws FormatException {
        if (token.equals("s") || token.equalsIgnoreCase("soup")) return CustomFeastData.ContainerKind.SOUP;
        if (token.equals("d") || token.equalsIgnoreCase("dish")) return CustomFeastData.ContainerKind.DISH;
        throw new FormatException("bad kind");
    }

    private static String facingToken(Direction facing) {
        return String.valueOf(facing.getName().charAt(0));
    }

    private static Direction parseFacing(String token) throws FormatException {
        if (token.length() != 1) throw new FormatException("bad facing");
        return switch (token.charAt(0)) {
            case 'n', 'N' -> Direction.NORTH;
            case 'e', 'E' -> Direction.EAST;
            case 's', 'S' -> Direction.SOUTH;
            case 'w', 'W' -> Direction.WEST;
            case 'u', 'U' -> Direction.UP;
            case 'd', 'D' -> Direction.DOWN;
            default -> throw new FormatException("bad facing");
        };
    }

    /** 格式非法时抛出的受检异常，由指令层翻译为玩家可读的失败提示。 */
    public static final class FormatException extends Exception {
        public FormatException(String message) {
            super(message);
        }

        public Component asComponent() {
            return Component.translatable("command.kaleidoscope_hodgepodge.import.invalid_format");
        }
    }
}
