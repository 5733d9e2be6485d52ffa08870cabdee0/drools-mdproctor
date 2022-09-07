/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.drools.core.reteoo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.drools.core.common.Memory;
import org.drools.core.common.MemoryFactory;
import org.drools.core.common.NetworkNode;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.TupleSets;
import org.drools.core.common.TupleSetsImpl;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.AsyncReceiveNode.AsyncReceiveMemory;
import org.drools.core.reteoo.PathEndNode.PathMemSpec;
import org.drools.core.reteoo.QueryElementNode.QueryElementNodeMemory;
import org.drools.core.reteoo.TimerNode.TimerNodeMemory;
import org.drools.core.util.LinkedList;
import org.drools.core.util.LinkedListNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.core.phreak.SegmentUtilities.getQuerySegmentMemory;

public class SegmentMemory extends LinkedList<SegmentMemory>
        implements
        LinkedListNode<SegmentMemory> {

    protected static final Logger log = LoggerFactory.getLogger(SegmentMemory.class);
    protected static final boolean IS_LOG_TRACE_ENABLED = log.isTraceEnabled();

    private          LeftTupleNode      rootNode;
    private          LeftTupleNode      tipNode;
    private          LinkedList<Memory> nodeMemories;
    private          long               linkedNodeMask;
    private          long               dirtyNodeMask;
    private          long               allLinkedMaskTest;
    private          List<PathMemory>   pathMemories;
    private          long               segmentPosMaskBit;
    private          int                pos = -1;
    private          TupleSets<LeftTuple> stagedLeftTuples;
    private          boolean            active;
    private          SegmentMemory      previous;
    private          SegmentMemory      next;

    private transient List<PathMemory>  dataDrivenPathMemories;

    private transient List<SegmentMemory> peersWithDataDrivenPathMemories;

    public SegmentMemory() { }

    public SegmentMemory(LeftTupleNode rootNode) {
        this.rootNode = rootNode;
        this.linkedNodeMask = 0L;
        this.dirtyNodeMask = 0L;
        this.pathMemories = new ArrayList<>(1);
        this.nodeMemories = new LinkedList<>();
        this.stagedLeftTuples = new TupleSetsImpl<>();
    }

    public LeftTupleNode getRootNode() {
        return rootNode;
    }

    public LeftTupleNode getTipNode() {
        return tipNode;
    }

    public void setTipNode(LeftTupleNode tipNode) {
        this.tipNode = tipNode;
    }

    public LeftTupleSink getSinkFactory() {
        return (LeftTupleSink) rootNode;
    }

    public <T extends Memory> T createNodeMemory(MemoryFactory<T> memoryFactory,
                                                 ReteEvaluator reteEvaluator) {
        T memory = reteEvaluator.getNodeMemory(memoryFactory);
        nodeMemories.add(memory);
        return memory;
    }

    public LinkedList<Memory> getNodeMemories() {
        return nodeMemories;
    }

    public long getLinkedNodeMask() {
        return linkedNodeMask;
    }

    public void setLinkedNodeMask(long linkedNodeMask) {
        this.linkedNodeMask = linkedNodeMask;
    }

    public long getDirtyNodeMask() {
        return dirtyNodeMask;
    }

    public void updateDirtyNodeMask(long mask) {
        dirtyNodeMask |= mask;
    }

    public void updateCleanNodeMask(long mask) {
        dirtyNodeMask &= (~mask);
    }


    public String getRuleNames() {
        StringBuilder sbuilder = new StringBuilder();
        for (int i = 0; i < pathMemories.size(); i++) {
            if (i > 0) {
                sbuilder.append(", ");
            }
            sbuilder.append(pathMemories.get(i));
        }

        return sbuilder.toString();
    }

    public boolean linkNode(long mask, ReteEvaluator reteEvaluator) {
        linkedNodeMask |= mask;
        if (IS_LOG_TRACE_ENABLED) {
            log.trace("LinkNode notify=true nmask={} smask={} spos={} rules={}", mask, linkedNodeMask, pos, getRuleNames());
        }

        return notifyRuleLinkSegment( reteEvaluator );
    }

    public boolean linkNodeWithoutRuleNotify(long mask) {
        linkedNodeMask |= mask;
        if (IS_LOG_TRACE_ENABLED) {
            log.trace("LinkNode notify=false nmask={} smask={} spos={} rules={}", mask, linkedNodeMask, pos, getRuleNames());
        }

        return linkSegmentWithoutRuleNotify();
    }

    public boolean linkSegmentWithoutRuleNotify(long mask) {
        dirtyNodeMask |= mask;
        return linkSegmentWithoutRuleNotify();
    }

    private boolean linkSegmentWithoutRuleNotify() {
        boolean dataDrivePmemLinked = false;
        if (isSegmentLinked()) {
            for (int i = 0, length = pathMemories.size(); i < length; i++) {
                // do not use foreach, don't want Iterator object creation
                PathMemory pmem = pathMemories.get(i);
                pmem.linkSegmentWithoutRuleNotify(segmentPosMaskBit);
                dataDrivePmemLinked |= ( pmem.isDataDriven() && pmem.isRuleLinked() );
            }
        }
        return dataDrivePmemLinked;
    }

    public boolean notifyRuleLinkSegment(ReteEvaluator reteEvaluator, long mask) {
        dirtyNodeMask |= mask;
        return notifyRuleLinkSegment(reteEvaluator);
    }

    public boolean notifyRuleLinkSegment(ReteEvaluator reteEvaluator) {
        boolean dataDrivePmemLinked = false;
        if (isSegmentLinked()) {
            for (int i = 0, length = pathMemories.size(); i < length; i++) {
                // do not use foreach, don't want Iterator object creation
                PathMemory pmem = pathMemories.get(i);
                notifyRuleLinkSegment(reteEvaluator, pmem);
                dataDrivePmemLinked |= ( pmem.isDataDriven() && pmem.isRuleLinked() );
            }
        }
        return dataDrivePmemLinked;
    }

    public void notifyRuleLinkSegment(ReteEvaluator reteEvaluator, PathMemory pmem) {
        pmem.linkSegment(segmentPosMaskBit, reteEvaluator);
    }

    public boolean unlinkNode(long mask, ReteEvaluator reteEvaluator) {
        boolean dataDrivePmemLinked = false;
        boolean linked = isSegmentLinked();
        // some node unlinking does not unlink the segment, such as nodes after a Branch CE
        linkedNodeMask ^= mask;
        dirtyNodeMask |= mask;

        if (IS_LOG_TRACE_ENABLED) {
            log.trace("UnlinkNode notify=true nmask={} smask={} spos={} rules={}", mask, linkedNodeMask, pos, getRuleNames());
        }

        if (linked && !isSegmentLinked()) {
            for (int i = 0, length = pathMemories.size(); i < length; i++) {
                // do not use foreach, don't want Iterator object creation
                PathMemory pmem = pathMemories.get(i);
                // the data driven pmem has to be flushed only if the pmem was formerly linked
                dataDrivePmemLinked |= ( pmem.isDataDriven() && pmem.isRuleLinked() );
                pmem.unlinkedSegment(segmentPosMaskBit, reteEvaluator);
            }
        } else {
            // if not unlinked, then we still need to notify if the rule is linked
            for (int i = 0, length = pathMemories.size(); i < length; i++) {
                // do not use foreach, don't want Iterator object creation
                if (pathMemories.get(i).isRuleLinked() ){
                    pathMemories.get(i).doLinkRule(reteEvaluator);
                }
            }
        }
        return dataDrivePmemLinked;
    }

    public void unlinkSegment(ReteEvaluator reteEvaluator) {
        for (int i = 0, length = pathMemories.size(); i < length; i++) {
            // do not use foreach, don't want Iterator object creation
            pathMemories.get(i).unlinkedSegment(segmentPosMaskBit, reteEvaluator);
        }
    }

    public void unlinkNodeWithoutRuleNotify(long mask) {
        linkedNodeMask ^= mask;
        if (IS_LOG_TRACE_ENABLED) {
            log.trace("UnlinkNode notify=false nmask={} smask={} spos={} rules={}", mask, linkedNodeMask, pos, getRuleNames());
        }
    }

    public long getAllLinkedMaskTest() {
        return allLinkedMaskTest;
    }

    public void setAllLinkedMaskTest(long allLinkedTestMask) {
        this.allLinkedMaskTest = allLinkedTestMask;
    }

    public boolean isSegmentLinked() {
        return (linkedNodeMask & allLinkedMaskTest) == allLinkedMaskTest;
    }

    public List<PathMemory> getPathMemories() {
        return pathMemories;
    }

    public void addPathMemory(PathMemory pathMemory) {
        pathMemories.add(pathMemory);
        if (isSegmentLinked()) {
            pathMemory.linkSegmentWithoutRuleNotify(segmentPosMaskBit);
        }
        if (pathMemory.isDataDriven()) {
            if (dataDrivenPathMemories == null) {
                dataDrivenPathMemories = new ArrayList<>();
            }
            dataDrivenPathMemories.add(pathMemory);
        }
    }

    public void mergePathMemories(SegmentMemory segmentMemory) {
        for (PathMemory pmem : segmentMemory.getPathMemories()) {
            if ( isAssociatedWith( pmem ) ) {
                addPathMemory( pmem );
            }
        }
    }

    private boolean isAssociatedWith( PathMemory pmem ) {
        if (pmem instanceof RiaPathMemory) {
            for (RuleImpl rule : (( RiaPathMemory ) pmem).getAssociatedRules()) {
                if (rootNode.isAssociatedWith( rule )) {
                    return true;
                }
            }
            return false;
        }
        return rootNode.isAssociatedWith( pmem.getRule() );
    }

    public void removePathMemory(PathMemory pathMemory) {
        pathMemories.remove( pathMemory );
        if (pathMemory.isDataDriven()) {
            dataDrivenPathMemories.remove( pathMemory );
            if (dataDrivenPathMemories.isEmpty()) {
                dataDrivenPathMemories = null;
            }
        }
    }

    public PathMemory getFirstDataDrivenPathMemory() {
        return dataDrivenPathMemories == null ? null : dataDrivenPathMemories.get(0);
    }

    public boolean hasDataDrivenPathMemories() {
        return dataDrivenPathMemories != null;
    }

    public List<PathMemory> getDataDrivenPathMemories() {
        return dataDrivenPathMemories;
    }

    public long getSegmentPosMaskBit() {
        return segmentPosMaskBit;
    }

    public void setSegmentPosMaskBit(long nodeSegmenMask) {
        this.segmentPosMaskBit = nodeSegmenMask;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean evaluating) {
        this.active = evaluating;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public TupleSets<LeftTuple> getStagedLeftTuples() {
        return stagedLeftTuples;
    }

    @Override
    public void add(SegmentMemory segmentMemory) {
        super.add(segmentMemory);
        if (segmentMemory.hasDataDrivenPathMemories()) {
            if (peersWithDataDrivenPathMemories == null) {
                peersWithDataDrivenPathMemories = new ArrayList<>();
            }
            peersWithDataDrivenPathMemories.add(segmentMemory);
        }
    }

    @Override
    public void remove(SegmentMemory segmentMemory) {
        super.remove(segmentMemory);
        if (peersWithDataDrivenPathMemories != null) {
            peersWithDataDrivenPathMemories.remove(segmentMemory);
            if (peersWithDataDrivenPathMemories.isEmpty()) {
                peersWithDataDrivenPathMemories = null;
            }
        }
    }

    public Iterator<SegmentMemory> getPeersWithDataDrivenPathMemoriesIterator() {
        return peersWithDataDrivenPathMemories == null ? Collections.emptyIterator() : peersWithDataDrivenPathMemories.iterator();
    }

    public SegmentMemory getNext() {
        return this.next;
    }

    public void setNext(SegmentMemory next) {
        this.next = next;
    }

    public SegmentMemory getPrevious() {
        return this.previous;
    }

    public void setPrevious(SegmentMemory previous) {
        this.previous = previous;
    }

    public void nullPrevNext() {
        previous = null;
        next = null;
    }

    @Override
    public int hashCode() {
        return rootNode.getId();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof SegmentMemory && rootNode.getId() == ((SegmentMemory) obj).rootNode.getId());
    }

    public SegmentPrototype asPrototype() {
        return new SegmentPrototype(this);
    }

    public List<NetworkNode> getNodesInSegment() {
        List<NetworkNode> nodes = new java.util.LinkedList<>();
        NetworkNode currentNode = tipNode;
        while (currentNode != rootNode) {
            nodes.add(0, currentNode);
            currentNode = ((LeftTupleSinkNode)currentNode).getLeftTupleSource();
        }
        nodes.add(0, currentNode);
        return nodes;
    }

    public void reset(SegmentPrototype segmentPrototype) {
        this.dirtyNodeMask = 0L;
        this.linkedNodeMask = segmentPrototype != null ? segmentPrototype.linkedNodeMask : 0L;
        stagedLeftTuples.resetAll();
    }

    @Override
    public String toString() {
        return "Segment from " + rootNode + " to " + tipNode;
    }

    public static class SegmentPrototype {
        LeftTupleNode rootNode;
        LeftTupleNode tipNode;
        long linkedNodeMask;
        long allLinkedMaskTest;
        long segmentPosMaskBit;
        int pos;
        List<MemoryPrototype> memories = new ArrayList<>();
        List<NetworkNode> nodesInSegment;

        public SegmentPrototype(LeftTupleNode rootNode, LeftTupleNode tipNode) {
            this.rootNode = rootNode;
            this.tipNode = tipNode;
        }

        public SegmentPrototype(SegmentMemory smem) {
            this.rootNode = smem.rootNode;
            this.tipNode = smem.tipNode;
            this.linkedNodeMask = smem.linkedNodeMask;
            this.allLinkedMaskTest = smem.allLinkedMaskTest;
            this.segmentPosMaskBit = smem.segmentPosMaskBit;
            this.pos = smem.pos;
            for (Memory mem = smem.nodeMemories.getFirst(); mem != null; mem = mem.getNext()) {
                memories.add(MemoryPrototype.get(mem));
            }
        }

        public SegmentMemory newSegmentMemory(ReteEvaluator reteEvaluator) {
            SegmentMemory smem = new SegmentMemory(rootNode);
            smem.tipNode = tipNode;
            smem.linkedNodeMask = linkedNodeMask;
            smem.allLinkedMaskTest = allLinkedMaskTest;
            smem.segmentPosMaskBit = segmentPosMaskBit;
            smem.pos = pos;
            int i = 0;
            for (NetworkNode node : getNodesInSegment(smem)) {
                Memory mem = reteEvaluator.getNodeMemory((MemoryFactory) node);
                mem.setSegmentMemory(smem);
                smem.getNodeMemories().add(mem);
                MemoryPrototype proto = memories.get(i++);
                if (proto != null) {
                    proto.populateMemory(reteEvaluator, mem);
                }
            }

            return smem;
        }

        private List<NetworkNode> getNodesInSegment(SegmentMemory smem) {
            if (nodesInSegment == null) {
                nodesInSegment = smem.getNodesInSegment();
            }
            return nodesInSegment;
        }

        public LeftTupleNode getRootNode() {
            return rootNode;
        }

        public void setRootNode(LeftTupleNode rootNode) {
            this.rootNode = rootNode;
        }

        public LeftTupleNode getTipNode() {
            return tipNode;
        }

        public void setTipNode(LeftTupleNode tipNode) {
            this.tipNode = tipNode;
        }

        public long getLinkedNodeMask() {
            return linkedNodeMask;
        }

        public void setLinkedNodeMask(long linkedNodeMask) {
            this.linkedNodeMask = linkedNodeMask;
        }

        public long getAllLinkedMaskTest() {
            return allLinkedMaskTest;
        }

        public void setAllLinkedMaskTest(long allLinkedMaskTest) {
            this.allLinkedMaskTest = allLinkedMaskTest;
        }

        public long getSegmentPosMaskBit() {
            return segmentPosMaskBit;
        }

        public void setSegmentPosMaskBit(long segmentPosMaskBit) {
            this.segmentPosMaskBit = segmentPosMaskBit;
        }

        public int getPos() {
            return pos;
        }

        public void setPos(int pos) {
            this.pos = pos;
        }

        public List<MemoryPrototype> getMemories() {
            return memories;
        }

        public void setMemories(List<MemoryPrototype> memories) {
            this.memories = memories;
        }

        public List<NetworkNode> getNodesInSegment() {
            return nodesInSegment;
        }

        public void setNodesInSegment(List<NetworkNode> nodesInSegment) {
            this.nodesInSegment = nodesInSegment;
        }
    }

    public abstract static class MemoryPrototype {
        public static MemoryPrototype get(Memory memory) {
            if (memory instanceof BetaMemory) {
                return new BetaMemoryPrototype((BetaMemory)memory);
            }
            if (memory instanceof LeftInputAdapterNode.LiaNodeMemory) {
                return new LiaMemoryPrototype((LeftInputAdapterNode.LiaNodeMemory)memory);
            }
            if (memory instanceof QueryElementNode.QueryElementNodeMemory) {
                return new QueryMemoryPrototype((QueryElementNode.QueryElementNodeMemory)memory);
            }
            if (memory instanceof TimerNodeMemory) {
                return new TimerMemoryPrototype((TimerNodeMemory)memory);
            }
            if (memory instanceof AccumulateNode.AccumulateMemory) {
                return new AccumulateMemoryPrototype((AccumulateNode.AccumulateMemory)memory);
            }
            if (memory instanceof ReactiveFromNode.ReactiveFromMemory) {
                return new ReactiveFromMemoryPrototype((ReactiveFromNode.ReactiveFromMemory)memory);
            }
            return null;
        }

        public abstract void populateMemory(ReteEvaluator reteEvaluator, Memory memory);
    }

    public static class BetaMemoryPrototype extends MemoryPrototype {

        private long nodePosMaskBit;
        private RightInputAdapterNode riaNode;

        public BetaMemoryPrototype(long nodePosMaskBit, RightInputAdapterNode riaNode) {
            this.nodePosMaskBit = nodePosMaskBit;
            this.riaNode = riaNode;
        }

        private BetaMemoryPrototype(BetaMemory betaMemory) {
            this.nodePosMaskBit = betaMemory.getNodePosMaskBit();
            if (betaMemory.getRiaRuleMemory() != null) {
                riaNode = betaMemory.getRiaRuleMemory().getRightInputAdapterNode();
            }
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory memory) {
            BetaMemory betaMemory = (BetaMemory)memory;
            betaMemory.setNodePosMaskBit(nodePosMaskBit);
            if (riaNode != null) {
                RightInputAdapterNode.RiaNodeMemory riaMem = reteEvaluator.getNodeMemory(riaNode);
                betaMemory.setRiaRuleMemory(riaMem.getRiaPathMemory());
            }
        }
    }

    public static class LiaMemoryPrototype extends MemoryPrototype {

        private final long nodePosMaskBit;

        public LiaMemoryPrototype(long nodePosMaskBit) {
            this.nodePosMaskBit = nodePosMaskBit;
        }

        private LiaMemoryPrototype(LeftInputAdapterNode.LiaNodeMemory liaMemory) {
            this.nodePosMaskBit = liaMemory.getNodePosMaskBit();
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory liaMemory) {
            ((SegmentNodeMemory)liaMemory).setNodePosMaskBit( nodePosMaskBit );
        }
    }

    public static class ReactiveFromMemoryPrototype extends MemoryPrototype {

        private final long nodePosMaskBit;

        public ReactiveFromMemoryPrototype(long nodePosMaskBit) {
            this.nodePosMaskBit = nodePosMaskBit;
        }

        private ReactiveFromMemoryPrototype(ReactiveFromNode.ReactiveFromMemory memory) {
            this.nodePosMaskBit = memory.getBetaMemory().getNodePosMaskBit();
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory memory) {
            ((ReactiveFromNode.ReactiveFromMemory)memory).setNodePosMaskBit( nodePosMaskBit );
        }
    }

    public static class QueryMemoryPrototype extends MemoryPrototype {

        private final long nodePosMaskBit;
        private final QueryElementNode queryNode;

        public QueryMemoryPrototype(QueryElementNode.QueryElementNodeMemory queryMemory) {
            nodePosMaskBit = queryMemory.getNodePosMaskBit();
            this.queryNode = queryMemory.getNode();
        }

        public QueryMemoryPrototype(long nodePosMaskBit, QueryElementNode queryNode) {
            this.nodePosMaskBit = nodePosMaskBit;
            this.queryNode = queryNode;
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory mem) {
            QueryElementNodeMemory qmem = (QueryElementNodeMemory)  mem;
            SegmentMemory querySmem = getQuerySegmentMemory(reteEvaluator, (LeftTupleSource)qmem.getSegmentMemory().getRootNode(), queryNode);
            qmem.setQuerySegmentMemory(querySmem);
            qmem.setNodePosMaskBit(nodePosMaskBit);
        }
    }

    public static class TimerMemoryPrototype extends MemoryPrototype {

        private final long nodePosMaskBit;

        public TimerMemoryPrototype(long nodePosMaskBit) {
            this.nodePosMaskBit = nodePosMaskBit;
        }

        private TimerMemoryPrototype(TimerNodeMemory timerMemory) {
            nodePosMaskBit = timerMemory.getNodePosMaskBit();
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory mem) {
            TimerNodeMemory tmem = (TimerNodeMemory)  mem;
            tmem.setNodePosMaskBit(nodePosMaskBit);
        }
    }

    public static class AsyncReceiveMemoryPrototype extends MemoryPrototype {

        private final long nodePosMaskBit;

        public AsyncReceiveMemoryPrototype(long nodePosMaskBit) {
            this.nodePosMaskBit = nodePosMaskBit;
        }

        private AsyncReceiveMemoryPrototype(AsyncReceiveMemory asyncReceiveMemory) {
            nodePosMaskBit = asyncReceiveMemory.getNodePosMaskBit();
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory mem) {
            AsyncReceiveMemory amem = (AsyncReceiveMemory)  mem;
            amem.setNodePosMaskBit(nodePosMaskBit);
        }
    }

    public static class AccumulateMemoryPrototype extends MemoryPrototype {

        private final BetaMemoryPrototype betaProto;

        private AccumulateMemoryPrototype(AccumulateNode.AccumulateMemory accMemory) {
            betaProto = new BetaMemoryPrototype(accMemory.getBetaMemory());
        }

        @Override
        public void populateMemory(ReteEvaluator reteEvaluator, Memory accMemory) {
            betaProto.populateMemory(reteEvaluator, ((AccumulateNode.AccumulateMemory)accMemory).getBetaMemory());
        }
    }
}
