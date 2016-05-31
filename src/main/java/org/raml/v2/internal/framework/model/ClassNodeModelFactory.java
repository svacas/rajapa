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
package org.raml.v2.internal.framework.model;

import org.raml.v2.internal.framework.nodes.KeyValueNode;
import org.raml.v2.internal.framework.nodes.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Class based factory
 */
public class ClassNodeModelFactory implements NodeModelFactory
{
    private Class<? extends NodeModel> aClass;
    private boolean polymorphic;

    public ClassNodeModelFactory(Class<? extends NodeModel> aClass, boolean polymorphic)
    {
        this.aClass = aClass;
        this.polymorphic = polymorphic;
    }

    @Override
    public NodeModel create(Node node)
    {

        Constructor<?> nodeConstructor = findNodeConstructor(aClass);
        try
        {
            if (KeyValueNode.class.isAssignableFrom(nodeConstructor.getParameterTypes()[0]))
            {
                // If constructor expects a key value pair we try the current not or the parent.
                if (node instanceof KeyValueNode)
                {
                    return (NodeModel) nodeConstructor.newInstance(node);
                }
                else
                {
                    return (NodeModel) nodeConstructor.newInstance(node.findAncestorWith(KeyValueNode.class));
                }
            }
            else
            {
                return (NodeModel) nodeConstructor.newInstance(node);
            }
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean polymorphic()
    {
        return polymorphic;
    }


    private static Constructor<?> findNodeConstructor(Class<?> aClass)
    {
        Constructor<?>[] constructors = aClass.getConstructors();
        for (Constructor<?> constructor : constructors)
        {
            if (constructor.getParameterTypes().length == 1)
            {
                if (Node.class.isAssignableFrom(constructor.getParameterTypes()[0]))
                {
                    return constructor;
                }
            }
        }
        throw new RuntimeException("No constructor with a single Node type was found.");
    }


}
