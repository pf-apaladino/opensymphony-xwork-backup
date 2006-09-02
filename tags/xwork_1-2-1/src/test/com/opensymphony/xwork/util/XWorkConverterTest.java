/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import ognl.OgnlException;
import ognl.OgnlRuntime;

import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ModelDrivenAction;
import com.opensymphony.xwork.SimpleAction;
import com.opensymphony.xwork.TestBean;
import com.opensymphony.xwork.config.ConfigurationManager;
import com.opensymphony.xwork.test.ModelDrivenAction2;
import com.opensymphony.xwork.test.User;


/**
 * @author $Author$
 * @version $Revision$
 */
public class XWorkConverterTest extends TestCase {

    Map context;
    XWorkConverter converter;

//    public void testConversionToSetKeepsOriginalSetAndReplacesContents() {
//        OgnlValueStack stack = new OgnlValueStack();
//
//        Map stackContext = stack.getContext();
//        stackContext.put(InstantiatingNullHandler.CREATE_NULL_OBJECTS, Boolean.TRUE);
//        stackContext.put(XWorkMethodAccessor.DENY_METHOD_EXECUTION, Boolean.TRUE);
//        stackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);
//
//        String[] param = new String[] {"abc", "def", "ghi"};
//        List paramList = Arrays.asList(param);
//
//        List originalList = new ArrayList();
//        originalList.add("jkl");
//        originalList.add("mno");
//
//        User user = new User();
//        user.setList(originalList);
//        stack.push(user);
//
//        stack.setValue("list", param);
//
//        List userList = user.getList();
//        assertEquals(3,userList.size());
//        assertEquals(paramList,userList);
//        assertSame(originalList,userList);
//    }

    public void testArrayToNumberConversion() {
        String[] value = new String[]{"12345"};
        assertEquals(new Integer(12345), converter.convertValue(context, null, null, null, value, Integer.class));
        assertEquals(new Long(12345), converter.convertValue(context, null, null, null, value, Long.class));
        value[0] = "123.45";
        assertEquals(new Float(123.45), converter.convertValue(context, null, null, null, value, Float.class));
        assertEquals(new Double(123.45), converter.convertValue(context, null, null, null, value, Double.class));
        value[0] = "1234567890123456789012345678901234567890";
        assertEquals(new BigInteger(value[0]), converter.convertValue(context, null, null, null, value, BigInteger.class));
        value[0] = "1234567890123456789.012345678901234567890";
        assertEquals(new BigDecimal(value[0]), converter.convertValue(context, null, null, null, value, BigDecimal.class));
    }

    public void testDateConversion() throws ParseException {
        java.sql.Date sqlDate = new java.sql.Date(System.currentTimeMillis());
        assertEquals(sqlDate, converter.convertValue(context, null, null, null, sqlDate, Date.class));

        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss");
        Date date = format.parse("01/10/2001 00:00:00");

        SimpleDateFormat formatt = new SimpleDateFormat("hh:mm:ss");
        java.sql.Time datet = new java.sql.Time( formatt.parse("10:11:12").getTime());

        String dateStr = (String) converter.convertValue(context, null, null, null, date, String.class);
        String datetStr = (String) converter.convertValue(context, null, null, null, datet, String.class);

        Date date2 = (Date) converter.convertValue(context, null, null, null, dateStr, Date.class);
        assertEquals(date, date2);
        java.sql.Date date3 = (java.sql.Date) converter.convertValue(context, null, null, null, dateStr, java.sql.Date.class);
        assertEquals(date, date3);
        java.sql.Timestamp ts = (java.sql.Timestamp) converter.convertValue(context, null, null, null, dateStr, java.sql.Timestamp.class);
        assertEquals(date, ts);
        java.sql.Time time1= (java.sql.Time) converter.convertValue(context, null, null, null, datetStr, java.sql.Time.class);
        assertEquals(datet, time1);    
    }

    public void testFieldErrorMessageAddedForComplexProperty() {
        SimpleAction action = new SimpleAction();
        action.setBean(new TestBean());

        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);

