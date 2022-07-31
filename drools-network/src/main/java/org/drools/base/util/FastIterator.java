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

package org.drools.base.util;

public interface FastIterator {
    public class NullFastIterator implements FastIterator {
        public static final NullFastIterator INSTANCE = new NullFastIterator();

        @Override public Entry next(Entry object) {
            return null;
        }

        @Override public boolean isFullIterator() {
            return true;
        }
    }

    Entry next(Entry object);
    
    boolean isFullIterator();

    public static FastIterator EMPTY = new FastIterator() {
        public Entry next(Entry object) {
            return null;
        }
        public boolean isFullIterator() {
            return false;
        }
    };

    public static class IteratorAdapter implements Iterator {
        private final FastIterator fastIterator;
        private Entry current = null;
        private boolean firstConsumed = false;

        public IteratorAdapter(FastIterator fastIterator, Entry first) {
            this.fastIterator = fastIterator;
            current = first;
        }

        public Object next() {
            if (!firstConsumed) {
                firstConsumed = true;
                return current;
            }
            current = fastIterator.next(current);
            return current;
        }
    }
}
