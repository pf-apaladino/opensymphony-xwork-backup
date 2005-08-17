/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.config.entities;

import com.opensymphony.xwork.interceptor.Interceptor;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Contains everything needed to configure and execute an action:
 * <ul>
 * <li>methodName - the method name to execute on the action. If this is null, the Action will be cast to the Action
 * Interface and the execute() method called</li>
 * <li>clazz - the class name for the action</li>
 * <li>params - the params to be set for this action just before execution</li>
 * <li>results - the result map {String -> View class}</li>
 * <li>resultParameters - params for results {String -> Map}</li>
 * <li>typeConverter - the Ognl TypeConverter to use when getting/setting properties</li>
 * </ul>
 *
 * @author $Author$
 * @version $Revision$
 */
public class ActionConfig implements InterceptorListHolder, Parameterizable, Serializable {
    //~ Instance fields ////////////////////////////////////////////////////////

    protected List externalRefs;
    protected List interceptors;
    protected Map params;
    protected Map results;
    protected List exceptionMappings;
    protected Method method;
    protected String className;
    protected String methodName;
    protected String packageName;
    protected Class cachedClass;

    //~ Constructors ///////////////////////////////////////////////////////////

    public ActionConfig() {
        params = new TreeMap();
        results = new TreeMap();
        interceptors = new ArrayList();
        externalRefs = new ArrayList();
        exceptionMappings = new ArrayList();
    }

    //Helper constuctor to maintain backward compatibility with objects that create ActionConfigs
    //TODO this should be removed if these changes are rolled in to xwork CVS
    public ActionConfig(String methodName, Class clazz, Map parameters, Map results, List interceptors) {
        this(methodName, clazz.getName(), parameters, results, interceptors);
    }

    public ActionConfig(String methodName, Class clazz, Map parameters, Map results, List interceptors, List exceptionMappings) {
        this(methodName, clazz.getName(), parameters, results, interceptors, exceptionMappings);
    }

    public ActionConfig(String methodName, String className, Map parameters, Map results, List interceptors) {
        this(methodName, className, parameters, results, interceptors, Collections.EMPTY_LIST, new String());
    }

    public ActionConfig(String methodName, String className, Map parameters, Map results, List interceptors, List exceptionMappings) {
        this(methodName, className, parameters, results, interceptors, Collections.EMPTY_LIST, exceptionMappings, new String());
    }

    //TODO If this is commited to CVS we should put the package arg at the front of the ctor and fix
    //code that uses it
    public ActionConfig(String methodName, String className, Map parameters, Map results, List interceptors, List externalRefs, String packageName) {
        this(methodName, className, parameters, results, interceptors, externalRefs, Collections.EMPTY_LIST, packageName);
    }

    public ActionConfig(String methodName, String className, Map parameters, Map results, List interceptors, List externalRefs, List exceptionMappings, String packageName) {
        this.methodName = methodName;
        this.interceptors = interceptors;
        this.params = parameters;
        this.results = results;
        this.className = className;
        this.externalRefs = externalRefs;
        this.exceptionMappings = exceptionMappings;
        this.packageName = packageName;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public List getExternalRefs() {
        return externalRefs;
    }

    public List getExceptionMappings() {
        return exceptionMappings;
    }

    public List getInterceptors() {
        if (interceptors == null) {
            interceptors = new ArrayList();
        }

        return interceptors;
    }

    /**
     * Returns cached instance of the action method or null if method name was not specified
     *
     * @param actionClass - passed in to check that the Action class hasn't changed since last time.
     *                    This is really a hack to get around a problem with proxied Actions in Spring.
     * @return cached instance of the action method or null if method name was not specified
     */
    public Method getMethod(Class actionClass) throws NoSuchMethodException {
        if (!actionClass.equals(cachedClass)) {
            cachedClass = actionClass;
            method = null;
        }
        if (method != null) {
            return method;
        }

        if (methodName != null) {
            try {
                method = cachedClass.getMethod(methodName, new Class[0]);
            } catch (NoSuchMethodException e) {
                try {
                    method = cachedClass.getMethod("do" + String.valueOf(methodName.charAt(0)).toUpperCase() + methodName.substring(1), new Class[0]);
                } catch (NoSuchMethodException e1) {
                    throw e;
                }
            }
        } else // return default execute() method if method name is not specified
        {
            method = cachedClass.getMethod("execute", new Class[0]);
        }

        return method;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    /**
     * Returns name of the action method
     *
     * @return name of the method to execute
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * @param packageName The packageName to set.
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return Returns the packageName.
     */
    public String getPackageName() {
        return packageName;
    }

    public void setParams(Map params) {
        this.params = params;
    }

    public Map getParams() {
        if (params == null) {
            params = new TreeMap();
        }

        return params;
    }

    public void setResults(Map results) {
        this.results = results;
    }

    public Map getResults() {
        if (results == null) {
            results = new TreeMap();
        }

        return results;
    }

    public void addExternalRef(ExternalReference reference) {
        getExternalRefs().add(reference);
    }

    public void addExternalRefs(List externalRefs) {
        getExternalRefs().addAll(externalRefs);
    }

    public void addExceptionMapping(ExceptionMappingConfig exceptionMapping) {
        getExceptionMappings().add(exceptionMapping);
    }

    public void addExceptionMappings(List exceptionMappings) {
        getExceptionMappings().addAll(exceptionMappings);
    }

    public void addInterceptor(Interceptor interceptor) {
        getInterceptors().add(interceptor);
    }

    public void addInterceptors(List interceptors) {
        getInterceptors().addAll(interceptors);
    }

    public void addParam(String name, Object value) {
        getParams().put(name, value);
    }

    public void addResultConfig(ResultConfig resultConfig) {
        getResults().put(resultConfig.getName(), resultConfig);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ActionConfig)) {
            return false;
        }

        final ActionConfig actionConfig = (ActionConfig) o;

        if ((className != null) ? (!className.equals(actionConfig.className)) : (actionConfig.className != null)) {
            return false;
        }

        if ((interceptors != null) ? (!interceptors.equals(actionConfig.interceptors)) : (actionConfig.interceptors != null)) {
            return false;
        }

        if ((method != null) ? (!method.equals(actionConfig.method)) : (actionConfig.method != null)) {
            return false;
        }

        if ((methodName != null) ? (!methodName.equals(actionConfig.methodName)) : (actionConfig.methodName != null)) {
            return false;
        }

        if ((params != null) ? (!params.equals(actionConfig.params)) : (actionConfig.params != null)) {
            return false;
        }

        if ((results != null) ? (!results.equals(actionConfig.results)) : (actionConfig.results != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((interceptors != null) ? interceptors.hashCode() : 0);
        result = (29 * result) + ((params != null) ? params.hashCode() : 0);
        result = (29 * result) + ((results != null) ? results.hashCode() : 0);
        result = (29 * result) + ((method != null) ? method.hashCode() : 0);
        result = (29 * result) + ((methodName != null) ? methodName.hashCode() : 0);
        result = (29 * result) + ((className != null) ? className.hashCode() : 0);

        return result;
    }

    public String toString() {
        return "{ActionConfig " + className + ((methodName != null) ? ("." + methodName + "()") : "") + "}";
    }
}
