/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.config.entities;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: May 6, 2003
 * Time: 11:55:34 AM
 * To change this template use Options | File Templates.
 */
public class InterceptorConfig implements Parameterizable {
    //~ Instance fields ////////////////////////////////////////////////////////

    Class clazz;
    Map params;
    String name;

    //~ Constructors ///////////////////////////////////////////////////////////

    public InterceptorConfig() {
    }

    public InterceptorConfig(String name, Class clazz, Map params) {
        this.name = name;
        this.clazz = clazz;
        this.params = params;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setParams(Map params) {
        this.params = params;
    }

    public Map getParams() {
        if (params == null) {
            params = new HashMap();
        }

        return params;
    }

    public void addParam(String name, Object value) {
        getParams().put(name, value);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InterceptorConfig)) {
            return false;
        }

        final InterceptorConfig interceptorConfig = (InterceptorConfig) o;

        if ((clazz != null) ? (!clazz.equals(interceptorConfig.clazz)) : (interceptorConfig.clazz != null)) {
            return false;
        }

        if ((name != null) ? (!name.equals(interceptorConfig.name)) : (interceptorConfig.name != null)) {
            return false;
        }

        if ((params != null) ? (!params.equals(interceptorConfig.params)) : (interceptorConfig.params != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((name != null) ? name.hashCode() : 0);
        result = (29 * result) + ((clazz != null) ? clazz.hashCode() : 0);
        result = (29 * result) + ((params != null) ? params.hashCode() : 0);

        return result;
    }
}