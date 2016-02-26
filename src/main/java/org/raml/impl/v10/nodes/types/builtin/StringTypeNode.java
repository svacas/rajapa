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
package org.raml.impl.v10.nodes.types.builtin;

import org.raml.nodes.Node;
import org.raml.nodes.NodeType;
import org.raml.nodes.ObjectNode;
import org.raml.nodes.AbstractRamlNode;
import org.raml.utils.NodeSelector;

public class StringTypeNode extends AbstractRamlNode implements ObjectNode
{

    public StringTypeNode()
    {
    }

    private StringTypeNode(StringTypeNode node)
    {
        super(node);
    }

    public int getMinLength()
    {
        return NodeSelector.selectIntValue("minLength", getSource());
    }

    public int getMaxLength()
    {
        return NodeSelector.selectIntValue("maxLength", getSource());
    }

    public String getPattern()
    {
        return NodeSelector.selectStringValue("pattern", getSource());
    }

    @Override
    public Node copy()
    {
        return new StringTypeNode(this);
    }

    @Override
    public NodeType getType()
    {
        return NodeType.Object;
    }
}