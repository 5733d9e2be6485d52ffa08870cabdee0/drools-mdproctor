package org.drools.core.time;

import java.util.Date;

import org.drools.core.base.BaseTuple;
import org.drools.core.base.ValueResolver;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.reteoo.Tuple;
import org.drools.core.rule.Declaration;
import org.drools.util.DateUtils;

public class TimerExpressionUtils {

    public static long evalTimeExpression(TimerExpression expr, BaseTuple tuple, Declaration[] declrs, ValueResolver valueResolver) {
        Object d = expr.getValue( tuple,  declrs, valueResolver );
        if ( d instanceof Number ) {
            return ((Number) d).longValue();
        }
        return TimeUtils.parseTimeString( d.toString() );
    }

    public static Date evalDateExpression(TimerExpression expr, BaseTuple tuple, Declaration[] declrs, ValueResolver valueResolver) {
        if (expr == null) {
            return null;
        }
        Object d = expr.getValue( tuple, declrs, valueResolver );
        if ( d == null ) {
            return null;
        }
        if ( d instanceof Number ) {
            return new Date( ((Number) d).longValue() );
        }
        return DateUtils.parseDate(d.toString());
    }
}
