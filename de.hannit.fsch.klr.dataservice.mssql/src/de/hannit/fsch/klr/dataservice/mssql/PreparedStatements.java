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
public static final String SELECT_MITARBEITER_AKTUELL = "SELECT l.Mitarbeiter_PNR, m.Nachname, m.Vorname, l.Berichtsmonat, l.Brutto, l.Tarifgruppe, l.Stellenanteil FROM dbo.LoGa AS l INNER JOIN dbo.Mitarbeiter AS m ON l.Mitarbeiter_PNR = m.PNr WHERE (l.Berichtsmonat = ?)";

public static final String SELECT_KOSTENSTELLE = "SELECT * FROM [dbo].[Kostenstellen] WHERE Kostenstelle = ?";
public static final String INSERT_KOSTENSTELLE = "INSERT INTO [dbo].[Kostenstellen] ([Kostenstelle],[Bezeichnung],[von]) VALUES (?, ?, ?)";
public static final String SELECT_COUNT_KOSTENSTELLE = "SELECT COUNT([Kostenstelle]) AS Anzahl FROM [dbo].[Kostenstellen]	WHERE [Kostenstelle] = ?";
public static final String SELECT_COUNT_KOSTENTRAEGER = "SELECT COUNT([Kostentraeger]) AS Anzahl FROM [dbo].[Kostentraeger]	WHERE [Kostentraeger] = ?";
public static final String SELECT_COUNT_PERSONALDURCHSCHNITTSKOSTEN = "SELECT COUNT(*) AS Anzahl FROM [dbo].[PersonaldurchschnittsKosten] WHERE Berichtsmonat = ?";
public static final String INSERT_KOSTENTRAEGER = "INSERT INTO [dbo].[Kostentraeger] ([Kostentraeger],[Bezeichnung],[von])VALUES (?, ?, ?)";
public static final String INSERT_AZV = "INSERT INTO [dbo].[Arbeitszeitanteile] ([Mitarbeiter_PNR],[TeamNR],[Berichtsmonat],[Kostenstelle],[Kostentraeger],[Prozentanteil]) VALUES (?, ?, ?, ?, ?, ?)";

public static final String SELECT_TARIFGRUPPEN = "SELECT Tarifgruppe, SUM(Brutto) AS [Summe Tarifgruppe], SUM(Stellenanteil) AS [Summe Stellen], SUM(Brutto) / SUM(Stellenanteil) AS Vollzeitäquivalent FROM dbo.LoGa WHERE (Berichtsmonat = ?) GROUP BY Tarifgruppe";

public static final String SELECT_ARBEITSZEITANTEILE = "SELECT * FROM [dbo].[Arbeitszeitanteile] WHERE Mitarbeiter_PNR = ?";
public static final String SELECT_ARBEITSZEITANTEILE_BERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Berichtsmonat = ?";
public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ?";

public static final String SELECT_MONATSSUMMEN = "SELECT * FROM [dbo].[Monatssummen]";
public static final String INSERT_MONATSSUMMEN = "INSERT INTO [dbo].[MonatssummenAZV]([ID], [Kostenobjekt], [Berichtsmonat], [Summe]) VALUES (NEWID(), ?, ?, ?)";
public static final String INSERT_MONATSSUMMENVZAE = "INSERT INTO [dbo].[MonatssummenVZAE] ([ID], [Berichtsmonat], [Tarifgruppe], [SummeTarifgruppe], [SummeStellen], [VZAE]) VALUES (NEWID(), ?, ?, ?, ?, ?)";

public static final String INSERT_LOGA = "INSERT INTO [dbo].[LoGa] ([Mitarbeiter_PNR], [Berichtsmonat], [Brutto], [Tarifgruppe], [Tarifstufe], [Stellenanteil]) VALUES (?, ?, ?, ?, ?, ?)";
}
