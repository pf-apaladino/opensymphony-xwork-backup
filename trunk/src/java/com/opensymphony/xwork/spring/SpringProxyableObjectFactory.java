/*
 * Copyright (c) 2005 Opensymphony. All Rights Reserved.
 */
package com.opensymphony.xwork.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SpringProxyableObjectFactory
 *
 * @author Jason Carreira <jcarreira@eplus.com>
 */
public class SpringProxyableObjectFactory extends SpringObjectFactory {
    private static final Log log = LogFactory.getLog(SpringProxyableObjectFactory.class);

    private List skipBeanNames = new ArrayList();

    public Object buildBean(String beanName, Map extraContext) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Building bean for name " + beanName);
        }
        if (!skipBeanNames.contains(beanName)) {
            ApplicationContext anAppContext = getApplicationContext(extraContext);
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Trying the application context... appContext = " + anAppContext + ",\n bean name = " + beanName);
                }
                return anAppContext.getBean(beanName);
            } catch (NoSuchBeanDefinitionException e) {
                if (log.isDebugEnabled()) {
                    log.debug("Did not find bean definition for bean named " + beanName + ", creating a new one...");
                }
                if (autoWiringFactory instanceof BeanDefinitionRegistry) {
                    try {
                        Class clazz = Class.forName(beanName);
                        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) autoWiringFactory;
                        RootBeanDefinition def = new RootBeanDefinition(clazz, autowireStrategy);
                        def.setSingleton(false);
                         if (log.isDebugEnabled()) {
                            log.debug("Registering a new bean definition for class " + beanName);
                        }
                        registry.registerBeanDefinition(beanName,def);
                        try {
                            return anAppContext.getBean(beanName);
                        } catch (NoSuchBeanDefinitionException e2) {
                            log.warn("Could not register new bean definition for bean " + beanName);
                            skipBeanNames.add(beanName);
                        }
                    } catch (ClassNotFoundException e1) {
                        skipBeanNames.add(beanName);
                    }
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Returning autowired instance created by default ObjectFactory");
        }
        return autoWireBean(super.buildBean(beanName, extraContext), autoWiringFactory);
    }

    /**
     * Subclasses may override this to return a different application context.
     * Note that this application context should see any changes made to the
     * {@link #autoWiringFactory}, so the application context should be either
     * the original or a child context of the original.
     * @param context
     */
    protected ApplicationContext getApplicationContext(Map context) {
        return appContext;
    }
}

