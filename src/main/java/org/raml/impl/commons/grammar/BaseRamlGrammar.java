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
package org.raml.impl.commons.grammar;

import com.google.common.collect.Range;
import org.raml.grammar.BaseGrammar;
import org.raml.grammar.rule.*;
import org.raml.impl.commons.nodes.*;
import org.raml.nodes.Node;

import java.math.BigInteger;

public abstract class BaseRamlGrammar extends BaseGrammar
{

    public static final String USES_KEY_NAME = "uses";
    public static final String RESOURCE_TYPES_KEY_NAME = "resourceTypes";
    public static final String TRAITS_KEY_NAME = "traits";
    public static final String SECURITY_SCHEMES_KEY_NAME = "securitySchemes";

    public ObjectRule raml()
    {
        return objectType()
                           .with(descriptionField())
                           .with(schemasField())
                           .with(traitsField())
                           .with(resourceTypesField())
                           .with(securitySchemesField())
                           .with(titleField().description("Short plain-text label for the API."))
                           .with(versionField())
                           .with(baseUriField())
                           .with(baseUriParametersField())
                           .with(protocolsField())
                           .with(mediaTypeField())
                           .with(securedByField().description("The security schemes that apply to every resource and method in the API."))
                           .with(resourceField().then(ResourceNode.class))
                           .with(nonOptionalField(documentationKey(), documentations()))
                           .then(RamlDocumentNode.class);
    }

    protected KeyValueRule baseUriParametersField()
    {
        return field(baseUriParametersKey(), parameters());
    }

    protected KeyValueRule baseUriField()
    {
        return field(baseUriKey(), stringType());
    }


    protected KeyValueRule titleField()
    {
        return requiredField(titleKey(), stringType());
    }


    protected KeyValueRule resourceField()
    {
        return field(resourceKey(), resourceValue());
    }


    protected KeyValueRule versionField()
    {
        return field(versionKey(), stringType());
    }

    protected KeyValueRule mediaTypeField()
    {
        return field(mediaTypeKey(), stringType());
    }

    public Rule resourceType()
    {
        return objectType()
                           .with(field(anyResourceTypeMethod(), methodValue()).then(MethodNode.class))
                           .with(resourceTypeReferenceField())
                           .with(isField())
                           .with(field(stringType(), any())); // match anything else
    }

    // Documentation
    protected Rule documentations()
    {
        return array(documentation());
    }

    public Rule documentation()
    {
        return objectType()
                           .with(titleField().description("Title of documentation section."))
                           .with(contentField().description("Content of documentation section."));
    }

    protected KeyValueRule contentField()
    {
        return requiredField(string("content"), stringType());
    }


    // Security scheme

    protected Rule securitySchemes()
    {
        return objectType()
                           .with(
                                   field(stringType(), securityScheme())
                                                                        .then(SecuritySchemeNode.class)
                           );
    }

    protected Rule securityScheme()
    {
        return objectType()
                           .with(descriptionField())
                           .with(field(
                                   typeKey(),
                                   anyOf(
                                           string("OAuth 1.0").description("The API's authentication uses OAuth 1.0 as described in RFC5849 [RFC5849]"),
                                           string("OAuth 2.0").description("The API's authentication uses OAuth 2.0 as described in RFC6749 [RFC6749]"),
                                           string("BasicSecurityScheme Authentication").description(
                                                   "The API's authentication uses BasicSecurityScheme Access Authentication as described in RFC2617 [RFC2617]"),
                                           string("DigestSecurityScheme Authentication").description(
                                                   "The API's authentication uses DigestSecurityScheme Access Authentication as described in RFC2617 [RFC2617]"),
                                           string("Pass Through").description("Headers or Query Parameters are passed through to the API based on a defined object."),
                                           regex("x-.+").description("The API's authentication uses an authentication method not listed above.")
                                   )))
                           .with(field(string("describedBy"), securitySchemePart()))
                           .with(field(string("settings"), securitySchemeSettings()));
    }

    protected ObjectRule securitySchemePart()
    {
        return objectType()
                           .with(displayNameField())
                           .with(descriptionField())
                           .with(field(headersKey(), parameters()))
                           .with(field(queryParametersKey(), parameters()))
                           .with(field(responseKey(), responses()));
    }

    protected Rule securitySchemeSettings()
    {
        return objectType()
                           .with(field(string("requestTokenUri"), stringType()))
                           .with(field(string("authorizationUri"), stringType()))
                           .with(field(string("tokenCredentialsUri"), stringType()))
                           .with(field(string("accessTokenUri"), stringType()))
                           .with(field(string("authorizationGrants"), array(stringType())))
                           .with(field(string("scopes"), array(stringType())));
    }

    // Traits
    protected Rule traits()
    {
        return anyOf(array(trait()), trait());
    }

