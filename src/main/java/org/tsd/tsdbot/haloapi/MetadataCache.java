package org.tsd.tsdbot.haloapi;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import org.tsd.tsdbot.haloapi.model.metadata.HaloMeta;
import org.tsd.tsdbot.haloapi.model.metadata.Metadata;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MetadataCache {

    private final HaloApiClient apiClient;

    private final LoadingCache<Class<? extends Metadata>, HashMap<String, Metadata>> cache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(new CacheLoader<Class<? extends Metadata>, HashMap<String, Metadata>>() {
                @Override
                public HashMap<String, Metadata> load(Class<? extends Metadata> aClass) throws Exception {
                    if(aClass.getAnnotation(HaloMeta.class).list()) {
                        List<? extends Metadata> list = apiClient.getMetadataList(aClass);
                        HashMap<String, Metadata> map = new HashMap<>();
                        for (Metadata m : list) {
                            map.put(m.getId(), m);
                        }
                        return map;
                    } else {
                        apiClient.getMetadataSingle(aClass, )
                    }
                }
            });

    @Inject
    public MetadataCache(HaloApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public <T extends Metadata> T getMetadata(Class<T> clazz, String id) throws Exception {
        return (T) cache.get(clazz).get(id);
    }
}
