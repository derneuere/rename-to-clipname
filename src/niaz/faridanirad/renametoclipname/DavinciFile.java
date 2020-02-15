package niaz.faridanirad.renametoclipname;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.opencsv.bean.CsvBindByName;

public class DavinciFile{
	
	@CsvBindByName(column = "File Name")
	String fileName;
	@CsvBindByName(column = "Clip Directory")
	String clipDirectory;
	@CsvBindByName(column = "Sound Roll #")
	String soundRoll;
	
	public DavinciFile(String fileName, String clipDirectory, String soundRoll) {
		super();
		this.fileName = fileName;
		this.clipDirectory = clipDirectory;
		this.soundRoll = soundRoll;
	}
	
	public DavinciFile() {
		
	}
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getClipDirectory() {
		return clipDirectory;
	}
	
	public void setClipDirectory(String clipDirectory) {
		this.clipDirectory = clipDirectory;
	}
	
	public String getSoundRoll() {
		return soundRoll;
	}
	
	public void setSoundRoll(String soundRoll) {
		this.soundRoll = soundRoll;
	}

	public Path getSource() {
		return Paths.get(clipDirectory, fileName);
	}
	
	public Path getTarget(String outputPath) {
		return Paths.get(outputPath, soundRoll + ".wav");
	}

	@Override
	public String toString() {
		return "DavinciFile [fileName=" + fileName + ", clipDirectory=" + clipDirectory + ", soundRoll=" + soundRoll
				+ "]";
	}
	
	
}