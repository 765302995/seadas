package gov.nasa.gsfc.seadas.ocsswrest.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 2/5/15
 * Time: 5:30 PM
 * To change this template use File | Settings | File Templates.
 */

public class SQLiteJDBC {

    public static String JOB_DB_URL = "jdbc:sqlite:ocssw.db";
    public static String DB_CLASS_FOR_NAME = "org.sqlite.JDBC";
    public static String SQL_INSERT_STRING = "INSERT INTO PROCESSOR_TABLE (JOB_ID,CLIENT_ID,PROCESSOR_ID,COMMAND_ARRAY, STATUS) ";
    public static String SQL_INSERT_STRING_SERVICES = "INSERT INTO FILE_TABLE (JOB_ID, I_FILE_TYPE, O_FILE_NAME, MISSION, STATUS) ";
    private static String username = "aynur";
    private static String password = "aynur";


    private static String NEXT_LEVEL_NAME_FINDER_PROGRAM_NAME = "next_level_name.py";
    private static String NEXT_LEVEL_FILE_NAME_TOKEN = "Output Name:";


    public SQLiteJDBC() {

    }

    public void addJob() {
        try {
            Connection db = DriverManager.getConnection(JOB_DB_URL, username, password);
            db.close();
        } catch (SQLException sqle) {

        }

    }

