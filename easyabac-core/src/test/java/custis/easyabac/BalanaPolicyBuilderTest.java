package custis.easyabac;

import custis.easyabac.core.init.BalanaPolicyBuilder;
import custis.easyabac.core.model.abac.*;
import custis.easyabac.core.model.abac.attribute.Attribute;
import custis.easyabac.core.model.abac.attribute.Category;
import custis.easyabac.core.model.abac.attribute.DataType;
import org.junit.Test;
import org.wso2.balana.AbstractPolicy;
import org.wso2.balana.TargetMatch;
import org.wso2.balana.attr.xacml3.AttributeDesignator;
import org.wso2.balana.cond.Apply;
import org.wso2.balana.cond.Evaluatable;
import org.wso2.balana.ctx.xacml3.Result;
import org.wso2.balana.xacml3.AllOfSelection;
import org.wso2.balana.xacml3.AnyOfSelection;
import org.wso2.balana.xacml3.ObligationExpression;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;

/**
 * Test suite for conversion from EasyAbac domain model to Balana domain model
 */
public class BalanaPolicyBuilderTest {
    
    @Test
    public void buildPolicyId_fromPolicyKey() throws URISyntaxException {
        AbstractPolicy policy = new SamplePolicyBuilder().build();
        assertEquals("Policy ID",
                new URI("urn:oasis:names:tc:xacml:3.0:easy-policy:policy1"),
                policy.getId());
    }

    @Test
    public void buildPolicyDescription_whenTitleProvided() {
        AbstractPolicy policy = new SamplePolicyBuilder().build();
        assertEquals("Policy description", policy.getDescription(), "Sample policy");
    }

    @Test
    public void buildPolicyTarget_whenSpecifiedInEasyPolicy() {
        org.wso2.balana.Policy policy = new SamplePolicyBuilder().build();

        final org.wso2.balana.xacml3.Target target = (org.wso2.balana.xacml3.Target) policy.getTarget();
        assertNotNull("Policy target", target);

        assertEquals("Policy target anyOf size", 1, target.getAnyOfSelections().size());
        final AnyOfSelection anyOfSelection = target.getAnyOfSelections().iterator().next();
        final List<AllOfSelection> allOfSelections = anyOfSelection.getAllOfSelections();
        assertEquals("Policy target allOf size", 2, allOfSelections.size());

        AllOfSelection actionSelection = allOfSelections.get(0);
        assertEquals("Match qty", 1, actionSelection.getMatches().size());

        TargetMatch targetMatch = actionSelection.getMatches().get(0);
        assertEquals("Match function",
                "urn:oasis:names:tc:xacml:1.0:function:string-equal",
                targetMatch.getMatchFunction().getIdentifier().toString());

        assertEquals("Match value", "CourseUnit.Edit", targetMatch.getMatchValue().encode());
        Evaluatable matchEvaluatable = targetMatch.getMatchEvaluatable();
        assertTrue("Match evaluatable is AttributeDesignator", matchEvaluatable instanceof AttributeDesignator);
        AttributeDesignator ad = (AttributeDesignator) matchEvaluatable;
        assertEquals("Match attribute category",
                "urn:oasis:names:tc:xacml:3.0:attribute-category:action",
                ad.getCategory().toString());
    }

    @Test
    public void buildRules_whenGiven() {
        org.wso2.balana.Policy policy = new SamplePolicyBuilder().build();

        final List<org.wso2.balana.Rule> balanaRules = extractRules(policy);
        assertEquals("Rules count", 2, balanaRules.size());
    }

    @Test
    public void buildRuleConditionAsSingleApply_whenGiven() {
        org.wso2.balana.Policy policy = new SamplePolicyBuilder().build();
        final List<org.wso2.balana.Rule> balanaRules = extractRules(policy);

        org.wso2.balana.Rule rule1 = balanaRules.get(0);
        final org.wso2.balana.cond.Condition condition = rule1.getCondition();
        assertNotNull("Rule 1 condition", condition);

        assertEquals("Rule 1 condition children qty", 1, condition.getChildren().size());
        final Object condChild = condition.getChildren().iterator().next();
        assertTrue("Single condition child is Apply", condChild instanceof Apply);

        Apply andApply = (Apply) condChild;
        assertEquals("Condition apply function",
                URI.create("urn:oasis:names:tc:xacml:1.0:function:and"),
                andApply.getFunction().getIdentifier());

        assertEquals("Apply's children qty", 2, andApply.getChildren().size());
    }

