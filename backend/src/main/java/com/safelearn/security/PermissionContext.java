package com.safelearn.security;

import java.util.List;

/** 当前登录管理员的权限上下文（请求级 ThreadLocal）。 */
public final class PermissionContext {

    private static final ThreadLocal<UserPermissionSnapshot> HOLDER = new ThreadLocal<>();

    private PermissionContext() {
    }

    public static void set(UserPermissionSnapshot snapshot) {
        HOLDER.set(snapshot);
    }

    public static UserPermissionSnapshot get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public record UserPermissionSnapshot(
            String userId,
            String department,
            String dataScope,
            List<String> customDeptNames,
            List<String> permissionCodes
    ) {
        public boolean hasPermission(String module, String action) {
            if (permissionCodes == null) {
                return false;
            }
            return permissionCodes.contains(PermissionCodes.build(module, action));
        }
    }
}
