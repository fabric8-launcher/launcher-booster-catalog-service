package io.openshift.booster.catalog;

import java.util.Map;

public interface BoosterDataTransformer {
    
    Map<String, Object> transform(Map<String, Object> data);

}
