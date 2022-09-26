/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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

package org.drools.core.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.drools.core.RuleBaseConfiguration;
import org.drools.core.SessionConfiguration;
import org.drools.core.base.ClassFieldAccessorCache;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.common.RuleBasePartitionId;
import org.drools.core.definitions.InternalKnowledgePackage;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.AsyncReceiveNode;
import org.drools.core.reteoo.EntryPointNode;
import org.drools.core.reteoo.LeftTupleNode;
import org.drools.core.reteoo.LeftTupleSource;
import org.drools.core.reteoo.Rete;
import org.drools.core.reteoo.ReteooBuilder;
import org.drools.core.reteoo.SegmentMemory;
import org.drools.core.reteoo.SegmentMemory.SegmentPrototype;
import org.drools.core.rule.InvalidPatternException;
import org.drools.core.rule.TypeDeclaration;
import org.drools.core.ruleunit.RuleUnitDescriptionRegistry;
import org.drools.core.rule.accessor.FactHandleFactory;
import org.kie.api.builder.ReleaseId;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.process.Process;
import org.kie.api.definition.rule.Query;
import org.kie.api.definition.rule.Rule;
import org.kie.api.definition.type.FactType;
import org.kie.api.io.Resource;

public interface RuleBase {

    Collection<KiePackage> getKiePackages();
    KiePackage getKiePackage( String packageName );
    void removeKiePackage( String packageName );

    Rule getRule( String packageName, String ruleName );
    void removeRule( String packageName, String ruleName );

    Query getQuery( String packageName, String queryName );
    void removeQuery( String packageName, String queryName );

    void removeFunction( String packageName, String functionName );

    FactType getFactType( String packageName, String typeName );

    Process getProcess( String processId );
    Collection<Process> getProcesses();
    void addProcess( Process process );
    void removeProcess( String processId );

    Set<String> getEntryPointIds();

    String getId();

    RuleBasePartitionId createNewPartitionId();

    RuleBaseConfiguration getConfiguration();

    void readLock();
    void readUnlock();

    FactHandleFactory newFactHandleFactory();

    FactHandleFactory newFactHandleFactory(long id, long counter) throws IOException;

    Map<String, Type> getGlobals();

    int getNodeCount();
    int getMemoryCount();

    void executeQueuedActions();

    ReteooBuilder getReteooBuilder();

    void registerAddedEntryNodeCache(EntryPointNode node);
    Set<EntryPointNode> getAddedEntryNodeCache();

    void registeRremovedEntryNodeCache(EntryPointNode node);
    Set<EntryPointNode> getRemovedEntryNodeCache();

    Rete getRete();

    ClassLoader getRootClassLoader();

    Class<?> registerAndLoadTypeDefinition( String className, byte[] def ) throws ClassNotFoundException;

    InternalKnowledgePackage[] getPackages();
    InternalKnowledgePackage getPackage(String name);
    Future<KiePackage> addPackage(KiePackage pkg );
    void addPackages( Collection<? extends KiePackage> newPkgs );
    Map<String, InternalKnowledgePackage> getPackagesMap();

    ClassFieldAccessorCache getClassFieldAccessorCache();

    boolean hasSegmentPrototypes();
    void invalidateSegmentPrototype(LeftTupleNode rootNode);
    SegmentMemory createSegmentFromPrototype(ReteEvaluator reteEvaluator, LeftTupleSource tupleSource);

    SegmentMemory createSegmentFromPrototype(ReteEvaluator reteEvaluator, SegmentPrototype smem);

    SegmentPrototype getSegmentPrototype(SegmentMemory segment);

    SegmentPrototype getSegmentPrototype(LeftTupleSource node);

    void processAllTypesDeclaration( Collection<InternalKnowledgePackage> pkgs );

    void addRules( Collection<RuleImpl> rules ) throws InvalidPatternException;
    void removeRules( Collection<RuleImpl> rules ) throws InvalidPatternException;

    default void beforeIncrementalUpdate(KieBaseUpdate kieBaseUpdate) { }
    default void afterIncrementalUpdate(KieBaseUpdate kieBaseUpdate) { }

    void addGlobal(String identifier, Type type);
    void removeGlobal(String identifier);

    boolean removeObjectsGeneratedFromResource(Resource resource, Collection<InternalWorkingMemory> workingMemories);

    TypeDeclaration getTypeDeclaration(Class<?> clazz );
    TypeDeclaration getExactTypeDeclaration( Class<?> clazz );
    TypeDeclaration getOrCreateExactTypeDeclaration( Class<?> clazz );
    Collection<TypeDeclaration> getTypeDeclarations();
    void registerTypeDeclaration( TypeDeclaration newDecl, InternalKnowledgePackage newPkg );

    ReleaseId getResolvedReleaseId();
    void setResolvedReleaseId(ReleaseId currentReleaseId);
    String getContainerId();
    void setContainerId(String containerId);

    RuleUnitDescriptionRegistry getRuleUnitDescriptionRegistry();
    boolean hasUnits();

    SessionConfiguration getSessionConfiguration();

    List<AsyncReceiveNode> getReceiveNodes();
    void addReceiveNode(AsyncReceiveNode node);

    boolean hasMultipleAgendaGroups();

    default int getWorkingMemoryCounter() {
        return 0;
    }

    void registerSegmentPrototype(LeftTupleSource tupleSource, SegmentPrototype smem);

    void registerSegmentPrototype(LeftTupleSource tupleSource, SegmentMemory smem);
}
