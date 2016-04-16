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
package org.raml.v2.impl.commons.phase;

import java.util.List;

import org.raml.v2.impl.commons.model.BuiltInType;
import org.raml.v2.impl.v10.nodes.types.builtin.BooleanTypeNode;
import org.raml.v2.impl.v10.nodes.types.builtin.NumericTypeNode;
import org.raml.v2.impl.v10.nodes.types.builtin.ObjectTypeNode;
import org.raml.v2.impl.v10.nodes.types.builtin.StringTypeNode;
import org.raml.v2.nodes.KeyValueNode;
import org.raml.v2.nodes.KeyValueNodeImpl;
import org.raml.v2.nodes.Node;
import org.raml.v2.nodes.ObjectNode;
import org.raml.v2.nodes.StringNode;
import org.raml.v2.nodes.StringNodeImpl;
import org.raml.v2.nodes.snakeyaml.SYNullNode;
import org.raml.v2.nodes.snakeyaml.SYStringNode;
import org.raml.v2.phase.Phase;

public class SugarRushPhase implements Phase
{

    @Override
    public Node apply(Node tree)
    {
        sweetenBuiltInTypes(tree);
        sweetenObjects(tree);
        sweetenTypeSystemObjects(tree);
        sweetenAnnotations(tree);
        return tree;
    }

    private void sweetenBuiltInTypes(Node tree)
    {
        final List<StringNode> basicSugar = tree.findDescendantsWith(StringNode.class);

        for (StringNode sugarNode : basicSugar)
        {
            if (BuiltInType.isBuiltInType(sugarNode.getValue()) && !isTypePresentBasic(sugarNode))
            {
                handleBuiltInType(sugarNode);
            }
            else if ("array".equals(sugarNode.getValue()))
            {
                handleArray(sugarNode);
            }
            else if (isArraySugar(sugarNode))
            {
                handleObjectArray(sugarNode);
            }
        }
    }

    private void sweetenObjects(Node tree)
    {
        final List<StringNode> basicSugar = tree.findDescendantsWith(StringNode.class);
        for (StringNode sugarNode : basicSugar)
        {
            if ("properties".equals(sugarNode.getValue()))
            {
                if (!isTypePresentObject(sugarNode))
                {
                    Node grandParent = sugarNode.getParent().getParent();
                    grandParent.addChild(new KeyValueNodeImpl(new StringNodeImpl("type"), new StringNodeImpl("object")));
                }
            }
        }
    }

    private void sweetenTypeSystemObjects(Node tree)
    {
        final List<StringNode> basicSugar = tree.findDescendantsWith(StringNode.class);
        for (StringNode sugarNode : basicSugar)
        {
            if (isTypeSystemObjectProperty(sugarNode))
            {
                if (sugarNode.getChildren().isEmpty() && isValidTypeSystemObject(tree, sugarNode))
                {
                    Node newNode = new ObjectTypeNode();
                    newNode.addChild(new KeyValueNodeImpl(new StringNodeImpl("type"), new StringNodeImpl(sugarNode.getValue())));
                    // handleExample(sugarNode, newNode);
                    sugarNode.replaceWith(newNode);
                }
            }
        }
    }

    private void sweetenAnnotations(Node tree)
    {
        Node annotationsNode = tree.get("annotationTypes");
        if (annotationsNode != null)
        {
            for (Node annotation : annotationsNode.getChildren())
            {
                if (isTypeMissingInAnnotation(annotation))
                {
                    if (isStringAnnotation(annotation))
                    {
                        setTypeString(annotation);
                    }
                }
            }
        }
    }

    private void handleObjectArray(StringNode sugarNode)
    {
        Node parent = sugarNode.getParent();
        Node key = isKeyValueNode(parent) ? ((KeyValueNode) parent).getKey() : null;
        String keyString = key instanceof StringNode ? ((StringNode) key).getValue() : null;
        if (parent instanceof KeyValueNode && "type".equals(keyString))
        {
            Node grandParent = parent.getParent();
            grandParent.removeChild(parent);
            KeyValueNodeImpl items = handleArraySugar(sugarNode, grandParent);
            items.setSource(parent);
            grandParent.addChild(items);
        }
        else
        {
            Node newNode = new ObjectTypeNode();
            KeyValueNodeImpl items = handleArraySugar(sugarNode, newNode);
            items.setSource(parent);
            newNode.addChild(items);
            sugarNode.replaceWith(newNode);
        }

    }

    private boolean isArraySugar(StringNode sugarNode)
    {
        return sugarNode.getValue() != null && sugarNode.getValue().endsWith("[]");
    }

    private void handleArray(StringNode sugarNode)
    {
        if (sugarNode.getParent() != null && sugarNode.getParent().getParent() != null)
        {
            Node itemsNode = sugarNode.getParent().getParent().get("items");
            if (itemsNode instanceof SYNullNode)
            {
                itemsNode.replaceWith(new StringNodeImpl(new StringNodeImpl("string")));
            }
        }
    }