    public Rule trait()
    {
        // TODO resourceRule().with(parameterKey(), any())
        return objectType("trait")
                                  .with(field(stringType(), any())
                                                                  .then(TraitNode.class));
    }

    // Resource Types
    protected Rule resourceTypes()
    {
        // TODO resourceRule().with(parameterKey(), any())
        return objectType().with(field(stringType(), any()).then(ResourceTypeNode.class));
    }


    // Resources
    protected ObjectRule resourceValue()
    {
        return objectType("resourceValue")
                                          .with(displayNameField())
                                          .with(descriptionField())
                                          .with(field(anyMethod(), methodValue()).then(MethodNode.class))
                                          .with(isField().description("A list of the traits to apply to all methods declared (implicitly or explicitly) for this resource. "))
                                          .with(resourceTypeReferenceField())
                                          .with(securedByField().description("The security schemes that apply to all methods declared (implicitly or explicitly) for this resource."))
                                          .with(field(uriParametersKey(), parameters()))
                                          .with(field(resourceKey(), ref("resourceValue")).then(ResourceNode.class));
    }


    protected Rule schemas()
    {
        return objectType()
                           .with(field(stringType(), stringType()));
    }

    // Method
    protected ObjectRule methodValue()
    {
        // TODO query string
        return objectType()
                           .with(descriptionField())
                           .with(displayNameField())
                           .with(field(queryParametersKey(), parameters()))
                           .with(headersField())
                           .with(field(responseKey(), responses()))
                           .with(bodyField())
                           .with(protocolsField().description("A method can override the protocols specified in the resource or at the API root, by employing this property."))
                           .with(isField()
                                          .description("A list of the traits to apply to this method."))
                           .with(securedByField().description("The security schemes that apply to this method."));
    }

    protected StringValueRule responseKey()
    {
        return string("responses")
                                  .description("Information about the expected responses to a request");
    }

    protected StringValueRule queryStringKey()
    {
        return string("queryString")
                                    .description("Specifies the query string needed by this method." +
                                                 " Mutually exclusive with queryParameters.");
    }

    protected StringValueRule queryParametersKey()
    {
        return string("queryParameters")
                                        .description("Detailed information about any query parameters needed by this method. " +
                                                     "Mutually exclusive with queryString.");
    }


    protected Rule responses()
    {
        return objectType()
                           .with(field(responseCodes(), response()));
    }

    protected ObjectRule response()
    {
        return objectType()
                           .with(displayNameField())
                           .with(descriptionField())
                           .with(headersField())
                           .with(bodyField());
    }

    protected Rule body()
    {
        return objectType().with(field(regex("[A-z-_]+\\/[A-z-_]+"), mimeType()));
    }

    protected ObjectRule mimeType()
    {
        return objectType()
                           .with(field(string("schema"), stringType()))
                           .with(field(string("example"), stringType()));
    }

    protected Rule parameters()
    {
        return objectType().with(field(stringType(), parameter()));
    }

    /**
     * Describes the rule for a parameter.
     */
    protected abstract Rule parameter();


    protected KeyValueRule securitySchemesField()
    {
        return field(securitySchemesKey(), anyOf(array(securitySchemes()), securitySchemes()));
    }

    protected StringValueRule securitySchemesKey()
    {
        return string(SECURITY_SCHEMES_KEY_NAME).description("Declarations of security schemes for use within this API.");
    }


    protected KeyValueRule schemasField()
    {
        return field(schemasKey(), anyOf(array(schemas()), schemas()));
    }

    protected StringValueRule schemasKey()
    {
        return string("schemas")
                                .description("Alias for the equivalent \"types\" property, for compatibility " +
                                             "with RAML 0.8. Deprecated - API definitions should use the \"types\" property, " +
                                             "as the \"schemas\" alias for that property name may be removed in a future RAML version. " +
                                             "The \"types\" property allows for XML and JSON schemas.");
    }

    protected KeyValueRule resourceTypesField()
    {
        return field(resourceTypesKey(), anyOf(array(resourceTypes()), resourceTypes()));
    }

    protected StringValueRule resourceTypesKey()
    {
        return string(RESOURCE_TYPES_KEY_NAME).description("Declarations of resource types for use within this API.");
    }

    protected KeyValueRule traitsField()
    {
        return field(traitsKey(), traits());
    }

    protected StringValueRule traitsKey()
    {
        return string(TRAITS_KEY_NAME).description("Declarations of traits for use within this API.");
    }

    protected KeyValueRule protocolsField()
    {
        return field(protocolsKey(), protocols());
    }

    protected StringValueRule protocolsKey()
    {
        return string("protocols").description("The protocols supported by the API.");
    }

    protected KeyValueRule bodyField()
    {
        return field(bodyKey(), body());
    }

    protected StringValueRule bodyKey()
    {
        return string("body")
                             .description("Some methods admit request bodies, which are described by this property.");
    }

