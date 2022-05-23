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

package org.drools.core.util.index;

import org.drools.core.base.ValueType;
import org.drools.core.reteoo.NodeTypeEnums;
import org.drools.core.rule.IndexableConstraint;
import org.drools.core.rule.constraint.BetaNodeFieldConstraint;
import org.drools.core.rule.accessor.ReadAccessor;
import org.drools.core.rule.accessor.TupleValueExtractor;
import org.drools.core.rule.constraint.Constraint;
import org.kie.internal.conf.IndexPrecedenceOption;

public class IndexUtil {

    // package private for test convenience
    static boolean USE_COMPARISON_INDEX = true;
    static boolean USE_COMPARISON_INDEX_JOIN = true;

    public static boolean compositeAllowed(BetaNodeFieldConstraint[] constraints, short betaNodeType, IndexConfiguration config) {
        // 1) If there is 1 or more unification restrictions it cannot be composite
        // 2) Ensures any non unification restrictions are first
        int firstUnification = -1;
        int firstNonUnification = -1;
        for ( int i = 0, length = constraints.length; i < length; i++ ) {
            if ( isIndexable(constraints[i], betaNodeType, config) ) {
                final boolean isUnification = ((IndexableConstraint) constraints[i]).isUnification();
                if ( isUnification && firstUnification == -1 ) {
                    firstUnification = i;
                } else if ( !isUnification &&firstNonUnification == -1 ) {
                    firstNonUnification = i;
                }
            }
            if ( firstUnification != -1 && firstNonUnification != -1) {
                break;
            }
        }

        if ( firstNonUnification > 0 ) {
            // Make sure a nonunification indexable constraint is first
            swap(constraints, 0, firstNonUnification);
        }

        return (firstUnification == -1);
    }

    public static boolean isIndexable(BetaNodeFieldConstraint constraint, short nodeType, IndexConfiguration config) {
        return constraint instanceof IndexableConstraint && ((IndexableConstraint)constraint).isIndexable(nodeType, config);
    }

    public static boolean canHaveRangeIndex(short nodeType, IndexableConstraint constraint, IndexConfiguration config) {
        return canHaveRangeIndexForNodeType(nodeType, config) && areRangeIndexCompatibleOperands(constraint);
    }

    private static boolean canHaveRangeIndexForNodeType(short nodeType, IndexConfiguration config) {
        if (USE_COMPARISON_INDEX_JOIN && config.isBetaNodeRangeIndexEnabled()) {
            return USE_COMPARISON_INDEX && (nodeType == NodeTypeEnums.NotNode || nodeType == NodeTypeEnums.ExistsNode || nodeType == NodeTypeEnums.JoinNode);
        } else {
            return USE_COMPARISON_INDEX && (nodeType == NodeTypeEnums.NotNode || nodeType == NodeTypeEnums.ExistsNode);
        }
    }

    private static boolean areRangeIndexCompatibleOperands(IndexableConstraint constraint) {
        ReadAccessor fieldExtractor = null;
        TupleValueExtractor indexingDeclaration = null;
        try {
            fieldExtractor = constraint.getFieldExtractor();
            indexingDeclaration = constraint.getIndexExtractor();
        } catch (UnsupportedOperationException uoe) {
            return false;
        }
        if (fieldExtractor == null || indexingDeclaration == null) {
            return false;
        }

        ValueType leftValueType = fieldExtractor.getValueType();
        ValueType rightValueType = indexingDeclaration.getValueType();

        if (leftValueType != null && rightValueType != null) {
            if (leftValueType.isNumber() && rightValueType.isNumber()) {
                return true; // Number vs Number
            }
            Class<?> leftClass = leftValueType.getClassType();
            Class<?> rightClass = rightValueType.getClassType();
            if (leftClass != null && rightClass != null && Comparable.class.isAssignableFrom(leftClass) && leftClass.equals(rightClass)) {
                return true; // Same Comparable class
            }
        }
        return false;
    }

    public static boolean isIndexableForNode(short nodeType, BetaNodeFieldConstraint constraint, IndexConfiguration config) {
        if ( !(constraint instanceof IndexableConstraint) ) {
            return false;
        }

        ConstraintOperatorType constraintType = ((IndexableConstraint)constraint).getConstraintType();
        return constraintType.isIndexableForNode(nodeType, (IndexableConstraint)constraint, config);
    }

    public static boolean[] isIndexableForNode(IndexPrecedenceOption indexPrecedenceOption, short nodeType, int keyDepth, BetaNodeFieldConstraint[] constraints, IndexConfiguration config) {
        if (keyDepth < 1) {
            return new boolean[constraints.length];
        }

        return indexPrecedenceOption == IndexPrecedenceOption.EQUALITY_PRIORITY ?
                findIndexableWithEqualityPriority(nodeType, keyDepth, constraints, config) :
                findIndexableWithPatternOrder(nodeType, keyDepth, constraints, config);
    }

    private static boolean[] findIndexableWithEqualityPriority(short nodeType, int keyDepth, BetaNodeFieldConstraint[] constraints, IndexConfiguration config) {
        boolean[] indexable = new boolean[constraints.length];
        if (hasEqualIndexable(keyDepth, indexable, constraints)) {
            return indexable;
        }

        if (!canHaveRangeIndexForNodeType(nodeType, config)) {
            return indexable;
        }

        for (int i = 0; i < constraints.length; i++) {
            if (isIndexable(constraints[i], nodeType, config)) {
                sortRangeIndexable(constraints, indexable, i);
                break;
            }
        }

        return indexable;
    }

    private static boolean[] findIndexableWithPatternOrder(short nodeType, int keyDepth, BetaNodeFieldConstraint[] constraints, IndexConfiguration config) {
        boolean[] indexable = new boolean[constraints.length];
        for (int i = 0; i < constraints.length; i++) {
            if (isIndexable(constraints[i], nodeType, config)) {
                if (isEqualIndexable(constraints[i])) {
                    sortEqualIndexable(keyDepth, indexable, constraints, i);
                } else {
                    sortRangeIndexable(constraints, indexable, i);
                }
                break;
            }
        }

        return indexable;
    }

    private static boolean hasEqualIndexable(int keyDepth, boolean[] indexable, BetaNodeFieldConstraint[] constraints) {
        return sortEqualIndexable(keyDepth, indexable, constraints, 0);
    }

    private static boolean sortEqualIndexable(int keyDepth, boolean[] indexable, BetaNodeFieldConstraint[] constraints, int start) {
        boolean hasEqualIndexable = false;
        int indexableCouter = 0;
        for (int i = start; i < constraints.length; i++) {
            if (isEqualIndexable(constraints[i])) {
                hasEqualIndexable = true;
                if (keyDepth > indexableCouter) {
                    swap(constraints, i, indexableCouter);
                    indexable[indexableCouter++] = true;
                }
            }
        }
        return hasEqualIndexable;
    }

    private static void sortRangeIndexable(BetaNodeFieldConstraint[] constraints, boolean[] indexable, int i) {
        swap(constraints, i, 0);
        indexable[0] = true;
    }

    private static boolean isEqualIndexable(BetaNodeFieldConstraint constraint) {
        return constraint instanceof IndexableConstraint && ((IndexableConstraint)constraint).getConstraintType() == ConstraintOperatorType.EQUAL;
    }

    private static void swap(BetaNodeFieldConstraint[] constraints, int p1, int p2) {
        if (p1 != p2) {
            BetaNodeFieldConstraint temp = constraints[p2];
            constraints[p2] = constraints[p1];
            constraints[p1] = temp;
        }
    }
}
