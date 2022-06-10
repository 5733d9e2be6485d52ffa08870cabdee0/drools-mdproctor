/*
 * Copyright 2010 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.core.time.impl;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.Map;

import org.drools.base.base.BaseTuple;
import org.drools.base.base.ValueResolver;
import org.drools.base.time.impl.Timer;
import org.drools.base.rule.ConditionalElement;
import org.drools.base.rule.Declaration;
import org.drools.base.time.JobHandle;
import org.drools.core.time.TimerExpression;
import org.drools.base.time.Trigger;
import org.kie.api.runtime.Calendars;

import static org.drools.core.time.TimerExpressionUtils.evalDateExpression;
import static org.drools.core.time.TimerExpressionUtils.evalTimeExpression;

public class ExpressionIntervalTimer  extends BaseTimer
    implements
        Timer,
    Externalizable {

    private TimerExpression startTime;
    private TimerExpression endTime;

    private int  repeatLimit;

    private TimerExpression delay;
    private TimerExpression period;

    public ExpressionIntervalTimer() {

    }



    public ExpressionIntervalTimer(TimerExpression startTime,
                                   TimerExpression endTime,
                                   int repeatLimit,
                                   TimerExpression delay,
                                   TimerExpression period) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatLimit = repeatLimit;
        this.delay = delay;
        this.period = period;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( startTime );
        out.writeObject( endTime );
        out.writeInt( repeatLimit );
        out.writeObject( delay );
        out.writeObject( period );
    }

    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        this.startTime = (TimerExpression) in.readObject();
        this.endTime = (TimerExpression) in.readObject();
        this.repeatLimit = in.readInt();
        this.delay = (TimerExpression) in.readObject();
        this.period = (TimerExpression) in.readObject();
    }
    
    public Declaration[] getStartDeclarations() {
        return this.startTime != null ? this.startTime.getDeclarations() : null;
    }  
    
    public Declaration[] getEndDeclarations() {
        return this.endTime != null ? this.endTime.getDeclarations() : null;
    }

    public Declaration[] getDelayDeclarations() {
        return this.delay.getDeclarations();
    }

    public Declaration[] getPeriodDeclarations() {
        return this.period.getDeclarations();
    }

    public Declaration[][] getTimerDeclarations(Map<String, Declaration> outerDeclrs) {
        return new Declaration[][] { sortDeclarations(outerDeclrs, getDelayDeclarations()),
                                     sortDeclarations(outerDeclrs, getPeriodDeclarations()),
                                     sortDeclarations(outerDeclrs, getStartDeclarations()),
                                     sortDeclarations(outerDeclrs, getEndDeclarations()) };
    }

    public Trigger createTrigger(long timestamp,
                                 BaseTuple tuple,
                                 JobHandle jh,
                                 String[] calendarNames,
                                 Calendars calendars,
                                 Declaration[][] declrs,
                                 ValueResolver valueResolver) {
        long timeSinceLastFire = 0;

        Declaration[] delayDeclarations = declrs[0];
        Declaration[] periodDeclarations = declrs[1];
        Declaration[] startDeclarations = declrs[2];
        Declaration[] endDeclarations = declrs[3];

        Date lastFireTime = null;
        Date createdTime = null;
        long newDelay = 0;

        if ( jh != null ) {
            IntervalTrigger preTrig = (IntervalTrigger) ((DefaultJobHandle)jh).getTimerJobInstance().getTrigger();
            lastFireTime = preTrig.getLastFireTime();
            createdTime = preTrig.getCreatedTime();
            if (lastFireTime != null) {
                // it is already fired calculate the new delay using the period instead of the delay
                newDelay = evalTimeExpression(this.period, tuple, delayDeclarations, valueResolver) - timestamp + lastFireTime.getTime();
            } else {
                newDelay = evalTimeExpression(this.delay, tuple, delayDeclarations, valueResolver) - timestamp + createdTime.getTime();
            }
        } else {
            newDelay = evalTimeExpression(this.delay, tuple, delayDeclarations, valueResolver);
        }

        if (newDelay < 0) {
            newDelay = 0;
        }

        return new IntervalTrigger(timestamp,
                                   evalDateExpression( this.startTime, tuple, startDeclarations, valueResolver ),
                                   evalDateExpression( this.endTime, tuple, startDeclarations, valueResolver ),
                                   this.repeatLimit,
                                   newDelay,
                                   period != null ? evalTimeExpression(this.period, tuple, periodDeclarations, valueResolver) : 0,
                                   calendarNames,
                                   calendars,
                                   createdTime,
                                   lastFireTime);
    }

    public Trigger createTrigger(long timestamp,
                                 String[] calendarNames,
                                 Calendars calendars) {
        return new IntervalTrigger( timestamp,
                                    null, // this.startTime,
                                    null, // this.endTime,
                                    this.repeatLimit,
                                    0,
                                    0,
                                    calendarNames,
                                    calendars );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + delay.hashCode();
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result + period.hashCode();
        result = prime * result + repeatLimit;
        result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        ExpressionIntervalTimer other = (ExpressionIntervalTimer) obj;
        if ( delay != other.delay ) return false;
        if ( repeatLimit != other.repeatLimit ) return false;
        if ( endTime == null ) {
            if ( other.endTime != null ) return false;
        } else if ( !endTime.equals( other.endTime ) ) return false;
        if ( period != other.period ) return false;
        if ( startTime == null ) {
            if ( other.startTime != null ) return false;
        } else if ( !startTime.equals( other.startTime ) ) return false;
        return true;
    }

    @Override
    public ConditionalElement clone() {
        return new ExpressionIntervalTimer(startTime,
                                           endTime,
                                           repeatLimit,
                                           delay,
                                           period);
    }
}
