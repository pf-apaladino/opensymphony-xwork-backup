/*
 * Copyright (c) 2002-2007 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.opensymphony.xwork2.*;
import com.opensymphony.xwork2.test.User;
import com.opensymphony.xwork2.test.Equidae;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.mock.MockActionInvocation;
import com.opensymphony.xwork2.mock.MockActionProxy;

public class ScopedModelDrivenInterceptorTest extends XWorkTestCase {

    protected ScopedModelDrivenInterceptor inter = null;
    
    /**
     * Set up instance variables required by this test case.
     */
    public void setUp() throws Exception {
        super.setUp();
        inter = new ScopedModelDrivenInterceptor();
        inter.setObjectFactory(new ProxyObjectFactory());
    }

    public void testResolveModel() throws Exception {
        ActionContext ctx = ActionContext.getContext();
        ctx.setSession(new HashMap());
        
        ObjectFactory factory = ObjectFactory.getObjectFactory();
        Object obj = inter.resolveModel(factory, ctx, "java.lang.String", "request", "foo");
        assertNotNull(obj);
        assertTrue(obj instanceof String);
        assertTrue(obj == ctx.get("foo"));

        obj = inter.resolveModel(factory, ctx, "java.lang.String", "session", "foo");
        assertNotNull(obj);
        assertTrue(obj instanceof String);
        assertTrue(obj == ctx.getSession().get("foo"));

        obj = inter.resolveModel(factory, ctx, "java.lang.String", "session", "foo");
        assertNotNull(obj);
        assertTrue(obj instanceof String);
        assertTrue(obj == ctx.getSession().get("foo"));
    }

    public void testScopedModelDrivenAction() throws Exception {
        inter.setScope("request");

        ScopedModelDriven action = new MyUserScopedModelDrivenAction();
        MockActionInvocation mai = new MockActionInvocation();
        MockActionProxy map = new MockActionProxy();
        ActionConfig ac = new ActionConfig();
        map.setConfig(ac);
        mai.setAction(action);
        mai.setProxy(map);

        inter.intercept(mai);
        inter.destroy();

        assertNotNull(action.getModel());
        assertNotNull(action.getScopeKey());
        assertEquals("com.opensymphony.xwork2.test.User", action.getScopeKey());

        Object model = ActionContext.getContext().get(action.getScopeKey());
        assertNotNull(model);
        assertTrue("Model should be an User object", model instanceof User);
    }

    public void testScopedModelDrivenActionWithSetClassName() throws Exception {
        inter.setScope("request");
        inter.setClassName("com.opensymphony.xwork2.test.Equidae");
        inter.setName("queen");

        ScopedModelDriven action = new MyEquidaeScopedModelDrivenAction();
        MockActionInvocation mai = new MockActionInvocation();
        MockActionProxy map = new MockActionProxy();
        ActionConfig ac = new ActionConfig();
        map.setConfig(ac);
        mai.setAction(action);
        mai.setProxy(map);

        inter.intercept(mai);
        inter.destroy();

        assertNotNull(action.getModel());
        assertNotNull(action.getScopeKey());
        assertEquals("queen", action.getScopeKey());

        Object model = ActionContext.getContext().get(action.getScopeKey());
        assertNotNull(model);
        assertTrue("Model should be an Equidae object", model instanceof Equidae);
    }

    public void testModelOnSession() throws Exception {
        inter.setScope("session");
        inter.setName("king");

        User user = new User();
        user.setName("King George");
        Map session = new HashMap();
        ActionContext.getContext().setSession(session);
        ActionContext.getContext().getSession().put("king", user);

        ScopedModelDriven action = new MyUserScopedModelDrivenAction();
        MockActionInvocation mai = new MockActionInvocation();
        MockActionProxy map = new MockActionProxy();
        ActionConfig ac = new ActionConfig();
        map.setConfig(ac);
        mai.setAction(action);
        mai.setProxy(map);

        inter.intercept(mai);
        inter.destroy();

        assertNotNull(action.getModel());
        assertNotNull(action.getScopeKey());
        assertEquals("king", action.getScopeKey());

        Object model = ActionContext.getContext().getSession().get(action.getScopeKey());
        assertNotNull(model);
        assertTrue("Model should be an User object", model instanceof User);
        assertEquals("King George", ((User) model).getName());
    }

    public void testModelAlreadySetOnAction() throws Exception {
        inter.setScope("request");
        inter.setName("king");

        User user = new User();
        user.setName("King George");

        ScopedModelDriven action = new MyUserScopedModelDrivenAction();
        action.setModel(user);
        MockActionInvocation mai = new MockActionInvocation();
        MockActionProxy map = new MockActionProxy();
        ActionConfig ac = new ActionConfig();
        map.setConfig(ac);
        mai.setAction(action);
        mai.setProxy(map);

        inter.intercept(mai);
        inter.destroy();

        assertNotNull(action.getModel());
        assertNull(action.getScopeKey()); // no scope key as nothing happended
    }

    public void testNoScopedModelAction() throws Exception {
        Action action = new SimpleAction();
        MockActionInvocation mai = new MockActionInvocation();
        MockActionProxy map = new MockActionProxy();
        ActionConfig ac = new ActionConfig();
        map.setConfig(ac);
        mai.setAction(action);
        mai.setProxy(map);

        inter.intercept(mai);
        inter.destroy();
        // nothing happends
    }

    private class MyUserScopedModelDrivenAction implements ScopedModelDriven, Action {

        private String key;
        private User model;

        public void setModel(Object model) {
            this.model = (User) model;
        }

        public void setScopeKey(String key) {
            this.key = key;
        }

        public String getScopeKey() {
            return key;
        }

        public User getModel() {
            return model;
        }

        public String execute() throws Exception {
            return SUCCESS;
        }

    }

    private class MyEquidaeScopedModelDrivenAction implements ScopedModelDriven, Action {

        private String key;
        private Equidae model;

        public void setModel(Object model) {
            this.model = (Equidae) model;
        }

        public void setScopeKey(String key) {
            this.key = key;
        }

        public String getScopeKey() {
            return key;
        }

        public Equidae getModel() {
            return model;
        }

        public String execute() throws Exception {
            return SUCCESS;
        }

    }

}

