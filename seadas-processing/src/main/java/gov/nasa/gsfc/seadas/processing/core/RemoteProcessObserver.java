package gov.nasa.gsfc.seadas.processing.core;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.OCSSWInfo;
import gov.nasa.gsfc.seadas.ocssw.OCSSWClient;

import javax.ws.rs.client.WebTarget;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static gov.nasa.gsfc.seadas.ocssw.OCSSWRemoteClient.PROCESS_STATUS_COMPLETED;
import static gov.nasa.gsfc.seadas.ocssw.OCSSWRemoteClient.PROCESS_STATUS_NONEXIST;
import static gov.nasa.gsfc.seadas.ocssw.OCSSWRemoteClient.PROCESS_STATUS_STARTED;

/**
 * Created by aabduraz on 9/12/17.
 */
public class RemoteProcessObserver extends ProcessObserver {

    OCSSWInfo ocsswInfo;
    WebTarget target;
    private String jobId;
    private boolean serverProcessCompleted = false;


    /**
     * Constructor.
     *
     * @param process         The process to be observed
     * @param processName     A name that represents the process
     * @param progressMonitor A progress monitor
     */
    public RemoteProcessObserver(Process process, String processName, ProgressMonitor progressMonitor) {
        super(process, processName, progressMonitor);
        this.ocsswInfo = OCSSWInfo.getInstance();
        OCSSWClient ocsswClient = new OCSSWClient(ocsswInfo.getResourceBaseUri());
        target = ocsswClient.getOcsswWebTarget();
    }


    /**
     * Starts observing the given process. The method blocks until both {@code stdout} and {@code stderr}
     * streams are no longer available. If the progress monitor is cancelled, the process will be destroyed.
     */
    @Override
    public final void startAndWait() {
        final Thread stdoutReaderThread = new RemoteProcessObserver.LineReaderThread(STDOUT);
        final Thread stderrReaderThread = new RemoteProcessObserver.LineReaderThread(STDERR);
        stdoutReaderThread.start();
        stderrReaderThread.start();
        awaitTermination(stdoutReaderThread, stderrReaderThread);
    }

    private void awaitTermination(Thread stdoutReaderThread, Thread stderrReaderThread) {
        while (stdoutReaderThread.isAlive() && stderrReaderThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // todo - check what is best done now:
                //      * 1. just leave, and let the process be unattended (current impl.)
                //        2. destroy the process
                //        3. throw a checked ProgressObserverException
                return;
            }
            if (progressMonitor.isCanceled()) {
                // todo - check what is best done now:
                //        1. just leave, and let the process be unattended
                //      * 2. destroy the process (current impl.)
                //        3. throw a checked ProgressObserverException
                process.destroy();
            }
        }
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public boolean isServerProcessCompleted() {
        return serverProcessCompleted;
    }

    public void setServerProcessCompleted(boolean serverProcessCompleted) {
        this.serverProcessCompleted = serverProcessCompleted;
    }

    /**
     * A handler that will be informed if a new line has been read from either {@code stdout} or {@code stderr}.
     */
    public static interface Handler {
        /**
         * Handle the new line that has been read from {@code stdout}.
         *
         * @param line            The line.
         * @param process         The process.
         * @param progressMonitor The progress monitor, that is used to monitor the progress of the running process.
         */
        void handleLineOnStdoutRead(String line, Process process, ProgressMonitor progressMonitor);

        /**
         * Handle the new line that has been read from {@code stderr}.
         *
         * @param line            The line.
         * @param process         The process.
         * @param progressMonitor The progress monitor, that is used to monitor the progress of the running process.
         */
        void handleLineOnStderrRead(String line, Process process, ProgressMonitor progressMonitor);
    }

    private class LineReaderThread extends Thread {
        private final String type;

        public LineReaderThread(String type) {
            super(processName + "-" + type);
            this.type = type;
        }

        @Override
        public void run() {
            try {
                read();
            } catch (IOException e) {
                // cannot be handled
            }
        }

        protected void readNew() throws IOException {
            OCSSWClient ocsswClient = new OCSSWClient();
            WebTarget target = ocsswClient.getOcsswWebTarget();

            String processStatus = "-1";

            while (!isServerProcessCompleted()) {
                switch (processStatus) {
                    case PROCESS_STATUS_NONEXIST:
                        setServerProcessCompleted(false);
                    case PROCESS_STATUS_STARTED:
                        setServerProcessCompleted(false);
                    case PROCESS_STATUS_COMPLETED:
                        setServerProcessCompleted(true);
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                processStatus = target.path("ocssw").path("processStatus").path(jobId).request().get(String.class);
                System.out.println("process status in progress monitor: " + processStatus);
            }
        }

        private void read() throws IOException {
            System.out.println("reading from process input stream ...");
            String processStatus = target.path("ocssw").path("processStatus").path(jobId).request().get(String.class);
            String line = null;
            while (!processStatus.equals(PROCESS_STATUS_COMPLETED)) {
                switch (type) {
                    case STDOUT:
                        line = target.path("ocssw").path("retrieveProcessInputStreamLine").path(jobId).request().get(String.class);
                        System.out.println(" in progress monitor: " + type + "  " + line);
                        break;
                    case STDERR:
                        line = target.path("ocssw").path("retrieveProcessErrorStreamLine").path(jobId).request().get(String.class);
                        System.out.println(" in progress monitor: " + type + "  " + line);
                }

                processStatus = target.path("ocssw").path("processStatus").path(jobId).request().get(String.class);
                fireLineRead(line);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        protected void fireLineRead(String line) {
            for (ProcessObserver.Handler handler : handlers) {
                if (type.equals("stdout")) {
                    handler.handleLineOnStdoutRead(line, process, progressMonitor);
                } else {
                    handler.handleLineOnStderrRead(line, process, progressMonitor);
                }
            }
        }

    }
}
