/**
 * 
 */
package de.hannit.fsch.klr.dataservice.mssql;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;

import de.hannit.fsch.common.mitarbeiter.Mitarbeiter;
import de.hannit.fsch.klr.dataservice.DataService;


/**
 * @author fsch
 *
 */
public class MSSQLDataService implements DataService 
{
private	Properties props;
private String url = "jdbc:sqlserver://localhost:1433;databaseName=AdventureWorks;user=MyUserName;password=*****;";	

	/**
	 * 
	 */
	public MSSQLDataService() 
	{
	props = new Properties();

		try 
		{
		props.loadFromXML(new FileInputStream("dbProperties.xml"));
		} 
		catch (IOException e) 
		{
		e.printStackTrace();
		}
	}

	@Override
	public ArrayList<Mitarbeiter> getMitarbeiter() 
	{
		// TODO Auto-generated method stub
		return null;
	}

}
