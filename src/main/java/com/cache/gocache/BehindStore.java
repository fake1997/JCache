package com.cache.gocache;

import java.io.Serializable;

public interface BehindStore<K extends Serializable, V extends Serializable> extends CacheStore<K,V> {
    // 设置同步到DB的时间
    void setSyncInterval(long syncInterval);
    Stats prevStats();
    void  prevStats(Stats stats);
    public static class Stats {
        long updateCount;
        long updateCost;

        long removedCount;
        long removedCost;

        long purgedCount;
        long purgedCost;

        public Stats(long updateCount, long updateCost, long removedCount, long removedCost, long purgedCount, long purgedCost) {
            this.updateCount = updateCount;
            this.updateCost = updateCost;
            this.removedCount = removedCount;
            this.removedCost = removedCost;
            this.purgedCount = purgedCount;
            this.purgedCost = purgedCost;
        }

        public Stats plus(Stats other) {
            return new Stats(
                    Math.max(0, updateCount + other.getUpdateCount()),
                    Math.max(0, updateCost + other.getUpdateCost()),
                    Math.max(0, removedCount + other.getRemovedCount()),
                    Math.max(0, removedCost + other.getRemovedCost()),
                    Math.max(0, purgedCount + other.getPurgedCount()),
                    Math.max(0, purgedCost + other.getPurgedCost())
            );
        }

        public Stats minus(Stats other) {
            return new Stats(
                    Math.max(0, updateCount - other.getUpdateCount()),
                    Math.max(0, updateCost - other.getUpdateCost()),
                    Math.max(0, removedCount - other.getRemovedCount()),
                    Math.max(0, removedCost - other.getRemovedCost()),
                    Math.max(0, purgedCount - other.getPurgedCount()),
                    Math.max(0, purgedCost - other.getPurgedCost())
            );
        }

        @Override
        public String toString() {
            return "Stats{" +
                    "updateCount=" + updateCount +
                    ", updateCost=" + updateCost +
                    ", removedCount=" + removedCount +
                    ", removedCost=" + removedCost +
                    ", purgedCount=" + purgedCount +
                    ", purgedCost=" + purgedCost +
                    '}';
        }

        public long getUpdateCount() {
            return updateCount;
        }

        public long getUpdateCost() {
            return updateCost;
        }

        public long getRemovedCount() {
            return removedCount;
        }

        public long getRemovedCost() {
            return removedCost;
        }

        public long getPurgedCount() {
            return purgedCount;
        }

        public long getPurgedCost() {
            return purgedCost;
        }
    }
}
