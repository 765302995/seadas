package gov.nasa.gsfc.seadas.processing.l2gen;

import gov.nasa.gsfc.seadas.processing.general.ProcessorModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by IntelliJ IDEA.
 * User: knowles
 * Date: 5/17/12
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class FileInfo {

    private final MissionInfo missionInfo = new MissionInfo();
    private final FileTypeInfo fileTypeInfo = new FileTypeInfo();

    private File file;


    public FileInfo() {
    }

    public FileInfo(File file) {
        this();
        setFile(file);
    }

    public void clear() {
        file = null;
        missionInfo.clear();
        fileTypeInfo.clear();
    }

    public void setFile(File file) {
        clear();

        if (file == null || file.getAbsolutePath().toString().length() == 0) {
            return;
        }

        this.file = file;

        String FILE_INFO_SYSTEM_CALL = "get_obpg_file_type.py";

        ProcessorModel processorModel = new ProcessorModel(FILE_INFO_SYSTEM_CALL);
        processorModel.setAcceptsParFile(false);
        processorModel.addParamInfo("file", file.getAbsolutePath(), 1);

        try {
            Process p = processorModel.executeProcess();
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = stdInput.readLine();
            if (line != null) {
                String splitLine[] = line.split(":");
                if (splitLine.length == 3) {
                    String missionName = splitLine[1].toString().trim();
                    String fileType = splitLine[2].toString().trim();

                    if (fileType.length() > 0) {
                        fileTypeInfo.setType(fileType);
                    }

                    if (missionName.length() > 0) {
                        missionInfo.setName(missionName);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("ERROR - Problem running " + FILE_INFO_SYSTEM_CALL);
            System.out.println(e.getMessage());
        }
    }


    //-------------------------- Direct Get Methods ----------------------------

    public File getFile() {
        return file;
    }

    public MissionInfo getMissionInfo() {
        return missionInfo;
    }

    //-------------------------- Indirect Get Methods ----------------------------

    public boolean isFileExists() {
        if (file != null) {
            return file.exists();
        }
        return false;
    }

    public MissionInfo.Id getMission() {
        return missionInfo.getId();
    }

    public boolean isMission(MissionInfo.Id missionId) {
        return missionInfo.isId(missionId);
    }

    public File getOFile() {
        return FilenamePatterns.getOFile(file, missionInfo);
    }

    public String getOFileName() {
        return FilenamePatterns.getOFileName(file, missionInfo);
    }

    public boolean geofileRequired() {
        return missionInfo.isGeofileRequired();
    }

    public File getGeoFile() {
        return FilenamePatterns.getGeoFile(file, missionInfo);
    }

    public String getGeoFileName() {
        return FilenamePatterns.getGeoFileName(file, missionInfo);
    }

    public String getMissionDirectory() {
        return missionInfo.getDirectory();
    }

    public FileTypeInfo.Id getType() {
        return fileTypeInfo.getType();
    }

    public void setType(FileTypeInfo.Id type) {
        fileTypeInfo.setType(type);
    }

    public boolean isType(FileTypeInfo.Id type) {
        return fileTypeInfo.isType(type);
    }

    public boolean isSupportedMission() {
        return missionInfo.isSupported();
    }
}
