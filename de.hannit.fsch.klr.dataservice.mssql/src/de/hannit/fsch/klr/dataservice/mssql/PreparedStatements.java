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
}
