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
package org.raml.v2;


import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.raml.v2.grammar.rule.Rule;
import org.raml.v2.impl.commons.RamlHeader;
import org.raml.v2.impl.commons.RamlVersion;
import org.raml.v2.impl.v08.grammar.Raml08Grammar;
import org.raml.v2.impl.v10.grammar.Raml10Grammar;
import org.raml.v2.nodes.KeyValueNode;
import org.raml.v2.nodes.Node;
import org.raml.v2.nodes.ObjectNode;
import org.raml.v2.nodes.StringNode;
import org.raml.v2.suggester.DefaultSuggestion;
import org.raml.v2.suggester.RamlParsingContext;
import org.raml.v2.suggester.RamlParsingContextType;
import org.raml.v2.suggester.Suggestion;
import org.raml.v2.suggester.Suggestions;
import org.raml.v2.utils.Inflector;

public class RamlSuggester
{

    /**
     * Returns the suggestions for the specified document at the given position.
     * In most common cases the offset will be the cursor position.
     * @param document The raml document
     * @param offset The offset from the begging of the document
     * @return The suggestions
     */
    public Suggestions suggestions(String document, int offset)
    {
        final List<Suggestion> result = new ArrayList<>();
        final RamlParsingContext ramlParsingContext = getContext(document, offset);
        final int location = ramlParsingContext.getLocation();
        final String content = ramlParsingContext.getContent();
        final List<Suggestion> suggestions = getSuggestions(ramlParsingContext, document, offset, location);
        if (content.isEmpty())
        {
            result.addAll(suggestions);
        }
        else
        {
            for (Suggestion suggestion : suggestions)
            {
                if (suggestion.getValue().startsWith(content))
                {
                    result.add(suggestion);
                }
            }
        }
        Collections.sort(result);
        return new Suggestions(result, content, location);

    }

    private List<Suggestion> getSuggestions(RamlParsingContext context, String document, int offset, int location)
    {
        switch (context.getContextType())
        {
        case FUNCTION_CALL:
            return getFunctionCallSuggestions();
        case STRING_TEMPLATE:
            return getTemplateParameterSuggestions(document, offset, location);
        case LIBRARY_CALL:
        case ITEM:
        case VALUE:
            return getSuggestionsAt(context, document, offset, location);
        default:
            return getSuggestionByColumn(context, document, offset, location);

        }
    }

    @Nonnull
    private List<Suggestion> getTemplateParameterSuggestions(String document, int offset, int location)
    {
        final Node rootNode = getRootNode(document, offset, location);
        Node node = searchNodeAt(rootNode, location);
        boolean inTrait = false;
        while (node != null)
        {

            if (node instanceof KeyValueNode)
            {
                if (((KeyValueNode) node).getKey() instanceof StringNode)
                {
                    final String value = ((StringNode) ((KeyValueNode) node).getKey()).getValue();
                    if (value.equals("traits") || value.equals("resourceTypes"))
                    {
                        inTrait = value.equals("traits");
                        break;
                    }
                }
            }
            node = node.getParent();
        }
        return inTrait ? defaultTraitParameters() : defaultResourceTypeParameters();
    }

    @Nonnull
    private List<Suggestion> defaultTraitParameters()
    {
        final List<Suggestion> suggestions = defaultResourceTypeParameters();
        suggestions.add(new DefaultSuggestion("methodName", "The name of the method", ""));
        return suggestions;
    }

