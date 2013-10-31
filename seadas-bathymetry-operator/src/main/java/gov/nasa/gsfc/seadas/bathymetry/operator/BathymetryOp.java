/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package gov.nasa.gsfc.seadas.bathymetry.operator;

import com.bc.ceres.core.ProgressMonitor;
import gov.nasa.gsfc.seadas.bathymetry.ui.BathymetryData;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import ucar.ma2.Array;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * The bathymetry operator is a GPF-Operator. It takes the geographic bounds of the input product and creates a new
 * product with the same bounds. The output product contains a single band, which is a land/water fraction mask.
 * For each pixel, it contains the fraction of water; a value of 0.0 indicates land, a value of 100.0 indicates water,
 * and every value in between indicates a mixed pixel.
 * <br/>
 * Since the base data may exhibit a higher resolution than the input product, a subsampling &ge;1 may be specified;
 * therefore, mixed pixels may occur.
 *
 * @author Danny Knowles
 */
@SuppressWarnings({"FieldCanBeLocal"})
@OperatorMetadata(alias = "bathymetry",
        version = "1.0",
        internal = false,
        authors = "Danny Knowles",
        copyright = "",
        description = "Operator creating a bathymetry band and bathymetry mask")
public class BathymetryOp extends Operator {

    public static final String BATHYMETRY_BAND_NAME = "bathymetry";

    @SourceProduct(alias = "source", description = "The Product the land/water-mask shall be computed for.",
            label = "Name")
    private Product sourceProduct;

    @Parameter(description = "Specifies on which resolution the water mask shall be based.", unit = "m/pixel",
            label = "Resolution", defaultValue = "1855", valueSet = {"1855"})
    private int resolution;

    @Parameter(description = "Bathymetry filename",
            label = "Filename", defaultValue = "ETOPO1_ocssw.nc",
            valueSet = {"ETOPO1_ocssw.nc"})
    private String filename;

    @Parameter(description = "Specifies the factor between the resolution of the source product and the bathymetry in " +
            "x direction. A value of '1' means no subsampling at all.",
            label = "Subsampling factor x", defaultValue = "3", notNull = true)
    private int subSamplingFactorX;

    @Parameter(description = "Specifies the factor between the resolution of the source product and the bathymetry in" +
            "y direction. A value of '1' means no subsampling at all.",
            label = "Subsampling factor y", defaultValue = "3", notNull = true)
    private int subSamplingFactorY;


    @TargetProduct
    private Product targetProduct;
//    private BathymetryMaskClassifier classifier;

    private BathymetryReader bathymetryReader;






    @Override
    public void initialize() throws OperatorException {

        File bathymetryFile = BathymetryData.getBathymetryFile(filename);

        try {
            bathymetryReader = new BathymetryReader(bathymetryFile);
        } catch (IOException e) {
            //
        }

        validateParameter();
        validateSourceProduct();


//        try {
//            classifier = new BathymetryMaskClassifier(resolution, filename);
//        } catch (IOException e) {
//            throw new OperatorException("Error creating class BathymetryMaskClassifier.", e);
//        }
        //todo this is order dependent, do not have to hardcode missing value if this is here
        initTargetProduct();

    }


    private void validateParameter() {
        if (resolution != BathymetryData.RESOLUTION_BATHYMETRY_FILE) {
            throw new OperatorException(String.format("Resolution needs to be %d ",
                    BathymetryData.RESOLUTION_BATHYMETRY_FILE));
        }
        if (subSamplingFactorX < 1) {
            String message = MessageFormat.format(
                    "Subsampling factor needs to be greater than or equal to 1; was: ''{0}''.", subSamplingFactorX);
            throw new OperatorException(message);
        }
    }

    private void validateSourceProduct() {
        final GeoCoding geoCoding = sourceProduct.getGeoCoding();
        if (geoCoding == null) {
            throw new OperatorException("The source product must be geo-coded.");
        }
        if (!geoCoding.canGetGeoPos()) {
            throw new OperatorException("The geo-coding of the source product can not be used.\n" +
                    "It does not provide the geo-position for a pixel position.");
        }
    }

