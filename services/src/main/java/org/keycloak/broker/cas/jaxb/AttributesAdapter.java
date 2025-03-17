package org.keycloak.broker.cas.jaxb;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.util.List;
import java.util.Map;

public class AttributesAdapter extends XmlAdapter<AttributesWrapper, Map<String, List<String>>> {

    @Override
    public AttributesWrapper marshal(final Map<String, List<String>> attributes) {
        throw new UnsupportedOperationException("This adapter only supports from xml to map.");
    }

    @Override
    public Map<String, List<String>> unmarshal(final AttributesWrapper attributesWrapper) {
        return attributesWrapper.toMap();
    }
}
