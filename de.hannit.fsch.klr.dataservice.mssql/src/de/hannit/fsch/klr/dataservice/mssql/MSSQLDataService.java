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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import org.eclipse.core.runtime.FileLocator;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import de.hannit.fsch.common.csv.azv.AZVDatensatz;
import de.hannit.fsch.common.loga.LoGaDatensatz;
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

private Calendar cal = Calendar.getInstance();

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
	public SQLException setLoGaDaten(LoGaDatensatz datenSatz)
	{
	SQLException e = null;	
	boolean result = false;
	try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_LOGA);
		ps.setInt(1, datenSatz.getPersonalNummer());
		ps.setDate(2, datenSatz.getAbrechnungsMonatSQL());
		ps.setDouble(3, datenSatz.getBrutto());
		ps.setString(4, datenSatz.getTarifGruppe());
		ps.setInt(5, datenSatz.getTarifstufe());
		ps.setDouble(6, datenSatz.getStellenAnteil());
		
		result = ps.execute();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}	
	return e;
	}
	
	@Override
	public SQLException setAZVDaten(AZVDatensatz datenSatz)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_AZV);
		ps.setInt(1, datenSatz.getPersonalNummer());
		ps.setInt(2, datenSatz.getiTeam());
		ps.setDate(3, datenSatz.getBerichtsMonatSQL());
		
			if (datenSatz.getKostenstelle() != null)
			{
			ps.setString(4, datenSatz.getKostenstelle());
			ps.setNull(5, Types.NULL);
			}
			else
			{
			ps.setNull(4, Types.NULL);
			ps.setString(5, datenSatz.getKostentraeger());
			}
		ps.setInt(6, datenSatz.getProzentanteil());
			
		result = ps.execute();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}	
	return e;
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

	@Override
	public boolean existsMitarbeiter(int personalNummer)
	{
	boolean result = false;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_MITARBEITER_PERSONALNUMMER);
		ps.setInt(1, personalNummer);
		rs = ps.executeQuery();
				
	    result = rs.next();
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return result;
	}

	@Override
	public boolean existsKostenstelle(String kostenStelle)
	{
	boolean exists = false;	
		try 
		{
			if (rs != null)
			{
			rs.close();
			rs = null;
			}	
		ps = con.prepareStatement(PreparedStatements.SELECT_COUNT_KOSTENSTELLE);
		ps.setString(1, kostenStelle);
		rs = ps.executeQuery();
		rs.next();
			
		exists = rs.getInt(PreparedStatements.COUNT_COLUMN) > 0 ? true : false;
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
		finally
		{
			try
			{
			rs.close();
			if (rs != null)	{rs = null;}
			}
			catch (SQLException e)
			{
			e.printStackTrace();
			}	
		}
	return exists;
	}

	@Override
	public boolean existsKostentraeger(String kostenTraeger)
	{
	boolean exists = false;	
		try 
		{
			if (rs != null)
			{
			rs.close();
			rs = null;
			}	
		ps = con.prepareStatement(PreparedStatements.SELECT_COUNT_KOSTENTRAEGER);
		ps.setString(1, kostenTraeger);
		rs = ps.executeQuery();
		rs.next();
			
		exists = rs.getInt(PreparedStatements.COUNT_COLUMN) > 0 ? true : false;
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
		finally
		{
			try
			{
			rs.close();
			if (rs != null)	{rs = null;}
			}
			catch (SQLException e)
			{
			e.printStackTrace();
			}	
		}
	return exists;
	}

	@Override
	public SQLException setKostenstelle(String kostenStelle, String kostenStellenBezeichnung)
	{
	SQLException e = null;	
	boolean result = false;
	// TODO: von Datum wird bisher nur mit Dummy = 01.01.2012 befüllt
	cal.set(2012, Calendar.JANUARY, 1);
		try 
			{
			ps = con.prepareStatement(PreparedStatements.INSERT_KOSTENSTELLE);
			ps.setString(1, kostenStelle);
			ps.setString(2, kostenStellenBezeichnung);
			ps.setDate(3, new Date(cal.getTimeInMillis()));
			
			result = ps.execute();
			} 
			catch (SQLException exception) 
			{
			exception.printStackTrace();
			e = exception;
			}	
	return e;
	}
	
	@Override
	public SQLException setKostentraeger(String kostenTraeger, String kostenTraegerBezeichnung)
	{
	SQLException e = null;	
	boolean result = false;
	// TODO: von Datum wird bisher nur mit Dummy = 01.01.2012 befüllt
	cal.set(2012, Calendar.JANUARY, 1);
		try 
			{
			ps = con.prepareStatement(PreparedStatements.INSERT_KOSTENTRAEGER);
			ps.setString(1, kostenTraeger);
			ps.setString(2, kostenTraegerBezeichnung);
			ps.setDate(3, new Date(cal.getTimeInMillis()));
			
			result = ps.execute();
			} 
			catch (SQLException exception) 
			{
			exception.printStackTrace();
			e = exception;
			}	
	return e;
	}

	@Override
	public SQLException setDatenimport(String name, String pfad, int anzahlDaten, Date berichtsMonat, String datenQuelle)
	{
	// [Importdatum],[Dateiname],[Pfad],[AnzahlDaten],[Berichtsmonat],[Datenquelle]	
	SQLException e = null;	
	boolean result = false;
	System.out.println(pfad);
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_DATENIMPORT);
		ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
		ps.setString(2, name);
		ps.setString(3, pfad);
		ps.setInt(4, anzahlDaten);
		ps.setDate(5, berichtsMonat);
		ps.setString(6, datenQuelle);
				
		result = ps.execute();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}	
	return e;
	}
}
