package com.gpomares.committracker.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.gpomares.committracker.models.CommitInfo
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class CommitCacheService(private val project: Project) {

    private val cache = ConcurrentHashMap<String, CachedValue<List<CommitInfo>>>()
    private val cacheDuration = Duration.ofMinutes(5)

    data class CachedValue<T>(
        val data: T,
        val timestamp: Instant
    ) {
        fun isExpired(duration: Duration): Boolean {
            return Instant.now().isAfter(timestamp.plus(duration))
        }
    }

    /**
     * Get cached commits if available and not expired
     */
    fun get(key: String): List<CommitInfo>? {
        val cached = cache[key] ?: return null
        return if (cached.isExpired(cacheDuration)) {
            cache.remove(key)
            null
        } else {
            cached.data
        }
    }

    /**
     * Cache commits with current timestamp
     */
    fun put(key: String, commits: List<CommitInfo>) {
        cache[key] = CachedValue(commits, Instant.now())
    }

    /**
     * Invalidate all cached data
     */
    fun invalidate() {
        cache.clear()
    }

    /**
     * Invalidate cache for a specific repository
     */
    fun invalidateRepository(repoPath: String) {
        cache.keys.removeIf { it.startsWith("$repoPath:") }
    }

    /**
     * Get cache statistics
     */
    fun getStats(): CacheStats {
        val now = Instant.now()
        val activeEntries = cache.count { !it.value.isExpired(cacheDuration) }
        val expiredEntries = cache.size - activeEntries

        return CacheStats(
            totalEntries = cache.size,
            activeEntries = activeEntries,
            expiredEntries = expiredEntries
        )
    }

    data class CacheStats(
        val totalEntries: Int,
        val activeEntries: Int,
        val expiredEntries: Int
    )
}