    private KeyValueNodeImpl handleArraySugar(StringNode sugarNode, Node grandParent)
    {
        String value = sugarNode.getValue().split("\\[")[0];
        grandParent.addChild(new KeyValueNodeImpl(new StringNodeImpl("type"), new StringNodeImpl("array")));
        return new KeyValueNodeImpl(new StringNodeImpl("items"), new StringNodeImpl(value));
    }

    private void handleBuiltInType(StringNode sugarNode)
    {
        if (sugarNode.getChildren().isEmpty())
        {
            Node newNode = getSugarNode(sugarNode.getValue());
            if (newNode != null)
            {
                newNode.addChild(new KeyValueNodeImpl(new StringNodeImpl("type"), new StringNodeImpl(sugarNode.getValue())));
                handleExample(sugarNode, newNode);
                sugarNode.replaceWith(newNode);
            }
        }
    }

    private void setTypeString(Node annotation)
    {
        if (isKeyValueNode(annotation))
        {
            Node stringTypeNode = new StringTypeNode();
            stringTypeNode.addChild(new KeyValueNodeImpl(new StringNodeImpl("type"), new StringNodeImpl("string")));
            ((KeyValueNode) annotation).getValue().replaceWith(stringTypeNode);
        }
    }

    private boolean isStringAnnotation(Node annotation)
    {
        return isKeyValueNode(annotation) && ((KeyValueNode) annotation).getValue().get("properties") == null;
    }

    private boolean isKeyValueNode(Node annotation)
    {
        return annotation instanceof KeyValueNode;
    }

    private boolean isTypeMissingInAnnotation(Node annotation)
    {
        return (isKeyValueNode(annotation)) && (((KeyValueNode) annotation).getValue().get("type") == null);
    }

    private boolean isValidTypeSystemObject(Node tree, StringNode sugarNode)
    {
        Node types = tree.get("types");
        // handling special union types, this will be resolved in the types transformation phase.
        String value = sugarNode.getValue();
        if (isUnion(sugarNode) || value.endsWith("[]"))
        {
            return true;
        }
        if (types != null)
        {
            Node object = types.get(value);
            return object != null && object instanceof ObjectNode;
        }
        return false;
    }

    private boolean isUnion(StringNode sugarNode)
    {
        String value = sugarNode.getValue();
        if (isKeyValueNode(sugarNode.getParent()))
        {
            KeyValueNode parent = (KeyValueNode) sugarNode.getParent();
            String key = ((StringNode) parent.getKey()).getValue();
            return value.contains("|") && !("type".equals(key) || "pattern".equals(key));
        }
        return false;
    }


    private boolean isTypeSystemObjectProperty(StringNode sugarNode)
    {
        // union type node, will be resolved in type transformation phase.
        if (isUnion(sugarNode))
        {
            return true;
        }
        Node properties = sugarNode.getParent().getParent().getParent();
        if (properties != null)
        {
            Node type = properties.getParent();
            if (isKeyValueNode(sugarNode.getParent()))
            {
                KeyValueNode parentNode = ((KeyValueNode) sugarNode.getParent());
                if (parentNode.getValue() instanceof StringNode && ((StringNode) parentNode.getValue()).getValue().equals(sugarNode.getValue()) && type.get("type") != null)
                {
                    return true;
                }
            }
        }
        return false;
    }

    private void handleExample(Node sugarNode, Node newNode)
    {
        Node example = sugarNode.getParent().getParent().get("example");
        if (example != null)
        {
            Node exampleRoot = example.getParent();
            exampleRoot.getParent().removeChild(exampleRoot);
            newNode.addChild(exampleRoot);
        }
    }

    private boolean isTypePresentBasic(Node sugarNode)
    {
        Node parent = sugarNode.getParent();
        if (isKeyValueNode(parent) && ((KeyValueNode) parent).getKey() instanceof SYStringNode)
        {
            SYStringNode key = (SYStringNode) ((KeyValueNode) parent).getKey();
            return "type".equals(key.getValue());
        }
        return false;
    }

    private boolean isTypePresentObject(Node sugarNode)
    {
        return sugarNode.getParent().getParent().get("type") != null;
    }

    private Node getSugarNode(String typeNode)
    {
        if (BuiltInType.STRING.getType().equals(typeNode))
        {
            return new StringTypeNode();
        }
        else if (BuiltInType.NUMBER.getType().equals(typeNode) || BuiltInType.INTEGER.getType().equals(typeNode))
        {
            return new NumericTypeNode();
        }
        else if (BuiltInType.BOOLEAN.getType().equals(typeNode))
        {
            return new BooleanTypeNode();
        }
        else if ("object".equals(typeNode))
        {
            return new ObjectTypeNode();
        }
        else
        {
            return null;
        }
    }
}