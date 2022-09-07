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

package org.drools.core.phreak;

import org.drools.core.common.MemoryFactory;
import org.drools.core.common.NetworkNode;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.impl.RuleBase;
import org.drools.core.reteoo.AlphaNode;
import org.drools.core.reteoo.AsyncReceiveNode;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.ExistsNode;
import org.drools.core.reteoo.LeftInputAdapterNode;
import org.drools.core.reteoo.LeftTupleNode;
import org.drools.core.reteoo.LeftTupleSinkNode;
import org.drools.core.reteoo.LeftTupleSinkPropagator;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.NodeTypeEnums;
import org.drools.core.reteoo.NotNode;
import org.drools.core.reteoo.ObjectSink;
import org.drools.core.reteoo.ObjectSource;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.QueryElementNode;
import org.drools.core.reteoo.RightInputAdapterNode;
import org.drools.core.reteoo.SegmentMemory;
import org.drools.core.reteoo.SegmentMemory.AsyncReceiveMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.BetaMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.LiaMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.QueryMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.SegmentPrototype;
import org.drools.core.reteoo.SegmentMemory.ReactiveFromMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.TimerMemoryPrototype;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.reteoo.TimerNode;
import org.drools.core.rule.constraint.QueryNameConstraint;

public class SegmentUtilities2 {

    public static void createPathMemories(TerminalNode tn, RuleBase rbase) {
        // Will initialise all segments in a path
        createPathMemories(tn.getLeftTupleSource(), null, rbase);
    }

    // notes, looking into if/why I need stopNode
    private static void createPathMemories(LeftTupleSource lts, LeftTupleSource stopNode, RuleBase rbase) {
        LeftTupleSource segmentRoot = lts;
        LeftTupleSource segmentTip = lts;
        while (segmentRoot.getType() != NodeTypeEnums.LeftInputAdapterNode && segmentRoot != stopNode) {
            // iterate to find the actual segment root
            while (!SegmentUtilities2.isRootNode(segmentRoot, null)) {
                segmentRoot = segmentRoot.getLeftTupleSource();
            }

            createSegmentMemory(segmentRoot, segmentTip, rbase);

            // reset to find the next segment
            segmentRoot = segmentRoot.getLeftTupleSource();
            segmentTip = segmentRoot.getLeftTupleSource();
        }
    }

