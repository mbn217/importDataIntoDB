package com.moe.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

@Service
public class DataImportServiceImpl implements DataImportService {

	private static final Logger logger = LoggerFactory.getLogger(DataImportServiceImpl.class);

	private static final String INSERT_SQL = "INSERT INTO tableName (column names) "
			+ "VALUES (?,?,?,?)";

	@Value("${csv.file}")
	private String csvFilePath;

	@Value("${db_url}")
	private String databaseUrl;

	@Value("${db_user}")
	private String databaseUser;

	@Value("${db_pass}")
	private String databasePassword;

	private Connection connection;
	private int batchSize = 500;

	public void importData() {
		ClassPathXmlApplicationContext appContext = null;

		try {
			initializeDatabaseDriver();
			initializeConnections();

			appContext = new ClassPathXmlApplicationContext();
			String fullPathForCsv = "classpath:/" + csvFilePath;
			logger.debug("Reading {} into a list", fullPathForCsv);
			InputStream inputStream = appContext.getResource(fullPathForCsv).getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			int counter = 0;
			while (reader.ready()) {
				++counter;
				String line = reader.readLine();
				line = line.trim();
				if (counter > 1) {
					int commaAt = line.indexOf(",");
					String fullApplicationNumber = line.substring(0, commaAt);
					String json = (line.substring(commaAt + 2, line.length() - 1)).replaceAll("\"\"", "\"");
					insertRow(fullApplicationNumber, json);
					/*
					 * Commit every n records
					 */
					if (batchSize != 0) {
						if ((counter % batchSize) == 0) {
							logger.debug("Imported {} records ", counter);
							connection.commit();
						}
					}

				}
			}
			logger.debug("Found {} elements in {}", fullPathForCsv);
		} catch (Exception e) {
			logger.error("Error importing data", e);
			throw new RuntimeException(e);
		} finally {
			if (appContext != null) {
				appContext.close();
			}
			DbUtils.closeQuietly(connection);
		}
	}

	private void insertRow(String applicationNumber, String json) throws SQLException, NoSuchAlgorithmException {
		PreparedStatement ps = connection.prepareStatement(INSERT_SQL);
		String md5String = getMd5Hash(json);
		ps.setString(1, applicationNumber);
		ps.setString(2, md5String);
		ps.setString(3, json);
		ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
		ps.execute();
		ps.close();
	}

	/**
	 * @throws ClassNotFoundException
	 */
	private void initializeDatabaseDriver() throws ClassNotFoundException {
		try {
			Class.forName(oracle.jdbc.OracleDriver.class.getName());
		} catch (ClassNotFoundException e) {
			throw e;
		}
	}

	/**
	 * @throws SQLException
	 */
	private void initializeConnections() throws SQLException {
		DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
		DriverManager.registerDriver(new org.hsqldb.jdbcDriver());
		this.connection = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword);
		this.connection.setAutoCommit(false);
	}

	/*
	 * from :
	 * https://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash
	 */
	private String getMd5Hash(String originalString) throws NoSuchAlgorithmException {
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.reset();
		m.update(originalString.getBytes());
		byte[] digest = m.digest();
		BigInteger bigInt = new BigInteger(1, digest);
		String hashtext = bigInt.toString(16);
		// Now we need to zero pad it if you actually want the full 32 chars.
		while (hashtext.length() < 32) {
			hashtext = "0" + hashtext;
		}
		return hashtext;
	}

}
