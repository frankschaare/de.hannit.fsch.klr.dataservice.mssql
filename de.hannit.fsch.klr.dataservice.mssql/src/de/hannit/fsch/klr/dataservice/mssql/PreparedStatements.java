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
public static final String INSERT_MITARBEITER = "INSERT INTO Mitarbeiter ([PNr],[Benutzer],[Nachname],[Vorname]) VALUES (?,?,?,?)";
public static final String SELECT_MITARBEITER = "SELECT * FROM [dbo].[Mitarbeiter]";
public static final String SELECT_MITARBEITER_PERSONALNUMMER = "SELECT * FROM [dbo].[Mitarbeiter] WHERE [PNr] = ?";

public static final String INSERT_LOGA = "INSERT INTO [dbo].[LoGa] ([Mitarbeiter_PNR], [Berichtsmonat], [Brutto], [Tarifgruppe], [Tarifstufe], [Stellenanteil]) VALUES (?, ?, ?, ?, ?, ?)";
}
