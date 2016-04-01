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

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.raml.nodes.Node;
import org.raml.suggester.Suggestion;

public class FirstOfRule extends AnyOfRule
{

    public FirstOfRule(List<Rule> rules)
    {
        super(rules);
    }

    @Override
    @Nonnull
    public List<Suggestion> getSuggestions(Node node)
    {
        for (Rule rule : rules)
        {
            if (rule.matches(node))
            {
                return rule.getSuggestions(node);
            }
        }
        return Collections.emptyList();
    }

}
