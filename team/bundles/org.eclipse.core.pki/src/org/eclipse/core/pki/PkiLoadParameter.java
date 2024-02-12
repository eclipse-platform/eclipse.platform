package org.eclipse.core.pki;

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
		// TODO Auto-generated method stub
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
