/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.model;


import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.ui.views.properties.*;

public class CVSRepositoryLocationPropertySource implements IPropertySource {
	ICVSRepositoryLocation location;
	
	// Property Descriptors
	static protected IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[5];
	{
		PropertyDescriptor descriptor;
		String category = CVSUIMessages.cvs; 
		
		// host
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_HOST, CVSUIMessages.CVSRepositoryLocationPropertySource_host); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[0] = descriptor;
		// user
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_USER, CVSUIMessages.CVSRepositoryLocationPropertySource_user); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[1] = descriptor;
		// port
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_PORT, CVSUIMessages.CVSRepositoryLocationPropertySource_port); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[2] = descriptor;
		// root
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_ROOT, CVSUIMessages.CVSRepositoryLocationPropertySource_root); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[3] = descriptor;
		// method
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_METHOD, CVSUIMessages.CVSRepositoryLocationPropertySource_method); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[4] = descriptor;
	}

	/**
	 * Create a PropertySource and store its file
	 */
	public CVSRepositoryLocationPropertySource(ICVSRepositoryLocation location) {
		this.location = location;
	}
	
	/**
	 * Do nothing because properties are read only.
	 */
	@Override
	public Object getEditableValue() {
		return this;
	}

	/**
	 * Return the Property Descriptors for the receiver.
	 */
	@Override
	public IPropertyDescriptor[] getPropertyDescriptors() {
		return propertyDescriptors;
	}

	@Override
	public Object getPropertyValue(Object id) {
		if (id.equals(ICVSUIConstants.PROP_HOST)) {
			return location.getHost();
		}
		if (id.equals(ICVSUIConstants.PROP_USER)) {
			return location.getUsername();
		}
		if (id.equals(ICVSUIConstants.PROP_METHOD)) {
			return location.getMethod().getName();
		}
		if (id.equals(ICVSUIConstants.PROP_ROOT)) {
			return location.getRootDirectory();
		}
		if (id.equals(ICVSUIConstants.PROP_PORT)) {
			int port = location.getPort();
			if (port == ICVSRepositoryLocation.USE_DEFAULT_PORT) {
				return CVSUIMessages.CVSRepositoryLocationPropertySource_default; 
			}
			return "" + port; //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * Answer true if the value of the specified property 
	 * for this object has been changed from the default.
	 */
	@Override
	public boolean isPropertySet(Object property) {
		return false;
	}
	/**
	 * Reset the specified property's value to its default value.
	 * Do nothing because properties are read only.
	 * 
	 * @param   property    The property to reset.
	 */
	@Override
	public void resetPropertyValue(Object property) {
	}
	/**
	 * Do nothing because properties are read only.
	 */
	@Override
	public void setPropertyValue(Object name, Object value) {
	}
}
