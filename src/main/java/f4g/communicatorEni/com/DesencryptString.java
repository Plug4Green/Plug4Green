/**
* ============================== Header ============================== 
* file:          DesencryptString.java
* project:       FIT4Green/CommunicatorEni
* created:       04/08/2011 by jclegea
* 
* $LastChangedDate: 2011-09-13 09:27:56 +0200 (mar, 13 sep 2011) $ 
* $LastChangedBy: jclegea $
* $LastChangedRevision: 745 $
* 
* short description:
*   {To be completed}
* ============================= /Header ==============================
*/
package org.f4g.com;

/**
 * {To be completed; use html notation, if necessary}
 * 
 *
 * @author jclegea
 */
public final class DesencryptString {
	private static String search_mask="4AD8y9JBCTN70E2H61KdLpPOixQbojRweInuVqFzXrkGS5ZafWghl3mYsvcUtM";
	private static String encrypt_mask="G679BquJDEwoOKWLaM1xNl8Qp4RkTUfVz52sXCZbIyrc0deHhP3iAjSmngFtvY";
	
	public static String desencryptString(String source)
	{
		int i=0;
		String result="";
		
		for(i=0;i<source.length();i++)
		{
			result += desencryptChar(source.substring(i, i + 1),source.length(), i);
		}
		return result;
	}
	
	public static String desencryptChar(String source, int variable, int a_index)
	{
		int index = 0;
		if(encrypt_mask.indexOf(source)!= -1)
		{
			if((encrypt_mask.indexOf(source) - variable - a_index) > 0)
			{
				index = (encrypt_mask.indexOf(source) - variable - a_index) % encrypt_mask.length();
			}
			else
			{
				index = ((search_mask.length()) + (encrypt_mask.indexOf(source) - variable - a_index)) % encrypt_mask.length();
			}
			return search_mask.substring(index, index + 1);
		}
		else
		{
			return source;
		}
			
		
	}
}
