/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.security.managers;

import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.ProtectionParameter;

import javax.security.auth.callback.CallbackHandler;

public class PkiLoadParameter implements LoadStoreParameter{
	ProtectionParameter protectionParameter;
	ProtectionParameter SOProtectionParameter;
	CallbackHandler eventHandler;
	boolean waitForSlot;
	Long slotId;
	boolean writeEnabled;
	@Override
	public ProtectionParameter getProtectionParameter() {
		return protectionParameter;
	}
	public ProtectionParameter getSOProtectionParameter() {
		return SOProtectionParameter;
	}

	public void setSOProtectionParameter(ProtectionParameter sOProtectionParameter) {
		SOProtectionParameter = sOProtectionParameter;
	}

	public CallbackHandler getEventHandler() {
		return eventHandler;
	}

	public void setEventHandler(CallbackHandler eventHandler) {
		this.eventHandler = eventHandler;
	}

	public boolean isWaitForSlot() {
		return waitForSlot;
	}

	public void setWaitForSlot(boolean waitForSlot) {
		this.waitForSlot = waitForSlot;
	}

	public Long getSlotId() {
		return slotId;
	}

	public void setSlotId(Long slotId) {
		this.slotId = slotId;
	}

	public boolean isWriteEnabled() {
		return writeEnabled;
	}

	public void setWriteEnabled(boolean writeEnabled) {
		this.writeEnabled = writeEnabled;
	}

	public void setProtectionParameter(ProtectionParameter protectionParameter) {
		this.protectionParameter = protectionParameter;
	}
}
