/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.interceptor;

import com.opensymphony.xwork.ActionInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * SNIPPET START: description
 * This interceptor logs the amount of time in milliseconds. In order for this interceptor to work properly, the
 * logging framework must be set to at least the
 * <a href="http://jakarta.apache.org/commons/logging/api/org/apache/commons/logging/Log.html">INFO</a> level.
 * This interceptor relies on the
 * <a href="http://jakarta.apache.org/commons/logging/">Commons Logging API</a> to report its execution-time value.
 * END SNIPPET: description
 *
 * @author Jason Carreira
 */
public class TimerInterceptor implements Interceptor {
    private static final Log log = LogFactory.getLog(TimerInterceptor.class);

    public void destroy() {
    }

    public void init() {
    }

    public String intercept(ActionInvocation invocation) throws Exception {
        if (log.isInfoEnabled()) {
            long startTime = System.currentTimeMillis();
            String result = invocation.invoke();
            long executionTime = System.currentTimeMillis() - startTime;
            String namespace = invocation.getProxy().getNamespace();
            StringBuffer message = new StringBuffer("Processed action ");

            if ((namespace != null) && (namespace.trim().length() > 0)) {
                message.append(namespace).append("/");
            }

            message.append(invocation.getProxy().getActionName());
            message.append(" in ").append(executionTime).append("ms.");
            log.info(message.toString());

            return result;
        } else {
            return invocation.invoke();
        }
    }
}
