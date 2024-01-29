/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki.auth;

public enum PKIState {
	CONTROL;
	private boolean isPKCS11on=false;
	private boolean isPKCS12on=false;
	public boolean isPKCS11on() {
		return isPKCS11on;
	}
	public void setPKCS11on(boolean isPKCS11on) {
		this.isPKCS11on = isPKCS11on;
	}
	public boolean isPKCS12on() {
		return isPKCS12on;
	}
	public void setPKCS12on(boolean isPKCS12on) {
		this.isPKCS12on = isPKCS12on;
	}

}
