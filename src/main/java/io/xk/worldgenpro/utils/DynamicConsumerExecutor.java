package io.xk.worldgenpro.utils;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 动态平衡任务分配的多线程消费者执行器
 * @param <T> 待处理对象类型
 */
public class DynamicConsumerExecutor<T> {

    private final int threadCount;

    public DynamicConsumerExecutor(int threadCount) {
        if (threadCount <= 0) threadCount = 1;
        this.threadCount = threadCount;
    }

    /**
     * 执行任务
     * @param items 待处理对象列表
     * @param consumer 消费者函数
     */
    public void execute(List<T> items, Consumer<T> consumer) {
        if (items == null || items.isEmpty()) return;

        // 使用线程安全队列存放待处理对象
        BlockingQueue<T> taskQueue = new LinkedBlockingQueue<>(items);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    T item;
                    while ((item = taskQueue.poll()) != null) {
                        try {
                            consumer.accept(item);
                        } catch (Exception e) {
                            errors.add(e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        executor.shutdown();

        try {
            // 等待所有线程完成
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("线程执行被中断", e);
        }

        if (!errors.isEmpty()) {
            RuntimeException e = new RuntimeException("执行过程中发生异常");
            errors.forEach(e::addSuppressed);
            throw e;
        }
    }
}
