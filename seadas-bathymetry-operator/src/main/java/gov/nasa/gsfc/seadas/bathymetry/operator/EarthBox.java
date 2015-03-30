package gov.nasa.gsfc.seadas.bathymetry.operator;


import org.esa.beam.framework.datamodel.GeoPos;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 10/30/13
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class EarthBox {

    public static final float NULL_COORDINATE = (float) -999;
    public static final int NULL_LENGTH = -999;

    private float minLatLimit = (float) -90;
    private float maxLatLimit = (float) 90;
    private float minLonLimit = (float) -180;
    private float maxLonLimit = (float) 180;

    private float minLat = NULL_COORDINATE;
    private float maxLat = NULL_COORDINATE;
    private float minLon = NULL_COORDINATE;
    private float maxLon = NULL_COORDINATE;

    float deltaLat = NULL_COORDINATE;
    float deltaLon = NULL_COORDINATE;

    private int latDimensionLength = NULL_LENGTH;
    private int lonDimensionLength = NULL_LENGTH;

    private short[][] values;
    private short[][] waterSurfaceValues;

    private short missingValue;

    private boolean getValueAverage = true;

    public EarthBox() {

    }

    public void add(GeoPos geoPos) {
        float lat = geoPos.lat;
        float lon = geoPos.lon;

        add(lat, lon);
    }

    public void add(float lat, float lon) {
        if (lat > getMaxLat() || getMaxLat() == NULL_COORDINATE) {
            setMaxLat(lat);
        }

        if (lat < getMinLat() || getMinLat() == NULL_COORDINATE) {
            setMinLat(lat);
        }

        if (lon > getMaxLon() || getMaxLon() == NULL_COORDINATE) {
            setMaxLon(lon);
        }

        if (lon < getMinLon() || getMinLon() == NULL_COORDINATE) {
            setMinLon(lon);
        }
    }


    private void setDeltaLon() {
        if (getMinLon() != NULL_COORDINATE && getMaxLon() != NULL_COORDINATE && getLonDimensionLength() != NULL_LENGTH) {
            deltaLon = (getMaxLon() - getMinLon()) / (getLonDimensionLength()-1);
        }
    }


    private void setDeltaLat() {
        if (getMinLat() != NULL_COORDINATE && getMaxLat() != NULL_COORDINATE && getLatDimensionLength() != NULL_LENGTH) {
            deltaLat = (getMaxLat() - getMinLat()) / (getLatDimensionLength()-1);
        }
    }


    private void setMinLat(float minLat) {
        if (minLat < minLatLimit) {
            minLat = minLatLimit;
        }
        this.minLat = minLat;
        this.deltaLat = NULL_COORDINATE;
    }


    private void setMaxLat(float maxLat) {
        if (maxLat > maxLatLimit) {
            maxLat = maxLatLimit;
        }
        this.maxLat = maxLat;
        this.deltaLat = NULL_COORDINATE;
    }


    private void setMinLon(float minLon) {
        if (minLon < minLonLimit) {
            minLon = minLonLimit;
        }
        this.minLon = minLon;
        this.deltaLon = NULL_COORDINATE;
    }


    private void setMaxLon(float maxLon) {
        if (maxLon > maxLonLimit) {
            maxLon = maxLonLimit;
        }
        this.maxLon = maxLon;
        this.deltaLon = NULL_COORDINATE;
    }


    private void setLatDimensionLength(int latDimensionLength) {
        this.latDimensionLength = latDimensionLength;
        this.deltaLat = NULL_COORDINATE;
    }

    private void setLonDimensionLength(int lonDimensionLength) {
        this.lonDimensionLength = lonDimensionLength;
        this.deltaLon = NULL_COORDINATE;
    }


    public void setValues(float minLat, float maxLat, float minLon, float maxLon, short[][] values, short[][] waterSurfaceValues, short missingValue) {
        this.values = values;
        this.waterSurfaceValues = waterSurfaceValues;

        setMaxLat(maxLat);
        setMinLat(minLat);
        setMaxLon(maxLon);
        setMinLon(minLon);

        setLatDimensionLength(values.length);
        setLonDimensionLength(values[0].length);

        setDeltaLat();
        setDeltaLon();

        setMissingValue(missingValue);
    }


    public float getMinLat() {
        return minLat;
    }

    public float getMaxLat() {
        return maxLat;
    }

    public float getMinLon() {
        return minLon;
    }

    public float getMaxLon() {
        return maxLon;
    }


    public int getLatDimensionLength() {
        return latDimensionLength;
    }

    public int getLonDimensionLength() {
        return lonDimensionLength;
    }


    public short getValue(GeoPos geoPos) {
        return getValue(geoPos.lat, geoPos.lon);
    }


    public short getWaterSurfaceValue(GeoPos geoPos) {
        return getWaterSurfaceValue(geoPos.lat, geoPos.lon);
    }

    public short getValue(float lat, float lon) {
        int latIndex = getLatIndex(lat);
        int lonIndex = getLonIndex(lon);


        short value;
        if (isGetValueAverage()) {
            float lutLat = getLat(latIndex);
            float lutLon = getLon(lonIndex);

            float lutLatN;
            float lutLatS;
            int lutLatIndexN;
            int lutLatIndexS;


            if (lat > lutLat) {
                if (latIndex < latDimensionLength - 1) {
                    lutLatIndexN = latIndex + 1;
                    lutLatN = getLat(lutLatIndexN);
                } else {
                    lutLatIndexN = latIndex;
                    lutLatN = lutLat;
                }

                lutLatIndexS = latIndex;
                lutLatS = lutLat;
            } else {
                lutLatN = lutLat;
                lutLatIndexN = latIndex;

                if (latIndex > 0) {
                    lutLatIndexS = latIndex - 1;
                    lutLatS = getLat(lutLatIndexS);
                } else {
                    lutLatIndexS = 0;
                    lutLatS = lutLat;
                }
            }


            float lutLonE;
            float lutLonW;
            int lutLonIndexE;
            int lutLonIndexW;

            if (lon > lutLon) {
                if (lonIndex < lonDimensionLength - 1) {
                    lutLonIndexE = lonIndex + 1;
                    lutLonE = getLon(lutLonIndexE);
                } else {
                    lutLonIndexE = lonIndex;
                    lutLonE = lutLon;
                }

                lutLonIndexW = lonIndex;
                lutLonW = lutLon;
            } else {
                lutLonE = lutLon;
                lutLonIndexE = lonIndex;

                if (lonIndex > 0) {
                    lutLonIndexW = lonIndex - 1;
                    lutLonW = getLon(lutLonIndexW);
                } else {
                    lutLonIndexW = 0;
                    lutLonW = lutLon;
                }
            }


            GeoPos geoPosNW = new GeoPos(lutLatN, lutLonW);
            GeoPos geoPosNE = new GeoPos(lutLatN, lutLonE);
            GeoPos geoPosSW = new GeoPos(lutLatS, lutLonW);
            GeoPos geoPosSE = new GeoPos(lutLatS, lutLonE);

            short cornerValueNW = getValue(lutLatIndexN, lutLonIndexW);
            short cornerValueNE = getValue(lutLatIndexN, lutLonIndexE);
            short cornerValueSW = getValue(lutLatIndexS, lutLonIndexW);
            short cornerValueSE = getValue(lutLatIndexS, lutLonIndexE);

            if (cornerValueNW != getMissingValue() && cornerValueNE != getMissingValue() && cornerValueSW != getMissingValue() && cornerValueSE != getMissingValue()) {
                float deltaLutLat = lutLatN - lutLatS;
                short sideValueW;
                short sideValueE;

                if (deltaLutLat > 0) {
                    float weightLutLatS = (lutLatN - lat) / deltaLutLat;
                    float weightLutLatN = 1 - weightLutLatS;

                    sideValueW = (short) (weightLutLatN * cornerValueNW + weightLutLatS * cornerValueSW);
                    sideValueE = (short) (weightLutLatN * cornerValueNE + weightLutLatS * cornerValueSE);
                } else {
                    sideValueW = cornerValueNW;
                    sideValueE = cornerValueNE;
                }


                float deltaLutLon = lutLonE - lutLonW;


                if (deltaLutLon > 0) {
                    float weightSideW = (lutLonE - lon) / deltaLutLon;
                    float weightSideE = 1 - weightSideW;

                    value = (short) (weightSideW * sideValueW + weightSideE * sideValueE);
                } else {
                    value = sideValueE;
                }
            } else {
                value = getValue(latIndex, lonIndex);
            }

        } else {
            value = getValue(latIndex, lonIndex);
        }

        return value;
    }


    public short getWaterSurfaceValue(float lat, float lon) {
        int latIndex = getLatIndex(lat);
        int lonIndex = getLonIndex(lon);


        short waterSurfaceValue;
        if (isGetValueAverage()) {
            float lutLat = getLat(latIndex);
            float lutLon = getLon(lonIndex);

            float lutLatN;
            float lutLatS;
            int lutLatIndexN;
            int lutLatIndexS;


            if (lat > lutLat) {
                if (latIndex < latDimensionLength - 1) {
                    lutLatIndexN = latIndex + 1;
                    lutLatN = getLat(lutLatIndexN);
                } else {
                    lutLatIndexN = latIndex;
                    lutLatN = lutLat;
                }

                lutLatIndexS = latIndex;
                lutLatS = lutLat;
            } else {
                lutLatN = lutLat;
                lutLatIndexN = latIndex;

                if (latIndex > 0) {
                    lutLatIndexS = latIndex - 1;
                    lutLatS = getLat(lutLatIndexS);
                } else {
                    lutLatIndexS = 0;
                    lutLatS = lutLat;
                }
            }


            float lutLonE;
            float lutLonW;
            int lutLonIndexE;
            int lutLonIndexW;

            if (lon > lutLon) {
                if (lonIndex < lonDimensionLength - 1) {
                    lutLonIndexE = lonIndex + 1;
                    lutLonE = getLon(lutLonIndexE);
                } else {
                    lutLonIndexE = lonIndex;
                    lutLonE = lutLon;
                }

                lutLonIndexW = lonIndex;
                lutLonW = lutLon;
            } else {
                lutLonE = lutLon;
                lutLonIndexE = lonIndex;

                if (lonIndex > 0) {
                    lutLonIndexW = lonIndex - 1;
                    lutLonW = getLon(lutLonIndexW);
                } else {
                    lutLonIndexW = 0;
                    lutLonW = lutLon;
                }
            }


            GeoPos geoPosNW = new GeoPos(lutLatN, lutLonW);
            GeoPos geoPosNE = new GeoPos(lutLatN, lutLonE);
            GeoPos geoPosSW = new GeoPos(lutLatS, lutLonW);
            GeoPos geoPosSE = new GeoPos(lutLatS, lutLonE);

            short cornerValueNW = getWaterSurfaceValue(lutLatIndexN, lutLonIndexW);
            short cornerValueNE = getWaterSurfaceValue(lutLatIndexN, lutLonIndexE);
            short cornerValueSW = getWaterSurfaceValue(lutLatIndexS, lutLonIndexW);
            short cornerValueSE = getWaterSurfaceValue(lutLatIndexS, lutLonIndexE);

            if (cornerValueNW != getMissingValue() && cornerValueNE != getMissingValue() && cornerValueSW != getMissingValue() && cornerValueSE != getMissingValue()) {
                float deltaLutLat = lutLatN - lutLatS;
                short sideValueW;
                short sideValueE;

                if (deltaLutLat > 0) {
                    float weightLutLatS = (lutLatN - lat) / deltaLutLat;
                    float weightLutLatN = 1 - weightLutLatS;

                    sideValueW = (short) (weightLutLatN * cornerValueNW + weightLutLatS * cornerValueSW);
                    sideValueE = (short) (weightLutLatN * cornerValueNE + weightLutLatS * cornerValueSE);
                } else {
                    sideValueW = cornerValueNW;
                    sideValueE = cornerValueNE;
                }


                float deltaLutLon = lutLonE - lutLonW;


                if (deltaLutLon > 0) {
                    float weightSideW = (lutLonE - lon) / deltaLutLon;
                    float weightSideE = 1 - weightSideW;

                    waterSurfaceValue = (short) (weightSideW * sideValueW + weightSideE * sideValueE);
                } else {
                    waterSurfaceValue = sideValueE;
                }
            } else {
                waterSurfaceValue = getWaterSurfaceValue(latIndex, lonIndex);
            }

        } else {
            waterSurfaceValue = getWaterSurfaceValue(latIndex, lonIndex);
        }

        return waterSurfaceValue;
    }



    public short getValue(int latIndex, int lonIndex) {
        return values[latIndex][lonIndex];
    }

    public short getWaterSurfaceValue(int latIndex, int lonIndex) {
        return waterSurfaceValues[latIndex][lonIndex];
    }

    public float getLon(int lonIndex) {

        if (lonIndex > getLonDimensionLength() - 1) {
            lonIndex = getLonDimensionLength() - 1;
        }

        if (lonIndex < 0) {
            lonIndex = 0;
        }

        return getMinLon() + lonIndex * getDeltaLon();
    }

    public float getLat(int latIndex) {

        if (latIndex > getLatDimensionLength() - 1) {
            latIndex = getLatDimensionLength() - 1;
        }

        if (latIndex < 0) {
            latIndex = 0;
        }

        return getMinLat() + latIndex * getDeltaLat();
    }


    private float getDeltaLat() {
        if (deltaLat == NULL_COORDINATE) {
            setDeltaLat();
        }

        return deltaLat;
    }

    private float getDeltaLon() {
        if (deltaLon == NULL_COORDINATE) {
            setDeltaLon();
        }

        return deltaLon;
    }

    private int getLatIndex(float lat) {
        int latIndex = (int) ((lat - getMinLat()) / getDeltaLat());

        if (latIndex > getLatDimensionLength() - 1) {
            latIndex = getLatDimensionLength() - 1;
        }

        if (latIndex < 0) {
            latIndex = 0;
        }
        return latIndex;
    }

    private int getLonIndex(float lon) {
        int lonIndex = (int) ((lon - getMinLon()) / getDeltaLon());
        if (lonIndex > getLonDimensionLength() - 1) {
            lonIndex = getLonDimensionLength() - 1;
        }

        if (lonIndex < 0) {
            lonIndex = 0;
        }
        return lonIndex;
    }


    public boolean isGetValueAverage() {
        return getValueAverage;
    }

    public void setGetValueAverage(boolean getValueAverage) {
        this.getValueAverage = getValueAverage;
    }

    public short getMissingValue() {
        return missingValue;
    }

    public void setMissingValue(short missingValue) {
        this.missingValue = missingValue;
    }
}
