package org.drools.core.util.index;

import java.util.ArrayList;
import java.util.List;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.reteoo.BetaMemory;
import org.drools.core.reteoo.TupleMemory;
import org.drools.core.rule.ContextEntry;
import org.drools.core.rule.IndexableConstraint;
import org.drools.core.rule.constraint.BetaNodeFieldConstraint;
import org.drools.core.util.FieldIndex;
import org.kie.internal.conf.IndexPrecedenceOption;

public class MemoryFactory {

    public static BetaMemory createBetaMemory(RuleBaseConfiguration config, short nodeType, BetaNodeFieldConstraint... constraints) {
        if (config.getCompositeKeyDepth() < 1) {
            return new BetaMemory(config.isSequential() ? null : new TupleList(),
                                  new TupleList(),
                                  createContext(constraints),
                                  nodeType);
        }

        IndexSpec indexSpec = new IndexSpec(nodeType, constraints, config);
        return new BetaMemory(createLeftMemory(config, indexSpec),
                              createRightMemory(config, indexSpec),
                              createContext(constraints),
                              nodeType);
    }

    private static TupleMemory createRightMemory(RuleBaseConfiguration config, IndexSpec indexSpec) {
        if (!config.isIndexRightBetaMemory() || !indexSpec.constraintType.isIndexable()) {
            return new TupleList();
        }

        if (indexSpec.constraintType == ConstraintOperatorType.EQUAL) {
            return new TupleIndexHashTable(indexSpec.indexes, false);
        }

        if (indexSpec.constraintType.isComparison()) {
            return new TupleIndexRBTree(indexSpec.constraintType, indexSpec.indexes[0], false);
        }

        return new TupleList();
    }

    private static TupleMemory createLeftMemory(RuleBaseConfiguration config, IndexSpec indexSpec) {
        if (config.isSequential()) {
            return null;
        }
        if (!config.isIndexLeftBetaMemory() || !indexSpec.constraintType.isIndexable()) {
            return new TupleList();
        }

        if (indexSpec.constraintType == ConstraintOperatorType.EQUAL) {
            return new TupleIndexHashTable(indexSpec.indexes, true);
        }

        if (indexSpec.constraintType.isComparison()) {
            return new TupleIndexRBTree(indexSpec.constraintType, indexSpec.indexes[0], true);
        }

        return new TupleList();
    }

    public static ContextEntry[] createContext(BetaNodeFieldConstraint... constraints) {
        ContextEntry[] entries = new ContextEntry[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            entries[i] = constraints[i].createContextEntry();
        }
        return entries;
    }

    private static class IndexSpec {

        private ConstraintOperatorType constraintType = ConstraintOperatorType.UNKNOWN;
        private FieldIndex[]           indexes;

        private IndexSpec(short nodeType, BetaNodeFieldConstraint[] constraints, RuleBaseConfiguration config) {
            init(nodeType, constraints, config);
        }

        private void init(short nodeType, BetaNodeFieldConstraint[] constraints, RuleBaseConfiguration config) {
            int                   keyDepth              = config.getCompositeKeyDepth();
            IndexPrecedenceOption indexPrecedenceOption = config.getIndexPrecedenceOption();
            int firstIndexableConstraint = indexPrecedenceOption == IndexPrecedenceOption.EQUALITY_PRIORITY ?
                                           determineTypeWithEqualityPriority(nodeType, constraints, config) :
                                           determineTypeWithPatternOrder(nodeType, constraints, config);

            if (constraintType == ConstraintOperatorType.EQUAL) {
                List<FieldIndex> indexList = new ArrayList<>();
                indexList.add(((IndexableConstraint) constraints[firstIndexableConstraint]).getFieldIndex());

                // look for other EQUAL constraint to eventually add them to the index
                for (int i = firstIndexableConstraint + 1; i < constraints.length && indexList.size() < keyDepth; i++) {
                    if (ConstraintOperatorType.getType(constraints[i]) == ConstraintOperatorType.EQUAL && !((IndexableConstraint) constraints[i]).isUnification()) {
                        indexList.add(((IndexableConstraint) constraints[i]).getFieldIndex());
                    }
                }
                indexes = indexList.toArray(new FieldIndex[indexList.size()]);
            } else if (constraintType.isComparison()) {
                // look for a dual constraint to create a range index
                indexes = new FieldIndex[]{((IndexableConstraint) constraints[firstIndexableConstraint]).getFieldIndex()};
            }
        }

        private int determineTypeWithEqualityPriority(short nodeType, BetaNodeFieldConstraint[] constraints, RuleBaseConfiguration config) {
            int indexedConstraintPos = 0;
            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i] instanceof IndexableConstraint) {
                    IndexableConstraint    indexableConstraint = (IndexableConstraint) constraints[i];
                    ConstraintOperatorType type                = indexableConstraint.getConstraintType();
                    if (type == ConstraintOperatorType.EQUAL) {
                        constraintType = type;
                        return i;
                    } else if (constraintType == ConstraintOperatorType.UNKNOWN && type.isIndexableForNode(nodeType, indexableConstraint, config)) {
                        constraintType = type;
                        indexedConstraintPos = i;
                    }
                }
            }
            return indexedConstraintPos;
        }

        private int determineTypeWithPatternOrder(short nodeType, BetaNodeFieldConstraint[] constraints, RuleBaseConfiguration config) {
            for (int i = 0; i < constraints.length; i++) {
                ConstraintOperatorType type = ConstraintOperatorType.getType(constraints[i]);
                if (type.isIndexableForNode(nodeType, (IndexableConstraint) constraints[i], config)) {
                    constraintType = type;
                    return i;
                }
            }
            return constraints.length;
        }
    }
}
