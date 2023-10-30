package org.eclipse.pki.interfaces;

import java.util.List;

import org.eclipse.pki.pkcs.ProviderImpl;

public interface IProvider {
	public IProvider iprovide = new ProviderImpl();
	public List getList();
	public String getAlias(); 
}
