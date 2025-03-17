package org.keycloak.broker.cas.jaxb;

import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlType;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@XmlType
public class AttributesWrapper {

    private final List<Node> attributes = new ArrayList<>();

    @XmlAnyElement
    public List<Node> getAttributes() {
        return attributes;
    }

    public Map<String, List<String>> toMap() {
        return attributes.stream()
                .collect(
                        Collectors.groupingBy(
                                Node::getLocalName, Collectors.mapping(Node::getTextContent, Collectors.toList())));
    }
}
