package org.tsd.tsdbot.haloapi;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import org.tsd.tsdbot.haloapi.model.metadata.HaloMeta;
import org.tsd.tsdbot.haloapi.model.metadata.Metadata;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class MetadataCache {

    private final HaloApiClient apiClient;

    // Some Metadata can only be retrieved in large collections
    //  e.g. http://haloapi.com/meta/objects -> list[object]
    // Others can only be retrieved individually
    //  e.g. http://haloapi.com/meta/object/{id} -> object

    // Setup two caches:
    //  - one that keys on (Metadata class) and points to whole collections (mapped by id)
    //  - one that keys on (Metadata class AND id) and points to individual objects

    private final LoadingCache<Class<? extends Metadata>, HashMap<String, Metadata>> collectionCache =
            CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Class<? extends Metadata>, HashMap<String, Metadata>>() {
                @Override
                public HashMap<String, Metadata> load(@Nullable Class<? extends Metadata> aClass) throws Exception {
                    Metadata[] list = apiClient.getMetadataCollection(aClass);
                    HashMap<String, Metadata> map = new HashMap<>();
                    for (Metadata m : list) {
                        map.put(m.getId(), m);
                    }
                    return map;
                }
            });

    private final LoadingCache<CacheKey, Metadata> individualCache =
            CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<CacheKey, Metadata>() {
                @Override
                public Metadata load(@Nullable CacheKey cacheKey) throws Exception {
                    return apiClient.getMetadata(cacheKey.metadataClass, cacheKey.id);
                }
            });

    @Inject
    public MetadataCache(HaloApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @SuppressWarnings("unchecked")
    public <T extends Metadata> T getMetadata(Class<T> clazz, String id) throws Exception {
        if(clazz.getAnnotation(HaloMeta.class).list()) {
            return (T) collectionCache.get(clazz).get(id);
        } else {
            return (T) individualCache.get(CacheKey.key(clazz, id));
        }
    }

    public static class CacheKey {
        public Class<? extends Metadata> metadataClass;
        public String id;

        <T extends Metadata> CacheKey(Class<T> clazz, String id) {
            this.metadataClass = clazz;
            this.id = id;
        }

        public static <T extends Metadata> CacheKey key(Class<T> clazz, String id) {
            return new CacheKey(clazz, id);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CacheKey cacheKey = (CacheKey) o;

            if (id != null ? !id.equals(cacheKey.id) : cacheKey.id != null) return false;
            if (metadataClass != null ? !metadataClass.equals(cacheKey.metadataClass) : cacheKey.metadataClass != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = metadataClass != null ? metadataClass.hashCode() : 0;
            result = 31 * result + (id != null ? id.hashCode() : 0);
            return result;
        }
    }
}
