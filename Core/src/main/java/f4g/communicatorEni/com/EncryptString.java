/**
* ============================== Header ============================== 
* file:          EncryptString.java
* project:       FIT4Green/CommunicatorEni
* created:       04/08/2011 by jclegea
* 
* $LastChangedDate: 2011-08-05 09:48:43 +0200 (vie, 05 ago 2011) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 672 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package f4g.communicatorEni.com;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public final class EncryptString {
	private static String search_mask="4AD8y9JBCTN70E2H61KdLpPOixQbojRweInuVqFzXrkGS5ZafWghl3mYsvcUtM";
	private static String encrypt_mask="G679BquJDEwoOKWLaM1xNl8Qp4RkTUfVz52sXCZbIyrc0deHhP3iAjSmngFtvY";
	
	public static String encryptString(String source)
	{
		int i=0;
		String result="";
		
		for(i=0;i<source.length();i++)
		{
			result += encryptChar(source.substring(i, i + 1),source.length(),i);
		}
		
		return result;
	}
	
	public static String encryptChar(String source, int variable, int a_index)
	{		
		int index = 0;
		if(search_mask.indexOf(source) != -1)
		{
			index = (search_mask.indexOf(source) + variable + a_index) % search_mask.length();
			return encrypt_mask.substring(index, index + 1);
		}
		return source;		
	}
}
