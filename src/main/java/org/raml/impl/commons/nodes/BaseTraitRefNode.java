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

import javax.annotation.Nonnull;

import org.raml.impl.v10.grammar.Raml10Grammar;
import org.raml.nodes.AbstractReferenceNode;
import org.raml.nodes.Node;
import org.raml.utils.NodeSelector;

public class BaseTraitRefNode extends AbstractReferenceNode
{

    private String name;

    public BaseTraitRefNode(String name)
    {
        this.name = name;
    }

    public BaseTraitRefNode(BaseTraitRefNode node)
    {
        super(node);
        this.name = node.name;
    }

    @Override
    public String getRefName()
    {
        return name;
    }

    @Override
    public TraitNode getRefNode()
    {
        // We add the .. as the node selector selects the value and we want the key value pair
        final Node resolve = NodeSelector.selectFrom(Raml10Grammar.TRAITS_KEY_NAME + "/*/" + getRefName() + "/..", getRelativeNode());
        if (resolve instanceof TraitNode)
        {
            return (TraitNode) resolve;
        }
        else
        {
            return null;
        }
    }

    @Nonnull
    @Override
    public Node copy()
    {
        return new BaseTraitRefNode(this);
    }
}
