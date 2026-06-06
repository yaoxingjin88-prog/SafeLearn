package com.safelearn.common;

public final class DifficultyLevel {
    public static final int BASIC = 1;          // 基础理论
    public static final int INTERMEDIATE = 2;   // 案例分析
    public static final int ADVANCED = 3;       // 高级实操

    private DifficultyLevel() {}

    public static String label(int level) {
        return switch (level) {
            case BASIC -> "基础理论";
            case INTERMEDIATE -> "案例分析";
            case ADVANCED -> "高级实操";
            default -> "未知";
        };
    }

    public static boolean isValid(int level) {
        return level >= BASIC && level <= ADVANCED;
    }
}
