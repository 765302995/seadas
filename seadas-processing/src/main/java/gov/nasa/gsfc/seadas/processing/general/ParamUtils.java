package gov.nasa.gsfc.seadas.processing.general;

import gov.nasa.gsfc.seadas.processing.l2gen.ParamInfo;
import gov.nasa.gsfc.seadas.processing.l2gen.ParamValidValueInfo;
import gov.nasa.gsfc.seadas.processing.l2gen.XmlReader;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Aynur Abdurazik (aabduraz)
 * Date: 3/19/12
 * Time: 8:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class ParamUtils {

    private String OCDATAROOT = System.getenv("OCDATAROOT");

    public static final String PAR = "par";
    public static final String GEOFILE = "geofile";
    public static final String SPIXL = "spixl";
    public static final String EPIXL = "epixl";
    public static final String DPIXL = "dpixl";
    public static final String SLINE = "sline";
    public static final String ELINE = "eline";
    public static final String DLINE = "dline";
    public static final String NORTH = "north";
    public static final String SOUTH = "south";
    public static final String WEST = "west";
    public static final String EAST = "east";
    public static final String IFILE = "ifile";
    public static final String OFILE = "ofile";
    public static final String L2PROD = "l2prod";

    public static final String OPTION_NAME = "name";
    public static final String OPTION_TYPE = "type";

    public static final String XML_ELEMENT_HAS_GEO_FILE = "hasGeoFile";
    public static final String XML_ELEMENT_HAS_PAR_FILE = "hasParFile";

    public static final String NO_XML_FILE_SPECIFIED = "No XML file Specified";

    public final String INVALID_IFILE_EVENT = "INVALID_IFILE_EVENT";
    public final String PARFILE_CHANGE_EVENT = "PARFILE_CHANGE_EVENT";

    public final String WAVE_LIMITER_CHANGE_EVENT = "WAVE_LIMITER_CHANGE_EVENT";

    public final String DEFAULTS_CHANGED_EVENT = "DEFAULTS_CHANGED_EVENT";


    private static int longestIFileNameLength;

    public enum nullValueOverrides {
        IFILE, OFILE, PAR, GEOFILE
    }


    public static void parseXMLForProcessor(String programXMLFileName) {

        XmlReader xmlReader = new XmlReader();
        InputStream xmlFileStream = ParamUtils.class.getResourceAsStream(programXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(xmlFileStream);

        NodeList optionNodelist = rootElement.getElementsByTagName("option");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            return;
        }

    }

    public ArrayList computeParamListNew(String paramXmlFileName) {

        if (paramXmlFileName.equals(NO_XML_FILE_SPECIFIED)) {
            return getDefaultParamList();
        }

        final ArrayList<ParamInfo> paramList = new ArrayList<ParamInfo>();

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(paramXmlFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("option");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            return null;
        }
        for (int i = 0; i < optionNodelist.getLength(); i++) {

            Element optionElement = (Element) optionNodelist.item(i);


            String name = XmlReader.getTextValue(optionElement, OPTION_NAME);
            String tmpType = optionElement.getAttribute(OPTION_TYPE);
            debug("option type in ParamUtils: " + tmpType);
            ParamInfo.Type type = null;

            if (tmpType != null) {
                if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_BOOLEAN )) {
                    type = ParamInfo.Type.BOOLEAN;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_INT)) {
                    type = ParamInfo.Type.INT;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_FLOAT )) {
                    type = ParamInfo.Type.FLOAT;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_STRING )) {
                    type = ParamInfo.Type.STRING;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_IFILE)) {
                    type = ParamInfo.Type.IFILE;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_OFILE )) {
                    type = ParamInfo.Type.OFILE;
                } else if (tmpType.toLowerCase().equals(ParamInfo.PARAM_TYPE_HELP  )) {
                    type = ParamInfo.Type.HELP;
                }
            }

            String value = XmlReader.getTextValue(optionElement, "value");


            if (name != null) {
                String nullValueOverrides[] = {ParamUtils.IFILE, ParamUtils.OFILE, ParamUtils.PAR, ParamUtils.GEOFILE};
                for (String nullValueOverride : nullValueOverrides) {
                    if (name.equals(nullValueOverride)) {
                        value = ParamInfo.NULL_STRING;
                    }
                }
            }

            String defaultValue = value;
            String description = XmlReader.getTextValue(optionElement, "description");
            String source = XmlReader.getTextValue(optionElement, "source");


            ParamInfo paramInfo = new ParamInfo(name, value, type);

            paramInfo.setDescription(description);
            paramInfo.setDefaultValue(defaultValue);
            paramInfo.setSource(source);

            NodeList validValueNodelist = optionElement.getElementsByTagName("validValue");

            if (validValueNodelist != null && validValueNodelist.getLength() > 0) {
                for (int j = 0; j < validValueNodelist.getLength(); j++) {

                    Element validValueElement = (Element) validValueNodelist.item(j);

                    String validValueValue = XmlReader.getTextValue(validValueElement, "value");
                    String validValueDescription = XmlReader.getTextValue(validValueElement, "description");

                    ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(validValueValue);

                    paramValidValueInfo.setDescription(validValueDescription);
                    paramValidValueInfo.setParent(paramInfo);      // why to set the parent?
                    paramInfo.addValidValueInfo(paramValidValueInfo);
                }

            }

            paramList.add(paramInfo);

        }

        return paramList;
    }

    public static boolean getParFilePreference(String parXMLFileName) {

        boolean acceptsParFile = false;
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("programMetaData");
        if (optionNodelist != null && optionNodelist.getLength() != 0) {
            Element metaDataElement = (Element) optionNodelist.item(0);
            acceptsParFile = XmlReader.getBooleanValue(metaDataElement, "hasParFile");
        }
        return acceptsParFile;
    }

       public static boolean getOptionStatus(String parXMLFileName, String elementName) {

        boolean optionStatus = false;
        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(parXMLFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName(elementName);
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            SeadasLogger.getLogger().warning(elementName + " exist: " + optionStatus)  ;
            return optionStatus;
        }
            Element metaDataElement = (Element) optionNodelist.item(0);

            String name = metaDataElement.getTagName();
            SeadasLogger.getLogger().fine("tag name: " + name);
         //   if (name.equals(elementName)) {
                optionStatus = Boolean.parseBoolean(metaDataElement.getFirstChild().getNodeValue())  ;
                SeadasLogger.getLogger().fine(name + " value = " + metaDataElement.getFirstChild().getNodeValue()  + " " + optionStatus );
          //  }

        return optionStatus;
    }

    public static int getLongestIFileNameLength() {
        return longestIFileNameLength;
    }
    public static ArrayList computeParamList(String paramXmlFileName) {

        if (paramXmlFileName.equals(NO_XML_FILE_SPECIFIED)) {
            return getDefaultParamList();
        }

        final ArrayList<ParamInfo> paramList = new ArrayList<ParamInfo>();

        XmlReader xmlReader = new XmlReader();
        InputStream paramStream = ParamUtils.class.getResourceAsStream(paramXmlFileName);
        Element rootElement = xmlReader.parseAndGetRootElement(paramStream);
        NodeList optionNodelist = rootElement.getElementsByTagName("option");
        if (optionNodelist == null || optionNodelist.getLength() == 0) {
            return null;
        }

        longestIFileNameLength = 0;

        for (int i = 0; i < optionNodelist.getLength(); i++) {

            Element optionElement = (Element) optionNodelist.item(i);

            String name = XmlReader.getTextValue(optionElement, OPTION_NAME);
            debug("option name: " + name);
            String tmpType = XmlReader.getAttributeTextValue(optionElement, OPTION_TYPE);
            debug("option type: " + tmpType);

            ParamInfo.Type type = null;

            if (tmpType != null) {
                if (tmpType.toLowerCase().equals("boolean")) {
                    type = ParamInfo.Type.BOOLEAN;
                } else if (tmpType.toLowerCase().equals("int")) {
                    type = ParamInfo.Type.INT;
                } else if (tmpType.toLowerCase().equals("float")) {
                    type = ParamInfo.Type.FLOAT;
                } else if (tmpType.toLowerCase().equals("string")) {
                    type = ParamInfo.Type.STRING;
                } else if (tmpType.toLowerCase().equals("ifile")) {
                    type = ParamInfo.Type.IFILE;
                    if (name.length() > longestIFileNameLength ) {
                        longestIFileNameLength = name.length();
                    }
                } else if (tmpType.toLowerCase().equals("ofile")) {
                    type = ParamInfo.Type.OFILE;
                }   else if (tmpType.toLowerCase().equals("help")) {
                    type = ParamInfo.Type.HELP;
                }
            }

            String value = XmlReader.getTextValue(optionElement, "value");


            if (name != null) {
                String nullValueOverrides[] = {ParamUtils.IFILE, ParamUtils.OFILE, ParamUtils.PAR, ParamUtils.GEOFILE};
                for (String nullValueOverride : nullValueOverrides) {
                    if (name.equals(nullValueOverride)) {
                        value = ParamInfo.NULL_STRING;
                    }
                }
            }

            String defaultValue = value;
            String description = XmlReader.getTextValue(optionElement, "description");
            String source = XmlReader.getTextValue(optionElement, "source");
            String order = XmlReader.getTextValue(optionElement, "order");

            ParamInfo paramInfo = new ParamInfo(name, value, type);

            paramInfo.setDescription(description);
            paramInfo.setDefaultValue(defaultValue);
            paramInfo.setSource(source);

            if (order != null) {
                paramInfo.setOrder(new Integer(order).intValue());
            }


            NodeList validValueNodelist = optionElement.getElementsByTagName("validValue");

            if (validValueNodelist != null && validValueNodelist.getLength() > 0) {
                for (int j = 0; j < validValueNodelist.getLength(); j++) {

                    Element validValueElement = (Element) validValueNodelist.item(j);

                    String validValueValue = XmlReader.getTextValue(validValueElement, "value");
                    String validValueDescription = XmlReader.getTextValue(validValueElement, "description");

                    ParamValidValueInfo paramValidValueInfo = new ParamValidValueInfo(validValueValue);

                    paramValidValueInfo.setDescription(validValueDescription);
                    paramValidValueInfo.setParent(paramInfo);      // why need a parent?
                    paramInfo.addValidValueInfo(paramValidValueInfo);
                }

            }

            paramList.add(paramInfo);

        }

        return paramList;
    }


    /**
     * Create a default array list with ifile, ofile,  spixl, epixl, sline, eline options
     *
     * @return
     */
    public static ArrayList getDefaultParamList() {
        ArrayList<ParamInfo> defaultParamList = new ArrayList<ParamInfo>();
        return defaultParamList;
    }

    static void debug(String debugMessage) {
        //System.out.println(debugMessage);
    }


}
