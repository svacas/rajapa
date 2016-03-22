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
package org.raml.impl.commons.phase;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.raml.grammar.rule.AllOfRule;
import org.raml.grammar.rule.BooleanTypeRule;
import org.raml.grammar.rule.KeyValueRule;
import org.raml.grammar.rule.MaxLengthRule;
import org.raml.grammar.rule.MinLengthRule;
import org.raml.grammar.rule.ObjectRule;
import org.raml.grammar.rule.RegexValueRule;
import org.raml.grammar.rule.Rule;
import org.raml.grammar.rule.StringTypeRule;
import org.raml.grammar.rule.StringValueRule;
import org.raml.impl.commons.nodes.PropertyNode;
import org.raml.impl.v10.nodes.types.builtin.BooleanTypeNode;
import org.raml.impl.v10.nodes.types.builtin.ObjectTypeNode;
import org.raml.impl.v10.nodes.types.builtin.StringTypeNode;
import org.raml.impl.v10.nodes.types.builtin.TypeNode;
import org.raml.impl.v10.nodes.types.builtin.TypeNodeVisitor;


public class TypeToRuleVisitor implements TypeNodeVisitor<Rule>
{


    @Override
    public Rule visitString(StringTypeNode stringTypeNode)
    {
        final AllOfRule typeRule = new AllOfRule(new StringTypeRule());
        if (StringUtils.isNotEmpty(stringTypeNode.getPattern()))
        {
            typeRule.and(new RegexValueRule(Pattern.compile(stringTypeNode.getPattern())));
        }

        if (stringTypeNode.getMaxLength() != null)
        {
            Integer maxLength = stringTypeNode.getMaxLength();
            typeRule.and(new MaxLengthRule(maxLength));
        }

        if (stringTypeNode.getMinLength() != null)
        {
            Integer maxLength = stringTypeNode.getMinLength();
            typeRule.and(new MinLengthRule(maxLength));
        }
        return typeRule;

    }

    @Override
    public Rule visitObject(ObjectTypeNode objectTypeNode)
    {
        ObjectRule objectRule = new ObjectRule();
        List<PropertyNode> properties = objectTypeNode.getProperties();
        for (PropertyNode property : properties)
        {
            TypeNode typeNode = property.getTypeNode();
            KeyValueRule field = new KeyValueRule(new StringValueRule(property.getName()), typeNode.visit(new TypeToRuleVisitor()));
            if (property.isRequired())
            {
                field.required();
            }
            objectRule.with(field);
        }

        return objectRule;
    }

    @Override
    public Rule visitBoolean(BooleanTypeNode booleanTypeNode)
    {
        return new BooleanTypeRule();
    }


}