package gov.nasa.gsfc.seadas.processing.general;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.logging.BeamLogManager;
import org.hsqldb.lib.FileUtil;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 5/10/12
 * Time: 2:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class SeadasLogger {

    private static boolean log = true;

    private static Logger logger; // Logger.getLogger(ProcessorModel.class.getName());

    private static String _loggerFileName = "seadasLog";

    public static void setLoggerFileName(String loggerFileName) {
        Guardian.assertNotNull("loggerFileName", loggerFileName);

        _loggerFileName = loggerFileName;
    }


    public static Logger getLogger() {
        return logger;
    }

    public static void initLogger(String loggerFileName, boolean printToConsole) {
        _loggerFileName = loggerFileName;
        if (log) {
            initLogger(printToConsole);
        } else {
            initSevereLogger();
        }
    }

    public static void initLogger(boolean printToConsole) {
        BeamLogManager.setSystemLoggerName("seadas");
        BeamLogManager.configureSystemLogger((new SimpleFormatter()), printToConsole);
        logger = BeamLogManager.getSystemLogger();
        FileHandler fileTxt;
        SimpleFormatter formatterTxt = new SimpleFormatter();

        FileHandler fileHTML;
        Formatter formatterHTML = new MyHtmlFormatter();

        logger.setLevel(Level.INFO);
        String txtLogFileName = _loggerFileName + ".txt";
        String htmlLogFileName = _loggerFileName + ".html";
        try {
            if (FileUtil.exists(txtLogFileName)) {
                FileUtil.delete(txtLogFileName);
            }
            if (FileUtil.exists(htmlLogFileName)) {
                FileUtil.delete(htmlLogFileName);
            }
            fileTxt = new FileHandler(txtLogFileName);
            fileHTML = new FileHandler(htmlLogFileName);

            //String currentDir = new File(".").getAbsolutePath();  //or String currentDir = System.getProperty("user.dir");

            File[] files = new File(System.getProperty("user.dir")).listFiles();
            for (File file : files) {
                String fileName = file.getName();
                System.out.println(fileName);
                if ((fileName.indexOf(htmlLogFileName) != -1 &&  fileName.substring(fileName.indexOf(htmlLogFileName) + htmlLogFileName.length()).trim().length() > 0)  ||
                    (fileName.indexOf(txtLogFileName) != -1 &&  fileName.substring(fileName.indexOf(txtLogFileName) + txtLogFileName.length()).trim().length() > 0) ) {
                     file.delete();
                }
            }
            fileTxt.setFormatter(formatterTxt);
            logger.addHandler(fileTxt);
            fileHTML.setFormatter(formatterHTML);
            logger.addHandler(fileHTML);
        } catch (IOException ioe) {

        }
    }

    public static void initSevereLogger() {
        BeamLogManager.setSystemLoggerName("seadas");
        BeamLogManager.configureSystemLogger((new SimpleFormatter()), true);
        logger = BeamLogManager.getSystemLogger();

        logger.setLevel(Level.SEVERE);

    }

    //This custom formatter formats parts of a log record to a single line
    private static class MyHtmlFormatter extends Formatter {
        // This method is called for every log records
        public String format(LogRecord rec) {
            StringBuffer buf = new StringBuffer(1000);
            // Bold any levels >= WARNING
            buf.append("<tr>");
            buf.append("<td>");

            if (rec.getLevel().intValue() >= Level.WARNING.intValue()) {
                buf.append("<b>");
                buf.append(rec.getLevel());
                buf.append("</b>");
            } else {
                buf.append(rec.getLevel());
            }
            buf.append("</td>");

            buf.append("<td>");
            buf.append(calcDate(rec.getMillis()));
            buf.append("</td>");
            buf.append("<td>");
            buf.append(' ');
            buf.append(formatMessage(rec));
            buf.append("</td>");
            buf.append("<td>");
            buf.append(rec.getSourceClassName());
            buf.append("</td>");
            buf.append("<td>");
            buf.append(rec.getSourceMethodName());
            buf.append("</td>");
            buf.append('\n');
            buf.append("</tr>\n");
            return buf.toString();
        }

        private String calcDate(long millisecs) {
            SimpleDateFormat date_format = new SimpleDateFormat("MMM dd,yyyy HH:mm");
            Date resultdate = new Date(millisecs);
            return date_format.format(resultdate);
        }

        // This method is called just after the handler using this
        // formatter is created
        public String getHead(Handler h) {
            return "<HTML>\n<HEAD>\n" + (new Date()) + "\n</HEAD>\n<BODY>\n<PRE>\n"
                    + "<table border>\n  "
                    + "<tr><th>Level</th><th>Time</th><th>Log Message</th><th>In Class</th><th>In Method</th></tr>\n";
        }

        // This method is called just after the handler using this
        // formatter is closed
        public String getTail(Handler h) {
            return "</table>\n  </PRE></BODY>\n</HTML>\n";
        }

    }

}
