/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import java.util.EventListener;

/**
 * A subscriber change listener is notified of changes to resources
 * regarding their subscriber synchronization state.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see Subscriber#addListener(ISubscriberChangeListener)
 * @since 3.0
 */
public interface ISubscriberChangeListener extends EventListener{

	/**
	 * Notifies this listener that some resources' subscriber properties have
	 * changed. The changes have already happened. For example, a resource's
	 * base revision may have changed. The resource tree may or may not be open for modification
	 * when this method is invoked.
	 *
	 * @param deltas detailing the kinds of changes
	 */
	public void subscriberResourceChanged(ISubscriberChangeEvent[] deltas);
}

