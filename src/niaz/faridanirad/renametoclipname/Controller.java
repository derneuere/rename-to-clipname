package niaz.faridanirad.renametoclipname;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class Controller {

	private static Logger logger = LogManager.getLogger(Controller.class);

	@FXML
	private Label outputFolderPath;
	
	@FXML
	private Label metadataPath;

	@FXML
	private Label pgpassPath;

	public void setPgpassPath() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open .pgpass File");
		File file = fileChooser.showOpenDialog(metadataPath.getScene().getWindow());
		pgpassPath.setText(file.getAbsolutePath());
	}

	public void setOutputFolderPath() {
		DirectoryChooser chooser = new DirectoryChooser();
		chooser.setTitle("Open Output Folder");
		File file = chooser.showDialog(metadataPath.getScene().getWindow());
		outputFolderPath.setText(file.getAbsolutePath());
	}
	
	public void setMetadataPath() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open metadata.csv");
		File file = fileChooser.showOpenDialog(metadataPath.getScene().getWindow());
		metadataPath.setText(file.getAbsolutePath());
	}

	public void loadMetadataFromDatabase() {
		writeInfoToCsv(pgpassPath.getText(), outputFolderPath.getText());
	}

	public void copyRenamedFilesToOutputfolder() {
		copyFromCSVToFolderWithNewNames(metadataPath.getText(), outputFolderPath.getText());
	}

	private static void copyFromCSVToFolderWithNewNames(String metaDataCsv, String outputPath) {
		logger.debug(metaDataCsv);
		logger.debug(outputPath);
		if (!isValidPath(metaDataCsv) && !isValidPath(outputPath)) {
			JOptionPane.showMessageDialog(new JFrame(), "Paths are not valid");
			return;
		}
		List<DavinciFile> files = getDavinciFilesFormCsv(metaDataCsv);
		logger.debug(files.size());
		if (duplicationInClipnames(files)) {
			JOptionPane.showMessageDialog(new JFrame(),
					"There are duplicated clipnames in your csv. I added a duplicatedClips.csv. Please rename them uniquely to continue.");
			writeCsvFromDavinciFile(getClipsWithDuplicatedClipname(files), outputPath, "\\duplicatedClips.csv");
			return;
		}
		writeDavinciFilesWithClipname(outputPath, files);

	}

	private static boolean duplicationInClipnames(List<DavinciFile> files) {
		return files.stream().filter(distinctByKey(DavinciFile::getSoundRoll)).collect(Collectors.toList())
				.size() < files.size();
	}

	private static List<DavinciFile> getClipsWithDuplicatedClipname(List<DavinciFile> files) {
		List<DavinciFile> distinctFiles = files.stream().filter(distinctByKey(DavinciFile::getSoundRoll))
				.collect(Collectors.toList());
		List<String> duplicatedName = files.stream().filter(i -> !distinctFiles.contains(i))
				.map(DavinciFile::getSoundRoll).collect(Collectors.toList());
		return files.stream().filter(i -> duplicatedName.contains(i.getSoundRoll())).collect(Collectors.toList());
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
		Map<Object, Boolean> map = new ConcurrentHashMap<>();
		return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	private static void writeInfoToCsv(String pgpassPath, String outputPath) {
		Optional<DatabaseConfig> optionalConfig = readPgpass(pgpassPath);
		if (optionalConfig.isPresent()) {
			DatabaseConfig config = optionalConfig.get();
			List<DavinciFile> files = getDisplaynamesFromDatabase(config);
			logger.debug(files.size());
			writeCsvFromDavinciFile(files, outputPath, "\\metadata.csv");
		}
	}

	private static Optional<DatabaseConfig> readPgpass(String pgpassPath) {
		List<String> lines = new ArrayList<>();
		try {
			lines = Files.readAllLines(Paths.get(pgpassPath), StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.info(e);
		}
		if (!lines.isEmpty()) {
			String line = lines.get(0);
			String[] parameters = line.split(":");
			return Optional
					.of(new DatabaseConfig(parameters[0], parameters[1], parameters[2], parameters[3], parameters[4]));
		}
		return Optional.empty();
	}

	private static List<DavinciFile> getDisplaynamesFromDatabase(DatabaseConfig config) {
		List<DavinciFile> filesFromDatabase = new ArrayList<>();
		String url = "jdbc:postgresql://localhost:" + config.getPort()  + "/" + config.getDatabase();
		logger.debug(url);
		logger.debug(config);
		Properties props = new Properties();
		props.setProperty("user", config.getUsername());
		props.setProperty("password", config.getPassword());
		try (Connection con = DriverManager.getConnection(url, props)) {
			String byteDisplayName = "\"FieldsBlob\"";
			String sourceNameString = "\"Name\"";
			String byteDirectory = "\"Clip\"";
			String generalMediaTable = "\"Sm2MpMedia\"";
			String audioInfoTable = "\"BtAudioInfo\"";
			String equals = "''";
			String tableSql = "SELECT o." + byteDisplayName + "::bytea, " + sourceNameString + ", " + byteDirectory
					+ " FROM " + generalMediaTable + " as o, " + audioInfoTable
					+ "as i  WHERE i.\"Sm2MpMedia_id\" = o.\"Sm2MpMedia_id\" AND o." + byteDisplayName + " != " + equals
					+ " AND o." + sourceNameString + " LIKE '%.wav'";
			logger.info(tableSql);
			try (ResultSet set = con.createStatement().executeQuery(tableSql)) {
				while (set.next()) {
					byte[] bytes = set.getBytes("FieldsBlob");
					String fixed = getCorrectBase64String(bytes);
					String displayName = fixed.subSequence(fixed.indexOf("DisplayName") + 11, fixed.length())
							.toString();
					logger.info(displayName);
					String name = set.getString("Name");
					logger.info(name);
					byte[] clipBytes = set.getBytes("Clip");
					String fixedClip = getCorrectBase64String(clipBytes);
					String fixedClipDisplayName = fixedClip
							.subSequence(fixedClip.indexOf("Path") + 5, fixedClip.indexOf("Name")).toString();
					logger.info(fixedClipDisplayName);
					if (!displayName.contentEquals("@")) {
						filesFromDatabase.add(new DavinciFile(name, fixedClipDisplayName, displayName));
					}
				}
			}
		} catch (SQLException e) {
			logger.info(e);
		}
		return filesFromDatabase;
	}

	private static String getCorrectBase64String(byte[] bytes) {
		String encode = Base64.getEncoder().withoutPadding().encodeToString(bytes);
		String decodedDirectlyFromByte = new String(Base64.getDecoder().decode(encode));
		return decodedDirectlyFromByte.replaceAll("[^\\x20-\\x7e]", "");
	}

	public static boolean isValidPath(String path) {
		try {
			Paths.get(path);
		} catch (InvalidPathException | NullPointerException ex) {
			return false;
		}
		return true;
	}

	private static void writeDavinciFilesWithClipname(String outputPath, List<DavinciFile> csvToBean) {
		for (DavinciFile file : csvToBean) {
			try {
				Files.copy(file.getSource(), file.getTarget(outputPath), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				logger.info(e);
			}
		}
	}

	private static void writeCsvFromDavinciFile(List<DavinciFile> files, String outputfolder, String name) {
		OutputStreamWriter writer = null;
		try (FileOutputStream stream = new FileOutputStream(outputfolder + name)) {
			writer = new OutputStreamWriter(stream, StandardCharsets.UTF_16);
			new StatefulBeanToCsvBuilder<DavinciFile>(writer).build().write(files);
			writer.flush();
			writer.close();
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			logger.info(e);
		}
	}

	private static List<DavinciFile> getDavinciFilesFormCsv(String metaDataCsv) {
		BufferedReader reader = null;
		try{
			reader = Files.newBufferedReader(Paths.get(metaDataCsv), StandardCharsets.UTF_16);
		} catch (IOException e) {
			logger.info(e);
		}
		RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
		CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(rfc4180Parser).build();

		return new CsvToBeanBuilder<DavinciFile>(csvReader).withType(DavinciFile.class)
				.withIgnoreLeadingWhiteSpace(true).build().parse();
	}

}
