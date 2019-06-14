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


import java.lang.reflect.InvocationTargetException;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.views.properties.*;

public class CVSRemoteFilePropertySource implements IPropertySource {
	ICVSRemoteFile file;
	ILogEntry entry;
	boolean initialized;
	
	// Property Descriptors
	static protected IPropertyDescriptor[] propertyDescriptors = new IPropertyDescriptor[5];
	{
		PropertyDescriptor descriptor;
		String category = CVSUIMessages.cvs; 
		
		// resource name
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_NAME, CVSUIMessages.CVSRemoteFilePropertySource_name); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[0] = descriptor;
		// revision
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_REVISION, CVSUIMessages.CVSRemoteFilePropertySource_revision); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[1] = descriptor;
		// date
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_DATE, CVSUIMessages.CVSRemoteFilePropertySource_date); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[2] = descriptor;
		// author
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_AUTHOR, CVSUIMessages.CVSRemoteFilePropertySource_author); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[3] = descriptor;
		// comment
		descriptor = new PropertyDescriptor(ICVSUIConstants.PROP_COMMENT, CVSUIMessages.CVSRemoteFilePropertySource_comment); 
		descriptor.setAlwaysIncompatible(true);
		descriptor.setCategory(category);
		propertyDescriptors[4] = descriptor;
	}

	/**
	 * Create a PropertySource and store its file
	 */
	public CVSRemoteFilePropertySource(ICVSRemoteFile file) {
		this.file = file;
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
		if (!initialized) {
			initialize();
			initialized = true;
		}
		if (id.equals(ICVSUIConstants.PROP_NAME)) {
			return file.getName();
		}
		if (entry != null) {
			if (id.equals(ICVSUIConstants.PROP_REVISION)) {
				return entry.getRevision();
			}
			if (id.equals(ICVSUIConstants.PROP_DATE)) {
				return entry.getDate();
			}
			if (id.equals(ICVSUIConstants.PROP_AUTHOR)) {
				return entry.getAuthor();
			}
			if (id.equals(ICVSUIConstants.PROP_COMMENT)) {
				return entry.getComment();
			}
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
	
	private void initialize() {
		try {
			CVSUIPlugin.runWithProgress(null, true /* cancelable */, monitor -> {
				try {
					ILogEntry[] entries = file.getLogEntries(monitor);
					String revision = file.getRevision();
					for (ILogEntry e : entries) {
						if (e.getRevision().equals(revision)) {
							entry = e;
							return;
						}
					}
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InterruptedException e) { // ignore cancellation
		} catch (InvocationTargetException e) {
			CVSUIPlugin.openError(null, null, null, e);
		}
	}
}