    /**
     * Initialises the NodeSegment memory for all nodes in the segment.
     */
    public static SegmentPrototype createSegmentMemory(LeftTupleSource segmentRoot, LeftTupleSource segmentTip, RuleBase rbase) {
        LeftTupleSource tupleSource = segmentRoot;
        int nodeTypesInSegment = 0;

        SegmentPrototype smem = new SegmentPrototype(segmentRoot, segmentTip);

        // Iterate all nodes on the same segment, assigning their position as a bit mask value
        // allLinkedTestMask is the resulting mask used to test if all nodes are linked in
        long nodePosMask = 1;
        long allLinkedTestMask = 0;
        boolean updateNodeBit = true;  // nodes after a branch CE can notify, but they cannot impact linking

        while (true) {
            nodeTypesInSegment = updateNodeTypesMask(tupleSource, nodeTypesInSegment);
            if (NodeTypeEnums.isBetaNode(tupleSource)) {
                allLinkedTestMask = processBetaNode((BetaNode)tupleSource, smem, nodePosMask, allLinkedTestMask, updateNodeBit, rbase);
            } else {
                switch (tupleSource.getType()) {
                    case NodeTypeEnums.LeftInputAdapterNode:
                        allLinkedTestMask = processLiaNode((LeftInputAdapterNode) tupleSource, smem, nodePosMask, allLinkedTestMask);
                        break;
                    case NodeTypeEnums.ConditionalBranchNode:
                        updateNodeBit = false;
                        break;
                    case NodeTypeEnums.ReactiveFromNode:
                        processReactiveFromNode((MemoryFactory) tupleSource, smem, nodePosMask);
                        break;
                    case NodeTypeEnums.TimerConditionNode:
                        processTimerNode((TimerNode) tupleSource, smem, nodePosMask);
                        break;
                    case NodeTypeEnums.AsyncReceiveNode:
                        processAsyncReceiveNode((AsyncReceiveNode) tupleSource, smem, nodePosMask);
                        break;
                    case NodeTypeEnums.QueryElementNode:
                        updateNodeBit = processQueryNode((QueryElementNode) tupleSource, segmentRoot, smem, nodePosMask);
                        break;
                }
            }

            nodePosMask = nextNodePosMask(nodePosMask);

            if (tupleSource == segmentTip) {
                break;
            }
            tupleSource = (LeftTupleSource) tupleSource.getSinkPropagator().getFirstLeftTupleSink();
        }
        smem.setAllLinkedMaskTest(allLinkedTestMask);

        // iterate to find root and determine the SegmentNodes position in the RuleSegment
        LeftTupleSource pathRoot = segmentRoot;
        int ruleSegmentPosMask = 1;
        int counter = 0;
        while (pathRoot.getType() != NodeTypeEnums.LeftInputAdapterNode) {
            LeftTupleSource leftTupleSource = pathRoot.getLeftTupleSource();
            if (SegmentUtilities2.isNonTerminalTipNode(leftTupleSource, null)) {
                // for each new found segment, increase the mask bit position
                ruleSegmentPosMask = ruleSegmentPosMask << 1;
                counter++;
            }
            pathRoot = leftTupleSource;
        }
        smem.setSegmentPosMaskBit(ruleSegmentPosMask);
        smem.setPos(counter);

        rbase.registerSegmentPrototype(segmentRoot, smem);

        return smem;
    }

    public static long nextNodePosMask(long nodePosMask) {
        // prevent overflow of segment and path memories masks when a segment has 64 or more nodes or a path has 64 or more segments
        // in this extreme case all the items after the 64th will be all mapped by the same bit and then the linking of one of them
        // will be enough to consider all those item linked
        long nextNodePosMask = nodePosMask << 1;
        return nextNodePosMask > 0 ? nextNodePosMask : nodePosMask;
    }

    private static SegmentMemory restoreSegmentFromPrototype(LeftTupleSource segmentRoot, int nodeTypesInSegment, ReteEvaluator reteEvaluator) {
        SegmentMemory smem = reteEvaluator.getKnowledgeBase().createSegmentFromPrototype(reteEvaluator, segmentRoot);
        if ( smem != null ) {
            SegmentUtilities.updateRiaAndTerminalMemory(segmentRoot, segmentRoot, smem, reteEvaluator, true, nodeTypesInSegment);
        }
        return smem;
    }

    private static boolean processQueryNode(QueryElementNode queryNode, LeftTupleSource segmentRoot, SegmentPrototype smem, long nodePosMask) {
        SegmentPrototype querySmem = null; //getQuerySegmentMemory(reteEvaluator, segmentRoot, queryNode);
        QueryMemoryPrototype queryNodeMem = new QueryMemoryPrototype(nodePosMask, queryNode);
        smem.getMemories().add(queryNodeMem);

        return ! queryNode.getQueryElement().isAbductive();
    }

    private static void processAsyncReceiveNode(AsyncReceiveNode tupleSource, SegmentPrototype smem, long nodePosMask) {
        AsyncReceiveMemoryPrototype mem = new AsyncReceiveMemoryPrototype(nodePosMask);
        smem.getMemories().add(mem);
    }

    private static void processReactiveFromNode(MemoryFactory tupleSource, SegmentPrototype smem, long nodePosMask) {
        ReactiveFromMemoryPrototype mem = new ReactiveFromMemoryPrototype(nodePosMask);
        smem.getMemories().add(mem);
    }

