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
package org.raml.v2.validator;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.raml.v2.RamlBuilder;
import org.raml.v2.impl.commons.nodes.PayloadValidationResultNode;
import org.raml.v2.impl.commons.nodes.RamlDocumentNode;
import org.raml.v2.utils.NodeValidator;

public class YamlNodeValidatorTest
{

    private NodeValidator nodeValidator;

    @Before
    public void setUp() throws IOException
    {
        RamlBuilder builder = new RamlBuilder();
        RamlDocumentNode tree = (RamlDocumentNode) builder.build(new File(this.getClass().getClassLoader().getResource("org/raml/v2/parser/examples/madness/input.raml").getPath()));
        this.nodeValidator = new NodeValidator(builder.getResourceLoader(), tree);
    }

    @Test
    public void testParsingFailure()
    {
        PayloadValidationResultNode validationNode =
                this.nodeValidator.validatePayload("/send:post:body:application/json", "{\"discriminator\":\"HasHome Parrot\", \"livesInside\": \"sometimes\", \"feathers\": \"colorful\"}");
        Assert.assertFalse(validationNode.validationSucceeded());
    }

    @Test
    public void testParsingOk()
    {
        PayloadValidationResultNode validationNode =
                this.nodeValidator.validatePayload("/send:post:body:application/json", "{\"discriminator\":\"HasHome Parrot\", \"livesInside\": true, \"feathers\": \"blue\"}");
        Assert.assertTrue(validationNode.validationSucceeded());
    }
}