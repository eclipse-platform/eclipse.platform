/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.registry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class TeamDecoratorDescription {

	private static final String TAG_TEAM_DECORATOR = "teamDecorator"; //$NON-NLS-1$
	private static final String ATT_REPOSITORY_ID = "repositoryId"; //$NON-NLS-1$
	private static final String ATT_DECORATOR_ID = "decoratorId"; //$NON-NLS-1$
	private static final String ATT_DECORATED_DIRECTION_ID = "decoratedDirection"; //$NON-NLS-1$
	private static final String OUTGOING_FLAG = "OUTGOING"; //$NON-NLS-1$
	private static final String INCOMING_FLAG = "INCOMING"; //$NON-NLS-1$

	private String repositoryId;
	private String decoratorId;
	private int decoratedDirection;

	public TeamDecoratorDescription(IExtension extension) throws CoreException {
		readExtension(extension);
	}

	/**
	 * Initialize this descriptor based on the provided extension point.
	 */
	protected void readExtension(IExtension extension) throws CoreException {
		//read the extension
		String id = extension.getUniqueIdentifier(); // id not required
		IConfigurationElement[] elements = extension.getConfigurationElements();
		int count = elements.length;
		for (int i = 0; i < count; i++) {
			IConfigurationElement element = elements[i];
			String name = element.getName();
			if (name.equalsIgnoreCase(TAG_TEAM_DECORATOR)) {
				repositoryId = element.getAttribute(ATT_REPOSITORY_ID);
				decoratorId = element.getAttribute(ATT_DECORATOR_ID);
				String flags = element.getAttribute(ATT_DECORATED_DIRECTION_ID);
				if (flags == null) {
					decoratedDirection = IThreeWayDiff.INCOMING | IThreeWayDiff.OUTGOING;
				} else {
					if (flags.contains(INCOMING_FLAG)) {
						decoratedDirection |= IThreeWayDiff.INCOMING;
					}
					if (flags.contains(OUTGOING_FLAG)) {
						decoratedDirection |= IThreeWayDiff.OUTGOING;
					}
					if (decoratedDirection == 0) {
						decoratedDirection = IThreeWayDiff.INCOMING | IThreeWayDiff.OUTGOING;
					}
				}
			}
		}
		if (repositoryId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_1, new String[] { ATT_REPOSITORY_ID, TAG_TEAM_DECORATOR, id == null ? "" : id})); //$NON-NLS-1$
		if (repositoryId == null)
			fail(NLS.bind(TeamUIMessages.TeamContentProviderDescriptor_1, new String[] { ATT_DECORATOR_ID, TAG_TEAM_DECORATOR, id == null ? "" : id})); //$NON-NLS-1$
	}

	protected void fail(String reason) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, reason, null));
	}

	public String getDecoratorId() {
		return decoratorId;
	}

	public String getRepositoryId() {
		return repositoryId;
	}

	public int getDecoratedDirectionFlags() {
		return decoratedDirection;
	}
}
