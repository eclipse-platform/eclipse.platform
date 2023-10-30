package org.eclipse.core.pki;

import java.security.KeyStore;

public interface AuthenticationService  {
	public KeyStore initialize(char[] p);
	public String findPkcs11CfgLocation();
}