    protected KeyValueRule headersField()
    {
        return field(headersKey(), parameters());
    }

    protected StringValueRule headersKey()
    {
        return string("headers")
                                .description("Detailed information about any request headers needed by this method.");
    }

    protected KeyValueRule descriptionField()
    {
        return field(descriptionKey(), stringType());
    }

    protected KeyValueRule displayNameField()
    {
        return field(displayNameKey(), stringType());
    }

    protected KeyValueRule securedByField()
    {
        return field(securedByKey(), array(anyOf(stringType().then(new NodeReferenceFactory(SecuritySchemeRefNode.class)), any(), nullValue())));
    }

    protected KeyValueRule isField()
    {
        return field(isKey(), array(anyTypeReference(TRAITS_KEY_NAME, TraitRefNode.class, ParametrizedTraitRefNode.class)));
    }

    protected KeyValueRule resourceTypeReferenceField()
    {
        return field(typeKey(), anyTypeReference(RESOURCE_TYPES_KEY_NAME, ResourceTypeRefNode.class, ParametrizedResourceTypeRefNode.class));
    }

    protected Rule anyTypeReference(String referenceKey, Class<? extends Node> simpleClass, Class<? extends Node> parametrisedClass)
    {
        final KeyValueRule paramsRule = field(stringType(), stringType());
        final KeyValueRule typeWithParams = field(stringType(), objectType().with(paramsRule));
        final NodeFactory factory = new NodeReferenceFactory(simpleClass);
        final NodeFactory parametrisedFactory = new NodeReferenceFactory(parametrisedClass);
        return anyOf(new NodeReferenceRule(referenceKey).then(factory), new ParametrizedNodeReferenceRule(referenceKey).with(typeWithParams).then(parametrisedFactory));
    }


    protected StringValueRule titleKey()
    {
        return string("title");
    }


    // Repeated keys

    protected RegexValueRule resourceKey()
    {
        return regex("/.*")
                           .label("/Resource")
                           .suggest("/<cursor>")
                           .description("The resources of the API, identified as relative URIs that begin with a slash (/). " +
                                        "Every property whose key begins with a slash (/), and is either at the root of the API definition " +
                                        "or is the child property of a resource property, is a resource property, e.g.: /users, /{groupId}, etc");
    }

    protected StringValueRule usesKey()
    {
        return string(USES_KEY_NAME).description("Importing libraries.");
    }


    protected StringValueRule uriParametersKey()
    {
        return string("uriParameters").description("Detailed information about any URI parameters of this resource");
    }

    protected StringValueRule securedByKey()
    {
        return string("securedBy");
    }

    protected StringValueRule typeKey()
    {
        return string("type")
                             .description("The resource type which this resource inherits.");
    }

    protected StringValueRule isKey()
    {
        return string("is");
    }

    protected StringValueRule displayNameKey()
    {
        return string("displayName")
                                    .description("An alternate, human-friendly name for the method (in the resource's context).");
    }


    protected StringValueRule descriptionKey()
    {
        return string("description").description("A longer, human-friendly description of the API");
    }

    protected StringValueRule documentationKey()
    {
        return string("documentation")
                                      .description("Additional overall documentation for the API.");
    }

    protected StringValueRule mediaTypeKey()
    {
        return string("mediaType")
                                  .description("The default media type to use for request and response bodies (payloads), e.g. \"application/json\".");
    }

    protected StringValueRule baseUriParametersKey()
    {
        return string("baseUriParameters").description("Named parameters used in the baseUri (template).");
    }

    protected StringValueRule baseUriKey()
    {
        return string("baseUri").description("A URI that's to be used as the base of all the resources' URIs." +
                                             " Often used as the base of the URL of each resource, containing the location of the API. " +
                                             "Can be a template URI.");
    }

    protected StringValueRule versionKey()
    {
        return string("version").description("The version of the API, e.g. \"v1\".");
    }


    // Enum of values

    protected AnyOfRule anyMethod()
    {
        return anyOf(
                string("get"),
                string("patch"),
                string("put"),
                string("post"),
                string("delete"),
                string("options"),
                string("head"));
    }

    protected AnyOfRule anyOptionalMethod()
    {
        return anyOf(string("get?"), string("patch?"), string("put?"), string("post?"), string("delete?"), string("options?"), string("head?"));
    }

    protected AnyOfRule anyBuiltinType()
    {
        return anyOf(string("string"), string("number"), string("integer"), string("boolean"), string("date"), string("file"));
    }

    protected AnyOfRule anyResourceTypeMethod()
    {
        return anyOf(anyMethod(), anyOptionalMethod());
    }

    protected Rule protocols()
    {
        return array(anyOf(string("HTTP"), string("HTTPS")));
    }

    protected Rule responseCodes()
    {
        return range(Range.closed(new BigInteger("100"), new BigInteger("599")));
    }
}
