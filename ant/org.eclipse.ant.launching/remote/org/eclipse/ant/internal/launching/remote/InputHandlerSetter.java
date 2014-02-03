/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.remote;


import java.text.MessageFormat; // can't use ICU, used by ant

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.DefaultInputHandler;
import org.apache.tools.ant.input.InputHandler;

/**
 * This class exists so that the Ant integration has backwards compatibility
 * with Ant releases previous to 1.5. InputHandlers are a new feature of Ant 1.5.
 */
class InputHandlerSetter {

	protected void setInputHandler(Project project, String inputHandlerClassname) {
		InputHandler handler = null;
		if (inputHandlerClassname == null) {
			handler = new DefaultInputHandler();
		} else {
			try {
				handler = (InputHandler)(Class.forName(inputHandlerClassname).newInstance());
			} catch (ClassCastException e) {
				String msg = MessageFormat.format(RemoteAntMessages.getString("InternalAntRunner.The_specified_input_handler_class_{0}_does_not_implement_the_org.apache.tools.ant.input.InputHandler_interface_5"), new String[]{inputHandlerClassname}); //$NON-NLS-1$
				throw new BuildException(msg, e);
			} catch (Exception e) {
				String msg = MessageFormat.format(RemoteAntMessages.getString("InternalAntRunner.Unable_to_instantiate_specified_input_handler_class_{0}___{1}_6"), new String[]{inputHandlerClassname, e.getClass().getName()}); //$NON-NLS-1$
				throw new BuildException(msg, e);
			}
		}
		project.setInputHandler(handler);
	}
}
