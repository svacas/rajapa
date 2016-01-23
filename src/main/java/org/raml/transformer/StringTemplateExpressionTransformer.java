/*
 *
 */
package org.raml.transformer;

import org.raml.nodes.Node;
import org.raml.nodes.Position;
import org.raml.nodes.StringNode;
import org.raml.nodes.impl.StringNodeImpl;
import org.raml.nodes.impl.TemplateExpressionNode;
import org.raml.nodes.impl.StringTemplateNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringTemplateExpressionTransformer implements Transformer
{
    static Pattern templatePattern = Pattern.compile("<<(.+)>>");

    @Override
    public boolean matches(Node node)
    {
        if (node instanceof StringNode)
        {
            final String value = ((StringNode) node).getValue();
            return templatePattern.matcher(value).find();
        }
        return false;
    }

    @Override
    public Node transform(Node node)
    {
        final String value = ((StringNode) node).getValue();
        final StringTemplateNode stringTemplateNode = new StringTemplateNode(value);
        final Matcher templateMatcher = templatePattern.matcher(value);
        final Position startPosition = node.getStartPosition();
        // The previous template expression end position.
        int previousEndPosition = 0;
        while (templateMatcher.find())
        {
            final int start = templateMatcher.start();
            final int end = templateMatcher.end();
            if (start > previousEndPosition)
            {
                final StringNodeImpl stringNode = new StringNodeImpl(value.substring(previousEndPosition, start));
                stringNode.setStartPosition(startPosition.rightShift(previousEndPosition));
                stringNode.setEndPosition(startPosition.rightShift(start));
                stringTemplateNode.addChild(stringNode);
            }
            final TemplateExpressionNode expressionNode = new TemplateExpressionNode(templateMatcher.group(1));
            expressionNode.setStartPosition(startPosition.rightShift(templateMatcher.start(1)));
            expressionNode.setEndPosition(startPosition.rightShift(templateMatcher.end(1)));
            stringTemplateNode.addChild(expressionNode);
            previousEndPosition = end;
        }

        if (value.length() > previousEndPosition)
        {
            final StringNodeImpl stringNode = new StringNodeImpl(value.substring(previousEndPosition, value.length()));
            stringNode.setStartPosition(startPosition.rightShift(previousEndPosition));
            stringNode.setEndPosition(startPosition.rightShift(value.length()));
            stringTemplateNode.addChild(stringNode);
        }

        return stringTemplateNode;
    }
}