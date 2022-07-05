package org.drools.kiesession.rulebase;

import org.drools.core.impl.DelegatingBaseConfiguration;
import org.drools.core.impl.KnowledgeBaseImpl;
import org.drools.core.impl.RuleBase;
import org.kie.api.KieBaseConfiguration;

public class KnowledgeBaseFactory {

    public static InternalKnowledgeBase newKnowledgeBase() {
        return new SessionsAwareKnowledgeBase();
    }

    public static InternalKnowledgeBase newKnowledgeBase(KieBaseConfiguration kbaseConfiguration) {
        return new SessionsAwareKnowledgeBase(kbaseConfiguration);
    }

    public static InternalKnowledgeBase newKnowledgeBase(String kbaseId, KieBaseConfiguration conf) {
        return newKnowledgeBase(new KnowledgeBaseImpl( kbaseId, (DelegatingBaseConfiguration) conf));
    }

    public static InternalKnowledgeBase newKnowledgeBase(RuleBase delegate) {
        return new SessionsAwareKnowledgeBase(delegate);
    }
}