    public static void main(String args[]) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL);

            stmt = c.createStatement();
            String processor_table_sql = "CREATE TABLE IF NOT EXISTS PROCESSOR_TABLE " +
                    "(JOB_ID INT PRIMARY KEY     NOT NULL, " +
                    " CLIENT_ID           INT    NOT NULL, " +
                    " PROCESSOR_ID            INT     NOT NULL, " +
                    " COMMAND_ARRAY  CHAR(500), " +
                    " STATUS        CHAR(50) )";
            String file_table_sql = "CREATE TABLE IF NOT EXISTS FILE_TABLE " +
                    "(JOB_ID INT PRIMARY KEY     NOT NULL, " +
                    " I_FILE_TYPE      CHAR(50)    NOT NULL, " +
                    " O_FILE_NAME      CHAR(100)    NOT NULL, " +
                    " MISSION  CHAR(50), " +
                    " STATUS        CHAR(50) )";
            stmt.executeUpdate(processor_table_sql);
            stmt.executeUpdate(file_table_sql);
            stmt.close();
            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

    public static void createTable() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);

            stmt = c.createStatement();
            String processor_table_sql = "CREATE TABLE IF NOT EXISTS PROCESSOR_TABLE " +
                    "(JOB_ID INT PRIMARY KEY     NOT NULL, " +
                    " CLIENT_ID           INT    NOT NULL, " +
                    " PROCESSOR_ID            INT     NOT NULL, " +
                    " COMMAND_ARRAY  CHAR(500), " +
                    " STATUS        CHAR(50) )";
            String file_table_sql = "CREATE TABLE IF NOT EXISTS FILE_TABLE " +
                    "(JOB_ID INT PRIMARY KEY     NOT NULL, " +
                    " I_FILE_TYPE      CHAR(50)    NOT NULL, " +
                    " O_FILE_NAME      CHAR(100)    NOT NULL, " +
                    " MISSION  CHAR(50), " +
                    " STATUS        CHAR(50) )";
            stmt.executeUpdate(processor_table_sql);
            stmt.executeUpdate(file_table_sql);
            stmt.close();
            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Opened database successfully");

    }

    public static void insertJob(String jobId, String clientId, String processorID, String status, String cmdArray) {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = SQL_INSERT_STRING +
                    "VALUES (" + jobId + "," + clientId + "," + processorID + "," + status + "," + cmdArray + ");";
            stmt.executeUpdate(sql);

            stmt.close();
            c.commit();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Records created successfully");
    }

    public void selectJob() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_TABLE;");
            while (rs.next()) {
                int id = rs.getInt("JOB_ID");
                String fileType = rs.getString("I_FILE_TYPE");
                String o_file_name = rs.getString("O_FILE_NAME");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + fileType);
                System.out.println("AGE = " + o_file_name);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }

    public static String retrieveItem(String jobId, String itemName) {
        Connection c = null;
        Statement stmt = null;
        String oFileName = "output";
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String queryString = "SELECT * FROM FILE_TABLE WHERE JOB_ID = " + jobId + ";";
            ResultSet rs = stmt.executeQuery(queryString);
            oFileName = rs.getString("O_FILE_NAME");
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(" in retrieve item : " + e.getClass().getName() + ": " + e.getMessage());
            //System.exit(0);
        }
        System.out.println("Operation done successfully");
        return oFileName;
    }

    public static String updateOFileName(String jobId, Process process) {
        int exitCode = process.exitValue();
        System.out.println("process exit code = " + exitCode);
        InputStream is;
        if (exitCode == 0) {
            is = process.getInputStream();
        } else {
            is = process.getErrorStream();
        }

        BufferedReader br;

        InputStreamReader isr = new InputStreamReader(is);
        br = new BufferedReader(isr);

        String oFileName = "output";

        try {

            if (exitCode == 0) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith(NEXT_LEVEL_FILE_NAME_TOKEN)) {
                        oFileName = (line.substring(NEXT_LEVEL_FILE_NAME_TOKEN.length())).trim();
                    }
                }
            }

        } catch (IOException ioe) {

            System.out.println(ioe.getMessage());
        }catch (NullPointerException npe) {

            System.out.println(npe.getMessage());
        }

        System.out.println("computed ofile Name = " + oFileName);

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "UPDATE FILE_TABLE set O_FILE_NAME = " + oFileName + " where JOB_ID=" + jobId;
            stmt.executeUpdate(sql);
            c.commit();

            ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_TABLE;");
            while (rs.next()) {
                int id = rs.getInt("JOB_ID");
                String fileType = rs.getString("I_FILE_TYPE");
                String o_file_name = rs.getString("O_FILE_NAME");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + fileType);
                System.out.println("AGE = " + o_file_name);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");
        return "ok";
    }


    public static void insertOFileName(String jobId, String ofileName) {

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "INSERT INTO FILE_TABLE (JOB_ID, I_FILE_TYPE, O_FILE_NAME, MISSION, STATUS) VALUES (" + jobId + ", , " + ofileName + ", , )";
            stmt.executeUpdate(sql);
            c.commit();

            ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_TABLE;");
            while (rs.next()) {
                int id = rs.getInt("JOB_ID");
                String fileType = rs.getString("I_FILE_TYPE");
                String o_file_name = rs.getString("O_FILE_NAME");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + fileType);
                System.out.println("AGE = " + o_file_name);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }


    public static void updateOFileName(String jobId, String ofileName) {

        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "UPDATE FILE_TABLE set O_FILE_NAME = " + ofileName + " where JOB_ID=" + jobId;
            stmt.executeUpdate(sql);
            c.commit();

            ResultSet rs = stmt.executeQuery("SELECT * FROM FILE_TABLE;");
            while (rs.next()) {
                int id = rs.getInt("JOB_ID");
                String fileType = rs.getString("I_FILE_TYPE");
                String o_file_name = rs.getString("O_FILE_NAME");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + fileType);
                System.out.println("AGE = " + o_file_name);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }

    public void updateJob() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "UPDATE JOBS set SALARY = 25000.00 where ID=1;";
            stmt.executeUpdate(sql);
            c.commit();

            ResultSet rs = stmt.executeQuery("SELECT * FROM JOBS;");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String address = rs.getString("address");
                float salary = rs.getFloat("salary");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + name);
                System.out.println("AGE = " + age);
                System.out.println("ADDRESS = " + address);
                System.out.println("SALARY = " + salary);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");

    }

    public void deleteJob() {
        Connection c = null;
        Statement stmt = null;
        try {
            Class.forName(DB_CLASS_FOR_NAME);
            c = DriverManager.getConnection(JOB_DB_URL, username, password);
            c.setAutoCommit(false);
            System.out.println("Opened database successfully");

            stmt = c.createStatement();
            String sql = "DELETE from JOBS where ID=2;";
            stmt.executeUpdate(sql);
            c.commit();

            ResultSet rs = stmt.executeQuery("SELECT * FROM JOBS;");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                String address = rs.getString("address");
                float salary = rs.getFloat("salary");
                System.out.println("ID = " + id);
                System.out.println("NAME = " + name);
                System.out.println("AGE = " + age);
                System.out.println("ADDRESS = " + address);
                System.out.println("SALARY = " + salary);
                System.out.println();
            }
            rs.close();
            stmt.close();
            c.close();
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        System.out.println("Operation done successfully");
    }
}


