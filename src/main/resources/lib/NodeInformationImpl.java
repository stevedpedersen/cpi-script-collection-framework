/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sap.esb.nodeinfo.NodeInformation
 *  com.sap.it.nm.node.NodeLocal
 */
package com.sap.esb.monitoring.nodeinfo.impl;

import com.sap.esb.nodeinfo.NodeInformation;
import com.sap.it.nm.node.NodeLocal;
import java.net.URI;

public class NodeInformationImpl
implements NodeInformation {
    private NodeLocal nodeLocal;

    public void setNode(NodeLocal node) {
        this.nodeLocal = node;
    }

    public String getNodeDiscriminator() {
        return this.nodeLocal.getLocalNode().getTenant().getId();
    }

    public String getTenantId() {
        return this.nodeLocal.getLocalNode().getTenant().getId();
    }

    public String getTenantName() {
        return this.nodeLocal.getLocalNode().getTenant().getName();
    }

    public URI getTmnUri() {
        return this.nodeLocal.getOperationsUri();
    }
}
