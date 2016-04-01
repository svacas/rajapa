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
package org.raml.impl.commons.nodes;

import org.raml.nodes.*;
import org.raml.nodes.StringNodeImpl;

import java.util.List;

import javax.annotation.Nonnull;

public class StringTemplateNode extends StringNodeImpl implements ExecutableNode
{

    public StringTemplateNode(String value)
    {
        super(value);
    }

    public StringTemplateNode(StringTemplateNode node)
    {
        super(node);
    }

    @Override
    public void addChild(Node node)
    {
        if (!(node instanceof StringNode))
        {
            throw new IllegalArgumentException("Only String nodes are valid as children");
        }
        super.addChild(node);
    }

    public Node execute(ExecutionContext context)
    {
        final List<Node> children = getChildren();
        StringBuilder content = new StringBuilder();
        for (Node child : children)
        {
            if (child instanceof ExecutableNode)
            {
                final Node result = ((ExecutableNode) child).execute(context);
                if (result instanceof ErrorNode)
                {
                    return result;
                }
                else
                {
                    content.append(((StringNode) result).getValue());
                }
            }
            else
            {
                content.append(((StringNode) child).getValue());
            }
        }
        return new StringNodeImpl(content.toString());
    }

    @Nonnull
    @Override
    public Node copy()
    {
        return new StringTemplateNode(this);
    }
}
