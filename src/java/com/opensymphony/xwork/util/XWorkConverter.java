/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.util;

import com.opensymphony.util.FileManager;
import com.opensymphony.xwork.ActionContext;
import com.opensymphony.xwork.ObjectFactory;
import com.opensymphony.xwork.XWorkMessages;
import ognl.DefaultTypeConverter;
import ognl.TypeConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.lang.reflect.Member;
import java.util.*;


/**
 * OGNL TypeConverter for WebWork.
 *
 * @author <a href="mailto:plightbo@gmail.com">Pat Lightbody</a>
 */
public class XWorkConverter extends DefaultTypeConverter {
    private static XWorkConverter instance;
    private static final Log LOG = LogFactory.getLog(XWorkConverter.class);
    public static final String REPORT_CONVERSION_ERRORS = "report.conversion.errors";
    public static final String CONVERSION_PROPERTY_FULLNAME = "conversion.property.fullName";
    public static final String CONVERSION_ERROR_PROPERTY_PREFIX = "invalid.fieldvalue.";
    public static final String CONVERSION_COLLECTION_PREFIX = "Collection_";

    public static final String LAST_BEAN_CLASS_ACCESSED = "last.bean.accessed";
    public static final String LAST_BEAN_PROPERTY_ACCESSED = "last.property.accessed";

    HashMap defaultMappings = new HashMap();
    HashMap mappings = new HashMap();
    HashSet noMapping = new HashSet();
    HashSet unknownMappings = new HashSet();
    TypeConverter defaultTypeConverter = new XWorkBasicConverter();
    ObjectTypeDeterminer keyElementDeterminer = ObjectTypeDeterminerFactory.getInstance();


    private XWorkConverter() {
        try {
            loadConversionProps("xwork-default-conversion.properties");
        } catch (Exception e) {
        }

        try {
            loadConversionProps("xwork-conversion.properties");
        } catch (Exception e) {
        }
    }

    public static String getConversionErrorMessage(String propertyName, OgnlValueStack stack) {
        String defaultMessage = LocalizedTextUtil.findDefaultText(XWorkMessages.DEFAULT_INVALID_FIELDVALUE,
                ActionContext.getContext().getLocale(),
                new Object[]{
                        propertyName
                });
        String getTextExpression = "getText('" + CONVERSION_ERROR_PROPERTY_PREFIX + propertyName + "','" + defaultMessage + "')";
        String message = (String) stack.findValue(getTextExpression);

        if (message == null) {
            message = defaultMessage;
        }

        return message;
    }

    public static XWorkConverter getInstance() {
        if (instance == null) {
            instance = new XWorkConverter();
        }

        return instance;
    }

    public static String buildConverterFilename(Class clazz) {
        String className = clazz.getName();
        String resource = className.replace('.', '/') + "-conversion.properties";

        return resource;
    }

    public static void resetInstance() {
        instance = null;
    }

    public void setDefaultConverter(TypeConverter defaultTypeConverter) {
        this.defaultTypeConverter = defaultTypeConverter;
    }

    public Object convertValue(Map map, Object o, Class aClass) {
        return convertValue(map, null, null, null, o, aClass);
    }

    public Object convertValue(Map context, Object target, Member member, String property, Object value, Class toClass) {
        //
        // Process the conversion using the default mappings, if one exists
        //
        TypeConverter tc = null;

        if ((value != null) && (toClass == value.getClass())) {
            return value;
        }

        // allow this method to be called without any context
        // i.e. it can be called with as little as "Object value" and "Class toClass"
        if (target != null) {
            Class clazz = target.getClass();

            Object[] classProp = null;

            // this is to handle weird issues with setValue with a different type
            if ((target instanceof CompoundRoot) && (context != null)) {
                classProp = getClassProperty(context);
            }

            if (classProp != null) {
                clazz = (Class) classProp[0];
                property = (String) classProp[1];
            }

            tc = (TypeConverter) getConverter(clazz, property);
        }

        if (tc == null) {
            if (toClass.equals(String.class) && (value != null) && !(value.getClass().equals(String.class) || value.getClass().equals(String[].class))) {
                // when converting to a string, use the source target's class's converter
                tc = lookup(value.getClass());
            } else {
                // when converting from a string, use the toClass's converter
                tc = lookup(toClass);
            }
        }

        if (tc != null) {
            try {
                return tc.convertValue(context, target, member, property, value, toClass);
            } catch (Exception e) {
                handleConversionException(context, property, value, target);

                return acceptableErrorValue(toClass);
            }
        }

        if (defaultTypeConverter != null) {
            try {
                return defaultTypeConverter.convertValue(context, target, member, property, value, toClass);
            } catch (Exception e) {
                handleConversionException(context, property, value, target);

                return acceptableErrorValue(toClass);
            }
        } else {
            try {
                return super.convertValue(context, target, member, property, value, toClass);
            } catch (Exception e) {
                handleConversionException(context, property, value, target);

                return acceptableErrorValue(toClass);
            }
        }
    }

