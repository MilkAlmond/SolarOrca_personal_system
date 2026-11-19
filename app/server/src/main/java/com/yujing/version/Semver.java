package com.yujing.version;

public final class Semver {

    private Semver() {
    }

    public static boolean isValid(String version) {
        return version != null && version.matches("^\\d{1,3}(\\.\\d{1,3}){0,3}$");
    }

    public static int compare(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int x = i < a.length ? Integer.parseInt(a[i]) : 0;
            int y = i < b.length ? Integer.parseInt(b[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }
}