    private static void processTimerNode(TimerNode tupleSource, SegmentPrototype smem, long nodePosMask) {
        TimerMemoryPrototype tnMem = new TimerMemoryPrototype(nodePosMask);
        smem.getMemories().add(tnMem);
    }

    private static long processLiaNode(LeftInputAdapterNode tupleSource, SegmentPrototype smem, long nodePosMask, long allLinkedTestMask) {
        LiaMemoryPrototype liaMemory = new LiaMemoryPrototype(nodePosMask);
        smem.getMemories().add(liaMemory);
        allLinkedTestMask = allLinkedTestMask | nodePosMask;
        return allLinkedTestMask;
    }

    private static long processBetaNode(BetaNode betaNode, SegmentPrototype smem, long nodePosMask, long allLinkedTestMask, boolean updateNodeBit, RuleBase rbase) {
        RightInputAdapterNode riaNode = null;
        if (betaNode.isRightInputIsRiaNode()) {
            // there is a subnetwork, so create all it's segment memory prototypes
            riaNode = (RightInputAdapterNode) betaNode.getRightInput();
            createPathMemories(riaNode.getLeftTupleSource(), riaNode.getStartTupleSource(), rbase);

            if (updateNodeBit && canBeDisabled(betaNode) && riaNode.getPathMemSpec().allLinkedTestMask() > 0) {
                // only ria's with reactive subnetworks can be disabled and thus need checking
                allLinkedTestMask = allLinkedTestMask | nodePosMask;
            }
        } else if (updateNodeBit && canBeDisabled(betaNode)) {
            allLinkedTestMask = allLinkedTestMask | nodePosMask;

        }

        BetaMemoryPrototype bm = new BetaMemoryPrototype(nodePosMask, riaNode);
        smem.getMemories().add(bm);
        return allLinkedTestMask;
    }

    private static boolean canBeDisabled(BetaNode betaNode) {
        // non empty not nodes and accumulates can never be disabled and thus don't need checking
        return (!(NodeTypeEnums.NotNode == betaNode.getType() && !((NotNode) betaNode).isEmptyBetaConstraints()) &&
                NodeTypeEnums.AccumulateNode != betaNode.getType() && !betaNode.isRightInputPassive());
    }

    /**
     * Is the LeftTupleSource a node in the sub network for the RightInputAdapterNode
     * To be in the same network, it must be a node is after the two output of the parent
     * and before the rianode.
     */
    public static boolean inSubNetwork(RightInputAdapterNode riaNode, LeftTupleSource leftTupleSource) {
        LeftTupleSource startTupleSource = riaNode.getStartTupleSource();
        LeftTupleSource parent = riaNode.getLeftTupleSource();

        while (parent != startTupleSource) {
            if (parent == leftTupleSource) {
                return true;
            }
            parent = parent.getLeftTupleSource();
        }

        return false;
    }

    /**
     * Returns whether the node is the root of a segment.
     * Lians are always the root of a segment.
     *
     * node cannot be null.
     *
     * The result should discount any removingRule. That means it gives you the result as
     * if the rule had already been removed from the network.
     */
    public static boolean isRootNode(LeftTupleNode node, TerminalNode removingTN) {
        return node.getType() == NodeTypeEnums.LeftInputAdapterNode || isNonTerminalTipNode( node.getLeftTupleSource(), removingTN );
    }

    /**
     * Returns whether the node is the tip of a segment.
     * EndNodes (rtn and rian) are always the tip of a segment.
     *
     * node cannot be null.
     *
     * The result should discount any removingRule. That means it gives you the result as
     * if the rule had already been removed from the network.
     */
    public static boolean isTipNode( LeftTupleNode node, TerminalNode removingTN ) {
        return NodeTypeEnums.isEndNode(node) || isNonTerminalTipNode( node, removingTN );
    }

