package com.safelearn.security;

public final class PermissionCodes {

    private PermissionCodes() {
    }

    public static final String PREFIX = "perm:";

    public static String build(String module, String action) {
        return PREFIX + module + ":" + action;
    }
}