    @Test
    public void buildObligations_whenGiven() {

        org.wso2.balana.Policy policy = new SamplePolicyBuilder()
                .attributeToReturn(new Attribute("attrRet1", DataType.STRING, Category.RESOURCE, false))
                .build();

        Set obligationExpressions = policy.getObligationExpressions();
        assertNotNull("Obligations", obligationExpressions);
        assertFalse("Obligations not found", obligationExpressions.isEmpty());

        ObligationExpression o = (ObligationExpression) obligationExpressions.iterator().next();
        assertEquals("Fulfill on", Result.DECISION_PERMIT, o.getFulfillOn());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCreateDateTimeAttributes_whenTimeInRuleConditionIsGiven() {
        Rule timedRule = new Rule("rule_time", "Rule with time", Operation.AND, singletonList(
                new Condition("rtc1", false,
                        new Attribute("env.current_time", DataType.TIME, Category.ENV, false),
                        singletonList("08:30"),
                        Function.LESS_OR_EQUAL)));

        org.wso2.balana.Policy policy = new SamplePolicyBuilder()
                .rules(singletonList(timedRule))
                .build();

        final List<org.wso2.balana.Rule> balanaRules = extractRules(policy);
        org.wso2.balana.Rule balanaRule = balanaRules.get(0);

        final Object condChild = balanaRule.getCondition().getChildren().iterator().next();
        assertTrue("Single condition child is Apply", condChild instanceof Apply);

        Apply andApply = (Apply) condChild;
        assertEquals("Condition apply function",
                URI.create("urn:oasis:names:tc:xacml:1.0:function:and"),
                andApply.getFunction().getIdentifier());
        assertTrue("AND Apply has time comparision function", andApply.getChildren().stream()
                .anyMatch(c -> {
                    if (!(c instanceof Apply)) {
                        return false;
                    }

                    Apply a = (Apply) c;
                    return a.getFunction().getIdentifier().equals(URI.create("urn:oasis:names:tc:xacml:1.0:function:time-less-than-or-equal"));
                }));
    }

    private List<org.wso2.balana.Rule> extractRules(org.wso2.balana.Policy policy) {
        return policy.getChildren().stream()
                .filter(e -> e instanceof org.wso2.balana.Rule)
                .map(e -> (org.wso2.balana.Rule) e)
                .collect(Collectors.toList());
    }

    private static class SamplePolicyBuilder {
        private Target target;
        private List<Rule> rules;
        private List<Attribute> attributesToReturn;

        SamplePolicyBuilder() {
            this.target = defaultTarget();
            this.rules = asList(rule1(), rule2());
            this.attributesToReturn = emptyList();
        }

        SamplePolicyBuilder attributeToReturn(Attribute a) {
            this.attributesToReturn = singletonList(a);
            return this;
        }

        SamplePolicyBuilder rules(List<Rule> rules) {
            this.rules = rules;
            return this;
        }

        org.wso2.balana.Policy build() {
            Policy abacPolicy = new Policy("policy1", "Sample policy", this.target, this.rules, this.attributesToReturn);
            AbacAuthModel authModel = new AbacAuthModel(singletonList(abacPolicy), Collections.emptyMap(), Collections.emptyMap());
            Map<URI, org.wso2.balana.Policy> policies = new BalanaPolicyBuilder().buildFrom(authModel);

            return policies.values().iterator().next();
        }

        private Rule rule2() {
            return new Rule("rule2", "Second rule", Operation.NAND, asList(
                    new Condition("r2c1", false,
                            new Attribute("CourseUnit.curatorId", DataType.STRING, Category.RESOURCE, false),
                            new Attribute("user.personId", DataType.STRING, Category.SUBJECT, false),
                            Function.EQUAL),
                    new Condition("r2c2", false,
                            new Attribute("CourseUnit.count", DataType.INT, Category.RESOURCE, false),
                            singletonList("15"),
                            Function.GREATER),
                    new Condition("r2c3", false,
                            new Attribute("CourseUnit.status", DataType.STRING, Category.RESOURCE, false),
                            asList("DRAFT", "TEST"),
                            Function.IN)));
        }

        private Rule rule1() {
            return new Rule("rule1", "First rule", Operation.AND, asList(
                    new Condition("r1c1", false,
                            new Attribute("CourseUnit.authorId", DataType.STRING, Category.RESOURCE, false),
                            new Attribute("user.personId", DataType.STRING, Category.SUBJECT, false),
                            Function.EQUAL),
                    new Condition("r1c2", true,
                            new Attribute("CourseUnit.status", DataType.STRING, Category.RESOURCE, false),
                            singletonList("DRAFT"),
                            Function.EQUAL)));
        }

        private Target defaultTarget() {
            return new Target(Operation.OR, asList(
                    new TargetCondition(Attribute.ACTION_ID, "CourseUnit.Edit", Function.EQUAL),
                    new TargetCondition(Attribute.ACTION_ID, "CourseUnit.Delete", Function.EQUAL)),
                    emptyList());
        }
    }
}