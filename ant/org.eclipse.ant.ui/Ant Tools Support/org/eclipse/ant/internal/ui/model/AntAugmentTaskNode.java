/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.RuntimeConfigurable;
import org.apache.tools.ant.Task;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.preferences.AntEditorPreferenceConstants;

/**
 * Supports the Ant augment task, which over-writes (augments) existing nodes based on their id
 *
 * @see "http://ant.apache.org/manual/Tasks/augment.html"
 */
public class AntAugmentTaskNode extends AntTaskNode {

	String attrId = null;

	public AntAugmentTaskNode(Task task, String label) {
		super(task, label);
		// we need to cache the id as it is removed from the wrapping attribute list when the AugemntReference is configured
		RuntimeConfigurable wrapper = task.getRuntimeConfigurableWrapper();
		attrId = (String) wrapper.getAttributeMap().get(IAntCoreConstants.ID);
	}

	@Override
	public boolean configure(boolean validateFully) {
		if (fConfigured) {
			return false;
		}
		// only configure if the user cares about the problems
		try {
			getTask().maybeConfigure();
			fConfigured = true;
			return true;
		}
		catch (BuildException be) {
			handleBuildException(be, AntEditorPreferenceConstants.PROBLEM_TASKS);
		}
		catch (AntSecurityException se) {
			// either a system exit or setting of system property was attempted
			handleBuildException(new BuildException(AntModelMessages.AntTaskNode_0), AntEditorPreferenceConstants.PROBLEM_SECURITY);
		}
		catch (IllegalStateException ise) {
			// this is thrown when the id to augment does not exist
			// https://bugs.eclipse.org/bugs/show_bug.cgi?id=396219
			handleBuildException(new BuildException(ise), AntEditorPreferenceConstants.PROBLEM_TASKS);
		}
		return false;
	}

	@Override
	public boolean containsOccurrence(String identifier) {
		if (identifier != null) {
			boolean prop = identifier.startsWith("${") && identifier.endsWith("}"); //$NON-NLS-1$ //$NON-NLS-2$
			// the 'id' is the only attribute of an augment
			if (!prop) {
				return identifier.equals(attrId);
			}
			return attrId != null && identifier.contains(attrId);
		}
		return false;
	}

	@Override
	public List<Integer> computeIdentifierOffsets(String identifier) {
		// augment nodes can only contain an occurrence of the id of the task
		// they augment, compute the offset based on the the index of the id + the node offset
		if (attrId != null && identifier != null && identifier.length() > 0) {
			String text = getAntModel().getText(getOffset(), getLength());
			if (text == null || text.length() == 0) {
				return null;
			}
			ArrayList<Integer> list = new ArrayList<>();
			int idx = text.indexOf(attrId);
			if (idx > -1) {
				list.add(Integer.valueOf(getOffset() + idx));
			}
			return list;
		}
		return null;
	}
}
