package gobblin.config.common.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.typesafe.config.Config;

import gobblin.config.store.api.ConfigKeyPath;
import gobblin.config.store.api.ConfigStore;

/**
 * InMemoryValueInspector provide the caching layer for getting the {@link com.typesafe.config.Config} from {@link ConfigStore}
 * 
 * @author mitu
 *
 */
public class InMemoryValueInspector implements ConfigStoreValueInspector{

  private final ConfigStoreValueInspector valueFallback;
  private final Cache<ConfigKeyPath, Config> ownConfigCache ;
  private final Cache<ConfigKeyPath, Config> recursiveConfigCache ;

  /**
   * 
   * @param valueFallback - the fall back {@link ConfigStoreValueInspector} which used to get the raw {@link com.typesafe.config.Config} 
   * @param useStrongRef  - if true, use Strong reference in cache, else, use Weak reference in cache
   */
  public InMemoryValueInspector (ConfigStoreValueInspector valueFallback, boolean useStrongRef){
    this.valueFallback = valueFallback;

    if (useStrongRef) {
      this.ownConfigCache = CacheBuilder.newBuilder().build();
      this.recursiveConfigCache = CacheBuilder.newBuilder().build();
    }
    else{
      this.ownConfigCache = CacheBuilder.newBuilder().softValues().build();
      this.recursiveConfigCache = CacheBuilder.newBuilder().softValues().build();
    }
  }

  /**
   * {@inheritDoc}.
   *
   * <p>
   *   If present in the cache, return the cached {@link com.typesafe.config.Config} for given input
   *   Otherwise, simply delegate the functionality to the internal {ConfigStoreValueInspector} and store the value into cache
   * </p>
   */
  @Override
  public Config getOwnConfig(final ConfigKeyPath configKey) {
    try {
      return this.ownConfigCache.get(configKey, new Callable<Config>() {
        @Override
        public Config call()  {
          return InMemoryValueInspector.this.valueFallback.getOwnConfig(configKey);
        }
      });
    } catch (ExecutionException e) {
      // should NOT come here
      throw new RuntimeException("Can not getOwnConfig for " + configKey);
    }
  }

  /**
   * {@inheritDoc}.
   *
   * <p>
   *   If present in the cache, return the cached {@link com.typesafe.config.Config} for given input
   *   Otherwise, simply delegate the functionality to the internal {ConfigStoreValueInspector} and store the value into cache
   * </p>
   */
  @Override
  public Config getResolvedConfig(final ConfigKeyPath configKey) {
    try {
      return this.recursiveConfigCache.get(configKey, new Callable<Config>() {
        @Override
        public Config call()  {
          return InMemoryValueInspector.this.valueFallback.getResolvedConfig(configKey);
        }
      });
    } catch (ExecutionException e) {
      // should NOT come here
      throw new RuntimeException("Can not getOwnConfig for " + configKey);
    }
  }
  
  /**
   * {@inheritDoc}.
   *
   * <p>
   *   If present in the cache, return the cached {@link com.typesafe.config.Config} for given input
   *   Otherwise, simply delegate the functionality to the internal {ConfigStoreValueInspector} and store the value into cache
   * </p>
   */
  @Override
  public Map<ConfigKeyPath, Config> getOwnConfigs(Collection<ConfigKeyPath> configKeys) {
    Collection<ConfigKeyPath> configKeysNotInCache = new ArrayList<>();
    Map<ConfigKeyPath, Config> result = new HashMap<>();
    for(ConfigKeyPath configKey: configKeys){
      Config cachedValue = this.ownConfigCache.getIfPresent(configKey);
      if(cachedValue==null){
        configKeysNotInCache.add(configKey);
      }
      else{
        result.put(configKey, cachedValue);
      }
    }
    
    // for ConfigKeyPath which are not in cache
    if(configKeysNotInCache.size()>0){
      Map<ConfigKeyPath, Config> configsFromFallBack = this.valueFallback.getOwnConfigs(configKeysNotInCache);
      this.ownConfigCache.putAll(configsFromFallBack);
      result.putAll(configsFromFallBack);
    }
    
    return result;
  }
  
  /**
   * {@inheritDoc}.
   *
   * <p>
   *   If present in the cache, return the cached {@link com.typesafe.config.Config} for given input
   *   Otherwise, simply delegate the functionality to the internal {ConfigStoreValueInspector} and store the value into cache
   * </p>
   */
  @Override
  public Map<ConfigKeyPath, Config> getResolvedConfigs(Collection<ConfigKeyPath> configKeys) {
    Collection<ConfigKeyPath> configKeysNotInCache = new ArrayList<>();
    Map<ConfigKeyPath, Config> result = new HashMap<>();
    for(ConfigKeyPath configKey: configKeys){
      Config cachedValue = this.recursiveConfigCache.getIfPresent(configKey);
      if(cachedValue==null){
        configKeysNotInCache.add(configKey);
      }
      else{
        result.put(configKey, cachedValue);
      }
    }
    
    // for ConfigKeyPath which are not in cache
    if(configKeysNotInCache.size()>0){
      Map<ConfigKeyPath, Config> configsFromFallBack = this.valueFallback.getResolvedConfigs(configKeysNotInCache);
      this.recursiveConfigCache.putAll(configsFromFallBack);
      result.putAll(configsFromFallBack);
    }
    
    return result;
  }
}
