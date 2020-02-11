package niaz.faridanirad.RenameToClipname;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

/**
 * Hello world!
 *
 */
public class App 
{
	private static Logger logger = LogManager.getLogger(App.class);
	
    public static void main(String[] args)
    {
    	writeInfoToCsv();
    	copyFromCSVToFolderWithNewNames();
    }
    
    private static void copyFromCSVToFolderWithNewNames() {
    	String metaDataCsv = getAbsolutPath("Choose the metadata.csv file: ",false);
    	String outputPath = getAbsolutPath("Choose the Output Directory: ",true);
    	
    	logger.debug(metaDataCsv);
    	logger.debug(outputPath);
    	if(!isValidPath(metaDataCsv) && !isValidPath(outputPath) ) {
    		JOptionPane.showMessageDialog(new JFrame(),
    			    "Paths are not valid");
    		return;
    	}
    	List<DavinciFile> files = getDavinciFilesFormCsv(metaDataCsv);
    	
    	if(duplicationInClipnames(files)) {
    		JOptionPane.showMessageDialog(new JFrame(),
    			    "There are duplicated clipnames in your csv. I added a duplicatedClips.csv. Please rename them uniquely to continue.");
    		writeCsvFromDavinciFile(getClipsWithDuplicatedClipname(files), outputPath, "\\duplicatedClips.csv");
    		return;
    	}
    	writeDavinciFilesWithClipname(outputPath, files);
    	
    }
    
    private static boolean duplicationInClipnames(List<DavinciFile> files) {
		return files.stream().filter(distinctByKey(p -> p.getSoundRoll())).collect(Collectors.toList()).size() < files.size();
	}
    
    private static List<DavinciFile> getClipsWithDuplicatedClipname(List<DavinciFile> files) {
    	List<DavinciFile> distinctFiles = files.stream().filter(distinctByKey(p -> p.getSoundRoll())).collect(Collectors.toList());
    	List<String> duplicatedName = files.stream().filter(i -> !distinctFiles.contains(i)).map( i -> i.getSoundRoll()).collect(Collectors.toList());
    	return files.stream().filter(i -> duplicatedName.contains(i.getSoundRoll())).collect(Collectors.toList());
    }
    
    public static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) 
    {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

	private static void writeInfoToCsv() {
    	List<DavinciFile> files = getDisplaynamesFromDatabase();
    	System.out.println(files.size());
    	String outputPath = getAbsolutPath("Choose the Output Directory: ",true);
    	writeCsvFromDavinciFile(files, outputPath, "\\metadata.csv");
    }
	
	

	private static List<DavinciFile> getDisplaynamesFromDatabase() {
		List<DavinciFile> filesFromDatabase = new ArrayList<>();
		try {
			Class.forName("org.postgresql.Driver");
			String url = "jdbc:postgresql://localhost/verstecken";
			Properties props = new Properties();
			props.setProperty("user","postgres");
			props.setProperty("password","DaVinci");
			Connection con = DriverManager.getConnection(url, props);
			Statement stmt = con.createStatement();
			String byteDisplayName = "\"FieldsBlob\"";
			String sourceNameString = "\"Name\"";
			String byteDirectory = "\"Clip\"";
			String generalMediaTable = "\"Sm2MpMedia\"";
			String audioInfoTable = "\"BtAudioInfo\"";
			String equals = "''";
			String tableSql = "SELECT o."+ byteDisplayName +"::bytea, " + sourceNameString + ", " + byteDirectory +  " FROM "+ generalMediaTable + " as o, " + audioInfoTable +  "as i  WHERE i.\"Sm2MpMedia_id\" = o.\"Sm2MpMedia_id\" AND o." + byteDisplayName + " != " + equals + " AND o." + sourceNameString + " LIKE '%.wav'";
			logger.info(tableSql);		
			ResultSet set = stmt.executeQuery(tableSql);
			while(set.next()) {
				byte[] bytes = set.getBytes("FieldsBlob");
				String fixed = getCorrectBase64String(bytes);
				String displayName = fixed.subSequence(fixed.indexOf("DisplayName")+11, fixed.length()).toString();
				logger.info(displayName);
				String name = set.getString("Name");
				logger.info(name);
				byte[] clipBytes = set.getBytes("Clip");
				String fixedClip = getCorrectBase64String(clipBytes);
				String fixedClipDisplayName = fixedClip.subSequence(fixedClip.indexOf("Path")+5, fixedClip.indexOf("Name")).toString();
				logger.info(fixedClipDisplayName);
				if(!displayName.contentEquals("@")) {
					filesFromDatabase.add(new DavinciFile(name, fixedClipDisplayName, displayName));
				}
			}
			con.close();
		    
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return filesFromDatabase;
	}

	private static String getCorrectBase64String(byte[] bytes) {
		String encode = Base64.getEncoder().withoutPadding().encodeToString(bytes);
		String decodedDirectlyFromByte = new String(Base64.getDecoder().decode(encode));
		String fixed = decodedDirectlyFromByte.replaceAll("[^\\x20-\\x7e]", "");
		return fixed;
	}
    
    public static boolean isValidPath(String path) {
        try {
            Paths.get(path);
        } catch (InvalidPathException | NullPointerException ex) {
            return false;
        }
        return true;
    }

	private static String getAbsolutPath(String dialogTitle, boolean directory) {
		JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
    	jfc.setDialogTitle(dialogTitle);
    	if(directory) {
    		jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	}
    	int returnValue = jfc.showOpenDialog(null);
		if (returnValue == JFileChooser.APPROVE_OPTION) {
				return jfc.getSelectedFile().getAbsolutePath();
		}
		return "";
	}

	private static void writeDavinciFilesWithClipname(String outputPath, List<DavinciFile> csvToBean) {
    	for (DavinciFile file : csvToBean) {
    		try {
				Files.copy(file.getSource(), file.getTarget(outputPath), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
	}
	
	private static void writeCsvFromDavinciFile(List<DavinciFile> files, String outputfolder, String name) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(outputfolder + name , StandardCharsets.UTF_16);
		}catch (IOException e) {
			e.printStackTrace();
		}
		try {
			new StatefulBeanToCsvBuilder<DavinciFile>(writer)
			.build().write(files);
			writer.flush();
			writer.close();
		} catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IOException e) {
			e.printStackTrace();
		};
	}

	private static List<DavinciFile> getDavinciFilesFormCsv(String metaDataCsv) {
		
		FileReader reader = null;
		try {
			reader = new FileReader(metaDataCsv, StandardCharsets.UTF_16);
		} catch (IOException e) {
			e.printStackTrace();
		}
		RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder().build();
		CSVReader csvReader = new CSVReaderBuilder(reader).withCSVParser(rfc4180Parser).build();
		
		return new CsvToBeanBuilder<DavinciFile>(csvReader)
                .withType(DavinciFile.class)
                .withIgnoreLeadingWhiteSpace(true)
                .build().parse();	
	}
    
 
}
