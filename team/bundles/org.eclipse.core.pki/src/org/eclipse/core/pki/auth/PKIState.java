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
