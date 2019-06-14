/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
	private Map<String, Object> properties = new HashMap<>();

	/**
	 * Create a description with the given state.
	 * @param state the state
	 */
	public TeamStateDescription(int state) {
		this.state = state;
	}

	@Override
	public int getStateFlags() {
		return state;
	}

	@Override
	public int getKind() {
		return getStateFlags() & Diff.KIND_MASK;
	}

	@Override
	public int getDirection() {
		return getStateFlags() & IThreeWayDiff.DIRECTION_MASK;
	}

	@Override
	public String[] getPropertyNames() {
		return properties.keySet().toArray(new String[properties.size()]);
	}

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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof TeamStateDescription) {
			TeamStateDescription dsd = (TeamStateDescription) obj;
			if (dsd.getStateFlags() == state) {
				if (haveSameProperties(this, dsd)) {
					String[] properties = getPropertyNames();
					for (String property : properties) {
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
		for (String s1 : p1) {
			boolean found = false;
			for (String s2 : p2) {
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
