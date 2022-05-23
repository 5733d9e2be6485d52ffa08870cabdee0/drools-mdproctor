package org.drools.core.time;

import java.util.Date;

import org.drools.core.common.ReteEvaluator;
import org.drools.core.reteoo.Tuple;
import org.drools.core.rule.Declaration;
import org.drools.util.DateUtils;

public class TimerExpressionUtils {

    public static long evalTimeExpression(TimerExpression expr, Tuple leftTuple, Declaration[] declrs, ReteEvaluator reteEvaluator) {
        Object d = expr.getValue( leftTuple,  declrs, reteEvaluator );
        if ( d instanceof Number ) {
            return ((Number) d).longValue();
        }
        return TimeUtils.parseTimeString( d.toString() );
    }

    public static Date evalDateExpression(TimerExpression expr, Tuple leftTuple, Declaration[] declrs, ReteEvaluator reteEvaluator) {
        if (expr == null) {
            return null;
        }
        Object d = expr.getValue( leftTuple, declrs, reteEvaluator );
        if ( d == null ) {
            return null;
        }
        if ( d instanceof Number ) {
            return new Date( ((Number) d).longValue() );
        }
        return DateUtils.parseDate(d.toString());
    }
}
