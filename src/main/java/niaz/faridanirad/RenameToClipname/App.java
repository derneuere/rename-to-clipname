package niaz.faridanirad.RenameToClipname;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileSystemView;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.RFC4180Parser;
import com.opencsv.RFC4180ParserBuilder;
import com.opencsv.bean.CsvToBeanBuilder;

/**
 * Hello world!
 *
 */
public class App 
{
	
    public static void main(String[] args )
    {
    	String metaDataCsv = getAbsolutPath("Choose the metadata.csv file: ",false);
    	String outputPath = getAbsolutPath("Choose the Output Directory: ",true);
    	System.out.println(metaDataCsv);
    	System.out.println(outputPath);
    	if(isValidPath(metaDataCsv) && isValidPath(outputPath)) {
    		writeDavinciFilesWithClipname(outputPath, getDavinciFilesFormCsv(metaDataCsv));
    	}
    	else{
    		JOptionPane.showMessageDialog(new JFrame(),
    			    "Paths are not valid");
    	}
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