    private void initTargetProduct() {
        targetProduct = new Product("Bathymetry Mask", ProductData.TYPESTRING_UINT8, sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());
        final Band waterBand = targetProduct.addBand(BATHYMETRY_BAND_NAME, ProductData.TYPE_FLOAT32);
        // todo Danny is fixing this, commented out because order is different, haven't used reader yet
        waterBand.setNoDataValue(bathymetryReader.getMissingValue());


        waterBand.setNoDataValueUsed(true);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(BathymetryOp.class);
        }
    }


    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        final String targetBandName = targetBand.getName();

        final Rectangle rectangle = targetTile.getRectangle();

        // not sure if this is really needed but just in case
        if (!targetBandName.equals(BATHYMETRY_BAND_NAME)) {
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    int dataValue = 0;
                    targetTile.setSample(x, y, dataValue);
                }
            }

            return;
        }


        try {
            final GeoCoding geoCoding = sourceProduct.getGeoCoding();
            final PixelPos pixelPos = new PixelPos();
            final GeoPos geoPos = new GeoPos();

            // establish 4 locations on the earth to minimize any effects of dateline and pole crossing
            EarthBox earthBoxNW = new EarthBox();
            EarthBox earthBoxNE = new EarthBox();
            EarthBox earthBoxSW = new EarthBox();
            EarthBox earthBoxSE = new EarthBox();

            // loop through perimeter of tile, adding each pixel geolocation to it's appropriate earthBox
            // at this point the earthBoxes will be adjusted their mins and maxes of the lats and lons.
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    pixelPos.setLocation(x, y);

                    geoCoding.getGeoPos(pixelPos, geoPos);

                    if (geoPos.lat >= 0) {
                        if (geoPos.lon >= 0) {
                            earthBoxNE.add(geoPos);
                        } else {
                            earthBoxNW.add(geoPos);
                        }
                    } else {
                        if (geoPos.lon >= 0) {
                            earthBoxSE.add(geoPos);
                        } else {
                            earthBoxSW.add(geoPos);
                        }
                    }
                }
            }


            // for all applicable earthBoxes add in the dimensions and bathymetry height array which is obtained at
            // this point from the source in a single chunk call.
            EarthBox earthBoxes[] = {earthBoxNE, earthBoxNW, earthBoxSE, earthBoxSW};

            for (EarthBox earthBox : earthBoxes) {
                if (earthBox.getMaxLat() != EarthBox.NULL_COORDINATE) {
                    // add dimensions to the earthBox
                    int minLatIndex = bathymetryReader.getLatIndex(earthBox.getMinLat());
                    int maxLatIndex = bathymetryReader.getLatIndex(earthBox.getMaxLat());

                    int minLonIndex = bathymetryReader.getLonIndex(earthBox.getMinLon());
                    int maxLonIndex = bathymetryReader.getLonIndex(earthBox.getMaxLon());

                    // determine length of each dimension for the chunk array to be pulled out of the netcdf source
                    int latDimensionLength = maxLatIndex - minLatIndex + 1;
                    int lonDimensionLength = maxLonIndex - minLonIndex + 1;

                    // get the bathymetry height array from the netcdf source
                    int[] origin = new int[]{minLatIndex, minLonIndex};
                    int[] shape = new int[]{latDimensionLength, lonDimensionLength};

                    // retrieve the bathymetry height array from netcdf
                    Array heightArray = bathymetryReader.getHeightArray(origin, shape);

                    // convert heightArray from ucar.ma2.Array format to regular java array
                    short heights[][] = (short[][]) heightArray.copyToNDJavaArray();

                    // add the value array to the earthBox
                    earthBox.setValues(heights);
                }
            }


            // loop through all the tile pixels, geolocate them, and get their bathymetry height from the
            // appropriate earthBox.
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    pixelPos.setLocation(x, y);
                    geoCoding.getGeoPos(pixelPos, geoPos);

                    final short bathymetryValue;
                    if (geoPos.isValid()) {
                        if (geoPos.lat >= 0) {
                            if (geoPos.lon >= 0) {
                                bathymetryValue = earthBoxNE.getValue(geoPos);
                            } else {
                                bathymetryValue = earthBoxNW.getValue(geoPos);
                            }
                        } else {
                            if (geoPos.lon >= 0) {
                                bathymetryValue = earthBoxSE.getValue(geoPos);
                            } else {
                                bathymetryValue = earthBoxSW.getValue(geoPos);
                            }
                        }
                    } else {
                        bathymetryValue = bathymetryReader.getMissingValue();
                    }

                    targetTile.setSample(x, y, bathymetryValue);
                }
            }

        } catch (Exception e) {
            throw new OperatorException("Error computing tile '" + targetTile.getRectangle().toString() + "'.", e);
        }
    }





}
