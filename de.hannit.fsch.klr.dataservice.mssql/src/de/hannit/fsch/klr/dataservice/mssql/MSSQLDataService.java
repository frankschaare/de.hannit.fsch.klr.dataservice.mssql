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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import de.hannit.fsch.klr.dataservice.DataService;
import de.hannit.fsch.klr.model.azv.AZVDaten;
import de.hannit.fsch.klr.model.azv.AZVDatensatz;
import de.hannit.fsch.klr.model.azv.Arbeitszeitanteil;
import de.hannit.fsch.klr.model.kostenrechnung.KostenStelle;
import de.hannit.fsch.klr.model.kostenrechnung.KostenTraeger;
import de.hannit.fsch.klr.model.loga.LoGaDatensatz;
import de.hannit.fsch.klr.model.mitarbeiter.Mitarbeiter;
import de.hannit.fsch.klr.model.mitarbeiter.Tarifgruppe;
import de.hannit.fsch.klr.model.mitarbeiter.Tarifgruppen;
import de.hannit.fsch.klr.model.organisation.Monatsbericht;
import de.hannit.fsch.klr.model.organisation.Organisation;
import de.hannit.fsch.klr.model.team.TeamMitgliedschaft;


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
private ResultSet subSelect;
private ResultSet rsAZV;
private String info = "Nicht verbunden";

