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
package org.raml.v2.nodes;

import org.raml.v2.nodes.snakeyaml.SYIncludeNode;

public class SchemaNodeImpl extends StringNodeImpl
{

    private String typeReference;

    private String schemaPath;

    public SchemaNodeImpl(String value, String schemaPath)
    {
        super(value);
        this.schemaPath = schemaPath;
    }

    public SchemaNodeImpl(StringNode node, String schemaPath)
    {
        super(node.getValue());
        if (node.getSource() instanceof SYIncludeNode)
        {
            this.typeReference = ((SYIncludeNode) node.getSource()).getIncludedType();
        }
        else if (node instanceof SchemaNodeImpl)
        {
            this.typeReference = ((SchemaNodeImpl) node).getTypeReference();
        }
        this.schemaPath = schemaPath;
    }

    public String getSchemaPath()
    {
        return schemaPath;
    }

    public String getTypeReference()
    {
        return this.typeReference;
    }
}
