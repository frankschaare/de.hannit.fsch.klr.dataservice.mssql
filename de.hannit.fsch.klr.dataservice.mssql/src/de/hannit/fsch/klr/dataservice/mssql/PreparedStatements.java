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

public static final String INSERT_ERGEBNIS = "INSERT INTO [dbo].[Ergebnisse] ([Kostenart],[TeamNR],[Berichtsmonat],[Ertrag],[Materialaufwand],[AfA],[SbA],[Personalkosten],[SummeEinzelkosten],[Deckungsbeitrag1],[Verteilung1110],[Verteilung2010],[Verteilung2020],[Verteilung3010],[Verteilung4010],[VerteilungGesamt],[Ergebnis]) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
public static final String SELECT_COUNT_ERGEBNIS = "SELECT COUNT(*) AS Anzahl FROM [dbo].[Ergebnisse] WHERE TeamNR = ? AND Berichtsmonat = ?";
public static final String DELETE_ERGEBNIS = "DELETE FROM [dbo].[Ergebnisse] WHERE Berichtsmonat = ? AND TeamNR = ?";

public static final String INSERT_MITARBEITER = "INSERT INTO Mitarbeiter ([PNr],[Benutzer],[Nachname],[Vorname]) VALUES (?,?,?,?)";
public static final String SELECT_MITARBEITER = "SELECT * FROM [dbo].[Mitarbeiter]";
public static final String SELECT_PERSONALNUMMERN = "SELECT [PNr] FROM [dbo].[Mitarbeiter] ORDER BY [PNr]";
public static final String SELECT_PERSONALNUMMER = "SELECT [PNr] FROM [dbo].[Mitarbeiter] WHERE Nachname = ?";
public static final String SELECT_PERSONALNUMMER_KOMPLETT = "SELECT * FROM [dbo].[Mitarbeiter] WHERE Nachname = ?";
public static final String SELECT_PERSONALNUMMER_BENUTZERNAME = "SELECT [PNr] FROM [dbo].[Mitarbeiter] WHERE Benutzer = ?";
public static final String SELECT_MITARBEITER_PERSONALNUMMER = "SELECT * FROM [dbo].[Mitarbeiter] WHERE [PNr] = ?";
public static final String SELECT_MITARBEITER_AKTUELL = "SELECT l.Mitarbeiter_PNR, m.Nachname, m.Vorname, l.Berichtsmonat, l.Brutto, l.Tarifgruppe, l.Stellenanteil FROM dbo.LoGa AS l INNER JOIN dbo.Mitarbeiter AS m ON l.Mitarbeiter_PNR = m.PNr WHERE (l.Berichtsmonat = ?)";
public static final String SELECT_MITARBEITER_LETZTER_STELLENANTEIL = "SELECT [Stellenanteil] FROM [dbo].[LoGa] WHERE Mitarbeiter_PNR = ? AND Berichtsmonat = (SELECT MAX([Berichtsmonat]) FROM [dbo].[LoGa] WHERE Mitarbeiter_PNR = ?)";

public static final String SELECT_KOSTENSTELLE = "SELECT * FROM [dbo].[Kostenstellen] WHERE Kostenstelle = ?";
public static final String SELECT_KOSTENSTELLEN = "SELECT * FROM [dbo].[Kostenstellen]";
public static final String SELECT_KOSTENTRAEGER = "SELECT * FROM [dbo].[Kostentraeger]";
public static final String INSERT_KOSTENSTELLE = "INSERT INTO [dbo].[Kostenstellen] ([Kostenstelle],[Bezeichnung],[von]) VALUES (?, ?, ?)";
public static final String SELECT_COUNT_KOSTENSTELLE = "SELECT COUNT([Kostenstelle]) AS Anzahl FROM [dbo].[Kostenstellen]	WHERE [Kostenstelle] = ?";
public static final String SELECT_COUNT_KOSTENTRAEGER = "SELECT COUNT([Kostentraeger]) AS Anzahl FROM [dbo].[Kostentraeger]	WHERE [Kostentraeger] = ?";

