/**
 * 
 */
package de.hannit.fsch.klr.dataservice.mssql;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import de.hannit.fsch.common.mitarbeiter.Mitarbeiter;
import de.hannit.fsch.klr.dataservice.DataService;


/**
 * @author fsch
 *
 */
public class MSSQLDataService implements DataService 
{
private	Properties props;
private SQLServerDataSource ds = new SQLServerDataSource();

private Connection con;
private PreparedStatement ps;
private ResultSet rs;
private String info = "Nicht verbunden";

private ArrayList<Mitarbeiter> mitarbeiter = null;	

	/**
	 * 
	 */
	public MSSQLDataService() 
	{
	props = new Properties();

		try 
		{
		URL configURL = this.getClass().getClassLoader().getResource("dbProperties.xml");
		File configFile = new File(FileLocator.toFileURL(configURL).getPath());	
		InputStream in = new FileInputStream(configFile);
		props.loadFromXML(in);
		
		ds.setServerName(props.getProperty("host", "localhost"));
		ds.setPortNumber(Integer.parseInt(props.getProperty("port", "1433")));
		ds.setDatabaseName(props.getProperty("databaseName"));
		ds.setUser(props.getProperty("user"));
		ds.setPassword(props.getProperty("password"));

		con = ds.getConnection();
		
	    DatabaseMetaData dbmd = con.getMetaData();
	    this.info = "Benutzer " + dbmd.getUserName();
	    this.info += " verbunden mit " + dbmd.getDatabaseProductName() + " (" + dbmd.getDatabaseProductVersion() + ")";
	    this.info += " - " + dbmd.getDriverName() + " (" + dbmd.getDriverVersion() + ")";
	    } 
		catch (IOException e) 
		{
		e.printStackTrace();
		}
		catch (SQLException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public ArrayList<Mitarbeiter> getMitarbeiter() 
	{
	Mitarbeiter m = null;	
	mitarbeiter = new ArrayList<Mitarbeiter>();
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_MITARBEITER);
		rs = ps.executeQuery();
		String test = rs.getStatement().toString();
		
	      while (rs.next()) 
	      {
	    	  m = new Mitarbeiter();
	    	  m.setPersonalNR(rs.getInt(1));
	    	  m.setBenutzerName((rs.getString(2) != null ? rs.getString(2) : "unbekannt"));
	    	  m.setNachname(rs.getString(3));
	    	  m.setVorname((rs.getString(4) != null ? rs.getString(4) : "unbekannt"));
	    	  
	    	  mitarbeiter.add(m);
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	
	return mitarbeiter;
	}

	@Override
	public void setMitarbeiter(ArrayList<String[]> fields) 
	{
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_MITARBEITER);
			for (String[] strings : fields) 
			{
			ps.setInt(1, Integer.parseInt(strings[0]));
				switch (strings[1].length()) 
				{
				case 2:
				ps.setNull(2, Types.VARCHAR);	
				break;
	
				default:
				String test = remMoveQuotes(strings[1]);
				ps.setString(2, test);
				break;
			}
			ps.setString(3, remMoveQuotes(strings[2]));
			ps.setString(4, remMoveQuotes(strings[3]));
			
			ps.execute();
			}
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}
		
	}

	private String remMoveQuotes(String string) 
	{
	return string.replace("\"","");	
	}

	@Override
	public String getConnectionInfo() 
	{
	return info;
	}

}
