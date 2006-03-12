/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork;

import com.opensymphony.xwork.interceptor.Interceptor;

import junit.framework.Assert;


/**
 * MockInterceptor
 * @author Jason Carreira
 * Created Apr 21, 2003 9:04:06 PM
 */
public class MockInterceptor implements Interceptor {
    //~ Static fields/initializers /////////////////////////////////////////////

    public static final String DEFAULT_FOO_VALUE = "fooDefault";

    //~ Instance fields ////////////////////////////////////////////////////////

    private String expectedFoo = DEFAULT_FOO_VALUE;
    private String foo = DEFAULT_FOO_VALUE;
    private boolean executed = false;

    //~ Methods ////////////////////////////////////////////////////////////////

    public boolean isExecuted() {
        return executed;
    }

    public void setExpectedFoo(String expectedFoo) {
        this.expectedFoo = expectedFoo;
    }

    public String getExpectedFoo() {
        return expectedFoo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public String getFoo() {
        return foo;
    }

    /**
    * Called to let an interceptor clean up any resources it has allocated.
    */
    public void destroy() {
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof MockInterceptor)) {
            return false;
        }

        final MockInterceptor testInterceptor = (MockInterceptor) o;

        if (executed != testInterceptor.executed) {
            return false;
        }

        if ((expectedFoo != null) ? (!expectedFoo.equals(testInterceptor.expectedFoo)) : (testInterceptor.expectedFoo != null)) {
            return false;
        }

        if ((foo != null) ? (!foo.equals(testInterceptor.foo)) : (testInterceptor.foo != null)) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = ((expectedFoo != null) ? expectedFoo.hashCode() : 0);
        result = (29 * result) + ((foo != null) ? foo.hashCode() : 0);
        result = (29 * result) + (executed ? 1 : 0);

        return result;
    }

    /**
    * Called after an Interceptor is created, but before any requests are processed using the intercept() methodName. This
    * gives the Interceptor a chance to initialize any needed resources.
    */
    public void init() {
    }

    /**
    * Allows the Interceptor to do some processing on the request before and/or after the rest of the processing of the
    * request by the DefaultActionInvocation or to short-circuit the processing and just return a String return code.
    *
    * @param invocation
    * @return
    * @throws Exception
    */
    public String intercept(ActionInvocation invocation) throws Exception {
        executed = true;
        Assert.assertNotSame(DEFAULT_FOO_VALUE, foo);
        Assert.assertEquals(expectedFoo, foo);

        return invocation.invoke();
    }
}