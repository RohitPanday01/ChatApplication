package com.rohit.ChatApplication.data.message;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

@Component
@Data
public class NodeIdentity {
    private final String nodeId;

    public NodeIdentity(@Value("${node.id:#{null}}") String nodeIdConfig) {
        if (nodeIdConfig != null && !nodeIdConfig.isBlank()) {
            this.nodeId = nodeIdConfig;
        } else {
            try {
                this.nodeId = InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to determine node identity", e);
            }
        }
    }

    public String getNodeId() {
        return nodeId;
    }
}
