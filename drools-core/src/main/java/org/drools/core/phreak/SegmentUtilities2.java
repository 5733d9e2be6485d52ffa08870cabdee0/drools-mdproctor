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

import org.drools.core.common.NetworkNode;
import org.drools.core.impl.RuleBase;
import org.drools.core.reteoo.AlphaNode;
import org.drools.core.reteoo.AlphaTerminalNode;
import org.drools.core.reteoo.AsyncReceiveNode;
import org.drools.core.reteoo.AsyncSendNode;
import org.drools.core.reteoo.BetaNode;
import org.drools.core.reteoo.ConditionalBranchNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.EvalConditionNode;
import org.drools.core.reteoo.ExistsNode;
import org.drools.core.reteoo.FromNode;
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
import org.drools.core.reteoo.PathEndNode;
import org.drools.core.reteoo.QueryElementNode;
import org.drools.core.reteoo.ReactiveFromNode;
import org.drools.core.reteoo.RightInputAdapterNode;
import org.drools.core.reteoo.SegmentMemory.AccumulateMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.AsyncReceiveMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.AsyncSendMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.BetaMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.ConditionalBranchMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.EvalMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.FromMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.LiaMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.MemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.QueryMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.RightInputAdapterPrototype;
import org.drools.core.reteoo.SegmentMemory.SegmentPrototype;
import org.drools.core.reteoo.SegmentMemory.ReactiveFromMemoryPrototype;
import org.drools.core.reteoo.SegmentMemory.TerminalPrototype;
import org.drools.core.reteoo.SegmentMemory.TimerMemoryPrototype;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.reteoo.TimerNode;
import org.drools.core.rule.constraint.QueryNameConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SegmentUtilities2 {

    public static void createPathMemories(TerminalNode tn, RuleBase rbase) {
        // Will initialise all segments in a path
        SegmentPrototype[] smems = createPathMemories(tn, null, rbase);

        // smems are null, if there is no beta network. Which means it has an AlphaTerminalNode
        if  (smems != null) {
            setSegments(tn, smems);
        }
    }

    private static void setSegments(PathEndNode endNode, SegmentPrototype[] smems) {
        List<SegmentPrototype> eager = new ArrayList<>();
        for (SegmentPrototype smem : smems) {
            // The segments before the start of a subnetwork, will be null for a rian path.
            if (smem != null && requiresAnEagerSegment(smem.getNodeTypesInSegment())) {
                eager.add(smem);
            }
        }
        endNode.setEagerSegmentPrototypes(eager.toArray(new SegmentPrototype[eager.size()]));

        endNode.setSegmentPrototypes(smems);
    }

    // notes, looking into if/why I need stopNode
    public static SegmentPrototype[] createPathMemories(LeftTupleNode lts, LeftTupleSource stopNode, RuleBase rbase) {
        LeftTupleNode segmentRoot = lts;
        LeftTupleNode segmentTip = lts;
        List<SegmentPrototype> smems = new ArrayList<>();
        boolean inside = true; // as it starts at the terminal aor ria node, we know that it starts inside.
        do {
            // iterate to find the actual segment root
            while (!SegmentUtilities2.isRootNode(segmentRoot, null)) {
                segmentRoot = segmentRoot.getLeftTupleSource();
            }

            if (segmentRoot instanceof AlphaTerminalNode &&
                NodeTypeEnums.isTerminalNode(segmentTip)) {
                return null; // this path has no beta nodes, so no pmem is needed
            }

            // Store all nodes for the main path. If this is a subnetwork, only store nodes inside of it.
            if (inside) {
                SegmentPrototype smem = createSegmentMemory(segmentRoot, segmentTip, rbase);
                smems.add(smem);
            } else {
                smems.add(null);
            }

            if ( inside && segmentRoot == stopNode) {
                inside = false;
            }

            // this is the new segment so set both to same, and it iterates for the actual segmentRoot next loop.
            segmentRoot = segmentRoot.getLeftTupleSource();
            segmentTip = segmentRoot;
        } while (segmentRoot != null); // it's after lian

        // node 0 needs to go first.
        Collections.reverse(smems);

        // reset to find the next segments and set their position and their bit mask
        int ruleSegmentPosMask = 1;
        for (int counter = 0; counter < smems.size(); counter++) {
            if ( smems.get(counter) != null) { // The segments before the start of a subnetwork, will be null for a rian path.
                smems.get(counter).setPos(counter);
                smems.get(counter).setSegmentPosMaskBit(ruleSegmentPosMask);
            }
            ruleSegmentPosMask = ruleSegmentPosMask << 1;
        }

        return smems.toArray(new SegmentPrototype[smems.size()]);
    }

    /**
     * Initialises the NodeSegment memory for all nodes in the segment.
     */
    public static SegmentPrototype createSegmentMemory(LeftTupleNode segmentRoot, LeftTupleNode segmentTip, RuleBase rbase) {
        LeftTupleNode node = segmentRoot;
        int nodeTypesInSegment = 0;

        SegmentPrototype smem = new SegmentPrototype(segmentRoot, segmentTip);
        List<MemoryPrototype> memories = new ArrayList<>();
        List<NetworkNode> nodes = new ArrayList<>();

        // Iterate all nodes on the same segment, assigning their position as a bit mask value
        // allLinkedTestMask is the resulting mask used to test if all nodes are linked in
        long nodePosMask = 1;
        long allLinkedTestMask = 0;
        boolean updateNodeBit = true;  // nodes after a branch CE can notify, but they cannot impact linking

        while (true) {
            nodeTypesInSegment = updateNodeTypesMask(node, nodeTypesInSegment);
            if (NodeTypeEnums.isBetaNode(node)) {
                allLinkedTestMask = processBetaNode((BetaNode)node, smem, memories, nodes, nodePosMask, allLinkedTestMask, updateNodeBit, rbase);
            } else {
                switch (node.getType()) {
                    case NodeTypeEnums.LeftInputAdapterNode:
                        allLinkedTestMask = processLiaNode((LeftInputAdapterNode) node, smem, memories, nodes, nodePosMask, allLinkedTestMask);
                        break;
                    case NodeTypeEnums.ConditionalBranchNode:
                        processConditionalBranchNode((ConditionalBranchNode) node, smem, memories, nodes);
                        updateNodeBit = false;
                        break;
                    case NodeTypeEnums.FromNode:
                        processFromNode((FromNode) node, smem, memories, nodes);
                        break;
                    case NodeTypeEnums.EvalConditionNode:
                        processEvalFromNode((EvalConditionNode) node, smem, memories, nodes);
                        break;
                    case NodeTypeEnums.ReactiveFromNode:
                        processReactiveFromNode((ReactiveFromNode) node, smem, memories, nodes, nodePosMask);
                        break;
                    case NodeTypeEnums.TimerConditionNode:
                        processTimerNode((TimerNode) node, smem, memories, nodes, nodePosMask);
                        break;
                    case NodeTypeEnums.AsyncSendNode:
                        processAsyncSendNode((AsyncSendNode) node, smem, memories, nodes);
                        break;
                    case NodeTypeEnums.AsyncReceiveNode:
                        processAsyncReceiveNode((AsyncReceiveNode) node, smem, memories, nodes, nodePosMask);
                        break;
                    case NodeTypeEnums.QueryElementNode:
                        updateNodeBit = processQueryNode((QueryElementNode) node, segmentRoot, smem, memories, nodes, nodePosMask);
                        break;
                    case NodeTypeEnums.RightInputAdapterNode:
                        processRightInputAdapterNode((RightInputAdapterNode) node, smem, memories, nodes);
                        break;
                    case NodeTypeEnums.RuleTerminalNode:
                    case NodeTypeEnums.QueryTerminalNode:
                        processTerminalNode((TerminalNode) node, smem, memories, nodes);
                        break;
                    default:
                        System.out.println("Node: " + node);
                }
            }

            nodePosMask = nextNodePosMask(nodePosMask);

            if (node == segmentTip) {
                break;
            }
            node = node.getSinkPropagator().getFirstLeftTupleSink();
        }
        smem.setAllLinkedMaskTest(allLinkedTestMask);

//
        List<PathEndNode> endNodes = new ArrayList<>();
        collectPathEndNodes(segmentTip, endNodes);

        smem.setNodesInSegment(nodes.toArray( new NetworkNode[nodes.size()]));
        smem.setMemories(memories.toArray( new MemoryPrototype[memories.size()]));
        smem.setPathEndNodes(endNodes.toArray( new PathEndNode[endNodes.size()]));
        smem.setNodeTypesInSegment(nodeTypesInSegment);

        rbase.registerSegmentPrototype(segmentRoot, smem);

        return smem;
    }

    public static boolean requiresAnEagerSegment(int nodeTypesInSegment) {
        // A Not node has to be eagerly initialized unless in its segment there is at least a join node
        return isSet(nodeTypesInSegment, NOT_NODE_BIT) &&
             !isSet(nodeTypesInSegment, JOIN_NODE_BIT) &&
             !isSet(nodeTypesInSegment, REACTIVE_EXISTS_NODE_BIT);
    }

    private static void collectPathEndNodes(LeftTupleNode lt,
                                            List<PathEndNode> endNodes) {
         if (NodeTypeEnums.isTerminalNode(lt)) {
            endNodes.add((PathEndNode) lt);
        } else if (NodeTypeEnums.RightInputAdapterNode == lt.getType()) {
            endNodes.add( (PathEndNode) lt );
        }

        for (LeftTupleSinkNode sink = lt.getSinkPropagator().getLastLeftTupleSink(); sink != null; sink = sink.getPreviousLeftTupleSinkNode()) {
            collectPathEndNodes(sink, endNodes);
        }
    }

    public static long nextNodePosMask(long nodePosMask) {
        // prevent overflow of segment and path memories masks when a segment has 64 or more nodes or a path has 64 or more segments
        // in this extreme case all the items after the 64th will be all mapped by the same bit and then the linking of one of them
        // will be enough to consider all those item linked
        long nextNodePosMask = nodePosMask << 1;
        return nextNodePosMask > 0 ? nextNodePosMask : nodePosMask;
    }

    private static boolean processQueryNode(QueryElementNode queryNode, LeftTupleNode segmentRoot, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes, long nodePosMask) {
        SegmentPrototype querySmem = null; //getQuerySegmentMemory(reteEvaluator, segmentRoot, queryNode);
        QueryMemoryPrototype queryNodeMem = new QueryMemoryPrototype(nodePosMask, queryNode);
        memories.add(queryNodeMem);
        nodes.add(queryNode);

        return ! queryNode.getQueryElement().isAbductive();
    }

    private static void processAsyncSendNode(AsyncSendNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        AsyncSendMemoryPrototype mem = new AsyncSendMemoryPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processAsyncReceiveNode(AsyncReceiveNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes,
                                                long nodePosMask) {
        AsyncReceiveMemoryPrototype mem = new AsyncReceiveMemoryPrototype(nodePosMask);
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processConditionalBranchNode(ConditionalBranchNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        ConditionalBranchMemoryPrototype mem = new ConditionalBranchMemoryPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processRightInputAdapterNode(RightInputAdapterNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        RightInputAdapterPrototype mem = new RightInputAdapterPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processTerminalNode(TerminalNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        TerminalPrototype mem = new TerminalPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processFromNode(FromNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        FromMemoryPrototype mem = new FromMemoryPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processEvalFromNode(EvalConditionNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes) {
        EvalMemoryPrototype mem = new EvalMemoryPrototype();
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processReactiveFromNode(ReactiveFromNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes,
                                                long nodePosMask) {
        ReactiveFromMemoryPrototype mem = new ReactiveFromMemoryPrototype(nodePosMask);
        memories.add(mem);
        nodes.add(tupleSource);
    }

    private static void processTimerNode(TimerNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes, long nodePosMask) {
        TimerMemoryPrototype tnMem = new TimerMemoryPrototype(nodePosMask);
        memories.add(tnMem);
        nodes.add(tupleSource);
    }

    private static long processLiaNode(LeftInputAdapterNode tupleSource, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes,
                                       long nodePosMask, long allLinkedTestMask) {
        LiaMemoryPrototype liaMemory = new LiaMemoryPrototype(nodePosMask);
        memories.add(liaMemory);
        nodes.add(tupleSource);
        allLinkedTestMask = allLinkedTestMask | nodePosMask;
        return allLinkedTestMask;
    }

    private static long processBetaNode(BetaNode betaNode, SegmentPrototype smem, List<MemoryPrototype> memories, List<NetworkNode> nodes,
                                        long nodePosMask, long allLinkedTestMask, boolean updateNodeBit, RuleBase rbase) {
        RightInputAdapterNode riaNode = null;
        if (betaNode.isRightInputIsRiaNode()) {
            // there is a subnetwork, so create all it's segment memory prototypes
            riaNode = (RightInputAdapterNode) betaNode.getRightInput();

            SegmentPrototype[] smems = createPathMemories(riaNode, riaNode.getStartTupleSource(), rbase);
            setSegments(riaNode, smems);

            if (updateNodeBit && canBeDisabled(betaNode) && riaNode.getPathMemSpec().allLinkedTestMask() > 0) {
                // only ria's with reactive subnetworks can be disabled and thus need checking
                allLinkedTestMask = allLinkedTestMask | nodePosMask;
            }
        } else if (updateNodeBit && canBeDisabled(betaNode)) {
            allLinkedTestMask = allLinkedTestMask | nodePosMask;

        }
        if (NodeTypeEnums.NotNode == betaNode.getType()) {
            // not nodes start up linked in
            smem.linkNode(nodePosMask);
        }

        BetaMemoryPrototype bm = new BetaMemoryPrototype(nodePosMask, riaNode);

        if (NodeTypeEnums.AccumulateNode == betaNode.getType())  {
            AccumulateMemoryPrototype am = new AccumulateMemoryPrototype(bm);
            memories.add(am);
        } else {
            memories.add(bm);
        }

        nodes.add(betaNode);
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
