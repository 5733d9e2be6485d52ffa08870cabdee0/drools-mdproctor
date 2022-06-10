package org.drools.base.definitions.rule;

import org.drools.base.rule.TypeDeclaration;

public interface RuleBase {
    ClassLoader getRootClassLoader();

    TypeDeclaration getOrCreateExactTypeDeclaration(Class<?> nodeClass);

    TypeDeclaration getTypeDeclaration(Class<?> classType);
}