public static final String SELECT_COUNT_PERSONALDURCHSCHNITTSKOSTEN = "SELECT COUNT(*) AS Anzahl FROM [dbo].[PersonaldurchschnittsKosten] WHERE Berichtsmonat = ?";
public static final String INSERT_PERSONALDURCHSCHNITTSKOSTEN = "INSERT INTO [dbo].[PersonaldurchschnittsKosten] ([TeamNR],[Berichtsmonat],[BruttoAngestellte],[VZÄAngestellte],[BruttoBeamte],[VZÄBeamte],[AbzugVorkostenstellen]) VALUES (?, ?, ?, ?, ?, ?, ?)";
public static final String DELETE_PERSONALDURCHSCHNITTSKOSTEN = "DELETE FROM [dbo].[PersonaldurchschnittsKosten] WHERE Berichtsmonat = ?";

public static final String INSERT_KOSTENTRAEGER = "INSERT INTO [dbo].[Kostentraeger] ([Kostentraeger],[Bezeichnung],[von])VALUES (?, ?, ?)";

public static final String INSERT_TEAMMITGLIEDSCHAFTEN = "INSERT INTO [dbo].[TeamMitglieder] ([ID],[Mitarbeiter_PNR],[TeamNR],[DatumVon],[DatumBis]) VALUES (NEWID(), ?, ?, ?, ?)";
public static final String UPDATE_TEAMMITGLIEDSCHAFT = "UPDATE [dbo].[TeamMitglieder] SET [DatumBis] = ? WHERE [Mitarbeiter_PNR] = ? AND [TeamNR] = ? AND [DatumBis] IS NULL";
public static final String SELECT_TEAMMITGLIEDSCHAFTEN = "SELECT [Mitarbeiter_PNR], [TeamNR], [DatumVon], [DatumBis] FROM [dbo].[TeamMitglieder] WHERE [Mitarbeiter_PNR] = ?";
public static final String SELECT_TEAMMITGLIEDSCHAFT_PERSONALNUMMER = "SELECT [Mitarbeiter_PNR],[TeamNR],[DatumVon],[DatumBis] FROM [dbo].[TeamMitglieder] WHERE [Mitarbeiter_PNR] = ?";
public static final String SELECT_AKTUELLES_TEAM = "SELECT [TeamNR] FROM [dbo].[TeamMitglieder] WHERE [Mitarbeiter_PNR] = ? AND [DatumBis] IS NULL";

public static final String SELECT_TARIFGRUPPEN = "SELECT Tarifgruppe, SUM(Brutto) AS [Summe Tarifgruppe], SUM(Stellenanteil) AS [Summe Stellen], SUM(Brutto) / SUM(Stellenanteil) AS Vollzeitäquivalent FROM dbo.LoGa WHERE (Berichtsmonat = ?) GROUP BY Tarifgruppe";
public static final String SELECT_TARIFGRUPPE_AUSHILFE = "SELECT Art FROM [dbo].[vwAushilfen] WHERE Mitarbeiter_PNr = ?";
public static final String INSERT_AZV = "INSERT INTO [dbo].[AZVMeldungen] ([ID], [Mitarbeiter_PNR],[TeamNR],[Berichtsmonat],[Kostenstelle],[Kostentraeger],[Prozentanteil]) VALUES (NEWID(),?, ?, ?, ?, ?, ?)";
public static final String UPDATE_AZV = "UPDATE [dbo].[AZVMeldungen] SET [Kostenstelle] = ?, [Kostentraeger] = ?,[Prozentanteil] = ? WHERE [ID] = ?";
public static final String SELECT_ARBEITSZEITANTEILE = "SELECT * FROM [dbo].[AZVMeldungen] WHERE Mitarbeiter_PNR = ?";
public static final String SELECT_ARBEITSZEITANTEILE_BERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Berichtsmonat = ?";
public static final String DELETE_ARBEITSZEITANTEILE_BERICHTSMONAT = "DELETE FROM [dbo].[AZVMeldungen] WHERE Berichtsmonat = ?";
public static final String SELECT_ARBEITSZEITANTEILE_LETZTERBERICHTSMONAT = "SELECT [Berichtsmonat], COUNT(*) AS Anzahl FROM [dbo].[AZVMeldungen] WHERE [Berichtsmonat] = (SELECT MAX([Berichtsmonat]) FROM [dbo].[AZVMeldungen]) GROUP BY [Berichtsmonat]";
public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER_BERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ? AND Berichtsmonat = ?";
public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER_LETZTERBERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ? AND Berichtsmonat = (SELECT MAX(Berichtsmonat) FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ?)";
public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ?";

