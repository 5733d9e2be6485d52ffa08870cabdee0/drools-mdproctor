package org.drools.core.rule;

import java.util.Comparator;

public class SortDeclarations
      implements
      Comparator<Declaration> {

    public final static SortDeclarations instance = new SortDeclarations();

    public int compare(Declaration d1,
                       Declaration d2) {
        return (d1.getIdentifier().compareTo(d2.getIdentifier()));
    }
}
