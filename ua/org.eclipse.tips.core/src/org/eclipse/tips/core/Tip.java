/****************************************************************************
 * Copyright (c) 2017, 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Wim Jongman <wim.jongman@remainsoftware.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.tips.core;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * This is the base Tip class of the UI agnostic Tip framework. You might want
 * to check specializations of this class that may make your life easier.
 *
 */
public abstract class Tip {

	private String providerId;

	private final List<TipAction> fActions = new ArrayList<>();

	/**
	 * Constructor for a Tip. For the best user experience, Tips should be created
	 * really fast.
	 *
	 */
	public Tip(String providerId) {
		this.providerId = providerId;
	}

	/**
	 * @return the id of the provider that created this tip.
	 */
	public String getProviderId() {
		return providerId;
	}

	/**
	 * A getter for a list of {@link TipAction}s for this tip. Clients may override
	 * or provide the actions through the constructor.
	 *
	 * @return the list of actions, never null but could be empty.
	 */
	public List<TipAction> getActions() {
		return fActions;
	}

	/**
	 * Return the publish date of the tip. The UI could decide to server newer tips
	 * first.
	 *
	 * @return the date this tip was published which may not be null.
	 */
	public abstract Date getCreationDate();

	/**
	 * @return the subject which may not be null.
	 */
	public abstract String getSubject();

	@Override
	public int hashCode() {
		return Objects.hash(getCreationDate(), providerId, getSubject());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Tip other = (Tip) obj;
		return Objects.equals(getCreationDate(), other.getCreationDate()) //
				&& Objects.equals(providerId, other.providerId) //
				&& Objects.equals(getSubject(), other.getSubject());
	}
}