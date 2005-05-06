/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PublicCertificateI.java,v 1.1 2005/05/06 15:55:58 evos Exp $
 * 
 * Created on 09-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

/**
 * @author Erik
 */
public interface PublicCertificateI extends Certificate {

	/**
	 * @return
	 */
	public boolean isAvailable();

	/**
	 * @return
	 */
	public Portfolio getPortfolio();

	/**
	 * @return
	 */
	public boolean isPresident();

	/**
	 * Get the number of shares that this certificate represents.
	 * @return The number of shares.
	 */
	public int getShares();

	/** 
	 * Get the percentage of ownership that this certificate represents.
	 * This is equal to the number of shares * the share unit.
	 * @return The share percentage.
	 */ 
	public int getShare();

	/**
	 * Get the current price of this certificate.
	 * @return The current certificate price.
	 */
	public int getCertificatePrice();
	
	/**
	 * @param b
	 */
	public void setAvailable(boolean b);

	/**
	 * @param portfolio
	 */
	public void setPortfolio(Portfolio portfolio);

	/**
	 * @param b
	 */
	public void setPresident(boolean b);

	/**
	 * @return
	 */
	public PublicCompanyI getCompany();

	/**
	 * @param companyI
	 */
	public void setCompany(PublicCompanyI companyI);
	
	/** Clone this certificate */
	public PublicCertificateI copy();

}
