package org.eclipse.ui.pki.pkcs;

import java.util.List;

import javax.net.ssl.SSLContext;


public class ProviderImpl  {
	public static SSLPkcs11Provider security = SSLPkcs11Provider.getInstance();
	public static SSLPkcs11Provider getNewInstance() {
		security=SSLPkcs11Provider.getInstance();
		return security;
	}
	@SuppressWarnings("unchecked")
	public List<String> getList() {
		return (List<String>) security.getList();
	}
	public String getAlias() {
		return security.getAlias();
	}
	@SuppressWarnings("static-access")
	public void off() {
		security.disable();
	}
}