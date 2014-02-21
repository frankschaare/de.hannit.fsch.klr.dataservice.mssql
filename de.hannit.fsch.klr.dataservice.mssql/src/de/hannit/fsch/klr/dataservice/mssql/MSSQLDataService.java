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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;
import java.util.TreeMap;

import org.eclipse.core.runtime.FileLocator;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;

import de.hannit.fsch.common.csv.azv.AZVDatensatz;
import de.hannit.fsch.common.csv.azv.Arbeitszeitanteil;
import de.hannit.fsch.common.loga.LoGaDatensatz;
import de.hannit.fsch.common.mitarbeiter.Mitarbeiter;
import de.hannit.fsch.common.mitarbeiter.besoldung.Tarifgruppe;
import de.hannit.fsch.common.mitarbeiter.besoldung.Tarifgruppen;
import de.hannit.fsch.common.organisation.hannit.Organisation;
import de.hannit.fsch.common.organisation.reporting.Monatsbericht;
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
private ResultSet subSelect;
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
			} 
			catch (SQLException e) 
			{
			e.printStackTrace();
			}	
	hannit.setMonatsBerichte(monatsBerichte);
	
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
}