private Calendar cal = Calendar.getInstance();
private DateFormat sqlServerDatumsFormat = new SimpleDateFormat( "yyyy-MM-dd" );

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
		
		ds.setServerName(props.getProperty("host"));
		ds.setPortNumber(Integer.parseInt(props.getProperty("port")));
		
		// ds.setServerName(props.getProperty("host", "localhost"));
		// ds.setPortNumber(Integer.parseInt(props.getProperty("port", "1433")));
		ds.setDatabaseName(props.getProperty("databaseName"));
		ds.setUser(props.getProperty("user"));
		ds.setPassword(props.getProperty("password"));

		con = ds.getConnection();

	    DatabaseMetaData dbmd = con.getMetaData();
	    String[] strURL = dbmd.getURL().split(";");
	    this.info = "Benutzer " + dbmd.getUserName();
	    this.info += " verbunden mit " + strURL[0] + " (" + dbmd.getDatabaseProductVersion() + ")";
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
		
	      while (rs.next()) 
	      {
	    	  m = new Mitarbeiter();
	    	  m.setPersonalNR(rs.getInt(1));
	    	  m.setBenutzerName((rs.getString(2) != null ? rs.getString(2) : "unbekannt"));
	    	  m.setNachname(rs.getString(3));
	    	  m.setVorname((rs.getString(4) != null ? rs.getString(4) : "unbekannt"));
	    	  
	    	  m.setArbeitszeitAnteile(getArbeitszeitanteile(m.getPersonalNR()));
	    	  mitarbeiter.add(m);
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	
	return mitarbeiter;
	}
	
	/**
	 * Liefert die Mitarbeiter inclusive AZV-Daten für den ausgewählten Monat
	 * Schritt 1: 	TreeMap 'aktuell' enthält alle Mitarbeiter, die für den Auswahlmonat Gehalt > 900 € erhalten haben 
	 * 				(so werden Erstattungen herausgehalten)
	 * Schritt 2: 	TreeMap 'mitarbeiter' enthält alle Mitarbeiter, die für den Auswahlmonat eine AZV Meldung agbegeben haben.
	 * Schritt 3: 	Hat Mitarbeiter für den Auswahlmonat keine AZV Meldung abgegeben, wird versucht, die letzte AZV zu laden
	 * Schritt 4: 	Wird auch keine letzte AZV, wird der Mitarbeiter besonders gekennzeichnet. In so einen Fall wird er im
	 * 				NavTree ausgegraut und die AZV muss aus alten Excel-Daten nachgefriemelt werden. Diese Mitarbeiter sollen
	 * 				der Teamleitung gemeldet werden.
	 */
	@Override
	public TreeMap<Integer, Mitarbeiter> getAZVMonat(java.util.Date selectedMonth)
	{
	Mitarbeiter m = null;	
	Arbeitszeitanteil azv = null;
	String strKey = null;
	TreeMap<Integer, Mitarbeiter> mitarbeiter = new TreeMap<Integer, Mitarbeiter>();
	cal.setTime(selectedMonth);
	java.sql.Date sqlDate = new java.sql.Date(cal.getTimeInMillis());
	
			// Schritt 1: alle für den Auswahlmonat 'bezahlten' Mitarbeiter ausgeben
			try
			{
			ps = con.prepareStatement(PreparedStatements.SELECT_MITARBEITER_AKTUELL);
			ps.setDate(1,sqlDate);
			rs = ps.executeQuery();
		    	while (rs.next())
		    	{
		    	Integer iPNR = rs.getInt(1);

			    		if (mitarbeiter.containsKey(iPNR))
			    		{
						m = mitarbeiter.get(iPNR);
			    		}
			    		else
			    		{
			    		m = new Mitarbeiter();
			    		m.setTeamMitgliedschaften(getTeamMitgliedschaften(iPNR));
			    		m.setPersonalNR(iPNR);
			    		m.setNachname(rs.getString(2));
			    		m.setVorname((rs.getString(3) != null ? rs.getString(3) : "unbekannt"));
			    		m.setAbrechnungsMonat(selectedMonth);
			    		m.setBrutto(rs.getDouble(5));
			    		m.setTarifGruppe(rs.getString(6));
			    		m.setStellenAnteil(rs.getDouble(7));
			    		// TODO: WAs ist mit der 900 € Grenze ???			    		
			    		mitarbeiter.put(iPNR, m);
			    		}
		    	}			
			}
			catch (SQLException ex)
			{
			ex.printStackTrace();
			}
			// Schritt 2: für jeden 'bezahlten Mitarbeiter' die AZV Meldungen holen
			try 
			{
			ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE_MITARBEITER);

				for (Integer pnr : mitarbeiter.keySet())
				{
				ArrayList<Arbeitszeitanteil> azvGesamt = new ArrayList<Arbeitszeitanteil>();	
				ps.setInt(1,pnr);
				rs = ps.executeQuery();
					
					while (rs.next())
					{
					azv = new Arbeitszeitanteil();
					azv.setBerichtsMonat(rs.getDate(4));
						if (rs.getString(5) != null)
						{
						azv.setKostenstelle(rs.getString(5));
						azv.setKostenStelleBezeichnung(rs.getString(6));
						}
						else
						{
						azv.setKostentraeger(rs.getString(7));
						azv.setKostenTraegerBezeichnung(rs.getString(8));
						}
					azv.setProzentanteil(rs.getInt(9));
					azv.setITeam(rs.getInt(10));

					azvGesamt.add(azv);  
					}
					
					// Nun liegen alle verfügbaren AZV-Anteile vor. Gibt es welche für den angeforderten Monat (selectedMonth) ?
					if (azvGesamt.size() > 0)
					{
					boolean azvAktuell = false;
						for (Arbeitszeitanteil arbeitszeitanteil : azvGesamt)
						{
							if (arbeitszeitanteil.getBerichtsMonat().equals(sqlDate))
							{
							azvAktuell = true;
							/*
							 * Leider hat sich herausgestellt, das einige Mitarbeiter mehrere Einträge zur gleichen KST / KTR abgeben.
							 * Ein einfaches put (wie bisher) überschreibt dabei einen möglicherweise bereits existierenden Arbeitszeitanteil:
							 */
							// mitarbeiter.get(pnr).getAzvMonat().put(arbeitszeitanteil.getKostenstelleOderKostentraegerLang(), arbeitszeitanteil);
							/*
							 * Es wird daher zunächst geprüft, ob bereits ein Arbeitszeitanteil vorhanden ist. Nur wenn nicht, wird der Anteil mit put gespeichert 
							 */
								try
								{
								Arbeitszeitanteil azAnteil = mitarbeiter.get(pnr).getAzvMonat().get(arbeitszeitanteil.getKostenstelleOderKostentraegerLang());
								// addiere den Prozentanteil zum bereits vorhandenem Wert:
								azAnteil.setProzentanteil((azAnteil.getProzentanteil() + arbeitszeitanteil.getProzentanteil()));
								}
								// Arbeitszeitanteil noch nicht gespeichert
								catch (NullPointerException e)
								{
								mitarbeiter.get(pnr).getAzvMonat().put(arbeitszeitanteil.getKostenstelleOderKostentraegerLang(), arbeitszeitanteil);
								}
							}
						}

						// Keine aktuellen AZV-Meldungen gefunden. Welches ist das aktuellste Datum ?
						if (! azvAktuell)
						{
	
						Date maxDate = null;
							for (Arbeitszeitanteil arbeitszeitanteil : azvGesamt)
							{
								if (maxDate == null)
								{
								maxDate = arbeitszeitanteil.getBerichtsMonat();	
								}
								else
								{
								maxDate = arbeitszeitanteil.getBerichtsMonat().after(maxDate) ? arbeitszeitanteil.getBerichtsMonat() : maxDate;
								}
							}

							mitarbeiter.get(pnr).setAzvAktuell(false);
							// dritte Runde: verarbeite alle AZV-Meldungen, die gleich maxDate sind	
							for (Arbeitszeitanteil arbeitszeitanteil : azvGesamt)
							{
								if (arbeitszeitanteil.getBerichtsMonat().equals(maxDate))
								{
								/*
								 * Leider hat sich herausgestellt, das einige Mitarbeiter mehrere Einträge zur gleichen KST / KTR abgeben.
								 * Ein einfaches put (wie bisher) überschreibt dabei einen möglicherweise bereits existierenden Arbeitszeitanteil:
								 */
								// mitarbeiter.get(pnr).getAzvMonat().put(arbeitszeitanteil.getKostenstelleOderKostentraegerLang(), arbeitszeitanteil);
								/*
								 * Es wird daher zunächst geprüft, ob bereits ein Arbeitszeitanteil vorhanden ist. Nur wenn nicht, wird der Anteil mit put gespeichert 
								 */
									try
									{
									Arbeitszeitanteil azAnteil = mitarbeiter.get(pnr).getAzvMonat().get(arbeitszeitanteil.getKostenstelleOderKostentraegerLang());
									// addiere den Prozentanteil zum bereits vorhandenem Wert:
									azAnteil.setProzentanteil((azAnteil.getProzentanteil() + arbeitszeitanteil.getProzentanteil()));
									}
									// Arbeitszeitanteil noch nicht gespeichert
									catch (NullPointerException e)
									{
									mitarbeiter.get(pnr).getAzvMonat().put(arbeitszeitanteil.getKostenstelleOderKostentraegerLang(), arbeitszeitanteil);
									}
								}
							}							
						}
					}
					
					// Keine AZV Daten gefunden. Der Mitarbeiter erhält eine leere AZV-Liste
					else 
					{
					mitarbeiter.get(pnr).setAzvMonat(new TreeMap<String, Arbeitszeitanteil>());	
					}
					
					
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
	public SQLException setMitarbeiter(Mitarbeiter m) 
	{
	SQLException ex = null;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_MITARBEITER);
		ps.setInt(1, m.getPersonalNR());
		
			if (m.getBenutzerName() != null) 
			{
			ps.setString(2, m.getBenutzerName());
			}
			else 
			{
			ps.setNull(2, Types.VARCHAR);	
			}
		ps.setString(3, m.getNachname());
			if (m.getVorname() != null) 
			{
			ps.setString(4, m.getVorname());
			}
			else 
			{
			ps.setNull(4, Types.VARCHAR);	
			}
		ps.execute();
		} 
		catch (SQLException e) 
		{
		ex = e;	
		e.printStackTrace();
		}
	return ex;	
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
	public boolean existsAZVDatensatz(int personalNummer, java.sql.Date berichtsMonat)
	{
	boolean result = false;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE_MITARBEITER_BERICHTSMONAT);
		ps.setInt(1, personalNummer);
		ps.setDate(2, berichtsMonat);
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
	public boolean existsTeammitgliedschaft(int personalNummer)
	{
	boolean result = false;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_TEAMMITGLIEDSCHAFT_PERSONALNUMMER);
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
	public boolean existsPersonaldurchschnittskosten(java.util.Date selectedMonth)
	{
	boolean exists = false;	
		try 
		{
			if (rs != null)
			{
			rs.close();
			rs = null;
			}	
		ps = con.prepareStatement(PreparedStatements.SELECT_COUNT_PERSONALDURCHSCHNITTSKOSTEN);
		ps.setString(1, sqlServerDatumsFormat.format(selectedMonth));
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

	/*
	 * Liefert die Arbeitszeitanteile für den ausgewählten Mitarbeiter im ausgewählten Berichtsmonat
	 * (non-Javadoc)
	 * @see de.hannit.fsch.klr.dataservice.DataService#getArbeitszeitanteile(int, java.util.Date)
	 */
	@Override
	public ArrayList<Arbeitszeitanteil> getArbeitszeitanteile(int personalNummer, java.util.Date selectedMonth)
	{
	ArrayList<Arbeitszeitanteil> arbeitszeitAnteile = new ArrayList<Arbeitszeitanteil>();	
	Arbeitszeitanteil azv = null;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE_MITARBEITER_BERICHTSMONAT);
		ps.setInt(1, personalNummer);
		ps.setString(2, sqlServerDatumsFormat.format(selectedMonth));
		rsAZV = ps.executeQuery();
					
		    while (rsAZV.next()) 
		    {
			azv = new Arbeitszeitanteil();
			azv.setBerichtsMonat(rsAZV.getDate(4));
				if (rsAZV.getString(5) != null)
				{
				azv.setKostenstelle(rsAZV.getString(5));
				azv.setKostenStelleBezeichnung(rsAZV.getString(6));
				}
				else
				{
				azv.setKostentraeger(rsAZV.getString(7));
				azv.setKostenTraegerBezeichnung(rsAZV.getString(8));
				}
			azv.setProzentanteil(rsAZV.getInt(9));
			azv.setITeam(rsAZV.getInt(10));
			azv.setID(rsAZV.getString(11));
		    
		    arbeitszeitAnteile.add(azv);
		    }
			} 
			catch (SQLException e) 
			{
			e.printStackTrace();
			}	
		
	return arbeitszeitAnteile;
	}
	
	@Override
	public ArrayList<Arbeitszeitanteil> getArbeitszeitanteileMAXMonat(int personalNummer)
	{
	ArrayList<Arbeitszeitanteil> arbeitszeitAnteile = new ArrayList<Arbeitszeitanteil>();	
	Arbeitszeitanteil azv = null;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE_MITARBEITER_LETZTERBERICHTSMONAT);
		ps.setInt(1, personalNummer);
		ps.setInt(2, personalNummer);
		rsAZV = ps.executeQuery();
					
		    while (rsAZV.next()) 
		    {
			azv = new Arbeitszeitanteil();
			azv.setBerichtsMonat(rsAZV.getDate(4));
				if (rsAZV.getString(5) != null)
				{
				azv.setKostenstelle(rsAZV.getString(5));
				azv.setKostenStelleBezeichnung(rsAZV.getString(6));
				}
				else
				{
				azv.setKostentraeger(rsAZV.getString(7));
				azv.setKostenTraegerBezeichnung(rsAZV.getString(8));
				}
			azv.setProzentanteil(rsAZV.getInt(9));
			azv.setITeam(rsAZV.getInt(10));
			azv.setID(rsAZV.getString(11));
		    
		    arbeitszeitAnteile.add(azv);
		    }
			} 
			catch (SQLException e) 
			{
			e.printStackTrace();
			}	
		
	return arbeitszeitAnteile;
	}	
	
	@Override
	public ArrayList<Arbeitszeitanteil> getArbeitszeitanteile(int personalNummer)
	{
	ArrayList<Arbeitszeitanteil> arbeitszeitAnteile = new ArrayList<Arbeitszeitanteil>();	
	Arbeitszeitanteil anteil = null;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE);
		ps.setInt(1, personalNummer);
		subSelect = ps.executeQuery();
					
		    while (subSelect.next()) 
		    {
		    anteil = new Arbeitszeitanteil();	  
		    anteil.setITeam(subSelect.getInt(2));
		    anteil.setBerichtsMonat(subSelect.getDate(3));
		    anteil.setKostenstelle(subSelect.getString(4));
		    anteil.setKostentraeger(subSelect.getString(5));
		    anteil.setProzentanteil(subSelect.getInt(6));
		    
		    arbeitszeitAnteile.add(anteil);
		    }
			} 
			catch (SQLException e) 
			{
			e.printStackTrace();
			}	
		
	return arbeitszeitAnteile;
	}

	@Override
	public Organisation getOrganisation()
	{
	Organisation hannit = new Organisation();
	TreeMap<java.util.Date, Monatsbericht> monatsBerichte = new TreeMap<java.util.Date, Monatsbericht>();
	Monatsbericht bericht = null;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_MONATSSUMMEN);
		rs = ps.executeQuery();
					
		    while (rs.next()) 
		    {
		    bericht = new Monatsbericht();	  
		    bericht.setBerichtsMonat(rs.getDate(1));
		    bericht.setSummeBrutto(rs.getDouble(2));
		    bericht.setAnzahlStellen(rs.getDouble(3));
		    bericht.setMitarbeiterGesamt(rs.getInt(4));
		    
		    monatsBerichte.put(bericht.getBerichtsMonat(), bericht);
		    }
	    hannit.setMonatsBerichte(monatsBerichte);
	    
	    TreeMap<Integer, KostenStelle> kostenstellen = new TreeMap<>();
		ps = con.prepareStatement(PreparedStatements.SELECT_KOSTENSTELLEN);
		rs = ps.executeQuery();
			
			int index = 0;
			KostenStelle kst;
		    while (rs.next()) 
		    {
		    kst = new KostenStelle();
		    kst.setBezeichnung(rs.getString(1));
		    kst.setBeschreibung(rs.getString(2));
		    kostenstellen.put(index, kst);
		    index++;
		    }
		hannit.setKostenstellen(kostenstellen);
		
	    TreeMap<Integer, KostenTraeger> kostentraeger = new TreeMap<>();
		ps = con.prepareStatement(PreparedStatements.SELECT_KOSTENTRAEGER);
		rs = ps.executeQuery();
			
			index = 0;
			KostenTraeger ktr;
		    while (rs.next()) 
		    {
		    ktr = new KostenTraeger();
		    ktr.setBezeichnung(rs.getString(1));
		    ktr.setBeschreibung(rs.getString(2));
		    kostentraeger.put(index, ktr);
		    index++;
		    }
		hannit.setKostentraeger(kostentraeger);		
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	
	return hannit;
	}

	@Override
	public Tarifgruppen getTarifgruppen(java.util.Date selectedMonth)
	{
	Tarifgruppen tarifgruppen = new Tarifgruppen();	
	java.sql.Date sqlDate = new java.sql.Date(cal.getTimeInMillis());
	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_TARIFGRUPPEN);
		ps.setDate(1, sqlDate);
		rs = ps.executeQuery();
					
		    while (rs.next()) 
		    {
		    Tarifgruppe t = new Tarifgruppe();
		    t.setBerichtsMonat(selectedMonth);
		    t.setTarifGruppe(rs.getString(1));
		    t.setSummeTarifgruppe(rs.getDouble(2));
		    t.setSummeStellen(rs.getDouble(3));
		    
		    tarifgruppen.getTarifGruppen().put(t.getTarifGruppe(), t);
		    }
			} 
			catch (SQLException e) 
			{
			e.printStackTrace();
			}
	return tarifgruppen;
	}

	@Override
	public SQLException setVZAEMonatsDaten(String strDatum, String strTarifgruppe, double dSummeTarifgruppe, double dSummeStellen, double dVZAE)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_MONATSSUMMENVZAE);
		ps.setString(1, strDatum);
		ps.setString(2, strTarifgruppe);
		ps.setDouble(3, dSummeTarifgruppe);
		ps.setDouble(4, dSummeStellen);
		ps.setDouble(5, dVZAE);
			
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
	public SQLException setAZVMonatsDaten(String kostenObjekt, String strDatum, double dSumme)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.INSERT_MONATSSUMMEN);
		ps.setString(1, kostenObjekt);
		ps.setString(2, strDatum);
		ps.setDouble(3, dSumme);
			
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
	public SQLException setPersonaldurchschnittskosten(int teamNR, java.util.Date datum, double bruttoAngestellte, double vzaeAngestellte, double bruttoBeamte, double vzaeBeamte, double abzugVorkostenstellen)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		con.setAutoCommit(false);	
		ps = con.prepareStatement(PreparedStatements.INSERT_PERSONALDURCHSCHNITTSKOSTEN);
		ps.setInt(1, teamNR);
		ps.setString(2, sqlServerDatumsFormat.format(datum));
		ps.setDouble(3, bruttoAngestellte);
		ps.setDouble(4, vzaeAngestellte);
		ps.setDouble(5, bruttoBeamte);
		ps.setDouble(6, vzaeBeamte);
		ps.setDouble(7, abzugVorkostenstellen);
				
		result = ps.execute();
		con.commit();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
			try
			{
			con.rollback();
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}
		}	
	return e;
	}

	@Override
	public SQLException deletePersonaldurchschnittskosten(java.util.Date datum)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.DELETE_PERSONALDURCHSCHNITTSKOSTEN);
		ps.setString(1, sqlServerDatumsFormat.format(datum));
		result = ps.execute();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}	
	return e;
	}

	/*
	 * (non-Javadoc)
	 * @see de.hannit.fsch.klr.dataservice.DataService#getPersonalnummern()
	 * 
	 * Liefert eine Liste aller gespeicherten Personalnummern.
	 * 
	 * Wird benutzt von:
	 * - AZVWebServiceEditablePart (ComboBoxCellEditor PNR)
	 */
	@Override
	public String[] getPersonalnummern()
	{
	ArrayList<String> pnrs = new ArrayList<>();
	int pnr = 0;
	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_PERSONALNUMMERN);
		rs = ps.executeQuery();
		
	      while (rs.next()) 
	      {
    	  pnr = rs.getInt(1);
    	  pnrs.add(String.valueOf(pnr));
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
		
	String[] result = new String[pnrs.size()];
		for (int i = 0; i < pnrs.size(); i++)
		{
		result[i] = pnrs.get(i);
		}
	return result;	
	}
	
	@Override
	public Integer getPersonalnummer(String nachname)
	{
	int personalNR = 0;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_PERSONALNUMMER);
		ps.setString(1, nachname);
		rs = ps.executeQuery();
		
	      while (rs.next()) 
	      {
    	  personalNR = rs.getInt(1);
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return personalNR;
	}

	@Override
	public Integer getPersonalnummer(String nachname, String username)
	{
	int personalNR = 0;	
	TreeMap<Integer, String> result = new TreeMap<Integer, String>();
	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_PERSONALNUMMER_KOMPLETT);
		ps.setString(1, nachname);
		rs = ps.executeQuery();
		
	      while (rs.next()) 
	      {
	      result.put(rs.getInt(1), rs.getString(2));	  
	      }
	      
	      switch (result.size())
	      {
	      case 0:
		  // Keine Treffer gefunden, Personalnummer bleibt 0
	      System.out.println("Für Mitarbeiter " + nachname + " wurde keine Personalnummer gefunden.");	    	  
	      break;
	      
	      case 1:
	      personalNR = result.firstKey();	
	      System.out.println("Für Mitarbeiter " + nachname + " wurde Personalnummer " + personalNR + " gefunden.");
	      break;

	      default:
		      for (Entry<Integer, String> entry : result.entrySet())
		      {
		    	  try
		    	  {
			    	  if (entry.getValue().equalsIgnoreCase(username))
			    	  {
			    	  personalNR = entry.getKey(); 	
			    	  }					
		    	  }
		    	  catch (NullPointerException e)
		    	  {
		    	  System.err.println("Nachname " + nachname + " existiert mehrfach in der DB, es gibt aber nicht für alle Einträge Benutzernamen ! ");	  
		    	  }
		      }	  
	      break;
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return personalNR;
	}	
	
	@Override
	public Integer getPersonalnummerbyUserName(String userName)
	{
	int personalNR = 0;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_PERSONALNUMMER_BENUTZERNAME);
		ps.setString(1, userName);
		rs = ps.executeQuery();
		
	      while (rs.next()) 
	      {
    	  personalNR = rs.getInt(1);
	      }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return personalNR;
	}	

	@Override
	public SQLException setErgebnis(String kostenArt, int teamNR, Date berichtsMonat, double ertrag, double materialAufwand, double afa, double sba, double personalkosten, double summeEinzelkosten, double deckungsbeitrag1, double verteilung1110, double verteilung2010, double verteilung2020, double verteilung3010, double verteilung4010, double verteilungGesamt, double ergebnis) 
	{
	SQLException e = null;	
	boolean result = false;
			try 
			{
			ps = con.prepareStatement(PreparedStatements.INSERT_ERGEBNIS);
			ps.setString(1, kostenArt);
			ps.setInt(2, teamNR);
			ps.setDate(3, berichtsMonat);
				if (ertrag != 0) {ps.setDouble(4, ertrag);} else {ps.setNull(4, Types.NULL);}
				if (materialAufwand != 0) {ps.setDouble(5, materialAufwand);} else {ps.setNull(5, Types.NULL);}
				if (afa != 0) {ps.setDouble(6, afa);} else {ps.setNull(6, Types.NULL);}
				if (sba != 0) {ps.setDouble(7, sba);} else {ps.setNull(7, Types.NULL);}
			ps.setDouble(8, personalkosten);
				if (summeEinzelkosten != 0) {ps.setDouble(9, summeEinzelkosten);} else {ps.setNull(9, Types.NULL);}
				if (deckungsbeitrag1 != 0) {ps.setDouble(10, deckungsbeitrag1);} else {ps.setNull(10, Types.NULL);}
				if (verteilung1110 != 0) {ps.setDouble(11, verteilung1110);} else {ps.setNull(11, Types.NULL);}
				if (verteilung2010 != 0) {ps.setDouble(12, verteilung2010);} else {ps.setNull(12, Types.NULL);}
				if (verteilung2020 != 0) {ps.setDouble(13, verteilung2020);} else {ps.setNull(13, Types.NULL);}
				if (verteilung3010 != 0) {ps.setDouble(14, verteilung3010);} else {ps.setNull(14, Types.NULL);}
				if (verteilung4010 != 0) {ps.setDouble(15, verteilung4010);} else {ps.setNull(15, Types.NULL);}
			ps.setDouble(16, verteilungGesamt);
			ps.setDouble(17, ergebnis);
			
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
	public boolean existsErgebnis(int teamNR, java.util.Date selectedMonth)
	{
	boolean exists = false;	
		try 
		{
			if (rs != null)
			{
			rs.close();
			rs = null;
			}	
		ps = con.prepareStatement(PreparedStatements.SELECT_COUNT_ERGEBNIS);
		ps.setInt(1, teamNR);
		ps.setString(2, sqlServerDatumsFormat.format(selectedMonth));
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
	public SQLException deleteErgebnis(Date berichtsMonat, int teamNR)
	{
	SQLException e = null;	
	boolean result = false;
		try 
		{
		ps = con.prepareStatement(PreparedStatements.DELETE_ERGEBNIS);
		ps.setString(1, sqlServerDatumsFormat.format(berichtsMonat));
		ps.setInt(2, teamNR);
			result = ps.execute();
			} 
			catch (SQLException exception) 
			{
			exception.printStackTrace();
			e = exception;
			}	
	return e;
	}

	/**
	 * Liefert die letzten gespeicherten AZV-Meldungen, 
	 * sowie die Anzahl der für diesen Monat erfassten Datensätze 
	 */
	@Override
	public TreeMap<Integer, String> getAZVMAXMonat()
	{
	TreeMap<Integer, String> result = new TreeMap<Integer, String>();
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_ARBEITSZEITANTEILE_LETZTERBERICHTSMONAT);
		rs = ps.executeQuery();
						
		    while (rs.next()) 
		    {
		    result.put(rs.getInt(2), rs.getString(1));
		    }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return result;
	}

	@Override
	public SQLException setTeammitgliedschaften(AZVDaten azvDaten)
	{
	SQLException e = null;	
	TreeMap<Integer, Integer> tm = azvDaten.getTeamMitglieder();
	
		try 
		{
		con.setAutoCommit(false);	
		ps = con.prepareStatement(PreparedStatements.INSERT_TEAMMITGLIEDSCHAFTEN);
			
			for (Integer pnr : tm.keySet())
			{
			ps.setInt(1, pnr);
			ps.setInt(2, tm.get(pnr));
			ps.setDate(3, azvDaten.getBerichtsMonatSQL());
			ps.setDate(4, null);
			
			ps.execute();
			}
		
		con.commit();
		con.setAutoCommit(true);	
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
			try
			{
			con.rollback();
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}
		}	
	return e;
	}

	@Override
	public SQLException updateTeammitgliedschaft(int personalNummer, int teamNummerAlt, int teamNummerNeu,  Date startDatum, Date endDatum)
	{
	SQLException e = null;	
		
		try 
		{
		con.setAutoCommit(false);	
		ps = con.prepareStatement(PreparedStatements.UPDATE_TEAMMITGLIEDSCHAFT);
			
		ps.setDate(1, endDatum);
		ps.setInt(2, personalNummer);
		ps.setInt(3, teamNummerAlt);
		ps.execute();
		
		ps = con.prepareStatement(PreparedStatements.INSERT_TEAMMITGLIEDSCHAFTEN);
		
		ps.setInt(1, personalNummer);
		ps.setInt(2, teamNummerNeu);
		ps.setDate(3, startDatum);
		ps.setDate(4, null);
			
		ps.execute();		
			
		con.commit();
		con.setAutoCommit(true);	
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
			try
			{
			con.rollback();
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}
		}	
	return e;
	}	
	
	@Override
	public SQLException setTeammitgliedschaft(int personalNummer, int teamNummer, Date startDatum)
	{
	SQLException e = null;	
		
		try 
		{
		con.setAutoCommit(false);	
		ps = con.prepareStatement(PreparedStatements.INSERT_TEAMMITGLIEDSCHAFTEN);
			
		ps.setInt(1, personalNummer);
		ps.setInt(2, teamNummer);
		ps.setDate(3, startDatum);
		ps.setDate(4, null);
			
		ps.execute();
			
		con.commit();
		con.setAutoCommit(true);	
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
			try
			{
			con.rollback();
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}
		}	
	return e;
	}

	@Override
	public int getAktuellesTeam(int personalNummer)
	{
	int teamNummer = - 1;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_AKTUELLES_TEAM);
		ps.setInt(1, personalNummer);
		rs = ps.executeQuery();
			if (rs.next())
			{
			teamNummer = rs.getInt(1);
			}
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
	return teamNummer;
	}
	
	@Override
	public double getLetzterStellenanteil(int personalNummer)
	{
	double letzterStellenanteil = 0;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_MITARBEITER_LETZTER_STELLENANTEIL);
		ps.setInt(1, personalNummer);
		ps.setInt(2, personalNummer);
		rs = ps.executeQuery();
			if (rs.next())
			{
			letzterStellenanteil = rs.getInt(1);
			}
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
	return letzterStellenanteil;
	}	

	@Override
	public ArrayList<TeamMitgliedschaft> getTeamMitgliedschaften(int personalNummer)
	{
	ArrayList<TeamMitgliedschaft> result = new ArrayList<>();
	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_TEAMMITGLIEDSCHAFTEN);
		ps.setInt(1, personalNummer);
		subSelect = ps.executeQuery();
			while (subSelect.next())
			{
			TeamMitgliedschaft tm = new TeamMitgliedschaft();	

			tm.setPersonalNummer(subSelect.getInt(1));
			tm.setTeamNummer(subSelect.getInt(2));
			tm.setSqlStartdatum(subSelect.getDate(3));
			
				if (subSelect.getDate(4) != null)
				{
				tm.setSqlEnddatum(subSelect.getDate(4));	
				}
			result.add(tm);	
			}
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return result;
	}

	@Override
	public SQLException deleteAZVDaten(Date datum)
	{
	SQLException e = null;	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.DELETE_ARBEITSZEITANTEILE_BERICHTSMONAT);
		ps.setDate(1, datum);
		boolean	result = ps.execute();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}	
	return e;
	}

	@Override
	public SQLException saveAZVChanges(TreeMap<String, Arbeitszeitanteil> unsavedChanges)
	{
	SQLException e = null;	
	
		try 
		{
		con.setAutoCommit(false);
		
			for (String id : unsavedChanges.keySet())
			{
			Arbeitszeitanteil azv = unsavedChanges.get(id);
				/*
				 * Vorhandene Datensätze haben bereits eine ID
				 * Hier wird diew Datenbank aktualisiert. 
				 * Ansonsten erfolgt ein Insert mit neuer ID
				 */
				if (id.equalsIgnoreCase("addRow"))
				{
				ps = con.prepareStatement(PreparedStatements.INSERT_AZV);
				ps.setInt(1, azv.getPersonalNummer());
				ps.setInt(2, azv.getITeam());
				ps.setDate(3, azv.getBerichtsMonat());
				
					if (azv.getKostenstelle() != null)
					{
					ps.setString(4, azv.getKostenstelle());
					ps.setNull(5, Types.NULL);
					}
					else
					{
					ps.setNull(4, Types.NULL);
					ps.setString(5, azv.getKostentraeger());
					}
				ps.setInt(6, azv.getProzentanteil());
				ps.execute();					
				}
				// ID vorhanden = Update
				else
				{
				ps = con.prepareStatement(PreparedStatements.UPDATE_AZV);
				ps.setString(1, azv.getKostenstelle() != null ? azv.getKostenstelle() : null);
				ps.setString(2, azv.getKostentraeger() != null ? azv.getKostentraeger() : null);
				ps.setInt(3, azv.getProzentanteil());
				ps.setString(4, azv.getID());
				ps.execute();
				}				
			}
		
		con.commit();
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
			try
			{
			con.rollback();
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}
		}
		finally
		{
			try
			{
			con.setAutoCommit(true);
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}	
		}
	return e;
	}

	@Override
	public SQLException setCallcenterDaten(List<String> lines)
	{
	SQLException e = null;	
	boolean result = false;

	DateFormat df = new SimpleDateFormat("dd.MM.yyyy");
	
	String[] parts = null;
	
	java.util.Date datum = null;
	java.sql.Time durchschnittlicheWartezeit = null;
	int durchschnittlicheWartezeitInSekunden = 0;
	Calendar cal = Calendar.getInstance();
	
	final int FELD_DATUM = 0;                        // Datum 
	final int FELD_STARTZEIT = 1;                    // von
	final int FELD_ENDZEIT = 2;                      // bis
	// final int FELD_THEMA = 3;
	final int FELD_EINGEHENDE_ANRUFE = 4;            // totNIncomeLT
	final int FELD_ZUGEORDNETE_ANRUFE = 5;           // totNAg
	final int FELD_ANGENOMMENE_ANRUFE = 6;           // totNConvAg
	final int FELD_ANRUFE_IN_WARTESCHLANGE = 7;      // totNQueuedAnn
	final int FELD_TROTZ_ZUORDNUNG_AUFGELEGT = 8;    // totNAbanAg
	final int FELD_IN_WARTESCHLANGE_AUFGELEGT = 9;   // totNAban
	final int FELD_DURCHSCHNITTLICHE_WARTEZEIT = 10; // øTQueued
	
		try 
		{
		con.setAutoCommit(false);	
		ps = con.prepareStatement(PreparedStatements.INSERT_CALLCENTERDATEN);
		
			for (String line : lines)
			{
			parts = line.split(";");
			
			datum = df.parse(parts[FELD_DATUM]);
		
			ps.setDate(1, new java.sql.Date(datum.getTime()));
			ps.setTime(2, java.sql.Time.valueOf(parts[FELD_STARTZEIT] + ":00"));
			ps.setTime(3, java.sql.Time.valueOf(parts[FELD_ENDZEIT] + ":00"));
			ps.setInt(4, Integer.parseInt(parts[FELD_EINGEHENDE_ANRUFE]));
			ps.setInt(5, Integer.parseInt(parts[FELD_ZUGEORDNETE_ANRUFE]));
			ps.setInt(6, Integer.parseInt(parts[FELD_ANGENOMMENE_ANRUFE]));
			ps.setInt(7, Integer.parseInt(parts[FELD_ANRUFE_IN_WARTESCHLANGE]));
			ps.setInt(8, Integer.parseInt(parts[FELD_TROTZ_ZUORDNUNG_AUFGELEGT]));
			ps.setInt(9, Integer.parseInt(parts[FELD_IN_WARTESCHLANGE_AUFGELEGT]));
			durchschnittlicheWartezeit = parts[FELD_DURCHSCHNITTLICHE_WARTEZEIT].equalsIgnoreCase("-") ? java.sql.Time.valueOf("00:00:00") : java.sql.Time.valueOf(parts[FELD_DURCHSCHNITTLICHE_WARTEZEIT]);
			ps.setTime(10, parts[FELD_DURCHSCHNITTLICHE_WARTEZEIT].trim().equalsIgnoreCase("-") ? java.sql.Time.valueOf("00:00:00") : durchschnittlicheWartezeit);

				if (durchschnittlicheWartezeit != null)
				{
				cal.setTimeInMillis(durchschnittlicheWartezeit.getTime());
				durchschnittlicheWartezeitInSekunden = (cal.get(Calendar.MINUTE) * 60) + cal.get(Calendar.SECOND);
				ps.setInt(11, durchschnittlicheWartezeitInSekunden);
				}
				else
				{
				ps.setInt(11, -1);
				}
			result = ps.execute();
			}

				
		} 
		catch (SQLException exception) 
		{
		exception.printStackTrace();
		e = exception;
		}
		catch (ParseException e1)
		{
		e1.printStackTrace();
		}
		finally
		{
			if (e == null)
			{
				try
				{
				con.commit();
				}
				catch (SQLException e1)
				{
				e1.printStackTrace();
				}	
			}
			else
			{
				try
				{
					con.rollback();
				}
				catch (SQLException e1)
				{
				e1.printStackTrace();
				}	
			}
			try
			{
			con.setAutoCommit(true);
			}
			catch (SQLException e1)
			{
			e1.printStackTrace();
			}	
		}
		
	return e;
	}

	@Override
	public String getTarifgruppeAushilfen(int personalNummer)
	{
	String result = null;
	
		try 
		{
		ps = con.prepareStatement(PreparedStatements.SELECT_TARIFGRUPPE_AUSHILFE);
		ps.setInt(1, personalNummer);
		rs = ps.executeQuery();
					
		    while (rs.next()) 
		    {
		    result = rs.getString(1);	
		    }
		} 
		catch (SQLException e) 
		{
		e.printStackTrace();
		}	
	return result;
	}
}

