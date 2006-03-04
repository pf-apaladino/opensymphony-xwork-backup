/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.interceptor;

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ActionInvocation;
import com.opensymphony.xwork.config.entities.ActionConfig;
import com.opensymphony.xwork.config.entities.Parameterizable;
import com.opensymphony.xwork.util.OgnlValueStack;

import java.util.Iterator;
import java.util.Map;


/**
 * <!-- START SNIPPET: description -->
 *
 * This interceptor populates the action with the static parameters defined in the action configuration. If the action
 * implements {@link Parameterizable}, a map of the static parameters will be also be passed directly to the action.
 *
 * <p/> Parameters are typically defined with &lt;param&gt; elements within xwork.xml.
 *
 * <!-- END SNIPPET: description -->
 *
 * <p/> <u>Interceptor parameters:</u>
 *
 * <!-- START SNIPPET: parameters -->
 *
 * <ul>
 *
 * <li>None</li>
 *
 * </ul>
 *
 * <!-- END SNIPPET: parameters -->
 *
 * <p/> <u>Extending the interceptor:</u>
 *
 * <!-- START SNIPPET: extending -->
 *
 * <p/>There are no extension points to this interceptor.
 *
 * <!-- END SNIPPET: extending -->
 *
 * <p/> <u>Example code:</u>
 *
 * <pre>
 * <!-- START SNIPPET: example -->
 * &lt;action name="someAction" class="com.examples.SomeAction"&gt;
 *     &lt;interceptor-ref name="static-params"/&gt;
 *     &lt;result name="success"&gt;good_result.ftl&lt;/result&gt;
 * &lt;/action&gt;
 * <!-- END SNIPPET: example -->
 * </pre>
 *
 * @author Patrick Lightbody
 */
public class StaticParametersInterceptor extends AroundInterceptor {

    protected void after(ActionInvocation invocation, String result) throws Exception {
    }

    protected void before(ActionInvocation invocation) throws Exception {
        ActionConfig config = invocation.getProxy().getConfig();
        Object action = invocation.getAction();

        final Map parameters = config.getParams();

        if (log.isDebugEnabled()) {
            log.debug("Setting static parameters " + parameters);
        }

        // for actions marked as Parameterizable, pass the static parameters directly
        if (action instanceof Parameterizable) {
            ((Parameterizable) action).setParams(parameters);
        }

        if (parameters != null) {
            final OgnlValueStack stack = ActionContext.getContext().getValueStack();

            for (Iterator iterator = parameters.entrySet().iterator();
                 iterator.hasNext();) {
                Map.Entry entry = (Map.Entry) iterator.next();
                stack.setValue(entry.getKey().toString(), entry.getValue());
            }
        }
    }
}