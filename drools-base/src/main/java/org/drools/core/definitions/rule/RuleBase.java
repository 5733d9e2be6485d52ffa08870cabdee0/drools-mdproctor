package org.drools.core.definitions.rule;

import org.drools.core.rule.TypeDeclaration;

public interface RuleBase {
    ClassLoader getRootClassLoader();

    TypeDeclaration getOrCreateExactTypeDeclaration(Class<?> nodeClass);

    TypeDeclaration getTypeDeclaration(Class<?> classType);
}
