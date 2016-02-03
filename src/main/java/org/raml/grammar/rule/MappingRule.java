/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.raml.grammar.rule;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.raml.nodes.KeyValueNode;
import org.raml.nodes.Node;
import org.raml.nodes.ObjectNode;
import org.raml.suggester.Suggestion;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MappingRule extends Rule
{
    private List<KeyValueRule> fields;
    private ConditionalRules conditionalRules;

    public MappingRule()
    {
        this.fields = new ArrayList<>();
    }

    @Nonnull
    @Override
    public List<Suggestion> getSuggestions(Node node)
    {
        List<Suggestion> result = new ArrayList<>();
        final List<KeyValueRule> fieldRules = getAllFieldRules(node);
        for (KeyValueRule rule : fieldRules)
        {
            if (rule.repeated() || !matchesAny(rule, node.getChildren()))
            {
                result.addAll(rule.getSuggestions(node));
            }
        }
        return result;
    }

    private boolean matchesAny(KeyValueRule rule, List<Node> children)
    {
        for (Node child : children)
        {
            if (rule.matches(child))
            {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Rule getInnerRule(Node node)
    {
        final List<KeyValueRule> allFields = getAllFieldRules(node);
        for (KeyValueRule field : allFields)
        {
            if (field.matches(node))
            {
                return field;
            }
        }
        return null;
    }


    @Override
    public boolean matches(@Nonnull Node node)
    {
        return node instanceof ObjectNode;
    }

    @Override
    public Node transform(@Nonnull Node node)
    {
        Node result = node;
        if (getFactory() != null)
        {
            result = getFactory().create();
        }
        final List<Node> children = node.getChildren();
        for (Node child : children)
        {
            final Rule matchingRule = findMatchingRule(getAllFieldRules(node), child);
            if (matchingRule != null)
            {
                final Node newChild = matchingRule.transform(child);
                child.replaceWith(newChild);
            }
            else
            {
                final Collection<String> options = Collections2.transform(getAllFieldRules(node), new Function<KeyValueRule, String>()
                {
                    @Override
                    public String apply(KeyValueRule rule)
                    {
                        return rule.getKeyRule().getDescription();
                    }
                });
                child.replaceWith(ErrorNodeFactory.createUnexpectedKey(((KeyValueNode) child).getKey(), options));
            }
        }

        return result;
    }

    private List<KeyValueRule> getAllFieldRules(Node node)
    {
        if (conditionalRules != null)
        {
            final List<KeyValueRule> rulesNode = conditionalRules.getRulesNode(node);
            final ArrayList<KeyValueRule> rules = new ArrayList<>(rulesNode);
            rules.addAll(fields);
            return rules;
        }
        else
        {
            return fields;
        }
    }

    @Nullable
    private Rule findMatchingRule(List<? extends Rule> rootRule, Node node)
    {
        for (Rule rule : rootRule)
        {
            if (rule.matches(node))
            {
                return rule;
            }
        }
        return null;
    }


    public MappingRule with(KeyValueRule field)
    {
        this.fields.add(field);
        return this;
    }

    @Override
    public String getDescription()
    {
        return "Mapping";
    }

    public MappingRule with(ConditionalRules conditional)
    {
        this.conditionalRules = conditional;
        return this;
    }
}
