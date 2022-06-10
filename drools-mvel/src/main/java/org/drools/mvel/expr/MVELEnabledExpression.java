/*
 * Copyright (c) 2020. Red Hat, Inc. and/or its affiliates.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.mvel.expr;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Map;

import org.drools.base.base.BaseTuple;
import org.drools.base.base.ValueResolver;
import org.drools.base.definitions.InternalKnowledgePackage;
import org.drools.base.definitions.rule.impl.RuleImpl;
import org.drools.core.impl.RuleBase;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.SortDeclarations;
import org.drools.base.rule.accessor.Enabled;
import org.drools.mvel.MVELDialectRuntimeData;
import org.mvel2.integration.VariableResolverFactory;

import static org.drools.mvel.expr.MvelEvaluator.createMvelEvaluator;

public class MVELEnabledExpression
    implements
    Enabled,
    MVELCompileable,
    Externalizable {

    private static final long   serialVersionUID = 510l;

    private MVELCompilationUnit unit;
    private String              id;

    private MvelEvaluator<Boolean> evaluator;

    public MVELEnabledExpression() {
    }

    public MVELEnabledExpression(final MVELCompilationUnit unit,
                                 final String id) {
        this.unit = unit;
        this.id = id;
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
                                            ClassNotFoundException {
        unit = (MVELCompilationUnit) in.readObject();
        id = in.readUTF();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject( unit );
        out.writeUTF( id );
    }

    public MVELCompilationUnit getMVELCompilationUnit() {
        return this.unit;
    }

    @Override
    public void compile( MVELDialectRuntimeData runtimeData) {
        evaluator = createMvelEvaluator( unit.getCompiledExpression( runtimeData ) );
    }

    @Override
    public void compile( MVELDialectRuntimeData runtimeData, RuleImpl rule ) {
        evaluator = createMvelEvaluator( unit.getCompiledExpression( runtimeData, rule.toRuleNameAndPathString() ) );
    }

    @Override
    public boolean getValue(final BaseTuple tuple,
                            final Declaration[] declarations,
                            final RuleImpl rule,
                            final ValueResolver valueResolver) {
        VariableResolverFactory factory = unit.getFactory( null, declarations,
                                                           rule, null, tuple, null, valueResolver, valueResolver.getGlobalResolver()  );

        // do we have any functions for this namespace?
        InternalKnowledgePackage pkg = ((RuleBase) valueResolver.getRuleBase()).getPackage( "MAIN" );
        if ( pkg != null ) {
            MVELDialectRuntimeData data = ( MVELDialectRuntimeData ) pkg.getDialectRuntimeRegistry().getDialectData( this.id );
            factory.setNextFactory( data.getFunctionFactory() );
        }

        return evaluator.evaluate( factory );
    }

    @Override
    public String toString() {
        return this.unit.getExpression();
    }

    @Override
    public Declaration[] findDeclarations( Map<String, Declaration> decls) {
        Declaration[] declrs = unit.getPreviousDeclarations();

        Declaration[] enabledDeclarations = new Declaration[declrs.length];
        int i = 0;
        for ( Declaration declr : declrs ) {
            enabledDeclarations[i++] = decls.get( declr.getIdentifier() );
        }
        Arrays.sort(enabledDeclarations, SortDeclarations.instance);
        return enabledDeclarations;
    }
}
