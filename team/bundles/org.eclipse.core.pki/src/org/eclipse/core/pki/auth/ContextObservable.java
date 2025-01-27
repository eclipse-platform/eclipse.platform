/**
 * Copyright (c) 2014 Codetrails GmbH.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marcel Bruch - initial API and implementation.
 */
package org.eclipse.core.pki.auth;

import java.util.Observable;
import org.eclipse.core.pki.util.LogUtil;

public class ContextObservable extends Observable {

	public ContextObservable() {

	}
	public void onchange(String s) {
		//LogUtil.logWarning("ContextObservable- BREAK for INPUT");
		
		try {
			setChanged();
			// notify observers for change
			notifyObservers(s);
		} catch (Exception e) {
			LogUtil.logError("ContextObservable - Failed to notify observers, PKI password input.", e); //$NON-NLS-1$
		}
		/*
		 * try { Thread.sleep(1000); } catch (InterruptedException e) {
		 * System.out.println("Error Occurred."); }
		 */
	}
}
