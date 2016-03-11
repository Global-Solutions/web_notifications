package jp.co.gsol.oss.notifications.impl;

import java.util.concurrent.ForkJoinPool;

/**
 * ForkJoinPool common pool.
 * @author Global Solutions company limited
 */
public final class ForkJoinCommonPool {
    /** common pool.*/
    public static final ForkJoinPool COMMON_POOL = new ForkJoinPool();
    /** .*/
    private ForkJoinCommonPool() { }
}