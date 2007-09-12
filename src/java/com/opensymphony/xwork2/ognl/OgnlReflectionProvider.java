package com.opensymphony.xwork2.ognl;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import com.opensymphony.xwork2.util.reflection.ReflectionException;
import com.opensymphony.xwork2.util.reflection.ReflectionProvider;

public class OgnlReflectionProvider implements ReflectionProvider {

    public Field getField(Class inClass, String name) {
        return OgnlRuntime.getField(inClass, name);
    }

    public Method getGetMethod(Class targetClass, String propertyName)
            throws IntrospectionException, ReflectionException {
        try {
            return OgnlRuntime.getGetMethod(null, targetClass, propertyName);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public Method getSetMethod(Class targetClass, String propertyName)
            throws IntrospectionException, ReflectionException {
        try {
            return OgnlRuntime.getSetMethod(null, targetClass, propertyName);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public void setProperties(Map props, Object o, Map context) {
        OgnlUtil.setProperties(props, o, context);
    }

    public void setProperties(Map props, Object o, Map context,
            boolean throwPropertyExceptions) {
        OgnlUtil.setProperties(props, o, context, throwPropertyExceptions);
        
    }

    public void setProperties(Map properties, Object o) {
        OgnlUtil.setProperties(properties, o);
    }

    public PropertyDescriptor getPropertyDescriptor(Class targetClass,
            String propertyName) throws IntrospectionException,
            ReflectionException {
        try {
            return OgnlRuntime.getPropertyDescriptor(targetClass, propertyName);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public void copy(Object from, Object to, Map context,
            Collection exclusions, Collection inclusions) {
        OgnlUtil.copy(from, to, context, exclusions, inclusions);
    }

    public Object getRealTarget(String property, Map context, Object root)
            throws ReflectionException {
        try {
            return OgnlUtil.getRealTarget(property, context, root);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public void setProperty(String name, Object value, Object o, Map context) {
        OgnlUtil.setProperty(name, value, o, context);
    }

    public Map getBeanMap(Object source) throws IntrospectionException,
            ReflectionException {
        try {
            return OgnlUtil.getBeanMap(source);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public Object getValue(String expression, Map context, Object root)
            throws ReflectionException {
        try {
            return OgnlUtil.getValue(expression, context, root);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public void setValue(String expression, Map context, Object root,
            Object value) throws ReflectionException {
        try {
            Ognl.setValue(expression, context, root, value);
        } catch (OgnlException e) {
            throw new ReflectionException(e);
        }
    }

    public PropertyDescriptor[] getPropertyDescriptors(Object source)
            throws IntrospectionException {
        return OgnlUtil.getPropertyDescriptors(source);
    }

}
