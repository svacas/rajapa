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
package org.raml.v2.impl.commons.nodes;

import org.raml.v2.nodes.KeyValueNodeImpl;
import org.raml.v2.nodes.Node;
import org.raml.v2.nodes.StringNodeImpl;
import org.raml.v2.internal.utils.NodeUtils;

public class PayloadValidationResultNode extends KeyValueNodeImpl
{
    public PayloadValidationResultNode(PayloadNode payload)
    {
        super(new StringNodeImpl("result"), payload);
        payload.setParent(this);
    }

    public Node getValue()
    {
        return super.getValue();
    }

    public boolean validationSucceeded()
    {
        return !NodeUtils.isErrorResult(this);
    }
}
