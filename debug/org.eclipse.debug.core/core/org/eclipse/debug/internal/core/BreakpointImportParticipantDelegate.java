/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.debug.core.model.IBreakpointImportParticipant;

/**
 * Proxy to a breakpointImportParticipant extension.
 * Client can contribute participant through the <code>breakpointImportParticipant</code> extension point
 *
 * Example contribution:
 * <pre>
 * <extension
         point="org.eclipse.debug.core.breakpointImportParticipant">
      <ImportParticipant
            participant="x.y.z.BreakpointImportParticipant"
            type="org.eclipse.jdt.debug.javaLineBreakpointMarker">
      </ImportParticipant>
   </extension>
 * </pre>
 *
 * @noextend This class is not intended to be sub-classed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 *
 * @since 3.5
 */
public class BreakpointImportParticipantDelegate {

	/**
	 * The configuration element for this delegate
	 */
	private IConfigurationElement fElement = null;
	private IBreakpointImportParticipant fParticipant = null;

	/**
	 * Constructor
	 * @param element the element this proxy is created on
	 */
	public BreakpointImportParticipantDelegate(IConfigurationElement element) {
		fElement = element;
	}

	/**
	 * Returns the {@link IBreakpointImportParticipant} delegate or <code>null</code> of there was
	 * a problem loading the delegate
	 *
	 * @return the {@link IBreakpointImportParticipant} or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 */
	public IBreakpointImportParticipant getDelegate() throws CoreException {
		if(fParticipant == null) {
			fParticipant = (IBreakpointImportParticipant) fElement.createExecutableExtension(IConfigurationElementConstants.PARTICIPANT);
		}
		return fParticipant;
	}

	/**
	 * Returns the marker type this participant is registered for.
	 *
	 * @return the marker type this participant is registered for
	 * @throws CoreException if a problem is encountered
	 */
	public String getType() throws CoreException {
		return fElement.getAttribute(IConfigurationElementConstants.TYPE);
	}
}
