/*
 * Copyright (c) 2002-2003 by OpenSymphony
 * All rights reserved.
 */
package com.opensymphony.xwork.validator.validators;

import com.opensymphony.xwork.validator.ShortCircuitingFieldValidator;


/**
 * FieldValidatorSupport
 *
 * Created : Jan 20, 2003 4:08:40 PM
 *
 * @author Jason Carreira
 */
public abstract class FieldValidatorSupport extends ValidatorSupport implements ShortCircuitingFieldValidator {
    //~ Instance fields ////////////////////////////////////////////////////////

    private String fieldName = null;
    private boolean m_shortCircuit;

    //~ Methods ////////////////////////////////////////////////////////////////

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setShortCircuit(boolean shortcircuit) {
        m_shortCircuit = shortcircuit;
    }

    public boolean isShortCircuit() {
        return m_shortCircuit;
    }
}
