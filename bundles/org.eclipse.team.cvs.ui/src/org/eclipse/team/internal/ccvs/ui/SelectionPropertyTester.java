/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.lang.reflect.*;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
//import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.actions.TeamAction;

public class SelectionPropertyTester extends PropertyTester {

	/**
	 * 
	 */
	public SelectionPropertyTester() {
		// TODO Auto-generated constructor stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object,
	 *      java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		// only 'isEnabled' property is allowed at the moment
		if (property == null || !property.equals("isEnabled")) { //$NON-NLS-1$
			return false;
		}
		
		if (!(receiver instanceof ISelection)) {
			return false;
		}

		if (args == null || args.length != 1) {
			return false;
		}

		Object obj = receiver;
		if (obj instanceof IStructuredSelection) {
			obj = ((IStructuredSelection) receiver).getFirstElement();
		}

		if (obj == null) {
			return false;
		}

		Object invoke;
		try {
			Class clazz = Class.forName((String) args[0]);
			Object instance = clazz.newInstance();
			
//			Field fld = clazz.getDeclaredField(SELECTION_FIELDNAME);
//			fld.set(instance, (ISelection) receiver);
//			((TeamAction) instance).selectionChanged((IAction)instance,
			// called only to set the selection
			((TeamAction) instance).selectionChanged(null,
					(ISelection) receiver);			

			Method method = findMethod(clazz, property);
			if (method != null) {
				invoke = method.invoke(instance, (Object[])null);
				return ((Boolean) invoke).booleanValue();
			}
		} catch (IllegalArgumentException e) {
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e));
		} catch (IllegalAccessException e) {
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e));
		} catch (InvocationTargetException e) {
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e));
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e.getTargetException()));			
		} catch (ClassNotFoundException e) {
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e));
		} catch (Exception e) {
			CVSUIPlugin.log(new Status(IStatus.ERROR, CVSUIPlugin.ID, null, e));
		}
		return false;
	}

	private static Method findMethod(Class clazz, String method)
			throws Exception {
		Method[] methods = clazz.getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getName().equals(method))
				return methods[i];
		}
		return null;
	}
}
