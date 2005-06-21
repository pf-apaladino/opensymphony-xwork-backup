/*
 * Copyright (c) 2005 Opensymphony. All Rights Reserved.
 */
package com.opensymphony.xwork.util;

/**
 * Determines what the key and and element class
 * of a Map or Collection should be. For Maps, the
 * elements are the values. For Collections, the
 * elements are the elements of the collection.
 *
 * @author Gabriel Zimmerman
 */
public interface ObjectTypeDeterminer {

    public Class getKeyClass(Class parentClass, String property);

    public Class getElementClass(Class parentClass, String property, Object key);

    public String getKeyProperty(Class parentClass, String property);
}
