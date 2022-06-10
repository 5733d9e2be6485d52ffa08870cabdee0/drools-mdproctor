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

package org.drools.core.rule;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.PriorityQueue;

import org.drools.base.rule.Behavior;
import org.drools.core.common.EventFactHandle;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.WorkingMemoryAction;
import org.drools.core.marshalling.MarshallerReaderContext;
import org.drools.core.phreak.PropagationEntry;
import org.drools.core.reteoo.ObjectTypeNode;
import org.drools.core.reteoo.WindowNode;
import org.drools.core.reteoo.WindowNode.WindowMemory;
import org.drools.core.common.PropagationContext;
import org.drools.core.time.Job;
import org.drools.core.time.JobContext;
import org.drools.base.time.JobHandle;
import org.drools.core.time.TimerService;
import org.drools.core.time.impl.PointInTimeTrigger;

import static org.drools.core.common.PhreakPropagationContextFactory.createPropagationContextForFact;

public class SlidingTimeWindow
        implements
        Externalizable,
        BehaviorRuntime {

    protected long size;
    // stateless job
    private static final BehaviorJob job = new BehaviorJob();

    protected int nodeId;

    public SlidingTimeWindow() {
        this( 0 );
    }

    public SlidingTimeWindow(final long size) {
        super();
        this.size = size;
    }

    /**
     * @inheritDoc
     *
     * @see java.io.Externalizable#readExternal(java.io.ObjectInput)
     */
    @Override
    public void readExternal(final ObjectInput in) throws IOException,
                                                          ClassNotFoundException {
        this.size = in.readLong();
        this.nodeId = in.readInt();
    }

    /**
     * @inheritDoc
     *
     * @see java.io.Externalizable#writeExternal(java.io.ObjectOutput)
     */
    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        out.writeLong( this.size );
        out.writeInt( this.nodeId );
    }

    @Override
    public Behavior.BehaviorType getType() {
        return Behavior.BehaviorType.TIME_WINDOW;
    }

    public void setWindowNode(WindowNode windowNode) {
        this.nodeId = windowNode.getId();
    }

    /**
     * @return the size
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(final long size) {
        this.size = size;
    }

    @Override
    public BehaviourContext createContext() {
        return new SlidingTimeWindowContext();
    }

    @Override
    public boolean assertFact(final Object context,
                              final InternalFactHandle fact,
                              final ReteEvaluator reteEvaluator) {
        final SlidingTimeWindowContext queue = (SlidingTimeWindowContext) context;
        final EventFactHandle handle = (EventFactHandle) fact;
        long currentTime = reteEvaluator.getTimerService().getCurrentTime();
        if ( isExpired( currentTime, handle ) ) {
            return false;
        }

        queue.add( handle );
        if ( handle.equals( queue.peek() ) ) {
            // update next expiration time
            updateNextExpiration( handle,
                                  reteEvaluator,
                                  queue,
                                  nodeId );
        }

        return true;
    }

    @Override
    public void retractFact(final Object context,
                            final InternalFactHandle fact,
                            final ReteEvaluator reteEvaluator) {
        final SlidingTimeWindowContext queue = (SlidingTimeWindowContext) context;
        final EventFactHandle handle = (EventFactHandle) fact;
        // it may be a call back to expire the tuple that is already being expired
        if ( !handle.equals( queue.getExpiringHandle() ) ) {
            if ( handle.equals( queue.peek() ) ) {
                // it was the head of the queue
                queue.poll();
                // update next expiration time
                updateNextExpiration( queue.peek(),
                                      reteEvaluator,
                                      queue,
                                      nodeId);
            } else {
                queue.remove( handle );
            }
            if ( queue.isEmpty() && queue.getJobHandle() != null ) {
                reteEvaluator.getTimerService().removeJob( queue.getJobHandle() );
            }
        }
    }

    @Override
    public void expireFacts(final Object context,
                            final ReteEvaluator reteEvaluator) {
        TimerService clock = reteEvaluator.getTimerService();
        long currentTime = clock.getCurrentTime();
        SlidingTimeWindowContext queue = (SlidingTimeWindowContext) context;

        EventFactHandle handle = queue.peek();
        while ( handle != null && isExpired( currentTime,
                                             handle ) ) {
            queue.setExpiringHandle( handle );
            queue.remove();
            if( handle.isValid()) {
                // if not expired yet, expire it
                final PropagationContext expiresPctx = createPropagationContextForFact( reteEvaluator, handle, PropagationContext.Type.EXPIRATION );
                ObjectTypeNode.doRetractObject(handle, expiresPctx, reteEvaluator);
            }
            queue.setExpiringHandle( null );
            handle = queue.peek();
        }
        // update next expiration time
        updateNextExpiration( handle,
                              reteEvaluator,
                              queue,
                              nodeId );
    }

    protected boolean isExpired(final long currentTime,
                                final EventFactHandle handle) {
        return handle.getStartTimestamp() + this.size <= currentTime;
    }

    protected void updateNextExpiration(final InternalFactHandle fact,
                                        final ReteEvaluator reteEvaluator,
                                        final BehaviourContext context,
                                        final int nodeId) {
        TimerService clock = reteEvaluator.getTimerService();
        if ( fact != null ) {
            long nextTimestamp = ((EventFactHandle) fact).getStartTimestamp() + getSize();
            if ( nextTimestamp < clock.getCurrentTime() ) {
                // Past and out-of-order events should not be insert,
                // but the engine silently accepts them anyway, resulting in possibly undesirable behaviors
                reteEvaluator.addPropagation(new BehaviorExpireWMAction(nodeId, this, context), true);
            } else {
                // if there exists already another job it meeans that the new one to be created
                // has to be triggered before the existing one and then we can remove the old one
                if ( context.getJobHandle() != null ) {
                    reteEvaluator.getTimerService().removeJob( context.getJobHandle() );
                }

                JobContext jobctx = new BehaviorJobContext( nodeId, reteEvaluator, this, context);
                JobHandle handle = clock.scheduleJob( job,
                                                      jobctx,
                                                      PointInTimeTrigger.createPointInTimeTrigger( nextTimestamp, null ) );
                jobctx.setJobHandle( handle );
            }
        }
    }

    @Override
    public long getExpirationOffset() {
        return this.size;
    }

    @Override
    public String toString() {
        return "SlidingTimeWindow( size=" + size + " )";
    }

    public static class SlidingTimeWindowContext
            implements
            BehaviourContext,
            Externalizable {

        private PriorityQueue<EventFactHandle> queue;
        private EventFactHandle                expiringHandle;
        private JobHandle                      jobHandle;

        public SlidingTimeWindowContext() {
            this.queue = new PriorityQueue<>(16); // arbitrary size... can we improve it?
        }

        @Override
        public JobHandle getJobHandle() {
            return this.jobHandle;
        }

        @Override
        public void setJobHandle(JobHandle jobHandle) {
            this.jobHandle = jobHandle;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void readExternal(ObjectInput in) throws IOException,
                                                        ClassNotFoundException {
            this.queue = (PriorityQueue<EventFactHandle>) in.readObject();
            this.expiringHandle = (EventFactHandle) in.readObject();
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            out.writeObject( this.queue );
            out.writeObject( this.expiringHandle );
        }

        public EventFactHandle getExpiringHandle() {
            return expiringHandle;
        }

        public void setExpiringHandle( EventFactHandle expiringHandle ) {
            this.expiringHandle = expiringHandle;
        }

        public void add(EventFactHandle handle) {
            queue.add( handle );
        }

        public void remove(EventFactHandle handle) {
            queue.remove( handle );
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public EventFactHandle peek() {
            return queue.peek( );
        }

        public EventFactHandle poll() {
            return queue.poll( );
        }

        public EventFactHandle remove() {
            return queue.remove( );
        }

        @Override
        public Collection<EventFactHandle> getFactHandles() {
            return queue;
        }
    }

    public static class BehaviorJobContext
            implements
            JobContext,
            Externalizable {
        public ReteEvaluator         reteEvaluator;
        public int              nodeId;
        public BehaviorRuntime  behavior;
        public BehaviourContext behaviorContext;

        public BehaviorJobContext(int                   nodeId,
                                  ReteEvaluator reteEvaluator,
                                  BehaviorRuntime behavior,
                                  BehaviourContext behaviorContext) {
            super();
            this.nodeId = nodeId;
            this.reteEvaluator = reteEvaluator;
            this.behavior = behavior;
            this.behaviorContext = behaviorContext;
        }

        /**
         * Do not use this constructor! It should be used just by deserialization.
         */
        public BehaviorJobContext() {
        }

        @Override
        public JobHandle getJobHandle() {
            return behaviorContext.getJobHandle();
        }

        @Override
        public void setJobHandle(JobHandle jobHandle) {
            behaviorContext.setJobHandle( jobHandle );
        }

        @Override
        public void readExternal(ObjectInput in) throws IOException,
                                                        ClassNotFoundException {
            //this.behavior = (O)
        }

        @Override
        public void writeExternal(ObjectOutput out) throws IOException {
            // TODO Auto-generated method stub
        }

        @Override
        public ReteEvaluator getReteEvaluator() {
            return reteEvaluator;
        }
    }

    public static class BehaviorJob
            implements
            Job {

        @Override
        public void execute(JobContext ctx) {
            BehaviorJobContext context = (BehaviorJobContext) ctx;
            context.reteEvaluator.addPropagation( new BehaviorExpireWMAction( context.nodeId, context.behavior, context.behaviorContext ), true );
        }

    }

    public static class BehaviorExpireWMAction
            extends PropagationEntry.AbstractPropagationEntry
            implements WorkingMemoryAction {
        protected BehaviorRuntime  behavior;
        protected BehaviourContext context;
        protected int              nodeId;

        protected BehaviorExpireWMAction() { }

        public BehaviorExpireWMAction(final int nodeId,
                                      BehaviorRuntime behavior,
                                      BehaviourContext context) {
            super();
            this.nodeId = nodeId;
            this.behavior = behavior;
            this.context = context;
        }

        public BehaviorExpireWMAction(MarshallerReaderContext inCtx) throws IOException {
            nodeId = inCtx.readInt();
            WindowNode windowNode = (WindowNode) inCtx.getSinks().get( nodeId );

            WindowMemory memory = inCtx.getWorkingMemory().getNodeMemory( windowNode );

            BehaviourContext[] behaviorContext = memory.behaviorContext;

            int i = inCtx.readInt();

            this.behavior = windowNode.getBehaviors()[i];
            this.context = behaviorContext[i];
        }

        @Override
        public void execute(ReteEvaluator reteEvaluator) {
            this.behavior.expireFacts(context, reteEvaluator);
        }
    }
}
