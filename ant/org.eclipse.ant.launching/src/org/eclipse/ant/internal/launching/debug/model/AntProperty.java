/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brock Janiczak (brockj@tpg.com.au) - bug 154907
 *******************************************************************************/
package org.eclipse.ant.internal.launching.debug.model;

import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;

/**
 * A property in an Ant build.
 */
public class AntProperty extends AntDebugElement implements IVariable, Comparable {

	private String fName;
	private AntValue fValue;
    private String fLabel;
	
	/**
	 * Constructs a variable associated with the debug target
	 * with the given name and value.
	 * 
	 * @param target the debug target
	 * @param name property name
	 * @param value property value
	 */
	public AntProperty(AntDebugTarget target, String name, String value) {
		super(target);
		fName = name;
		fValue= new AntValue(target, value);
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IVariable#getValue()
	 */
	public IValue getValue() {
		return fValue;
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IVariable#getName()
	 */
	public String getName() {
		return fName;
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IVariable#getReferenceTypeName()
	 */
	public String getReferenceTypeName() {
		return ""; //$NON-NLS-1$
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IVariable#hasValueChanged()
	 */
	public boolean hasValueChanged() {
		return false;
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(java.lang.String)
	 */
	public void setValue(String expression) {
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IValueModification#setValue(org.eclipse.debug.core.model.IValue)
	 */
	public void setValue(IValue value) {
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IValueModification#supportsValueModification()
	 */
	public boolean supportsValueModification() {
		return false;
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(java.lang.String)
	 */
	public boolean verifyValue(String expression) {
		return false;
	}
	
	/*
	 * @see org.eclipse.debug.core.model.IValueModification#verifyValue(org.eclipse.debug.core.model.IValue)
	 */
	public boolean verifyValue(IValue value) {
		return false;
	}

    /**
     * @return the text used to render this property
     */
    public String getText() {
        if (fLabel == null) {
            StringBuffer buffer= new StringBuffer(getName());
            buffer.append("= "); //$NON-NLS-1$
            buffer.append(fValue.getValueString());          
            fLabel=  buffer.toString();
        } 
        return fLabel;
    }
    
    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object other) {
    	AntProperty otherProperty = (AntProperty) other;
    	return fName.compareToIgnoreCase(otherProperty.getName());
    }
}
