/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui.stringsubstitution;

import org.eclipse.core.variables.IStringVariable;
import org.eclipse.swt.widgets.Shell;


/**
 * A variable presentation extension can contribute an argument selector
 * which is use to configure the argument for a string substitution
 * variable.
 *
 * @since 3.9
 */
public interface IArgumentSelector {

	/**
	 * Selects and returns an argument for the given variable,
	 * or <code>null</code> if none.
	 *
	 * @param variable the variable an argument is being selected for
	 * @param shell the shell to create any dialogs on, or <code>null</code> if none
	 * @return argument for the given variable or <code>null</code>
	 *  if none
	 */
	String selectArgument(IStringVariable variable, Shell shell);
}
