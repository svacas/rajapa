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
package org.raml.v2.utils;

import static org.raml.v2.impl.commons.grammar.BaseRamlGrammar.MIME_TYPE_REGEX;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.raml.v2.grammar.rule.ErrorNodeFactory;
import org.raml.v2.impl.commons.nodes.RamlDocumentNode;
import org.raml.v2.impl.commons.nodes.ResourceNode;
import org.raml.v2.nodes.ArrayNode;
import org.raml.v2.nodes.KeyValueNode;
import org.raml.v2.nodes.Node;
import org.raml.v2.nodes.ObjectNode;
import org.raml.v2.nodes.SimpleTypeNode;
import org.raml.v2.nodes.snakeyaml.SYArrayNode;

public class NodeSelector
{

    public static final String PARENT_EXPR = "..";
    public static final String WILDCARD_SELECTOR = "*";
    public static final String ENCODED_SLASH = "\\\\/";
    private final static Pattern mimeTypePattern = Pattern.compile(MIME_TYPE_REGEX);

    /**
     * Resolves a path in the specified node. The path uses a very simple expression system like xpath where each element is separated by /.
     * <p><b>"name"</b> -> return the value of field with key that matches the specified name. <br/>
     * <b>..</b>        -> returns the parent <br/>
     * <b>*</b>         -> wild card selector <br/>
     * <b>number</b>    -> returns the element at that index zero base index. The number should be bigger than zero</p><br/>
     *
     * @param path The path example schemas/foo
     * @param from The source where to query
     * @return The result null if no match
     */
    @Nullable
    public static Node selectFrom(String path, Node from)
    {
        if (path.startsWith("/"))
        {
            return selectFrom(path.substring(1), from.getRootNode());
        }
        else
        {
            final String[] tokens = path.split("(?<!\\\\)/");
            return selectFrom(Arrays.asList(tokens), from);
        }
    }

    @Nullable
    public static Integer selectIntValue(String path, Node from)
    {
        return selectType(path, from, null);
    }

    @Nullable
    public static String selectStringValue(String path, Node from)
    {
        return selectType(path, from, null);
    }

    public static <T> T selectType(String path, Node from, T defaultValue)
    {
        SimpleTypeNode<T> selectedNode = (SimpleTypeNode<T>) selectFrom(path, from);
        if (selectedNode != null)
        {
            return selectedNode.getValue();
        }
        return defaultValue;
    }

    public static List<String> selectStringCollection(String path, Node from)
    {
        return selectCollection(path, from);
    }

    private static <T> List<T> selectCollection(String path, Node from)
    {
        ArrayList<T> selectedValues = Lists.newArrayList();
        Node selectedNode = NodeSelector.selectFrom(path, from);
        if (selectedNode != null)
        {
            if (selectedNode instanceof SimpleTypeNode)
            {
                selectedValues.add(((SimpleTypeNode<T>) selectedNode).getValue());
            }
            else if (selectedNode instanceof SYArrayNode)
            {
                for (Node node : selectedNode.getChildren())
                {
                    if (node instanceof SimpleTypeNode)
                    {
                        selectedValues.add(((SimpleTypeNode<T>) node).getValue());
                    }
                }
            }
        }
        return selectedValues;
    }


    @Nullable
    private static Node selectFrom(List<String> pathTokens, Node from)
    {
        Node currentNode = from;
        for (int i = 0; i < pathTokens.size() && currentNode != null; i++)
        {
            String token = pathTokens.get(i);
            if (token.equals(WILDCARD_SELECTOR))
            {
                if (currentNode instanceof ArrayNode)
                {
                    final List<Node> children = currentNode.getChildren();
                    final List<String> remainingTokens = pathTokens.subList(i + 1, pathTokens.size());
                    for (Node child : children)
                    {
                        final Node resolve = selectFrom(remainingTokens, child);
                        if (resolve != null)
                        {
                            currentNode = resolve;
                            break;
                        }
                    }
                    break;
                }
                // else we ignore the *
            }
            else if (token.equals(PARENT_EXPR))
            {
                currentNode = currentNode.getParent();
            }
            else if (currentNode instanceof ObjectNode)
            {
                currentNode = findValueWithName(currentNode, token);
            }
            else if (currentNode instanceof ArrayNode)
            {
                final int index = Integer.parseInt(token);
                currentNode = findElementAtIndex(currentNode, index);
            }
            else if (currentNode instanceof ResourceNode)
            {
                currentNode = findValueWithName(currentNode.getChildren().get(1), token);
            }
            else
            {
                currentNode = null;
            }
        }

        return currentNode;
    }

    @Nullable
    private static Node findElementAtIndex(final Node currentNode, int index)
    {
        Node result = null;
        final List<Node> children = currentNode.getChildren();
        if (children.size() > index)
        {
            result = children.get(index);
        }
        return result;
    }

    @Nullable
    private static Node findValueWithName(final Node currentNode, String token)
    {
        Node result = null;
        final List<Node> children = currentNode.getChildren();
        for (Node child : children)
        {
            if (child instanceof KeyValueNode)
            {
                final Node key = ((KeyValueNode) child).getKey();
                if (key instanceof SimpleTypeNode)
                {
                    if (token.equals(encodePath(String.valueOf(((SimpleTypeNode) key).getValue()))))
                    {
                        result = ((KeyValueNode) child).getValue();
                        break;
                    }
                }
            }
        }
        return result;
    }

    public static String encodePath(final String path)
    {
        return path.replaceAll("/", ENCODED_SLASH);
    }


    public static Node getNodeFromPath(String path, RamlDocumentNode tree)
    {
        String[] parts = path.split(":");
        if (parts.length < 2)
        {
            return ErrorNodeFactory.createInvalidNode(null);
        }
        else
        {
            if ("baseUriParameters".equals(parts[0]))
            {
                return selectBaseUriParameters(tree, parts);
            }
            else
            {
                return selectResourceBody(tree, parts);
            }
        }
    }

    private static Node selectResourceBody(RamlDocumentNode tree, String[] parts)
    {
        List<ResourceNode> resources = tree.findDescendantsWith(ResourceNode.class);
        if (resources.isEmpty())
        {
            return ErrorNodeFactory.createInvalidNode(null);
        }
        else
        {
            for (ResourceNode resource : resources)
            {
                if (parts[0].equals(resource.getResourcePath()))
                {
                    Node children = resource.get(parts[1]);
                    boolean missingChildren = children == null;
                    for (int i = 2; i <= parts.length - 1 && !missingChildren; i++)
                    {
                        if (i == parts.length - 1 && mimeTypePattern.matcher(parts[i]).matches())
                        {
                            children = children.get(encodePath(parts[i]));
                        }
                        else
                        {
                            children = children.get(parts[i]);
                        }
                        if (children == null)
                        {
                            missingChildren = true;
                        }
                    }
                    if (children != null)
                    {
                        return children;
                    }
                    else
                    {
                        return ErrorNodeFactory.createInvalidNode(null);
                    }
                }
            }
            return ErrorNodeFactory.createInvalidNode(null);
        }
    }

    private static Node selectBaseUriParameters(RamlDocumentNode tree, String[] parts)
    {
        if (parts.length != 2)
        {
            return ErrorNodeFactory.createInvalidNode(null);
        }
        else
        {
            Node baseUriParameters = tree.get("baseUriParameters");
            if (baseUriParameters != null)
            {
                return baseUriParameters.get(parts[1]) != null ? baseUriParameters.get(parts[1]) : ErrorNodeFactory.createInvalidNode(null);
            }
            else
            {
                return ErrorNodeFactory.createInvalidNode(null);
            }
        }
    }

}
