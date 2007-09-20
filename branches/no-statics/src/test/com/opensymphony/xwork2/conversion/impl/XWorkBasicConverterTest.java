/*
 * Copyright (c) 2002-2007 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork2.conversion.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.opensymphony.xwork2.XWorkException;
import com.opensymphony.xwork2.ActionContext;

import junit.framework.TestCase;

/**
 * Test case for XWorkBasicConverter
 *
 * @author tm_jee
 * @version $Date$ $Id$
 */
public class XWorkBasicConverterTest extends TestCase {

    // TODO: test for every possible conversion
    // take into account of empty string
    // primitive -> conversion error when empty string is passed
    // object -> return null when empty string is passed

    public void testDateConversionWithEmptyValue() {
        XWorkBasicConverter basicConverter = new XWorkBasicConverter();
        Object convertedObject = basicConverter.convertValue(new HashMap(), null, null, null, "", Date.class);
        // we must not get XWorkException as that will caused a conversion error
        assertNull(convertedObject);
    }

    public void testDateConversionWithInvalidValue() throws Exception {
        XWorkBasicConverter basicConverter = new XWorkBasicConverter();
        try {
            Object convertedObject = basicConverter.convertValue(new HashMap(), null, null, null, "asdsd", Date.class);
            fail("XWorkException expected - conversion error occurred");
        } catch (XWorkException e) {
            // we MUST get this exception as this is a conversion error
        }
    }

    /* the code below has been disabled as it causes sideffects in Strtus2 (XW-512)
    public void testXW490ConvertStringToDobule() throws Exception {
        Locale locale = new Locale("DA"); // let's use a not common locale such as Denmark

        Map ctx = new HashMap();
        ctx.put(ActionContext.LOCALE, locale);

        XWorkBasicConverter conv = new XWorkBasicConverter();
        // decimal seperator is , in Denmark so we should write 123,99 as input
        Double value = (Double) conv.convertValue(ctx, null, null, null, "123,99", Double.class);
        assertNotNull(value);

        // output is as expected a real double value converted using Denmark as locale
        assertEquals(123.99d, value.doubleValue(), 0.001d);
    }

    public void testXW49ConvertDobuleToString() throws Exception {
        Locale locale = new Locale("DA"); // let's use a not common locale such as Denmark

        Map ctx = new HashMap();
        ctx.put(ActionContext.LOCALE, locale);

        XWorkBasicConverter conv = new XWorkBasicConverter();
        // decimal seperator is , in Denmark so we should write 123,99 as input
        String value = (String) conv.convertValue(ctx, null, null, null, new Double("123.99"), String.class);
        assertNotNull(value);

        // output should be formatted according to Danish locale using , as decimal seperator
        assertEquals("123,99", value);
    }
    */
}