    @Nonnull
    private List<Suggestion> defaultResourceTypeParameters()
    {
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new DefaultSuggestion("resourcePath", "The resource's full URI relative to the baseUri (if any)", ""));
        suggestions.add(new DefaultSuggestion("resourcePathName", "The rightmost path fragment of the resource's relative URI, " +
                                                                  "omitting any parametrize brackets (\"{\" and \"}\")", ""));
        return suggestions;
    }

    @Nonnull
    private List<Suggestion> getFunctionCallSuggestions()
    {
        List<Suggestion> suggestions = new ArrayList<>();
        final Method[] declaredMethods = Inflector.class.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods)
        {
            if (Modifier.isStatic(declaredMethod.getModifiers()) && Modifier.isPublic(declaredMethod.getModifiers()))
            {
                suggestions.add(new DefaultSuggestion("!" + declaredMethod.getName(), "", declaredMethod.getName()));
            }
        }
        return suggestions;
    }

    private List<Suggestion> getSuggestionsAt(RamlParsingContext context, String document, int offset, int location)
    {
        final Node root = getRootNode(document, offset, location);
        Node node = searchNodeAt(root, location);
        if (node != null)
        {
            // If is the key of a key value pair
            if (node.getParent() instanceof KeyValueNode && node.getParent().getChildren().indexOf(node) == 0)
            {
                node = node.getParent().getParent();
            }
            // Recreate path with the node at the correct indentation
            final List<Node> pathToRoot = createPathToRoot(node);
            // Follow the path from the root to the node and apply the rules for auto-completion.
            final Rule rootRule = getRuleFor(document);
            return rootRule != null ? rootRule.getSuggestions(pathToRoot, context) : Collections.<Suggestion> emptyList();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    private Node getRootNode(String document, int offset, int location)
    {
        // We only run the first phase
        final RamlBuilder ramlBuilder = new RamlBuilder(RamlBuilder.FIRST_PHASE);
        try
        {
            // We try the with the original document
            return ramlBuilder.build(document);
        }
        catch (Exception e)
        {
            // We remove some current keywords to see if it parses
            final String header = document.substring(0, location + 1);
            final String footer = getFooter(document, offset);
            final String realDocument = header + footer;
            return ramlBuilder.build(realDocument);
        }
    }

    private List<Suggestion> getSuggestionByColumn(RamlParsingContext context, String document, int offset, int location)
    {
        // // I don't care column number unless is an empty new line
        int columnNumber = getColumnNumber(document, offset);
        final Node root = getRootNode(document, offset, location);
        Node node = searchNodeAt(root, location);
        if (node != null)
        {
            node = getValueNodeAtColumn(columnNumber, node);
            // Recreate path with the node at the correct indentation
            final List<Node> pathToRoot = createPathToRoot(node);

            // Follow the path from the root to the node and apply the rules for auto-completion.
            final Rule rootRule = getRuleFor(document);
            return rootRule != null ? rootRule.getSuggestions(pathToRoot, context) : Collections.<Suggestion> emptyList();
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Nonnull
    private RamlParsingContext getContext(String document, int offset)
    {
        RamlParsingContext context = null;
        int location = offset;
        final StringBuilder content = new StringBuilder();
        while (location >= 0 && context == null)
        {
            char character = document.charAt(location);
            switch (character)
            {
            case ':':
                context = new RamlParsingContext(RamlParsingContextType.VALUE, revertAndTrim(content), location + 1);
                break;
            case ',':
            case '[':
            case '{':
            case '-':
                context = new RamlParsingContext(RamlParsingContextType.ITEM, revertAndTrim(content), location);
                break;
            case '<':
                if (location > 0)
                {
                    if (document.charAt(location - 1) == '<')
                    {
                        location--;
                        final String contextContent = revertAndTrim(content);
                        final String[] split = contextContent.split("\\|");
                        if (split.length > 1)
                        {
                            context = new RamlParsingContext(RamlParsingContextType.FUNCTION_CALL, split[split.length - 1].trim(), location);
                        }
                        else if (contextContent.endsWith("|"))
                        {
                            context = new RamlParsingContext(RamlParsingContextType.FUNCTION_CALL, "", location);
                        }
                        else
                        {
                            context = new RamlParsingContext(RamlParsingContextType.STRING_TEMPLATE, contextContent, location);
                        }
                        break;
                    }
                }
                content.append(character);
                break;
            case '.':
                context = new RamlParsingContext(RamlParsingContextType.LIBRARY_CALL, revertAndTrim(content), location);
                break;
            case '\n':
                context = new RamlParsingContext(RamlParsingContextType.ANY, revertAndTrim(content), location);
                break;
            default:
                content.append(character);
            }
            location--;
        }

        if (context == null)
        {
            context = new RamlParsingContext(RamlParsingContextType.ANY, revertAndTrim(content), location);
        }

        return context;
    }

    @Nonnull
    private String revertAndTrim(StringBuilder content)
    {
        return content.reverse().toString().trim();
    }

    private Node getValueNodeAtColumn(int columnNumber, Node node)
    {
        if (columnNumber == 0)
        {
            return node.getRootNode();
        }
        else
        {
            // Create the path from the selected node to the root.
            final List<Node> path = createPathToRoot(node);
            // Find the node with the indentation in the path
            for (Node element : path)
            {
                if (element instanceof KeyValueNode)
                {
                    if (element.getStartPosition().getColumn() < columnNumber)
                    {
                        // In an object we need that it should be minor for arrays
                        node = ((KeyValueNode) element).getValue();
                    }
                }
                else if (element instanceof ObjectNode)
                {
                    // In an object we need that it should be equals or minor
                    if (element.getStartPosition().getColumn() <= columnNumber)
                    {
                        node = element;
                    }
                }
            }
            return node;
        }
    }

    @Nonnull
    private List<Node> createPathToRoot(Node node)
    {
        final List<Node> path = new ArrayList<>();
        Node parent = node;
        while (parent != null)
        {
            path.add(0, parent);
            parent = parent.getParent();
        }
        return path;
    }

    @Nonnull
    private String getFooter(String document, int offset)
    {
        int loc = offset;
        char current = document.charAt(loc);
        while (loc < document.length() - 1 && current != '\n' && current != '}' && current != ']' && current != ',')
        {
            loc++;
            current = document.charAt(loc);
        }

        return loc < document.length() ? document.substring(loc) : "";
    }

    private int getColumnNumber(String document, int offset)
    {
        final StringBuilder contextLine = getContextLine(document, offset);
        int columnNumber = 0;
        for (int i = 0; i < contextLine.length(); i++)
        {
            if (Character.isWhitespace(contextLine.charAt(i)))
            {
                columnNumber++;
            }
            else
            {
                break;
            }
        }
        return columnNumber;
    }

    @Nonnull
    private StringBuilder getContextLine(String document, int offset)
    {
        final StringBuilder contextLine = new StringBuilder();

        int location = offset;
        char character = document.charAt(location);
        while (location > 0 && character != '\n')
        {
            location--;
            contextLine.append(character);
            character = document.charAt(location);

        }
        return contextLine.reverse();
    }

    @Nullable
    private Node searchNodeAt(Node root, int location)
    {
        if (root.getEndPosition().getIndex() != location || !root.getChildren().isEmpty())
        {
            final List<Node> children = root.getChildren();
            for (Node child : children)
            {
                if (child.getEndPosition().getIndex() == location)
                {
                    if (child.getChildren().isEmpty())
                    {
                        return child;
                    }
                    else
                    {
                        return searchNodeAt(child, location);
                    }
                }
                else if (child.getEndPosition().getIndex() > location || isLastNode(child))
                {
                    if (child.getChildren().isEmpty())
                    {
                        return child;
                    }
                    else
                    {
                        return searchNodeAt(child, location);
                    }
                }
            }
            return null;
        }
        else
        {
            return root;
        }
    }

    private boolean isLastNode(Node node)
    {
        final Node parent = node.getParent();
        if (parent == null)
        {
            return false;
        }
        List<Node> children = parent.getChildren();
        Node lastChild = children.get(children.size() - 1);
        return node.equals(lastChild);
    }


    @Nullable
    public Rule getRuleFor(String stringContent)
    {
        try
        {
            RamlHeader ramlHeader = RamlHeader.parse(stringContent);
            if (RamlVersion.RAML_08 == ramlHeader.getVersion())
            {
                return new Raml08Grammar().raml();
            }
            if (ramlHeader.getFragment() != null)
            {
                return ramlHeader.getFragment().getRule(new Raml10Grammar());
            }
        }
        catch (RamlHeader.InvalidHeaderException e)
        {
            // ignore, just return null
        }
        return null;
    }

}