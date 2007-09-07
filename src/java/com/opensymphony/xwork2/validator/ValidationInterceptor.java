/*
 * Copyright (c) 2002-2007 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.validator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.ActionProxy;
import com.opensymphony.xwork2.Validateable;
import com.opensymphony.xwork2.ValidationAware;
import com.opensymphony.xwork2.interceptor.DefaultWorkflowInterceptor;
import com.opensymphony.xwork2.interceptor.MethodFilterInterceptor;
import com.opensymphony.xwork2.interceptor.PrefixMethodInvocationUtil;

/**
 * <!-- START SNIPPET: description -->
 *
 * This interceptor runs the action through the standard validation framework, which in turn checks the action against
 * any validation rules (found in files such as <i>ActionClass-validation.xml</i>) and adds field-level and action-level
 * error messages (provided that the action implements {@link com.opensymphony.xwork2.ValidationAware}). This interceptor
 * is often one of the last (or second to last) interceptors applied in a stack, as it assumes that all values have
 * already been set on the action.
 *
 * <p/>This interceptor does nothing if the name of the method being invoked is specified in the <b>excludeMethods</b>
 * parameter. <b>excludeMethods</b> accepts a comma-delimited list of method names. For example, requests to
 * <b>foo!input.action</b> and <b>foo!back.action</b> will be skipped by this interceptor if you set the
 * <b>excludeMethods</b> parameter to "input, back".
 * 
 * </ol>
 * 
 * <p/> The workflow of the action request does not change due to this interceptor. Rather,
 * this interceptor is often used in conjuction with the <b>workflow</b> interceptor.
 *
 * <p/>
 * 
 * <b>NOTE:</b> As this method extends off MethodFilterInterceptor, it is capable of
 * deciding if it is applicable only to selective methods in the action class. See
 * <code>MethodFilterInterceptor</code> for more info.
 *
 * <!-- END SNIPPET: description -->
 *
 * <p/> <u>Interceptor parameters:</u>
 *
 * <!-- START SNIPPET: parameters -->
 *
 * <ul>
 *
 * <li>alwaysInvokeValidate - Defaults to true. If true validate() method will always
 * be invoked, otherwise it will not.</li>
 *
 * <li>programmatic - Defaults to true. If true and the action is Validateable call validate(),
 * and any method that starts with "validate".
 * </li>
 * 
 * <li>declarative - Defaults to true. Perform validation based on xml or annotations.</li>
 * 
 * </ul>
 *
 * <!-- END SNIPPET: parameters -->
 *
 * <p/> <u>Extending the interceptor:</u>
 *
 * <p/>
 *
 * <!-- START SNIPPET: extending -->
 *
 * There are no known extension points for this interceptor.
 *
 * <!-- END SNIPPET: extending -->
 *
 * <p/> <u>Example code:</u>
 *
 * <pre>
 * <!-- START SNIPPET: example -->
 * 
 * &lt;action name="someAction" class="com.examples.SomeAction"&gt;
 *     &lt;interceptor-ref name="params"/&gt;
 *     &lt;interceptor-ref name="validation"/&gt;
 *     &lt;interceptor-ref name="workflow"/&gt;
 *     &lt;result name="success"&gt;good_result.ftl&lt;/result&gt;
 * &lt;/action&gt;
 * 
 * &lt;-- in the following case myMethod of the action class will not
 *        get validated --&gt;
 * &lt;action name="someAction" class="com.examples.SomeAction"&gt;
 *     &lt;interceptor-ref name="params"/&gt;
 *     &lt;interceptor-ref name="validation"&gt;
 *         &lt;param name="excludeMethods"&gt;myMethod&lt;/param&gt;
 *     &lt;/interceptor-ref&gt;
 *     &lt;interceptor-ref name="workflow"/&gt;
 *     &lt;result name="success"&gt;good_result.ftl&lt;/result&gt;
 * &lt;/action&gt;
 * 
 * &lt;-- in the following case only annotated methods of the action class will
 *        be validated --&gt;
 * &lt;action name="someAction" class="com.examples.SomeAction"&gt;
 *     &lt;interceptor-ref name="params"/&gt;
 *     &lt;interceptor-ref name="validation"&gt;
 *         &lt;param name="validateAnnotatedMethodOnly"&gt;true&lt;/param&gt;
 *     &lt;/interceptor-ref&gt;
 *     &lt;interceptor-ref name="workflow"/&gt;
 *     &lt;result name="success"&gt;good_result.ftl&lt;/result&gt;
 * &lt;/action&gt;
 *
 *
 * <!-- END SNIPPET: example -->
 * </pre>
 *
 * @author Jason Carreira
 * @author Rainer Hermanns
 * @author <a href='mailto:the_mindstorm[at]evolva[dot]ro'>Alexandru Popescu</a>
 * @see ActionValidatorManager
 * @see com.opensymphony.xwork2.interceptor.DefaultWorkflowInterceptor
 */
