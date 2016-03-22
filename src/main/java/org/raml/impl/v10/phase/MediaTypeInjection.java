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
package org.raml.impl.v10.phase;


import java.util.ArrayList;
import java.util.List;

import org.raml.impl.commons.grammar.BaseRamlGrammar;
import org.raml.impl.commons.nodes.BodyNode;
import org.raml.nodes.BaseNode;
import org.raml.nodes.KeyValueNode;
import org.raml.nodes.KeyValueNodeImpl;
import org.raml.nodes.Node;
import org.raml.nodes.NodeType;
import org.raml.nodes.ObjectNode;
import org.raml.nodes.Position;
import org.raml.nodes.StringNode;
import org.raml.phase.Phase;
import org.raml.utils.NodeSelector;

public class MediaTypeInjection implements Phase
{

    @Override
    public Node apply(Node tree)
    {
        List<StringNode> defaultMediaTypes = getDefaultMediaTypes(tree);
        if (!defaultMediaTypes.isEmpty())
        {
            List<BodyNode> bodyNodes = tree.findDescendantsWith(BodyNode.class);
            for (BodyNode bodyNode : bodyNodes)
            {
                if (!hasExplicitMimeTypes(bodyNode))
                {
                    injectMediaTypes(bodyNode, defaultMediaTypes);
                }
            }
        }
        return tree;
    }

    private List<StringNode> getDefaultMediaTypes(Node tree)
    {
        List<StringNode> result = new ArrayList<>();
        Node mediaTypeNode = NodeSelector.selectFrom("mediaType", tree);
        if (mediaTypeNode != null)
        {
            if (mediaTypeNode instanceof StringNode)
            {
                result.add((StringNode) mediaTypeNode);
            }
            else
            {
                for (Node node : mediaTypeNode.getChildren())
                {
                    result.add((StringNode) node);
                }
            }
        }
        return result;
    }

    private void injectMediaTypes(BodyNode bodyNode, List<StringNode> defaultMediaTypes)
    {
        Node injected = new MediaTypeInjectedNode();
        for (StringNode defaultMediaType : defaultMediaTypes)
        {
            Node copy = bodyNode.getValue().copy();
            KeyValueNode keyValue = new KeyValueNodeImpl(defaultMediaType.copy(), copy);
            injected.addChild(keyValue);
        }
        bodyNode.setChild(1, injected);
    }

    private boolean hasExplicitMimeTypes(BodyNode bodyNode)
    {
        List<Node> children = bodyNode.getValue().getChildren();
        if (!children.isEmpty() && !children.get(0).getChildren().isEmpty())
        {
            Node key = children.get(0).getChildren().get(0);
            return key.toString().matches(BaseRamlGrammar.MIME_TYPE_REGEX);
        }
        return false;
    }

    public static class MediaTypeInjectedNode extends BaseNode implements ObjectNode
    {

        @Override
        public Position getStartPosition()
        {
            return null;
        }

        @Override
        public Position getEndPosition()
        {
            return null;
        }

        @Override
        public Node copy()
        {
            return null;
        }

        @Override
        public NodeType getType()
        {
            return NodeType.Object;
        }
    }
}