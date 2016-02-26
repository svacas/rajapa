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
package org.raml.impl.v10;

import org.raml.impl.v10.phase.TypesTransformer;
import org.raml.phase.GrammarPhase;
import org.raml.grammar.rule.ErrorNodeFactory;
import org.raml.impl.v10.grammar.Raml10Grammar;
import org.raml.loader.ResourceLoader;
import org.raml.nodes.Node;
import org.raml.nodes.snakeyaml.RamlNodeParser;
import org.raml.phase.Phase;
import org.raml.phase.TransformationPhase;
import org.raml.impl.commons.phase.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Raml10Builder
{

    public Node build(String stringContent, String fragmentText, ResourceLoader resourceLoader, String resourceLocation, int maxPhaseNumber) throws IOException
    {
        RamlFragment fragment = RamlFragment.byName(fragmentText);
        if (fragment == null)
        {
            return ErrorNodeFactory.createInvalidFragmentName(fragmentText);
        }
        Node rootNode = RamlNodeParser.parse(stringContent);
        final List<Phase> phases = createPhases(resourceLoader, resourceLocation, fragment);
        for (int i = 0; i < phases.size(); i++)
        {
            if (i < maxPhaseNumber)
            {
                Phase phase = phases.get(i);
                rootNode = phase.apply(rootNode);
            }
        }
        return rootNode;
    }


    private List<Phase> createPhases(ResourceLoader resourceLoader, String resourceLocation, RamlFragment fragment)
    {
        // The first phase expands the includes.
        final TransformationPhase first = new TransformationPhase(new IncludeResolver(resourceLoader, resourceLocation), new StringTemplateExpressionTransformer(),
                new TypesTransformer());
        // Overlays and extensions.

        // Runs Schema. Applies the Raml rules and changes each node for a more specific. Annotations Library TypeSystem
        final GrammarPhase second = new GrammarPhase(fragment.getRule(new Raml10Grammar()));
        // Detect invalid references. Library resourceTypes and Traits. This point the nodes are good enough for Editors.

        // Normalize resources and detects duplicated ones and more than one use of url parameters. ???

        // Applies resourceTypes and Traits Library
        final TransformationPhase third = new TransformationPhase(new ResourceTypesTraitsTransformer(), new TypesTransformer());

        // Schema Types example validation
        return Arrays.asList(first, second, third);

    }
}