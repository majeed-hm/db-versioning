package com.axonivy.utils.dbversioning.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.ClassicConfiguration;

import com.zaxxer.hikari.HikariDataSource;

import ch.ivyteam.ivy.environment.Ivy;


public class DBVersioningService {
	private static final String IVY_VAR_DB_TYPE = "dbType";
	private static final String IVY_VAR_DB_HOST = "dbHost";
	private static final String IVY_VAR_DB_PORT = "dbPort";
	private static final String IVY_VAR_DB_NAME = "dbName";
	private static final String IVY_VAR_DB_USERNAME = "dbUsername";
	private static final String IVY_VAR_DB_PASSWORD = "dbPassword";
	
	private static final String LOCATION_TYPE = "filesystem:";
	
	
	private static String initJdbcUrl(SupportedDatabase database) {
		StringBuilder jdbcUrl = new StringBuilder(database.getProtocol());
		
		String dbHost = Ivy.var().get(IVY_VAR_DB_HOST);
		if(StringUtils.isNotBlank(dbHost)) {
			jdbcUrl.append(":").append(dbHost);
		}
		String dbPort = Ivy.var().get(IVY_VAR_DB_PORT);
		if(StringUtils.isNotBlank(dbPort)) {
			jdbcUrl.append(":").append(dbPort);
		}
		String dbName = Ivy.var().get(IVY_VAR_DB_NAME);
		if(StringUtils.isNotBlank(dbName)) {
			jdbcUrl.append("mem".equals(dbHost)? ":": "/").append(dbName);
		}
		
		return jdbcUrl.toString();
	}
	
	private static HikariDataSource initDataSource(SupportedDatabase database) {
		HikariDataSource dataSource = new HikariDataSource();
		
		String jdbcUrl = initJdbcUrl(database);
		String dbUsername = Ivy.var().get(IVY_VAR_DB_USERNAME);
		String dbPassword = Ivy.var().get(IVY_VAR_DB_PASSWORD);
		
		dataSource.setDriverClassName(database.driverClassName);
		dataSource.setJdbcUrl(jdbcUrl);
		dataSource.setUsername(dbUsername);
		dataSource.setPassword(dbPassword);
		
		Ivy.log().info("Flyway Config initialized with jdbcUrl={0}", jdbcUrl);
		
		return dataSource;
	}

	private static ClassicConfiguration initFlywayConfig(File... sqlDirectories) {
		ClassicConfiguration flywayConfig = new ClassicConfiguration();
		
		List<String> listSqlLocations = Arrays.asList(sqlDirectories).stream()
				.map(l -> LOCATION_TYPE + l.getAbsolutePath())
				.collect(Collectors.toList());
		listSqlLocations.stream().forEach(l -> Ivy.log().info("This directory: {0} will be used as migration directory.", l));
		
		flywayConfig.setLocationsAsStrings(listSqlLocations.toArray(new String[0]));
		
		return flywayConfig;
	}

	public static void startFlywayMigration(File... sqlDirectories) {
		SupportedDatabase database = null;
		String dbType = Ivy.var().get(IVY_VAR_DB_TYPE);
		if(StringUtils.isNotBlank(dbType)) {
			database = SupportedDatabase.valueOf(dbType);
		}
		try(HikariDataSource dataSource = initDataSource(database)) {
			ClassicConfiguration flywayConfig = initFlywayConfig(sqlDirectories);
			flywayConfig.setDataSource(dataSource);
			/*
			String migrationDirectory = copyAllToMigrationDirectory(database, sqlDirectories);
			flywayConfig.setLocationsAsStrings(migrationDirectory);
			*/
			Flyway flyway = new Flyway(flywayConfig);
			flyway.migrate();
			Ivy.log().info("DB Flyway migration successfully done.");
		} catch (FlywayException e) {
			Ivy.log().error("Error when starting Flyway Migration.", e);
		}
	}
	
	private static String copyAllToMigrationDirectory(SupportedDatabase database, File... sqlDirectories) {
		String migrationDirectoryPath = "db/migration-" + database.name();
		ch.ivyteam.ivy.scripting.objects.File migrationDirectory = null;
		try {
			migrationDirectory = new ch.ivyteam.ivy.scripting.objects.File(migrationDirectoryPath, true);
			migrationDirectoryPath = migrationDirectory.getAbsolutePath();
		} catch (IOException e) {
			Ivy.log().error("Error when creating Migration directory: {0}", e, migrationDirectoryPath);
		}
		
		if(migrationDirectory.exists()) { // Cleans a directory without deleting it.
			try {
				FileUtils.cleanDirectory(migrationDirectory.getJavaFile());
			} catch (IOException e) {
				Ivy.log().error("Error when cleaning Migration directory: {0}", e, migrationDirectoryPath);
			}
		}
		for(File sqlDirectory: sqlDirectories) {
			try {
				FileUtils.copyDirectory(sqlDirectory, migrationDirectory.getJavaFile());
				
				Ivy.log().info("All files in the directory: {0} are copied to: {1}", 
						sqlDirectory.getAbsolutePath(), migrationDirectoryPath);
			} catch (IOException e) {
				Ivy.log().error("Error when copying files from: {0} to {1}", e, 
						sqlDirectory.getAbsolutePath(), migrationDirectoryPath);
			}
		}
		return "filesystem:" + migrationDirectoryPath;
	}
	
	
	private static enum SupportedDatabase {
		DB2("jdbc:", ""),
		H2("jdbc:h2", "org.h2.Driver"),
		HSQL("jdbc:", ""),
		MYSQL("jdbc:mysql", ""),
		ORACLE("jdbc:oracle", ""),
		POSTGRESQL("jdbc:postgresql", "org.postgresql.Driver"),
		SQL_SERVER("jdbc:", ""),
		SYBASE("jdbc:", "")
		;

		private String protocol;
		private String driverClassName;
		
		
		private SupportedDatabase(String protocol, String driverClassName) {
			this.protocol = protocol;
			this.driverClassName = driverClassName;
		}
		

		public String getProtocol() {
			return protocol;
		}
		
		public String getDriverClassName() {
			return driverClassName;
		}
	}
}
