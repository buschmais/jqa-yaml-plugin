package com.buschmais.jqassistant.plugin.yaml.api.model;

import java.util.List;

import com.buschmais.xo.neo4j.api.annotation.Label;
import com.buschmais.xo.neo4j.api.annotation.Property;
import com.buschmais.xo.neo4j.api.annotation.Relation;

@Label("Value")
public interface YAMLValueDescriptor extends YAMLDescriptor, YAMLValueBucket {

    @Property("value")
    String getValue();

    void setValue(String value);


    // Required for sequence of sequences...
    @Relation("CONTAINS_VALUE")
    List<YAMLValueDescriptor> getValues();


    /**
     * Returns the position of the value relative to its parent
     * in document order.
     *
     * The position starts with zero and is relative to
     * it's parent. The parent could either be the containing
     * document or a key.
     *
     * @return the position of the key relative to it's parent.
     */
    @Property("position")
    int getPosition();
}
