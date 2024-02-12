package org.eclipse.ui.pki.pkcs;

import java.util.List;

import org.eclipse.core.pki.AuthenticationBase;

public interface EclipsePkiProvider {
	public AuthenticationBase security = AuthenticationBase.INSTANCE;
	public List<String> getList(); 
	public String getAlias();
	public void off();
}
