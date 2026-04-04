package com.extremecraft.progression.skilltree;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SkillTree {
    private final String id;
    private final Map<String, SkillNode> nodes = new LinkedHashMap<>();

    public SkillTree(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public void addNode(SkillNode node) {
        nodes.put(node.id(), node);
    }

    public SkillNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public Collection<SkillNode> nodes() {
        return nodes.values();
    }

    public boolean isEmpty() {
        return nodes.isEmpty();
    }
}
