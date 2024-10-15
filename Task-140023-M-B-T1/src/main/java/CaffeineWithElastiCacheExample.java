import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.elasticache.model.CacheCluster;
import com.amazonaws.services.elasticache.model.CacheNode;
import com.amazonaws.services.elasticache.model.DescribeCacheClustersRequest;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class CaffeineWithElastiCacheExample {
    // Update with your actual Redis endpoint and cluster ID
    private static final String ENDPOINT = "your-redis-cluster.xxxxxx.clustercfg.use1.cache.amazonaws.com";
    private static final String CLUSTER_ID = "my-redis-cluster-id";

    public static void main(String[] args) {
        // Set region explicitly here
        AmazonElastiCache client = AmazonElastiCacheClientBuilder.standard()
                .withRegion(Regions.US_EAST_1) // Replace with the region where your ElastiCache instance is deployed
                .build();

        // Retrieve the Redis endpoint from ElastiCache
        String redisEndpoint = getRedisEndpoint(client);

        // Create a Caffeine cache with ElastiCache as the backing store
        LoadingCache<String, String> cache = createCaffeineCache(redisEndpoint);

        // Put data into the cache
        cache.put("key1", "value1");
        cache.put("key2", "value2");

        // Get data from the cache
        String value1 = cache.get("key1");
        System.out.println("Value for key1: " + value1);

        // Shutdown the cache and client
        cache.invalidateAll();
        client.shutdown();
    }

    private static String getRedisEndpoint(AmazonElastiCache client) {
        DescribeCacheClustersRequest request = new DescribeCacheClustersRequest()
                .withCacheClusterId(CLUSTER_ID);

        String redisEndpoint = null;
        for (CacheCluster cluster : client.describeCacheClusters(request).getCacheClusters()) {
            for (CacheNode node : cluster.getCacheNodes()) {
                if (node.getEndpoint().getAddress() != null) {
                    redisEndpoint = node.getEndpoint().getAddress() + ":" + node.getEndpoint().getPort();
                    break;
                }
            }
        }
        if (redisEndpoint == null) {
            throw new IllegalStateException("Could not find Redis endpoint for ElastiCache cluster: " + CLUSTER_ID);
        }
        return redisEndpoint;
    }

    private static LoadingCache<String, String> createCaffeineCache(String redisEndpoint) {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build(key -> {
                    // In a real implementation, you would fetch the data from Redis using redisEndpoint
                    return "Dummy value for key: " + key;
                });
    }
}