// public static final String INSERT_AZV = "INSERT INTO [dbo].[Arbeitszeitanteile] ([Mitarbeiter_PNR],[TeamNR],[Berichtsmonat],[Kostenstelle],[Kostentraeger],[Prozentanteil]) VALUES (?, ?, ?, ?, ?, ?)";
// public static final String SELECT_ARBEITSZEITANTEILE = "SELECT * FROM [dbo].[Arbeitszeitanteile] WHERE Mitarbeiter_PNR = ?";
// public static final String SELECT_ARBEITSZEITANTEILE_BERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Berichtsmonat = ?";
// public static final String DELETE_ARBEITSZEITANTEILE_BERICHTSMONAT = "DELETE FROM [dbo].[Arbeitszeitanteile] WHERE Berichtsmonat = ?";
// public static final String SELECT_ARBEITSZEITANTEILE_LETZTERBERICHTSMONAT = "SELECT [Berichtsmonat], COUNT(*) AS Anzahl FROM [dbo].[Arbeitszeitanteile] WHERE [Berichtsmonat] = (SELECT MAX([Berichtsmonat]) FROM [dbo].[Arbeitszeitanteile]) GROUP BY [Berichtsmonat]";
// public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER_BERICHTSMONAT = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ? AND Berichtsmonat = ?";
// public static final String SELECT_ARBEITSZEITANTEILE_MITARBEITER = "SELECT * FROM [dbo].[vwArbeitszeitanteile] WHERE Mitarbeiter_PNR = ?";

public static final String SELECT_MONATSSUMMEN = "SELECT * FROM [dbo].[Monatssummen]";
public static final String INSERT_MONATSSUMMEN = "INSERT INTO [dbo].[MonatssummenAZV]([ID], [Kostenobjekt], [Berichtsmonat], [Summe]) VALUES (NEWID(), ?, ?, ?)";
public static final String INSERT_MONATSSUMMENVZAE = "INSERT INTO [dbo].[MonatssummenVZAE] ([ID], [Berichtsmonat], [Tarifgruppe], [SummeTarifgruppe], [SummeStellen], [VZAE]) VALUES (NEWID(), ?, ?, ?, ?, ?)";

public static final String INSERT_LOGA = "INSERT INTO [dbo].[LoGa] ([Mitarbeiter_PNR], [Berichtsmonat], [Brutto], [Tarifgruppe], [Tarifstufe], [Stellenanteil]) VALUES (?, ?, ?, ?, ?, ?)";

public static final String INSERT_CALLCENTERDATEN = "INSERT INTO [dbo].[CallcenterMonitoring] ([ID],[Datum],[ZeitVon],[ZeitBis],[EingehendeAnrufe],[ZugeordneteAnrufe],[AngenommeneAnrufe],[AnrufeInWarteschlange],[TrotzZuordnungAufgelegt],[InWarteschlangeAufgelegt],[DuschnittlicheWarteZeit],[DuschnittlicheWarteZeitSekunden]) VALUES (NEWID(),?,?,?,?,?,?,?,?,?,?,?)";
}
