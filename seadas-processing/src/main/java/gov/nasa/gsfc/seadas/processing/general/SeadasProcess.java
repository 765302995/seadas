package gov.nasa.gsfc.seadas.processing.general;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by aabduraz on 6/11/15.
 */

public class SeadasProcess extends Process {
    int exitValue;
    private InputStream inputStream;
    private InputStream errorStream;
    private OutputStream outputStream;
    int waitFor;

    public void destroy(){

    }
    public int exitValue(){
        return exitValue;
    }

    @Override
    public InputStream getErrorStream(){
        return errorStream;
    }


    @Override
    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setErrorStream(InputStream errorStream) {
        this.errorStream = errorStream;
    }

    @Override
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public int waitFor(){
        return waitFor;
    }
}
