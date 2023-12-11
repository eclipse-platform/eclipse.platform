package org.eclipse.ui.pki.util;

public class ExpiredX509CertificateData {
	private String alias;
	private String certLocation;
	private String distinguishedName;
	private String expirationDate;
	private String issuedBy;
	private String serialNumber;
	
	public ExpiredX509CertificateData() {}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getCertLocation() {
		return certLocation;
	}

	public void setCertLocation(String certLocation) {
		this.certLocation = certLocation;
	}

	public String getDistinguishedName() {
		return distinguishedName;
	}

	public void setDistinguishedName(String distinguishedName) {
		this.distinguishedName = distinguishedName;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public String getIssuedBy() {
		return issuedBy;
	}

	public void setIssuedBy(String issuedBy) {
		this.issuedBy = issuedBy;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}	
}