    public static boolean isNonTerminalTipNode( LeftTupleNode node, TerminalNode removingTN ) {
        LeftTupleSinkPropagator sinkPropagator = node.getSinkPropagator();

        if (removingTN == null) {
            return sinkPropagator.size() > 1;
        }

        if (sinkPropagator.size() == 1) {
            return false;
        }

        // we know the sink size is creater than 1 and that there is a removingRule that needs to be ignored.
        int count = 0;
        for ( LeftTupleSinkNode sink = sinkPropagator.getFirstLeftTupleSink(); sink != null; sink = sink.getNextLeftTupleSinkNode() ) {
            if ( sinkNotExclusivelyAssociatedWithTerminal( removingTN, sink ) ) {
                count++;
                if ( count > 1 ) {
                    // There is more than one sink that is not for the removing rule
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean sinkNotExclusivelyAssociatedWithTerminal( TerminalNode removingTN, LeftTupleSinkNode sink ) {
        return sink.getAssociationsSize() > 1 || !sink.isAssociatedWith( removingTN.getRule() ) ||
               !removingTN.isTerminalNodeOf( sink ) || hasTerminalNodesDifferentThan( sink, removingTN );
    }

    private static boolean hasTerminalNodesDifferentThan(LeftTupleSinkNode node, TerminalNode tn) {
        LeftTupleSinkPropagator sinkPropagator = node.getSinkPropagator();
        for ( LeftTupleSinkNode sink = sinkPropagator.getFirstLeftTupleSink(); sink != null; sink = sink.getNextLeftTupleSinkNode() )  {
            if (sink instanceof TerminalNode) {
                if (tn.getId() != sink.getId()) {
                    return true;
                }
            } else if (hasTerminalNodesDifferentThan(sink, tn)) {
                return true;
            }
        }
        return false;
    }

    private static ObjectTypeNode getQueryOtn(LeftTupleSource lts) {
        while (!(lts instanceof LeftInputAdapterNode)) {
            lts = lts.getLeftTupleSource();
        }

        LeftInputAdapterNode liaNode = (LeftInputAdapterNode) lts;
        ObjectSource os = liaNode.getObjectSource();
        while (!(os instanceof EntryPointNode)) {
            os = os.getParentObjectSource();
        }

        return ((EntryPointNode) os).getQueryNode();
    }

    private static LeftInputAdapterNode getQueryLiaNode(String queryName, ObjectTypeNode queryOtn) {
        for (ObjectSink sink : queryOtn.getObjectSinkPropagator().getSinks()) {
            AlphaNode alphaNode = (AlphaNode) sink;
            QueryNameConstraint nameConstraint = (QueryNameConstraint) alphaNode.getConstraint();
            if (queryName.equals(nameConstraint.getQueryName())) {
                return (LeftInputAdapterNode) alphaNode.getObjectSinkPropagator().getSinks()[0];
            }
        }

        throw new RuntimeException("Unable to find query '" + queryName + "'");
    }

    private static final int NOT_NODE_BIT               = 1 << 0;
    private static final int JOIN_NODE_BIT              = 1 << 1;
    private static final int REACTIVE_EXISTS_NODE_BIT   = 1 << 2;
    private static final int PASSIVE_EXISTS_NODE_BIT    = 1 << 3;

    public static int updateNodeTypesMask(NetworkNode node, int mask) {
        if (node != null) {
            switch ( node.getType() ) {
                case NodeTypeEnums.JoinNode:
                    mask |= JOIN_NODE_BIT;
                    break;
                case NodeTypeEnums.ExistsNode:
                    if ( ( (ExistsNode) node ).isRightInputPassive() ) {
                        mask |= PASSIVE_EXISTS_NODE_BIT;
                    } else {
                        mask |= REACTIVE_EXISTS_NODE_BIT;
                    }
                    break;
                case NodeTypeEnums.NotNode:
                    mask |= NOT_NODE_BIT;
                    break;
            }
        }
        return mask;
    }

    public static boolean isSet(int mask, int bit) {
        return (mask & bit) == bit;
    }
}
