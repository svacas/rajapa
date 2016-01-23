/*
 *
 */
package org.raml.nodes.impl;

import org.raml.grammar.rule.ErrorNodeFactory;
import org.raml.nodes.ExecutableNode;
import org.raml.nodes.ExecutionContext;
import org.raml.nodes.Node;
import org.raml.utils.Inflector;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.StringTokenizer;

public class TemplateExpressionNode extends StringNodeImpl implements ExecutableNode
{
    public TemplateExpressionNode(@Nonnull String value)
    {
        super(value);
    }

    @Nullable
    public String getVariableName()
    {
        final StringTokenizer expressionTokens = getExpressionTokens();
        return expressionTokens.hasMoreTokens() ? expressionTokens.nextToken() : null;
    }

    public Node execute(ExecutionContext context)
    {
        final StringTokenizer expressionTokens = getExpressionTokens();
        String result = null;
        if (expressionTokens.hasMoreTokens())
        {
            final String token = expressionTokens.nextToken();
            if (context.containsVariable(token))
            {
                result = context.getVariable(token);
            }
            else
            {
                return ErrorNodeFactory.createInvalidTemplateParameterExpression(this, token);
            }
        }
        while (expressionTokens.hasMoreTokens())
        {
            final String token = expressionTokens.nextToken();
            if (token.startsWith("!"))
            {
                try
                {
                    Method method = Inflector.class.getMethod(token.substring(1), String.class);
                    result = String.valueOf(method.invoke(null, result));
                }
                catch (Exception e)
                {
                    return ErrorNodeFactory.createInvalidTemplateFunctionExpression(this, token);
                }
            }
            else
            {
                return ErrorNodeFactory.createInvalidTemplateFunctionExpression(this, token);
            }
        }

        return new StringNodeImpl(result);

    }

    private StringTokenizer getExpressionTokens()
    {
        final String value = getValue();
        return new StringTokenizer(value, "|");
    }
}