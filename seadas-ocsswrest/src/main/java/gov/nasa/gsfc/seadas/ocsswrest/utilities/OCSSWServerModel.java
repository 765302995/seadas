package gov.nasa.gsfc.seadas.ocsswrest.utilities;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 5/16/13
 * Time: 10:52 AM
 * To change this template use File | Settings | File Templates.
 */

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;


public class OCSSWServerModel {


    public static final String OCSSWROOT_ENVVAR = "OCSSWROOT";

    public static final String OCSSWROOT_PROPERTY = "ocssw.root";
    public static final String SEADASHOME_PROPERTY = "home";

    public static String OCSSW_INSTALLER = "install_ocssw.py";

    public static String OCSSW_INSTALLER_URL = "http://oceandata.sci.gsfc.nasa.gov/ocssw/install_ocssw.py";
    public static String OCSSW_INSTALL_DIR = System.getProperty("user.home") + "/ocssw";
    public static String OCSSW_INSTALLER_FILE_LOCATION = (new File(System.getProperty("java.io.tmpdir"), "install_ocssw.py")).getPath();

    public static final String missionDataDir = OCSSW_INSTALL_DIR + System.getProperty("file.separator") + "run"
                + System.getProperty("file.separator") + "data"
                + System.getProperty("file.separator");

    private static boolean ocsswExist = false;
    private static File ocsswRoot = null;
    private static boolean ocsswInstalScriptDownloadSuccessful = false;



    public static File getOcsswRoot() throws IOException {
        return ocsswRoot;
    }

    public static boolean isOCSSWExist() {
        return ocsswExist;
    }

    public static void checkOCSSW() {

        // Check if ${ocssw.root}/run/scripts directory exists in the system.
        // Precondition to detect the existing installation:
        // the user needs to provide "seadas.ocssw.root" value in seadas.config
        // or set OCSSWROOT in the system env.
        ocsswRoot = new File(OCSSW_INSTALL_DIR);
        final File dir = new File(OCSSW_INSTALL_DIR + System.getProperty("file.separator") + "run" + System.getProperty("file.separator") + "scripts");
        if (dir.isDirectory()) {
            ocsswExist = true;
            return;
        }
    }
//    public static File getOcsswRoot() throws IOException {
//        //String dirPath = RuntimeContext.getConfig().getContextProperty(OCSSWROOT_PROPERTY, System.getenv(OCSSWROOT_ENVVAR));
//        String dirPath = System.getProperty(OCSSWROOT_PROPERTY, System.getenv(OCSSWROOT_ENVVAR));
//        if (dirPath == null) {
//            throw new IOException(String.format("Either environment variable '%s' or\n" +
//                    "configuration parameter '%s' must be given.", OCSSWROOT_ENVVAR, OCSSWROOT_PROPERTY));
//        }
//        final File dir = new File(dirPath);
//        if (!dir.isDirectory()) {
//            throw new IOException(String.format("The directory pointed to by the environment variable  '%s' or\n" +
//                    "configuration parameter '%s' seems to be invalid.", OCSSWROOT_ENVVAR, OCSSWROOT_PROPERTY));
//        }
//        return dir;
//    }


    public static String getOcsswScriptPath() {
        final File ocsswRoot = getOcsswRootFile();
        if (ocsswRoot != null) {
            return ocsswRoot.getPath() + "/run/scripts/ocssw_runner";
        } else {
            return null;
        }

    }

    public static String[] getOcsswEnv() {
        final File ocsswRoot = getOcsswRootFile();
        if (ocsswRoot != null) {
            final String[] envp = {"OCSSWROOT=" + ocsswRoot.getPath()};
            return envp;
        } else {
            return null;
        }
    }

    private static File getOcsswRootFile() {
        final File ocsswRoot;
        try {
            ocsswRoot = OCSSWServerModel.getOcsswRoot();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(new JFrame(), e.getMessage(), "Dialog", JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return ocsswRoot;
    }

    /**
     * this method doanloads the latest ocssw_install.py program from   "http://oceandata.sci.gsfc.nasa.gov/ocssw/install_ocssw.py" location.
     * @return  true if download is successful.
     */
    public static boolean downloadOCSSWInstaller() {

        if (isOcsswInstalScriptDownloadSuccessful()) {
            return ocsswInstalScriptDownloadSuccessful;
        }
        try {
            URL website = new URL(OCSSW_INSTALLER_URL);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            FileOutputStream fos = new FileOutputStream(OCSSW_INSTALLER_FILE_LOCATION);
            fos.getChannel().transferFrom(rbc, 0, 1 << 24);
            fos.close();
            (new File(OCSSW_INSTALLER_FILE_LOCATION)).setExecutable(true);
            ocsswInstalScriptDownloadSuccessful = true;
        } catch (MalformedURLException malformedURLException) {
            handleException("URL for downloading install_ocssw.py is not correct!");
        } catch (FileNotFoundException fileNotFoundException) {
            handleException("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the 'seadas.config' file. \n" +
                    "possible cause of error: " + fileNotFoundException.getMessage());
        } catch (IOException ioe) {
            handleException("ocssw installation script failed to download. \n" +
                    "Please check network connection or 'seadas.ocssw.root' variable in the \"seadas.config\" file. \n" +
                    "possible cause of error: " + ioe.getLocalizedMessage());
        } finally {
            return ocsswInstalScriptDownloadSuccessful;
        }
    }

    private static void handleException(String errorMessage) {
        System.out.println(errorMessage);
    }

    public static boolean isOcsswInstalScriptDownloadSuccessful() {
        return ocsswInstalScriptDownloadSuccessful;
    }



}
