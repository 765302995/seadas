package gov.nasa.gsfc.seadas.sandbox.l2gen;

import java.util.ArrayList;
import java.util.Collections;

/**
 * A ...
 *
 * @author Danny Knowles
 * @since SeaDAS 7.0
 */
public class ParamInfo implements Comparable {

    private String name = NULL_STRING;
    private String value = NULL_STRING;
    private Type type = null;
    private String defaultValue = NULL_STRING;
    private String description = NULL_STRING;
    private String source = NULL_STRING;

    private ArrayList<ParamValidValueInfo> validValueInfos = new ArrayList<ParamValidValueInfo>();

    public static enum Type {
        BOOLEAN, STRING, INT, FLOAT
    }

    public static String NULL_STRING = "";
    public static String BOOLEAN_TRUE = "1";
    public static String BOOLEAN_FALSE = "0";

    public ParamInfo(String name, String value, Type type) {
        setName(name);
        setValue(value);
        setType(type);
    }

    public ParamInfo(String name, String value) {
        setName(name);
        setValue(value);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Clean up and handle input exceptions
        if (name == null) {
            this.name = NULL_STRING;
            return;
        }
        name = name.trim();


        if (name.length() == 0) {
            this.name = NULL_STRING;
        } else {
            this.name = name;
        }
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        // Clean up and handle input exceptions
        if (value == null) {
            this.value = NULL_STRING;
            return;
        }
        value = value.trim();

        if (value.length() == 0) {
            this.value = NULL_STRING;
        } else if (getType() == Type.BOOLEAN) {
            this.value = getStandardizedBooleanString(value);
        } else {
            this.value = value;
        }
    }


    public static String getStandardizedBooleanString(String booleanString) {

        if (booleanString == null) {
            return NULL_STRING;
        }

        String allowedTrueValues[] = {"1", "t", "true", "y", "yes", "on"};
        String allowedFalseValue[] = {"0", "f", "false", "n", "no", "off"};

        for (String trueValue : allowedTrueValues) {
            if (booleanString.toLowerCase().equals(trueValue)) {
                return BOOLEAN_TRUE;
            }
        }

        for (String falseValue : allowedFalseValue) {
            if (booleanString.toLowerCase().equals(falseValue)) {
                return BOOLEAN_FALSE;
            }
        }

        return NULL_STRING;
    }


    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        // Clean up and handle input exceptions
        if (defaultValue == null) {
            this.defaultValue = NULL_STRING;
            return;
        }
        defaultValue = defaultValue.trim();

        if (defaultValue.length() == 0) {
            this.defaultValue = NULL_STRING;
        } else if (getType() == Type.BOOLEAN) {
            this.defaultValue = getStandardizedBooleanString(defaultValue);
        } else {
            this.defaultValue = defaultValue;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        // Clean up and handle input exceptions
        if (description == null) {
            this.description = NULL_STRING;
            return;
        }
        description = description.trim();

        if (description.length() == 0) {
            this.description = NULL_STRING;
        } else {
            this.description = description;
        }
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        // Clean up and handle input exceptions
        if (source == null) {
            this.source = NULL_STRING;
            return;
        }
        source = source.trim();

        if (source.length() == 0) {
            this.source = NULL_STRING;
        } else {
            this.source = source;
        }
    }

    public ArrayList<ParamValidValueInfo> getValidValueInfos() {
        return validValueInfos;
    }

    public void setValidValueInfos(ArrayList<ParamValidValueInfo> validValueInfos) {
        this.validValueInfos = validValueInfos;
    }

    public void addValidValueInfo(ParamValidValueInfo paramValidValueInfo) {
        this.validValueInfos.add(paramValidValueInfo);
    }

    public void clearValidValueInfos() {
        this.validValueInfos.clear();
    }


    public void sortValidValueInfos() {
        //  Collections.sort(validValueInfos, new ParamValidValueInfo.ValueComparator());
        Collections.sort(validValueInfos);
    }

    @Override
    public int compareTo(Object o) {
        return getName().compareToIgnoreCase(((ParamInfo) o).getName());
    }
}
