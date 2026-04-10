package com.network.deployment.model;

import java.util.ArrayList;
import java.util.List;

public class TopologyNode {

    private final String macAddress;
    private final List<TopologyNode> children = new ArrayList<>();

    public TopologyNode(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public List<TopologyNode> getChildren() {
        return children;
    }

    public void addChild(TopologyNode child) {
        this.children.add(child);
    }
}
