package com.axonivy.utils.dbversioning.demo;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import ch.ivyteam.ivy.environment.Ivy;


public class DemoFileUtils {
	private static final String IVY_VAR_DB_SQL_LOCATIONS = "dbVersioning.demo.dbSqlLocations";

	
	
	public static File getMigrationDirectory() {
		String migrationDirectoryPath = Ivy.var().get(IVY_VAR_DB_SQL_LOCATIONS);
		
		try {
			return Paths.get(DemoFileUtils.class.getClassLoader().getResource(migrationDirectoryPath).toURI()).toFile();
		} catch (URISyntaxException e) {
			Ivy.log().error("Error when getting the Migration directory.", e);
		}
		return null;
	}
}
