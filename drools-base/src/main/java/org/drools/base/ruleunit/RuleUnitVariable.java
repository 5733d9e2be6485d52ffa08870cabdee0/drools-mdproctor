package org.drools.base.ruleunit;

public interface RuleUnitVariable {

    boolean isDataSource();

    String getName();

    String getter();

    String setter();

    Class<?> getType();

    Class<?> getDataSourceParameterType();

    Class<?> getBoxedVarType();
}
