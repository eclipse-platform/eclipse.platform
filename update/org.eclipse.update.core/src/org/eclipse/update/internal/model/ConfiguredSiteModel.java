package org.eclipse.update.internal.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.update.core.model.ModelObject;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * 
 */
public class ConfiguredSiteModel extends ModelObject {
	private String[] previousPluginPath;

	private SiteModel site;
	private String platformURLString;
	private ConfigurationPolicyModel policy;
	private InstallConfigurationModel installConfiguration;
	private boolean installable = false;

	/**
	 * Constructor
	 */
	public ConfiguredSiteModel() {
		super();
	}

	/**
	 * returns the site
	 * @return The ISite 
	 * @since 2.0
	 */
	public SiteModel getSiteModel() {
		return site;
	}

	/**
	 * Sets the site.
	 * @param site The site to set
	 */
	public void setSiteModel(SiteModel site) {
		assertIsWriteable();
		this.site = site;
	}

	/**
	 * returns the policy
	 */
	public ConfigurationPolicyModel getConfigurationPolicyModel() {
		return policy;
	}

	/**
	 * 
	 * @since 2.0
	 */
	public void setConfigurationPolicyModel(ConfigurationPolicyModel policy) {
		assertIsWriteable();
		this.policy = policy;
		policy.setConfiguredSiteModel(this);
	}

	/**
	 * @since
	 */
	public boolean isUpdatable() {
		return installable;
	}

	/**
	 * @since 2.0
	 */
	public void isUpdatable(boolean installable) {
		assertIsWriteable();
		this.installable = installable;
	}

	/**
	 * Gets the installConfiguration.
	 * @return Returns a InstallConfigurationModel
	 */
	public InstallConfigurationModel getInstallConfigurationModel() {
		return installConfiguration;
	}

	/**
	 * Sets the installConfiguration.
	 * @param installConfiguration The installConfiguration to set
	 */
	public void setInstallConfigurationModel(InstallConfigurationModel installConfiguration) {
		assertIsWriteable();
		this.installConfiguration = installConfiguration;
	}

	/**
	 * Gets the platformURLString.
	 * @return Returns a String
	 */
	public String getPlatformURLString() {
		return platformURLString;
	}

	/**
	 * Sets the platformURLString.
	 * @param platformURLString The platformURLString to set
	 */
	public void setPlatformURLString(String platformURLString) {
		this.platformURLString = platformURLString;
	}

	
		/**
	 * Gets the previousPluginPath. The list of plugins the platform had.
	 * @return Returns a String[]
	 */
	public String[] getPreviousPluginPath() {
		if (previousPluginPath == null)
			previousPluginPath = new String[0];
		return previousPluginPath;
	}

	/**
	 * Sets the previousPluginPath.
	 * @param previousPluginPath The previousPluginPath to set
	 */
	public void setPreviousPluginPath(String[] previousPluginPath) {
		this.previousPluginPath = new String[previousPluginPath.length];
		System.arraycopy(previousPluginPath, 0, this.previousPluginPath, 0, previousPluginPath.length);
	}

	/*
	 * creates a Status
	 */
	protected IStatus createStatus(int statusType, String msg, Exception e){
		if (statusType!=IStatus.OK) statusType = IStatus.ERROR;
		return createStatus(statusType,IStatus.OK, msg.toString(), e);
	}

	/*
	 * creates a Status
	 */
	protected IStatus createStatus(int statusSeverity, int statusCode, String msg, Exception e){
		String id =
			UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();
	
		StringBuffer completeString = new StringBuffer("");
		if (msg!=null)
			completeString.append(msg);
		if (e!=null){
			completeString.append("\r\n[");
			completeString.append(e.toString());
			completeString.append("]\r\n");
		}
		return new Status(statusSeverity, id, statusCode, completeString.toString(), e);
	}
}