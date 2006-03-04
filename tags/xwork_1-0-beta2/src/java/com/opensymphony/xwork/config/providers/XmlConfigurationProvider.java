/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.config.providers;

import com.opensymphony.util.ClassLoaderUtil;
import com.opensymphony.util.FileManager;
import com.opensymphony.util.TextUtils;

import com.opensymphony.xwork.config.Configuration;
import com.opensymphony.xwork.config.ConfigurationException;
import com.opensymphony.xwork.config.ConfigurationProvider;
import com.opensymphony.xwork.config.ConfigurationUtil;
import com.opensymphony.xwork.config.entities.ActionConfig;
import com.opensymphony.xwork.config.entities.InterceptorConfig;
import com.opensymphony.xwork.config.entities.InterceptorStackConfig;
import com.opensymphony.xwork.config.entities.PackageConfig;
import com.opensymphony.xwork.config.entities.ResultConfig;
import com.opensymphony.xwork.config.entities.ResultTypeConfig;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.IOException;
import java.io.InputStream;

import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Looks in the classpath for "xwork.xml" and uses it for the XWork configuration.
 *
 * @author $Author$
 * @version $Revision$
 */
public class XmlConfigurationProvider implements ConfigurationProvider {
    //~ Static fields/initializers /////////////////////////////////////////////

    private static final Log LOG = LogFactory.getLog(XmlConfigurationProvider.class);

    //~ Instance fields ////////////////////////////////////////////////////////

    private Configuration configuration;
    private Set includedFileNames = new HashSet();
    private String configFileName = "xwork.xml";

    //~ Constructors ///////////////////////////////////////////////////////////

    public XmlConfigurationProvider() {
    }

    public XmlConfigurationProvider(String filename) {
        this.configFileName = filename;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public void destroy() {
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof XmlConfigurationProvider)) {
            return false;
        }

        final XmlConfigurationProvider xmlConfigurationProvider = (XmlConfigurationProvider) o;

