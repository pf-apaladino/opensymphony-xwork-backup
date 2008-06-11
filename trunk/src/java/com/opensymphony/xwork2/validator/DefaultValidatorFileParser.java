/*
 * Copyright (c) 2002-2006 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.validator;

import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.util.DomHelper;
import com.opensymphony.xwork2.util.logging.Logger;
import com.opensymphony.xwork2.util.logging.LoggerFactory;
import com.opensymphony.xwork2.XWorkException;
import com.opensymphony.xwork2.config.ConfigurationException;
import com.opensymphony.xwork2.config.providers.XmlHelper;
import com.opensymphony.xwork2.inject.Inject;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.util.*;


/**
 * Parse the validation file. (eg. MyAction-validation.xml, MyAction-actionAlias-validation.xml)
 * to return a List of ValidatorConfig encapsulating the validator information.
 *
 * @author Jason Carreira
 * @author James House
 * @author tm_jee ( tm_jee (at) yahoo.co.uk )
 * @author Rob Harrop
 * @author Rene Gielen
 * @author Martin Gilday
 * 
 * @see com.opensymphony.xwork2.validator.ValidatorConfig
 */
public class DefaultValidatorFileParser implements ValidatorFileParser {

    private static Logger LOG = LoggerFactory.getLogger(DefaultValidatorFileParser.class);

    static final String MULTI_TEXTVALUE_SEPARATOR = " ";
    private ObjectFactory objectFactory;

    @Inject
    public void setObjectFactory(ObjectFactory fac) {
        this.objectFactory = fac;
    }

    public List<ValidatorConfig> parseActionValidatorConfigs(ValidatorFactory validatorFactory, InputStream is, final String resourceName) {
        List validatorCfgs = new ArrayList();
        Document doc = null;

        InputSource in = new InputSource(is);
        in.setSystemId(resourceName);

        Map dtdMappings = new HashMap();
        dtdMappings.put("-//OpenSymphony Group//XWork Validator 1.0//EN", "xwork-validator-1.0.dtd");
        dtdMappings.put("-//OpenSymphony Group//XWork Validator 1.0.2//EN", "xwork-validator-1.0.2.dtd");
        dtdMappings.put("-//OpenSymphony Group//XWork Validator 1.0.3//EN", "xwork-validator-1.0.3.dtd");

        doc = DomHelper.parse(in, dtdMappings);

        if (doc != null) {
            NodeList fieldNodes = doc.getElementsByTagName("field");

            // BUG: xw-305: Let validator be parsed first and hence added to 
            // the beginning of list and therefore evaluated first, so short-circuting
            // it will not cause field-level validator to be kicked off.
            {
                NodeList validatorNodes = doc.getElementsByTagName("validator");
                addValidatorConfigs(validatorFactory, validatorNodes, new HashMap(), validatorCfgs);
            }

            for (int i = 0; i < fieldNodes.getLength(); i++) {
                Element fieldElement = (Element) fieldNodes.item(i);
                String fieldName = fieldElement.getAttribute("name");
                Map extraParams = new HashMap();
                extraParams.put("fieldName", fieldName);

                NodeList validatorNodes = fieldElement.getElementsByTagName("field-validator");
                addValidatorConfigs(validatorFactory, validatorNodes, extraParams, validatorCfgs);
            }
        }

        return validatorCfgs;
    }


    public void parseValidatorDefinitions(Map<String, String> validators, InputStream is, String resourceName) {

        InputSource in = new InputSource(is);
        in.setSystemId(resourceName);

        Map dtdMappings = new HashMap();
        dtdMappings.put("-//OpenSymphony Group//XWork Validator Config 1.0//EN", "xwork-validator-config-1.0.dtd");

        Document doc = DomHelper.parse(in, dtdMappings);

        if (doc != null) {
            NodeList nodes = doc.getElementsByTagName("validator");

            for (int i = 0; i < nodes.getLength(); i++) {
                Element validatorElement = (Element) nodes.item(i);
                String name = validatorElement.getAttribute("name");
                String className = validatorElement.getAttribute("class");

                try {
                    // catch any problems here
                    objectFactory.buildValidator(className, new HashMap(), null);
                    validators.put(name, className);
                } catch (Exception e) {
                    throw new ConfigurationException("Unable to load validator class " + className, e, validatorElement);
                }
            }
        }
    }

