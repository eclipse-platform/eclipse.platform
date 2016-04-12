/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.provider.Diff;
import org.eclipse.team.ui.mapping.ITeamStateDescription;

/**
 * An implementation of {@link ITeamStateDescription}.
 * <p>
 * This class may be subclassed by clients.
 * @since 3.2
 */
public class TeamStateDescription implements ITeamStateDescription {

	private int state;
	private Map properties = new HashMap();

	/**
	 * Create a description with the given state.
	 * @param state the state
	 */
	public TeamStateDescription(int state) {
		this.state = state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getStateFlags()
	 */
	@Override
	public int getStateFlags() {
		return state;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getKind()
	 */
	@Override
	public int getKind() {
		return getStateFlags() & Diff.KIND_MASK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.IDecoratedStateDescription#getDirection()
	 */
	@Override
	public int getDirection() {
		return getStateFlags() & IThreeWayDiff.DIRECTION_MASK;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateDescription#getProperties()
	 */
	@Override
	public String[] getPropertyNames() {
		return (String[]) properties.keySet().toArray(new String[properties.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.ITeamStateDescription#getProperty(java.lang.String)
	 */
	@Override
	public Object getProperty(String property) {
		return properties.get(property);
	}
	
	/**
	 * Set the given property to the given value
	 * @param property the property
	 * @param value the value
	 */
	public void setProperty(String property, Object value) {
		properties.put(property, value);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TeamStateDescription) {
			TeamStateDescription dsd = (TeamStateDescription) obj;
			if (dsd.getStateFlags() == state) {
				if (haveSameProperties(this, dsd)) {
					String[] properties = getPropertyNames();
					for (int i = 0; i < properties.length; i++) {
						String property = properties[i];
						Object o1 = this.getProperty(property);
						Object o2 = dsd.getProperty(property);
						if (!o1.equals(o2)) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}
		return super.equals(obj);
	}

	private boolean haveSameProperties(TeamStateDescription d1, TeamStateDescription d2) {
		String[] p1 = d1.getPropertyNames();
		String[] p2 = d2.getPropertyNames();
		if (p1.length != p2.length) {
			return false;
		}
		for (int i = 0; i < p1.length; i++) {
			String s1 = p1[i];
			boolean found = false;
			for (int j = 0; j < p2.length; j++) {
				String s2 = p2[j];
				if (s1.equals(s2)) {
					found = true;
					break;
				}
			}
			if (!found)
				return false;
		}
		return true;
	}

}