        Map ognlStackContext = stack.getContext();
        ognlStackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);
        ognlStackContext.put(XWorkConverter.CONVERSION_PROPERTY_FULLNAME, "bean.birth");

        String[] value = new String[]{"invalid date"};
        assertEquals("Conversion should have failed.", OgnlRuntime.NoConversionPossible, converter.convertValue(ognlStackContext, action.getBean(), null, "birth", value, Date.class));
        stack.pop();

        Map conversionErrors = (Map) stack.getContext().get(ActionContext.CONVERSION_ERRORS);
        assertNotNull(conversionErrors);
        assertTrue(conversionErrors.size() == 1);
        assertEquals(value, conversionErrors.get("bean.birth"));
    }

    public void testFieldErrorMessageAddedWhenConversionFails() {
        SimpleAction action = new SimpleAction();
        action.setDate(null);

        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);

        Map ognlStackContext = stack.getContext();
        ognlStackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);

        String[] value = new String[]{"invalid date"};
        assertEquals("Conversion should have failed.", OgnlRuntime.NoConversionPossible, converter.convertValue(ognlStackContext, action, null, "date", value, Date.class));
        stack.pop();

        Map conversionErrors = (Map) ognlStackContext.get(ActionContext.CONVERSION_ERRORS);
        assertNotNull(conversionErrors);
        assertEquals(1, conversionErrors.size());
        assertNotNull(conversionErrors.get("date"));
        assertEquals(value, conversionErrors.get("date"));
    }

    public void testFieldErrorMessageAddedWhenConversionFailsOnModelDriven() {
        ModelDrivenAction action = new ModelDrivenAction();
        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);
        stack.push(action.getModel());

        Map ognlStackContext = stack.getContext();
        ognlStackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);

        String[] value = new String[]{"invalid date"};
        assertEquals("Conversion should have failed.", OgnlRuntime.NoConversionPossible, converter.convertValue(ognlStackContext, action, null, "birth", value, Date.class));
        stack.pop();
        stack.pop();

        Map conversionErrors = (Map) ognlStackContext.get(ActionContext.CONVERSION_ERRORS);
        assertNotNull(conversionErrors);
        assertEquals(1, conversionErrors.size());
        assertNotNull(conversionErrors.get("birth"));
        assertEquals(value, conversionErrors.get("birth"));
    }

    public void testDateStrictConversion() throws Exception {
        // see XW-341
        String dateStr = "13/01/2005"; // us date format is used in context
        Object res = converter.convertValue(context, null, null, null, dateStr, Date.class);
        assertEquals(res, OgnlRuntime.NoConversionPossible);
    	
        dateStr = "02/30/2005"; // us date format is used in context
        res = converter.convertValue(context, null, null, null, dateStr, Date.class);
        assertEquals(res, OgnlRuntime.NoConversionPossible);
    	
        // and test a date that is passable
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        dateStr = "12/31/2005"; // us date format
        res = converter.convertValue(context, null, null, null, dateStr, Date.class);
        Date date = format.parse(dateStr);
        assertNotSame(res, OgnlRuntime.NoConversionPossible);
        assertEquals(date, res);
    }
    
    
    public void testFindConversionErrorMessage() {
        ModelDrivenAction action = new ModelDrivenAction();
        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);
        stack.push(action.getModel());

        String message = XWorkConverter.getConversionErrorMessage("birth", stack);
        assertNotNull(message);
        assertEquals("Invalid date for birth.", message);

        message = XWorkConverter.getConversionErrorMessage("foo", stack);
        assertNotNull(message);
        assertEquals("Invalid field value for field \"foo\".", message);
    }

    public void testFindConversionMappingForInterface() {
        ModelDrivenAction2 action = new ModelDrivenAction2();
        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);
        stack.push(action.getModel());

        Map ognlStackContext = stack.getContext();
        ognlStackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);

        String value = "asdf:123";
        Object o = converter.convertValue(ognlStackContext, action.getModel(), null, "barObj", value, Bar.class);
        assertNotNull(o);
        assertTrue(o instanceof Bar);

        Bar b = (Bar) o;
        assertEquals(value, b.getTitle() + ":" + b.getSomethingElse());
    }

    public void testLocalizedDateConversion() throws Exception {
        Date date = new Date(System.currentTimeMillis());
        Locale locale = Locale.GERMANY;
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        String dateString = df.format(date);
        context.put(ActionContext.LOCALE, locale);
        assertEquals(dateString, converter.convertValue(context, null, null, null, date, String.class));
    }

    public void testStringToIntConversions() {
        SimpleAction action = new SimpleAction();
        action.setBean(new TestBean());

        OgnlValueStack stack = new OgnlValueStack();
        stack.push(action);

        Map ognlStackContext = stack.getContext();
        ognlStackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);

        assertEquals("Conversion should have failed.", OgnlRuntime.NoConversionPossible, converter.convertValue(ognlStackContext, action.getBean(), null, "count", "111.1", int.class));
        stack.pop();

        Map conversionErrors = (Map) stack.getContext().get(ActionContext.CONVERSION_ERRORS);
        assertNotNull(conversionErrors);
        assertTrue(conversionErrors.size() == 1);
    }
    
    public void testStringArrayToCollection() {
        List list = new ArrayList();
        list.add("foo");
        list.add("bar");
        list.add("baz");
        assertEquals(list, converter.convertValue(context, null, null, null, new String[]{
                "foo", "bar", "baz"
        }, Collection.class));
    }

    public void testStringArrayToList() {
        List list = new ArrayList();
        list.add("foo");
        list.add("bar");
        list.add("baz");
        assertEquals(list, converter.convertValue(context, null, null, null, new String[]{
                "foo", "bar", "baz"
        }, List.class));
    }

    public void testStringArrayToPrimitiveWrappers() {
        Long[] longs = (Long[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, Long[].class);
        assertNotNull(longs);
        assertTrue(Arrays.equals(new Long[]{new Long(123), new Long(456)}, longs));

        Integer[] ints = (Integer[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, Integer[].class);
        assertNotNull(ints);
        assertTrue(Arrays.equals(new Integer[]{
                new Integer(123), new Integer(456)
        }, ints));

        Double[] doubles = (Double[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, Double[].class);
        assertNotNull(doubles);
        assertTrue(Arrays.equals(new Double[]{new Double(123), new Double(456)}, doubles));

        Float[] floats = (Float[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, Float[].class);
        assertNotNull(floats);
        assertTrue(Arrays.equals(new Float[]{new Float(123), new Float(456)}, floats));

        Boolean[] booleans = (Boolean[]) converter.convertValue(context, null, null, null, new String[]{
                "true", "false"
        }, Boolean[].class);
        assertNotNull(booleans);
        assertTrue(Arrays.equals(new Boolean[]{Boolean.TRUE, Boolean.FALSE}, booleans));
    }

    public void testStringArrayToPrimitives() throws OgnlException {
        long[] longs = (long[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, long[].class);
        assertNotNull(longs);
        assertTrue(Arrays.equals(new long[]{123, 456}, longs));

        int[] ints = (int[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, int[].class);
        assertNotNull(ints);
        assertTrue(Arrays.equals(new int[]{123, 456}, ints));

        double[] doubles = (double[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, double[].class);
        assertNotNull(doubles);
        assertTrue(Arrays.equals(new double[]{123, 456}, doubles));

        float[] floats = (float[]) converter.convertValue(context, null, null, null, new String[]{
                "123", "456"
        }, float[].class);
        assertNotNull(floats);
        assertTrue(Arrays.equals(new float[]{123, 456}, floats));

        boolean[] booleans = (boolean[]) converter.convertValue(context, null, null, null, new String[]{
                "true", "false"
        }, boolean[].class);
        assertNotNull(booleans);
        assertTrue(Arrays.equals(new boolean[]{true, false}, booleans));
    }

    public void testStringArrayToSet() {
        Set list = new HashSet();
        list.add("foo");
        list.add("bar");
        list.add("baz");
        assertEquals(list, converter.convertValue(context, null, null, null, new String[]{
                "foo", "bar", "bar", "baz"
        }, Set.class));
    }

    public void testStringToCollectionConversion() {
        OgnlValueStack stack = new OgnlValueStack();
        Map stackContext = stack.getContext();
        stackContext.put(InstantiatingNullHandler.CREATE_NULL_OBJECTS, Boolean.TRUE);
        stackContext.put(XWorkMethodAccessor.DENY_METHOD_EXECUTION, Boolean.TRUE);
        stackContext.put(XWorkConverter.REPORT_CONVERSION_ERRORS, Boolean.TRUE);

        User user = new User();
        stack.push(user);

        stack.setValue("list", "asdf");
        assertNotNull(user.getList());
        assertEquals(1, user.getList().size());
        assertEquals(String.class, user.getList().get(0).getClass());
        assertEquals("asdf", user.getList().get(0));
    }

    public void testStringToCustomTypeUsingCustomConverter() {
        // the converter needs to be registered as the Bar.class converter 
        // it won't be detected from the Foo-conversion.properties
        // because the Foo-conversion.properties file is only used when converting a property of Foo
        converter.registerConverter(Bar.class.getName(), new FooBarConverter());

        Bar bar = (Bar) converter.convertValue(null, null, null, null, "blah:123", Bar.class);
        assertNotNull("conversion failed", bar);
        assertEquals(123, bar.getSomethingElse());
        assertEquals("blah", bar.getTitle());
    }

    public void testStringToPrimitiveWrappers() {
        assertEquals(new Long(123), converter.convertValue(context, null, null, null, "123", Long.class));
        assertEquals(new Integer(123), converter.convertValue(context, null, null, null, "123", Integer.class));
        assertEquals(new Double(123.5), converter.convertValue(context, null, null, null, "123.5", Double.class));
        assertEquals(new Float(123.5), converter.convertValue(context, null, null, null, "123.5", float.class));
        assertEquals(new Boolean(false), converter.convertValue(context, null, null, null, "false", Boolean.class));
        assertEquals(new Boolean(true), converter.convertValue(context, null, null, null, "true", Boolean.class));
    }

    public void testStringToPrimitives() {
        assertEquals(new Long(123), converter.convertValue(context, null, null, null, "123", long.class));
        assertEquals(new Double(123.5), converter.convertValue(context, null, null, null, "123.5", double.class));
        assertEquals(new Float(123.5), converter.convertValue(context, null, null, null, "123.5", float.class));
        assertEquals(new Boolean(false), converter.convertValue(context, null, null, null, "false", boolean.class));
        assertEquals(new Boolean(true), converter.convertValue(context, null, null, null, "true", boolean.class));
        assertEquals(new BigDecimal(123.5), converter.convertValue(context, null, null, null, "123.5", BigDecimal.class));
        assertEquals(new BigInteger("123"), converter.convertValue(context, null, null, null, "123", BigInteger.class));
    }

    public void testStringToInt() {
        assertEquals(new Integer(123), converter.convertValue(context, null, null, null, "123", int.class));
        context.put(ActionContext.LOCALE, Locale.US);
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123.12", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,23", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", int.class));
        context.put(ActionContext.LOCALE, Locale.GERMANY);
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123.12", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,23", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234", int.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", int.class));
    }
    
    
    public void testStringToInteger() {
        assertEquals(new Integer(123), converter.convertValue(context, null, null, null, "123", Integer.class));
        context.put(ActionContext.LOCALE, Locale.US);
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123.12", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", Integer.class));
        assertEquals(new Integer(1234), converter.convertValue(context, null, null, null, "1,234", Integer.class));
        // WRONG: locale separator is wrongly placed
        assertEquals(new Integer(123), converter.convertValue(context, null, null, null, "1,23", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", Integer.class));

        context.put(ActionContext.LOCALE, Locale.GERMANY);
        // WRONG: locale separator is wrongly placed
        assertEquals(new Integer(12312), converter.convertValue(context, null, null, null, "123.12", Integer.class));
        assertEquals(new Integer(1234), converter.convertValue(context, null, null, null, "1.234", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,23", Integer.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", Integer.class));
    }
    
    public void testStringToPrimitiveDouble() {
        assertEquals(new Double(123), converter.convertValue(context, null, null, null, "123", double.class));
        context.put(ActionContext.LOCALE, Locale.US);
        assertEquals(new Double(123.12), converter.convertValue(context, null, null, null, "123.12", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,23", double.class));
        assertEquals(new Double(1.234), converter.convertValue(context, null, null, null, "1.234", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", double.class));
        
        context.put(ActionContext.LOCALE, Locale.GERMANY);
        assertEquals(new Double(123.12), converter.convertValue(context, null, null, null, "123.12", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,23", double.class));
        assertEquals(new Double(1.234), converter.convertValue(context, null, null, null, "1.234", double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", double.class));
    }

    public void testStringToDouble() {
        assertEquals(new Double(123), converter.convertValue(context, null, null, null, "123", Double.class));
        context.put(ActionContext.LOCALE, Locale.US);
        assertEquals(new Double(123.12), converter.convertValue(context, null, null, null, "123.12", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", Double.class));
        assertEquals(new Double(1234), converter.convertValue(context, null, null, null, "1,234", Double.class));
        assertEquals(new Double(1234.12), converter.convertValue(context, null, null, null, "1,234.12", Double.class));
        // WRONG: locale separator is wrongly placed 
        assertEquals(new Double(123), converter.convertValue(context, null, null, null, "1,23", Double.class));
        assertEquals(new Double(1.234), converter.convertValue(context, null, null, null, "1.234", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1.234,12", Double.class));
        
        context.put(ActionContext.LOCALE, Locale.GERMANY);
        // WRONG: locale separator is wrongly placed
        assertEquals(new Double(12312), converter.convertValue(context, null, null, null, "123.12", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "123aa", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "aa123", Double.class));
        assertEquals(new Double(1.234), converter.convertValue(context, null, null, null, "1,234", Double.class));
        assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, null, null, null, "1,234.12", Double.class));
        assertEquals(new Double(1.23), converter.convertValue(context, null, null, null, "1,23", Double.class));
        assertEquals(new Double(1234), converter.convertValue(context, null, null, null, "1.234", Double.class));
        assertEquals(new Double(1234.12), converter.convertValue(context, null, null, null, "1.234,12", Double.class));

    }

    // Testing for null result on non-primitive Number types supplied as empty String or 
    public void testNotPrimitiveDefaultsToNull() {
        assertEquals(null, converter.convertValue(context, null, null, null, null, Double.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Double.class));

        assertEquals(null, converter.convertValue(context, null, null, null, null, Integer.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Integer.class));

        assertEquals(null, converter.convertValue(context, null, null, null, null, Float.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Float.class));

        assertEquals(null, converter.convertValue(context, null, null, null, null, Character.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Character.class));

        assertEquals(null, converter.convertValue(context, null, null, null, null, Long.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Long.class));

        assertEquals(null, converter.convertValue(context, null, null, null, null, Short.class));
        assertEquals(null, converter.convertValue(context, null, null, null, "", Short.class));

    }
    
    public void testConvertChar() {
        assertEquals(new Character('A'), converter.convertValue(context, "A", char.class));
        assertEquals(new Character('Z'), converter.convertValue(context, "Z", char.class));
        assertEquals(new Character('A'), converter.convertValue(context, "A", Character.class));
        assertEquals(new Character('Z'), converter.convertValue(context, "Z", Character.class));

        assertEquals(new Character('A'), converter.convertValue(context, new Character('A'), char.class));
        assertEquals(new Character('Z'), converter.convertValue(context, new Character('Z'), char.class));
        assertEquals(new Character('A'), converter.convertValue(context, new Character('A'), Character.class));
        assertEquals(new Character('Z'), converter.convertValue(context, new Character('Z'), Character.class));

        assertEquals(new Character('D'), converter.convertValue(context, "DEF", char.class));
        assertEquals(new Character('X'), converter.convertValue(context, "XYZ", Character.class));
        assertEquals(new Character(' '), converter.convertValue(context, " ", Character.class));
        assertEquals(new Character(' '), converter.convertValue(context, "   ", char.class));

        assertEquals(null, converter.convertValue(context, "", char.class));
    }
    
    public void testConvertClass() {
    	Class clazz = (Class) converter.convertValue(context, "java.util.Date", Class.class);
        assertEquals(Date.class.getName(), clazz.getName());

    	Class clazz2 = (Class) converter.convertValue(context, "com.opensymphony.xwork.util.Bar", Class.class);
        assertEquals(Bar.class.getName(), clazz2.getName());

    	assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, "com.opensymphony.xwork.util.IDoNotExist", Class.class));

    	assertEquals(OgnlRuntime.NoConversionPossible, converter.convertValue(context, new Bar(), Class.class)); // only supports string values
    }

    public void testConvertBoolean() {
        assertEquals(Boolean.TRUE, converter.convertValue(context, "true", Boolean.class));
        assertEquals(Boolean.FALSE, converter.convertValue(context, "false", Boolean.class));

        assertEquals(Boolean.TRUE, converter.convertValue(context, Boolean.TRUE, Boolean.class));
        assertEquals(Boolean.FALSE, converter.convertValue(context, Boolean.FALSE, Boolean.class));

    	assertEquals(null, converter.convertValue(context, null, Boolean.class));
    	assertEquals(Boolean.TRUE, converter.convertValue(context, new Bar(), Boolean.class)); // Ognl converter will default to true
    }
    
    public void testConvertPrimitiveArraysToString() {
    	assertEquals("2, 3, 1", converter.convertValue(context, new int[]{2,3,1}, String.class)); 
    	assertEquals("100, 200, 300", converter.convertValue(context, new long[]{100,200,300}, String.class)); 
    	assertEquals("1.5, 2.5, 3.5", converter.convertValue(context, new double[]{1.5,2.5,3.5}, String.class)); 
    	assertEquals("true, false, true", converter.convertValue(context, new boolean[]{true, false, true}, String.class)); 
    }

    public void testConvertSameCollectionToCollection() {
    	Collection names = new ArrayList();
    	names.add("XWork");
    	names.add("WebWork");
    	
    	Collection col = (Collection) converter.convertValue(context, names, Collection.class);
    	assertSame(names, col);
    }

    public void testConvertSqlTimestamp() {
    	assertNotNull(converter.convertValue(context, new Timestamp(new Date().getTime()), String.class));
    	assertNotNull(converter.convertValue(null, new Timestamp(new Date().getTime()), String.class));
    }

    public void testOgnlValueStackWithTypeParameter() {
        OgnlValueStack stack = new OgnlValueStack();
        stack.push(new Foo1());
        Bar1 bar = (Bar1) stack.findValue("bar", Bar1.class);
        assertNotNull(bar);
    }

    public void testNestedConverters() {
        OgnlValueStack stack = new OgnlValueStack();
        Cat cat = new Cat();
        cat.setFoo(new Foo());
        stack.push(cat);
        stack.setValue("foo.number", "123");
        assertEquals(321, cat.getFoo().getNumber());
    }

    public static class Foo1 {
        public Bar1 getBar() {
            return new Bar1Impl();
        }
    }

    public interface Bar1 {
    }

    public static class Bar1Impl implements Bar1 {
    }

    protected void setUp() throws Exception {
        converter = XWorkConverter.getInstance();
        ConfigurationManager.destroyConfiguration();

        OgnlValueStack stack = new OgnlValueStack();
        ActionContext ac = new ActionContext(stack.getContext());
        ac.setLocale(Locale.US);
        ActionContext.setContext(ac);
        context = ac.getContextMap();
    }

    protected void tearDown() throws Exception {
        XWorkConverter.resetInstance();
        ActionContext.setContext(null);
    }
}
