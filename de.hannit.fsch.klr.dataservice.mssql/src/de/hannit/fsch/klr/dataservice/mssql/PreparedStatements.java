/**
 * 
 */
package de.hannit.fsch.klr.dataservice.mssql;

/**
 * @author fsch
 *
 */
public interface PreparedStatements 
{
public static final String COUNT_COLUMN = "Anzahl";
public static final String INSERT_DATENIMPORT = "INSERT INTO [dbo].[Datenimporte] ([ID], [Importdatum],[Dateiname],[Pfad],[AnzahlDaten],[Berichtsmonat],[Datenquelle])  VALUES (NEWID(),?,?,?,?,?,?)";

public static final String INSERT_MITARBEITER = "INSERT INTO Mitarbeiter ([PNr],[Benutzer],[Nachname],[Vorname]) VALUES (?,?,?,?)";
public static final String SELECT_MITARBEITER = "SELECT * FROM [dbo].[Mitarbeiter]";
public static final String SELECT_MITARBEITER_PERSONALNUMMER = "SELECT * FROM [dbo].[Mitarbeiter] WHERE [PNr] = ?";

public static final String SELECT_KOSTENSTELLE = "SELECT * FROM [dbo].[Kostenstellen] WHERE Kostenstelle = ?";
public static final String INSERT_KOSTENSTELLE = "INSERT INTO [dbo].[Kostenstellen] ([Kostenstelle],[Bezeichnung],[von]) VALUES (?, ?, ?)";
public static final String SELECT_COUNT_KOSTENSTELLE = "SELECT COUNT([Kostenstelle]) AS Anzahl FROM [dbo].[Kostenstellen]	WHERE [Kostenstelle] = ?";
public static final String SELECT_COUNT_KOSTENTRAEGER = "SELECT COUNT([Kostentraeger]) AS Anzahl FROM [dbo].[Kostentraeger]	WHERE [Kostentraeger] = ?";
public static final String INSERT_KOSTENTRAEGER = "INSERT INTO [dbo].[Kostentraeger] ([Kostentraeger],[Bezeichnung],[von])VALUES (?, ?, ?)";
public static final String INSERT_AZV = "INSERT INTO [dbo].[Arbeitszeitanteile] ([Mitarbeiter_PNR],[TeamNR],[Berichtsmonat],[Kostenstelle],[Kostentraeger],[Prozentanteil]) VALUES (?, ?, ?, ?, ?, ?)";

public static final String INSERT_LOGA = "INSERT INTO [dbo].[LoGa] ([Mitarbeiter_PNR], [Berichtsmonat], [Brutto], [Tarifgruppe], [Tarifstufe], [Stellenanteil]) VALUES (?, ?, ?, ?, ?, ?)";
}
