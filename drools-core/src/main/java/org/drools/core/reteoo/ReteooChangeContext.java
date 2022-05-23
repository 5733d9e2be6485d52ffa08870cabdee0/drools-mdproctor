package org.drools.core.reteoo;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.common.BaseNode;

public class ReteooChangeContext {
    private List<TerminalNode> terminalsAdded = new ArrayList<>();

    private List<TerminalNode> terminalsRemoved = new ArrayList<>();
    private List<EntryPointNode> entryPointNodes = new ArrayList<>();

    private List<BaseNode> nodesToClear = new ArrayList<>();

    public ReteooChangeContext() {

    }

    public List<TerminalNode> getTerminalsAdded() {
        return terminalsAdded;
    }

    public List<TerminalNode> getTerminalsRemoved() {
        return terminalsRemoved;
    }

    public List<EntryPointNode> getEntryPointNodes() {
        return entryPointNodes;
    }

    public List<BaseNode> getNodesToClear() {
        return nodesToClear;
    }
}
