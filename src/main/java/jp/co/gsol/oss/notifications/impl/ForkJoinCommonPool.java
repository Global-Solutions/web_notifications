package jp.co.gsol.oss.notifications.impl;

import java.util.concurrent.ForkJoinPool;

public class ForkJoinCommonPool {
    public static final ForkJoinPool commonPool = new ForkJoinPool();
}