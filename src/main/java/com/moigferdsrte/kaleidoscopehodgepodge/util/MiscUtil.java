package com.moigferdsrte.kaleidoscopehodgepodge.util;

public final class MiscUtil {
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        // 首字母大写 + 截取后面的部分（保留原样）
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