public class ValidationInterceptor extends MethodFilterInterceptor {

    private boolean validateAnnotatedMethodOnly;

    private static final Log _log = LogFactory.getLog(DefaultWorkflowInterceptor.class);
    
    private final static String VALIDATE_PREFIX = "validate";
    private final static String ALT_VALIDATE_PREFIX = "validateDo";
    
    private boolean alwaysInvokeValidate = true;
    private boolean programmatic = true;
    private boolean declarative = true;

    /**
     * Determines if {@link Validateable}'s <code>validate()</code> should be called,
     * as well as methods whose name that start with "validate". Defaults to "true".
     * 
     * @param programmatic <tt>true</tt> then <code>validate()</code> is invoked.
     */
    public void setProgrammatic(boolean programmatic) {
        this.programmatic = programmatic;
    }

    /**
     * Determines if validation based on annotations or xml should be performed. Defaults 
     * to "true".
     * 
     * @param declarative <tt>true</tt> then perform validation based on annotations or xml.
     */
    public void setDeclarative(boolean declarative) {
        this.declarative = declarative;
    }

    /**
     * Determines if {@link Validateable}'s <code>validate()</code> should always 
     * be invoked. Default to "true".
     * 
     * @param alwaysInvokeValidate <tt>true</tt> then <code>validate()</code> is always invoked.
     */
    public void setAlwaysInvokeValidate(String alwaysInvokeValidate) {
            this.alwaysInvokeValidate = Boolean.parseBoolean(alwaysInvokeValidate);
    }

    /**
     * Gets if <code>validate()</code> should always be called or only per annotated method.
     *
     * @return <tt>true</tt> to only validate per annotated method, otherwise <tt>false</tt> to always validate.
     */
    public boolean isValidateAnnotatedMethodOnly() {
        return validateAnnotatedMethodOnly;
    }

    /**
     * Determine if <code>validate()</code> should always be called or only per annotated method.
     * Default to <tt>false</tt>.
     *
     * @param validateAnnotatedMethodOnly  <tt>true</tt> to only validate per annotated method, otherwise <tt>false</tt> to always validate.
     */
    public void setValidateAnnotatedMethodOnly(boolean validateAnnotatedMethodOnly) {
        this.validateAnnotatedMethodOnly = validateAnnotatedMethodOnly;
    }

    /**
     * Gets the current action and its context and delegates to {@link ActionValidatorManager} proper validate method.
     *
     * @param invocation  the execution state of the Action.
     * @throws Exception if an error occurs validating the action.
     */
    protected void doBeforeInvocation(ActionInvocation invocation) throws Exception {
        Object action = invocation.getAction();
        ActionProxy proxy = invocation.getProxy();
        String context = proxy.getActionName();
        String method = proxy.getMethod();

        if (log.isDebugEnabled()) {
            log.debug("Validating "
                    + invocation.getProxy().getNamespace() + "/" + invocation.getProxy().getActionName() + " with method "+ method +".");
        }
        

        if(declarative) {
            if (validateAnnotatedMethodOnly) {
                ActionValidatorManagerFactory.getInstance().validate(action, context, method);
            } else {
                ActionValidatorManagerFactory.getInstance().validate(action, context);
            }
        }
        
        if (action instanceof Validateable && programmatic) {
            // keep exception that might occured in validateXXX or validateDoXXX
            Exception exception = null; 
            
            Validateable validateable = (Validateable) action;
            if (_log.isDebugEnabled()) {
                _log.debug("Invoking validate() on action "+validateable);
            }
            
            try {
                PrefixMethodInvocationUtil.invokePrefixMethod(
                                invocation, 
                                new String[] { VALIDATE_PREFIX, ALT_VALIDATE_PREFIX });
            }
            catch(Exception e) {
                // If any exception occurred while doing reflection, we want 
                // validate() to be executed
                _log.warn("an exception occured while executing the prefix method", e);
                exception = e;
            }
            
            
            if (alwaysInvokeValidate) {
                validateable.validate();
            }
            
            if (exception != null) { 
                // rethrow if something is wrong while doing validateXXX / validateDoXXX 
                throw exception;
            }
        }
    }

    protected String doIntercept(ActionInvocation invocation) throws Exception {
        doBeforeInvocation(invocation);
        
        return invocation.invoke();
    }

}