    /**
     * Extract trimmed text value from the given DOM element, ignoring XML comments. Appends all CharacterData nodes
     * and EntityReference nodes into a single String value, excluding Comment nodes.
     * This method is based on a method originally found in DomUtils class of Springframework.
     *
     * @see org.w3c.dom.CharacterData
     * @see org.w3c.dom.EntityReference
     * @see org.w3c.dom.Comment
     */
    public static String getTextValue(Element valueEle) {
        StringBuffer value = new StringBuffer();
        NodeList nl = valueEle.getChildNodes();
        boolean firstCDataFound = false;
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                final String nodeValue = item.getNodeValue();
                if (nodeValue != null) {
                    if (firstCDataFound) {
                        value.append(MULTI_TEXTVALUE_SEPARATOR);
                    } else {
                        firstCDataFound = true;
                    }
                    value.append(nodeValue.trim());
                }
            }
        }
        return value.toString().trim();
    }

    private void addValidatorConfigs(ValidatorFactory factory, NodeList validatorNodes, Map extraParams, List validatorCfgs) {
        for (int j = 0; j < validatorNodes.getLength(); j++) {
            Element validatorElement = (Element) validatorNodes.item(j);
            String validatorType = validatorElement.getAttribute("type");
            Map params = new HashMap(extraParams);

            params.putAll(XmlHelper.getParams(validatorElement));

            // ensure that the type is valid...
            try {
                factory.lookupRegisteredValidatorType(validatorType);
            } catch (IllegalArgumentException ex) {
                throw new ConfigurationException("Invalid validation type: " + validatorType, validatorElement);
            }

            ValidatorConfig.Builder vCfg = new ValidatorConfig.Builder(validatorType)
                    .addParams(params)
                    .location(DomHelper.getLocationObject(validatorElement))
                    .shortCircuit(Boolean.valueOf(validatorElement.getAttribute("short-circuit")).booleanValue());

            NodeList messageNodes = validatorElement.getElementsByTagName("message");
            Element messageElement = (Element) messageNodes.item(0);

            final Node defaultMessageNode = messageElement.getFirstChild();
            String defaultMessage = (defaultMessageNode == null) ? "" : defaultMessageNode.getNodeValue();
            vCfg.defaultMessage(defaultMessage);

            Map messageParams = XmlHelper.getParams(messageElement);
            String key = messageElement.getAttribute("key");


            if ((key != null) && (key.trim().length() > 0)) {
                vCfg.messageKey(key);

                // Get the default message when pattern 2 is used. We are only interested in the
                // i18n message parameters when an i18n message key is specified.
                // pattern 1:
                // <message key="someKey">Default message</message>
                // pattern 2:
                // <message key="someKey">
                //     <param name="1">'param1'</param>
                //     <param name="2">'param2'</param>
                //     <param name="defaultMessage>The Default Message</param>
                // </message>

                if (messageParams.containsKey("defaultMessage")) {
                    vCfg.defaultMessage((String) messageParams.get("defaultMessage"));
                }

                // Sort the message param. those with keys as '1', '2', '3' etc. (numeric values)
                // are i18n message parameter, others are excluded.
                TreeMap sortedMessageParameters = new TreeMap();
                for (Iterator i = messageParams.entrySet().iterator(); i.hasNext();) {
                    Map.Entry messageParamEntry = (Map.Entry) i.next();
                    try {
                        int _order = Integer.parseInt((String) messageParamEntry.getKey());
                        sortedMessageParameters.put(new Integer(_order), messageParamEntry.getValue());
                    }
                    catch (NumberFormatException e) {
                        // ignore if its not numeric.
                    }
                }
                vCfg.messageParams((String[]) sortedMessageParameters.values().toArray(new String[0]));
            } else {
                if (messageParams != null && (messageParams.size() > 0)) {
                    // we are i18n message parameters defined but no i18n message,
                    // let's warn the user.
                    LOG.warn("validator of type ["+validatorType+"] have i18n message parameters defined but no i18n message key, it's parameters will be ignored");
                }
            }

            validatorCfgs.add(vCfg.build());
        }
    }
}