    /**
     * Looks for a TypeConverter in the default mappings.
     *
     * @param className name of the class the TypeConverter must handle
     * @return a TypeConverter to handle the specified class or null if none can
     *         be found
     */
    public TypeConverter lookup(String className) {
        if (unknownMappings.contains(className)) {
            return null;
        }

        TypeConverter result = (TypeConverter) defaultMappings.get(className);

        //Looks for super classes
        if (result == null) {
            Class clazz = null;

            try {
                clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
            } catch (ClassNotFoundException cnfe) {
            }

            result = lookupSuper(clazz);

            if (result != null) {
                //Register now, the next lookup will be faster
                registerConverter(className, result);
            } else {
                // if it isn't found, never look again (also faster)
                registerConverterNotFound(className);
            }
        }

        return result;
    }

    /**
     * Looks for a TypeConverter in the default mappings.
     *
     * @param clazz the class the TypeConverter must handle
     * @return a TypeConverter to handle the specified class or null if none can
     *         be found
     */
    public TypeConverter lookup(Class clazz) {
        return lookup(clazz.getName());
    }

    protected Object getConverter(Class clazz, String property) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Property: " + property);
            LOG.debug("Class: " + clazz.getName());
        }
        synchronized (clazz) {
            if ((property != null) && !noMapping.contains(clazz)) {
                try {
                    Map mapping = (Map) mappings.get(clazz);

                    if (mapping == null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Map is null.");
                        }
                        mapping = buildConverterMapping(clazz);
                    } else {
                        mapping = conditionalReload(clazz, mapping);
                    }

                    Object converter = mapping.get(property);
                    if (LOG.isDebugEnabled() && converter == null) {
                        LOG.debug("converter is null for property " + property + ". Mapping size: " + mapping.size());
                        Iterator iter = mapping.keySet().iterator();
                        while (iter.hasNext()) {
                            Object next = iter.next();
                            LOG.debug(next + ":" + mapping.get(next));
                        }
                    }
                    return converter;
                } catch (Throwable t) {
                    noMapping.add(clazz);
                }
            }
        }

        return null;
    }

    protected void handleConversionException(Map context, String property, Object value, Object object) {
        if ((context.get(REPORT_CONVERSION_ERRORS) == Boolean.TRUE)) {
            String realProperty = property;
            String fullName = (String) context.get(CONVERSION_PROPERTY_FULLNAME);

            if (fullName != null) {
                realProperty = fullName;
            }

            Map conversionErrors = (Map) context.get(ActionContext.CONVERSION_ERRORS);

            if (conversionErrors == null) {
                conversionErrors = new HashMap();
                context.put(ActionContext.CONVERSION_ERRORS, conversionErrors);
            }

            conversionErrors.put(realProperty, value);
        }
    }

    protected synchronized void registerConverter(String className, TypeConverter converter) {
        defaultMappings.put(className, converter);
    }

    protected synchronized void registerConverterNotFound(String className) {
        unknownMappings.add(className);
    }

    private Object[] getClassProperty(Map context) {
        return (Object[]) context.get("__link");
    }

    private Object acceptableErrorValue(Class toClass) {
        if (!toClass.isPrimitive()) {
            return null;
        }

        if (toClass == int.class) {
            return new Integer(0);
        } else if (toClass == double.class) {
            return new Double(0);
        } else if (toClass == long.class) {
            return new Long(0);
        } else if (toClass == boolean.class) {
            return Boolean.FALSE;
        } else if (toClass == short.class) {
            return new Short((short) 0);
        } else if (toClass == float.class) {
            return new Float(0);
        } else if (toClass == byte.class) {
            return new Byte((byte) 0);
        } else if (toClass == char.class) {
            return new Character((char) 0);
        }

        return null;
    }

    /**
     * Looks for converter mappings for the specified class and adds it to an
     * existing map.  Only new converters are added.  If a converter is defined
     * on a key that already exists, the converter is ignored.
     *
     * @param mapping an existing map to add new converter mappings to
     * @param clazz   class to look for converter mappings for
     */
    private void addConverterMapping(Map mapping, Class clazz) {
        try {
            InputStream is = FileManager.loadFile(buildConverterFilename(clazz), clazz);

            if (is != null) {
                Properties prop = new Properties();
                prop.load(is);

                Iterator it = prop.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    String key = (String) entry.getKey();

                    if (mapping.containsKey(key)) {
                        break;
                    }
                    if (LOG.isDebugEnabled()) {
                        LOG.debug(key + ":" + entry.getValue());
                    }

                    if (key.startsWith(DefaultObjectTypeDeterminer.KEY_PROPERTY_PREFIX)) {
                        mapping.put(key, entry.getValue());
                    }
                    //for properties of classes
                    else if (!(key.startsWith(DefaultObjectTypeDeterminer.ELEMENT_PREFIX) ||
                            key.startsWith(DefaultObjectTypeDeterminer.KEY_PREFIX) ||
                            key.startsWith(DefaultObjectTypeDeterminer.DEPRECATED_ELEMENT_PREFIX))
                            ) {
                        mapping.put(key, createTypeConverter((String) entry.getValue()));
                    }
                    //for keys of Maps
                    else if (key.startsWith(DefaultObjectTypeDeterminer.KEY_PREFIX)) {

                        Class converterClass = Thread.currentThread().getContextClassLoader().loadClass((String) entry.getValue());
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Converter class: " + converterClass);
                        }
                        //check if the converter is a type converter if it is one
                        //then just put it in the map as is. Otherwise
                        //put a value in for the type converter of the class
                        if (converterClass.isAssignableFrom(TypeConverter.class)) {

                            mapping.put(key, createTypeConverter((String) entry.getValue()));


                        } else {

                            mapping.put(key, converterClass);
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Object placed in mapping for key "
                                        + key
                                        + " is "
                                        + mapping.get(key));
                            }

                        }


                    }
                    //elements(values) of maps / lists
                    else {
                        mapping.put(key, Thread.currentThread().getContextClassLoader().loadClass((String) entry.getValue()));
                    }
                }
            }
        } catch (Exception ex) {
            LOG.error("Problem loading properties for " + clazz.getName(), ex);
        }
    }

    /**
     * Looks for converter mappings for the specified class, traversing up its
     * class hierarchy and interfaces and adding any additional mappings it may
     * find.  Mappings lower in the hierarchy have priority over those higher
     * in the hierarcy.
     *
     * @param clazz the class to look for converter mappings for
     * @return the converter mappings
     */
    private Map buildConverterMapping(Class clazz) throws Exception {
        Map mapping = new HashMap();

        // check for conversion mapping associated with super classes and any implemented interfaces
        Class curClazz = clazz;

        while (!curClazz.equals(Object.class)) {
            // add current class' mappings
            addConverterMapping(mapping, curClazz);

            // check interfaces' mappings
            Class[] interfaces = curClazz.getInterfaces();

            for (int x = 0; x < interfaces.length; x++) {
                addConverterMapping(mapping, interfaces[x]);
            }

            curClazz = curClazz.getSuperclass();
        }

        if (mapping.size() > 0) {
            mappings.put(clazz, mapping);
        } else {
            noMapping.add(clazz);
        }

        return mapping;
    }

    private Map conditionalReload(Class clazz, Map oldValues) throws Exception {
        Map mapping = oldValues;

        if (FileManager.isReloadingConfigs()) {
            if (FileManager.fileNeedsReloading(buildConverterFilename(clazz))) {
                mapping = buildConverterMapping(clazz);
            }
        }

        return mapping;
    }

    private TypeConverter createTypeConverter(String className) throws Exception {
        Class conversionClass = Thread.currentThread().getContextClassLoader().loadClass(className);

        return (TypeConverter) ObjectFactory.getObjectFactory().buildBean(conversionClass);
    }

    private void loadConversionProps(String propsName) throws Exception {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(propsName);
        Properties props = new Properties();
        props.load(is);

        for (Iterator iterator = props.entrySet().iterator();
             iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String key = (String) entry.getKey();

            try {
                defaultMappings.put(key, createTypeConverter((String) entry.getValue()));
            } catch (Exception e) {
                LOG.error("Conversion registration error", e);
            }
        }
    }

    /**
     * Recurses through a class' interfaces and class hierarchy looking for a
     * TypeConverter in the default mapping that can handle the specified class.
     *
     * @param clazz the class the TypeConverter must handle
     * @return a TypeConverter to handle the specified class or null if none can
     *         be found
     */
    private TypeConverter lookupSuper(Class clazz) {
        TypeConverter result = null;

        if (clazz != null) {
            result = (TypeConverter) defaultMappings.get(clazz.getName());

            if (result == null) {
                // Looks for direct interfaces (depth = 1 )
                Class[] interfaces = clazz.getInterfaces();

                for (int i = 0; i < interfaces.length; i++) {
                    if (defaultMappings.containsKey(interfaces[i].getName())) {
                        result = (TypeConverter) defaultMappings.get(interfaces[i].getName());
                    }

                    break;
                }

                if (result == null) {
                    // Looks for the superclass
                    // If 'clazz' is the Object class, an interface, a primitive type or void then clazz.getSuperClass() returns null
                    result = lookupSuper(clazz.getSuperclass());
                }
            }
        }

        return result;
    }

    /**
     * @return
     */
    public ObjectTypeDeterminer getObjectTypeDeterminer() {
        return keyElementDeterminer;
    }

    /**
     * @param determiner
     */
    public void setKeyElementDeterminer(ObjectTypeDeterminer determiner) {
        keyElementDeterminer = determiner;
    }

}
