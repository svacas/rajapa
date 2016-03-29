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
package org.raml.nodes.snakeyaml;

import javax.annotation.Nullable;

import org.raml.nodes.BaseNode;
import org.raml.nodes.Position;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.ScalarNode;

public abstract class SYBaseRamlNode extends BaseNode
{

    private Node yamlNode;

    public SYBaseRamlNode(SYBaseRamlNode node)
    {
        super(node);
        this.yamlNode = node.yamlNode;
    }

    public SYBaseRamlNode(Node yamlNode)
    {
        this.yamlNode = yamlNode;
    }

    protected Node getYamlNode()
    {
        return yamlNode;
    }

    @Override
    public Position getStartPosition()
    {
        return new SYPosition(yamlNode.getStartMark());
    }

    @Override
    public Position getEndPosition()
    {
        return new SYPosition(yamlNode.getEndMark());
    }

    @Nullable
    public String getLiteralValue()
    {
        if (yamlNode instanceof ScalarNode)
        {
            return ((ScalarNode) getYamlNode()).getValue();
        }
        return null;
    }

}