        if ((configFileName != null) ? (!configFileName.equals(xmlConfigurationProvider.configFileName)) : (xmlConfigurationProvider.configFileName != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return ((configFileName != null) ? configFileName.hashCode() : 0);
    }

    public void init(Configuration configuration) {
        this.configuration = configuration;
        includedFileNames.clear();

        DocumentBuilder db;

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            dbf.setNamespaceAware(true);

            db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                        if ("-//OpenSymphony Group//XWork 1.0//EN".equals(publicId)) {
                            return new InputSource(ClassLoaderUtil.getResourceAsStream("xwork-1.0.dtd", XmlConfigurationProvider.class));
                        }

                        return null;
                    }
                });
            db.setErrorHandler(new ErrorHandler() {
                    public void warning(SAXParseException exception) throws SAXException {
                    }

                    public void error(SAXParseException exception) throws SAXException {
                        LOG.error(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
                        throw exception;
                    }

                    public void fatalError(SAXParseException exception) throws SAXException {
                        LOG.fatal(exception.getMessage() + " at (" + exception.getLineNumber() + ":" + exception.getColumnNumber() + ")");
                        throw exception;
                    }
                });
            loadConfigurationFile(configFileName, db);
        } catch (Exception e) {
            LOG.fatal("Could not load XWork configuration file, failing");
            throw new ConfigurationException("Error loading configuration file " + configFileName, e);
        }
    }

    /**
    * Tells whether the ConfigurationProvider should reload its configuration. This method should only be called
    * if ConfigurationManager.isReloadingConfigs() is true.
    * @return true if the file has been changed since the last time we read it
    */
    public boolean needsReload() {
        boolean needsReload = FileManager.fileNeedsReloading(configFileName);
        Iterator fileNameIterator = includedFileNames.iterator();

        while (!needsReload && (fileNameIterator.hasNext())) {
            String fileName = (String) fileNameIterator.next();
            needsReload = FileManager.fileNeedsReloading(fileName);
        }

        return needsReload;
    }

    protected InputStream getInputStream(String fileName) {
        InputStream is = FileManager.loadFile(fileName, this.getClass());

        return is;
    }

    protected void addAction(Element actionElement, PackageConfig packageContext) throws ConfigurationException {
        String name = actionElement.getAttribute("name");
        String className = actionElement.getAttribute("class");
        String methodName = actionElement.getAttribute("method");

        //methodName should be null if it's not set
        methodName = (methodName.trim().length() > 0) ? methodName.trim() : null;

        Class clazz = null;

        try {
            clazz = ClassLoaderUtil.loadClass(className, XmlConfigurationProvider.class);
        } catch (ClassNotFoundException e) {
            LOG.error("Action class [" + className + "] not found, skipping action [" + name + "]", e);

            return;
        }

        HashMap actionParams = XmlHelper.getParams(actionElement);

        Map results;

        try {
            results = buildResults(actionElement, packageContext);
        } catch (ConfigurationException e) {
            throw new ConfigurationException("Error building results for action " + name + " in namespace " + packageContext.getNamespace(), e);
        }

        List interceptorList = buildInterceptorList(actionElement, packageContext);

        ActionConfig actionConfig = new ActionConfig(methodName, clazz, actionParams, results, interceptorList);
        packageContext.addActionConfig(name, actionConfig);
    }

    /**
    * Create a PackageConfig from an XML element representing it.
    */
    protected void addPackage(Element packageElement) throws ConfigurationException {
        PackageConfig newPackage = buildPackageContext(packageElement);

        // add result types (and default result) to this package
        addResultTypes(newPackage, packageElement);

        // load the interceptors and interceptor stacks for this package
        loadInterceptors(newPackage, packageElement);

        // load the default interceptor reference for this package
        loadDefaultInterceptorRef(newPackage, packageElement);

        // load the global result list for this package
        loadGlobalResults(newPackage, packageElement);

        // get actions
        NodeList actionList = packageElement.getElementsByTagName("action");

        for (int i = 0; i < actionList.getLength(); i++) {
            Element actionElement = (Element) actionList.item(i);
            addAction(actionElement, newPackage);
        }

        configuration.addPackageConfig(newPackage.getName(), newPackage);
    }

    protected void addResultTypes(PackageConfig packageContext, Element element) {
        NodeList resultTypeList = element.getElementsByTagName("result-type");

        for (int i = 0; i < resultTypeList.getLength(); i++) {
            Element resultTypeElement = (Element) resultTypeList.item(i);
            String name = resultTypeElement.getAttribute("name");
            String className = resultTypeElement.getAttribute("class");
            String def = resultTypeElement.getAttribute("default");

            try {
                Class clazz = ClassLoaderUtil.loadClass(className, XmlConfigurationProvider.class);
                ResultTypeConfig resultType = new ResultTypeConfig(name, clazz);
                packageContext.addResultTypeConfig(resultType);

                // set the default result type
                if ("true".equals(def)) {
                    packageContext.setDefaultResultType(name);
                }
            } catch (ClassNotFoundException e) {
                LOG.error("Result class [" + className + "] doesn't exist, ignoring");
            }
        }
    }

    protected List buildInterceptorList(Element element, PackageConfig context) throws ConfigurationException {
        List interceptorList = new ArrayList();
        NodeList interceptorRefList = element.getElementsByTagName("interceptor-ref");

        for (int i = 0; i < interceptorRefList.getLength(); i++) {
            Element interceptorRefElement = (Element) interceptorRefList.item(i);

            if (interceptorRefElement.getParentNode().equals(element)) {
                List interceptors = lookupInterceptorReference(context, interceptorRefElement);
                interceptorList.addAll(interceptors);
            }
        }

        return interceptorList;
    }

    /**
    * This method builds a package context by looking for the parents of this new package.
    *
    * If no parents are found, it will return a root package.
    */
    protected PackageConfig buildPackageContext(Element packageElement) {
        String parent = packageElement.getAttribute("extends");
        String abstractVal = packageElement.getAttribute("abstract");
        boolean isAbstract = Boolean.valueOf(abstractVal).booleanValue();
        String name = TextUtils.noNull(packageElement.getAttribute("name"));
        String namespace = TextUtils.noNull(packageElement.getAttribute("namespace"));

        if (!TextUtils.stringSet(TextUtils.noNull(parent))) { // no parents

            return new PackageConfig(name, namespace, isAbstract);
        } else { // has parents, let's look it up

            List parents = ConfigurationUtil.buildParentsFromString(configuration, parent);

            if (parents.size() <= 0) {
                LOG.error("Unable to find parent packages " + parent);

                return new PackageConfig(name, namespace, isAbstract);
            } else {
                return new PackageConfig(name, namespace, isAbstract, parents);
            }
        }
    }

    /**
    * Build a map of ResultConfig objects from below a given XML element.
    */
    protected Map buildResults(Element element, PackageConfig packageContext) {
        NodeList resultEls = element.getElementsByTagName("result");

        Map results = new HashMap();

        for (int i = 0; i < resultEls.getLength(); i++) {
            Element resultElement = (Element) resultEls.item(i);

            if (resultElement.getParentNode().equals(element)) {
                String resultName = resultElement.getAttribute("name");
                String resultType = resultElement.getAttribute("type");

                if (!TextUtils.stringSet(resultType)) {
                    resultType = packageContext.getFullDefaultResultType();
                }

                ResultTypeConfig config = (ResultTypeConfig) packageContext.getAllResultTypeConfigs().get(resultType);

                if (config == null) {
                    throw new ConfigurationException("There is no result type defined for type '" + resultType + "' mapped with name '" + resultName + "'");
                }

                Class resultClass = config.getClazz();

                // invalid result type specified in result definition
                if (resultClass == null) {
                    LOG.error("Result type '" + resultType + "' is invalid. Modify your xwork.xml file.");
                }

                HashMap params = XmlHelper.getParams(resultElement);

                if (params.size() == 0) // maybe we just have a body - therefore a default parameter
                 {
                    // if <result ...>something</result> then we add a parameter of 'something' as this is the most used result param
                    if ((resultElement.getChildNodes().getLength() == 1) && (resultElement.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE)) {
                        params = new HashMap();

                        try {
                            String paramName = (String) resultClass.getField("DEFAULT_PARAM").get(null);
                            params.put(paramName, resultElement.getChildNodes().item(0).getNodeValue());
                        } catch (Throwable t) {
                        }
                    }
                }

                ResultConfig resultConfig = new ResultConfig(resultName, resultClass, params);
                results.put(resultConfig.getName(), resultConfig);
            }
        }

        return results;
    }

    protected void loadDefaultInterceptorRef(PackageConfig packageContext, Element element) {
        NodeList resultTypeList = element.getElementsByTagName("default-interceptor-ref");

        if (resultTypeList.getLength() > 0) {
            Element defaultRefElement = (Element) resultTypeList.item(0);
            packageContext.setDefaultInterceptorRef(defaultRefElement.getAttribute("name"));
        }
    }

    /**
    * Load all of the global results for this package from the XML element.
    */
    protected void loadGlobalResults(PackageConfig packageContext, Element packageElement) {
        NodeList globalResultList = packageElement.getElementsByTagName("global-results");

        if (globalResultList.getLength() > 0) {
            Element globalResultElement = (Element) globalResultList.item(0);
            Map results = buildResults(globalResultElement, packageContext);
            packageContext.addGlobalResultConfigs(results);
        }
    }

    //    protected void loadIncludes(Element rootElement, DocumentBuilder db) throws Exception {
    //        NodeList includeList = rootElement.getElementsByTagName("include");
    //
    //        for (int i = 0; i < includeList.getLength(); i++) {
    //            Element includeElement = (Element) includeList.item(i);
    //            String fileName = includeElement.getAttribute("file");
    //            includedFileNames.add(fileName);
    //            loadConfigurationFile(fileName, db);
    //        }
    //    }
    protected InterceptorStackConfig loadInterceptorStack(Element element, PackageConfig context) throws ConfigurationException {
        String name = element.getAttribute("name");

        InterceptorStackConfig config = new InterceptorStackConfig(name);
        NodeList interceptorRefList = element.getElementsByTagName("interceptor-ref");

        for (int j = 0; j < interceptorRefList.getLength(); j++) {
            Element interceptorRefElement = (Element) interceptorRefList.item(j);
            List interceptors = lookupInterceptorReference(context, interceptorRefElement);
            config.addInterceptors(interceptors);
        }

        return config;
    }

    protected void loadInterceptorStacks(Element element, PackageConfig context) throws ConfigurationException {
        NodeList interceptorStackList = element.getElementsByTagName("interceptor-stack");

        for (int i = 0; i < interceptorStackList.getLength(); i++) {
            Element interceptorStackElement = (Element) interceptorStackList.item(i);

            InterceptorStackConfig config = loadInterceptorStack(interceptorStackElement, context);

            context.addInterceptorStackConfig(config);
        }
    }

    protected void loadInterceptors(PackageConfig context, Element element) throws ConfigurationException {
        NodeList interceptorList = element.getElementsByTagName("interceptor");

        for (int i = 0; i < interceptorList.getLength(); i++) {
            Element interceptorElement = (Element) interceptorList.item(i);
            String name = interceptorElement.getAttribute("name");
            String className = interceptorElement.getAttribute("class");
            Class clazz = null;

            try {
                clazz = ClassLoaderUtil.loadClass(className, XmlConfigurationProvider.class);
            } catch (ClassNotFoundException e) {
                String s = "Unable to load class " + className + " for interceptor name " + name + ". This interceptor will not be available.";
                LOG.error(s);
                throw new ConfigurationException(s, e);
            }

            Map params = XmlHelper.getParams(interceptorElement);
            InterceptorConfig config = new InterceptorConfig(name, clazz, params);
            context.addInterceptorConfig(config);
        }

        loadInterceptorStacks(element, context);
    }

    //    protected void loadPackages(Element rootElement) throws ConfigurationException {
    //        NodeList packageList = rootElement.getElementsByTagName("package");
    //
    //        for (int i = 0; i < packageList.getLength(); i++) {
    //            Element packageElement = (Element) packageList.item(i);
    //            addPackage(packageElement);
    //        }
    //    }
    private void loadConfigurationFile(String fileName, DocumentBuilder db) {
        if (!includedFileNames.contains(fileName)) {
            includedFileNames.add(fileName);

            Document doc = null;
            InputStream is = null;

            try {
                is = getInputStream(fileName);

                if (is == null) {
                    throw new Exception("Could not open file " + fileName);
                }

                doc = db.parse(is);
            } catch (Exception e) {
                final String s = "Caught exception while loading file " + fileName;
                LOG.error(s, e);
                throw new ConfigurationException(s, e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOG.error("Unable to close input stream", e);
                    }
                }
            }

            Element rootElement = doc.getDocumentElement();
            NodeList children = rootElement.getChildNodes();
            int childSize = children.getLength();

            for (int i = 0; i < childSize; i++) {
                Node childNode = children.item(i);

                if (childNode instanceof Element) {
                    Element child = (Element) childNode;

                    final String nodeName = child.getNodeName();

                    if (nodeName.equals("package")) {
                        addPackage(child);
                    } else if (nodeName.equals("include")) {
                        String includeFileName = child.getAttribute("file");
                        loadConfigurationFile(includeFileName, db);
                    }
                }
            }
        }

        //        loadPackages(rootElement);
        //        loadIncludes(rootElement, db);
    }

    /**
    * Looks up the Interceptor Class from the interceptor-ref name and creates an instance, which is added to the
    * provided List, or, if this is a ref to a stack, it adds the Interceptor instances from the List to this stack.
    * @param interceptorRefElement Element to pull interceptor ref data from
    * @param context The PackageConfig to lookup the interceptor from
    * @return A list of Interceptor objects
    */
    private List lookupInterceptorReference(PackageConfig context, Element interceptorRefElement) throws ConfigurationException {
        String refName = interceptorRefElement.getAttribute("name");
        Map refParams = XmlHelper.getParams(interceptorRefElement);

        return InterceptorBuilder.constructInterceptorReference(context, refName, refParams);
    }
}