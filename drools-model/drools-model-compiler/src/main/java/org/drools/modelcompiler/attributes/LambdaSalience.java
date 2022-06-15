/*
 * Copyright 2005 JBoss Inc
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

package org.drools.modelcompiler.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.drools.base.base.BaseTuple;
import org.drools.base.base.ValueResolver;
import org.drools.base.rule.Declaration;
import org.drools.base.rule.accessor.Salience;
import org.drools.core.reteoo.Tuple;
import org.drools.model.DynamicValueSupplier;
import org.drools.model.Variable;
import org.kie.api.definition.rule.Rule;

import static org.drools.modelcompiler.consequence.LambdaConsequence.declarationsToFacts;

public class LambdaSalience extends DynamicAttributeEvaluator<Integer> implements Salience {

    public LambdaSalience( DynamicValueSupplier<Integer> supplier ) {
        super(supplier);
    }

    @Override
    public int getValue(BaseTuple tuple, Declaration[] declarations, Rule rule, ValueResolver valueResolver) {
        Object[] facts = declarationsToFacts( valueResolver, (Tuple) tuple, declarations, supplier.getVariables() );
        return supplier.supply( facts );
    }

    @Override
    public Declaration[] findDeclarations( Map<String, Declaration> decls) {
        List<Declaration> salienceDeclarations = new ArrayList<>();
        for (Variable salienceVar : supplier.getVariables()) {
            salienceDeclarations.add( decls.get( salienceVar.getName() ) );
        }
        return salienceDeclarations.toArray(new Declaration[salienceDeclarations.size()]);
    }

    @Override
    public int getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return false;
    }
}
