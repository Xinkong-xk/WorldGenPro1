package io.xk.worldgenpro.utils;


public class Timer {
    private static long startTime = -1;
    private static long endTime = -1;
    private static String label = "";

    /** 开始计时 */
    public static void start(String name) {
        label = name;
        startTime = System.nanoTime();
        endTime = -1;
    }

    /** 结束计时 */
    public static void end() {
        if (startTime == -1) {
            System.err.println("[Timer] 未调用 start() 就调用了 end()。");
            return;
        }
        endTime = System.nanoTime();
    }

    /** 输出计时结果（毫秒） */
    public static void output() {
        if (startTime == -1 || endTime == -1) {
            System.err.println("[Timer] 未完成计时。请先调用 start() 和 end()。");
            return;
        }
        double ms = (endTime - startTime) / 1_000_000.0;
        System.out.printf("[Timer] %s 耗时：%.3f ms%n", label.isEmpty() ? "Unnamed" : label, ms);
    }

    /** 重置计时器 */
    public static void reset() {
        startTime = -1;
        endTime = -1;
        label = "";
    }
}
