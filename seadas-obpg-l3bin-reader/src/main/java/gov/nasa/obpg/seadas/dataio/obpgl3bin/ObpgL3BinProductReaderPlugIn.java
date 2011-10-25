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

package gov.nasa.obpg.seadas.dataio.obpgl3bin;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ObpgL3BinProductReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "NASA-OBPG-L3";
    public static final String FORMAT_DESCRIPTION = "OBPG Level-3 Binned Data Product";
    public static final String FILE_EXTENSION = ".L3B";

    /**
     * Constructs a new MERIS Binned Level-3 product reader plug-in instance.
     */
    public ObpgL3BinProductReaderPlugIn() {
    }


    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if
     * it's content has the MERIS Binned Level-3 format by checking for a correct filename and if it
     * has specific attributes.
     * <p/>
     * MERIS Binned Level-3 product reader accepts as input a <code>java.lang.String</code> - a file path or a
     * <code>java.io.File</code> - an abstract file path.
     * </p>
     *
     * @param input the input object - must be <code>java.lang.String</code> or a <code>java.io.File</code>
     *
     * @return <code>true</code> if the given input is an object referencing a physical MERIS Binned Level-3
     *         data source.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        if (input == null) {
            return DecodeQualification.UNABLE;
        }
        final String path = input.toString();
        final NetcdfFile netcdfFile;
        try {
            netcdfFile = NetcdfFile.open(path);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }
        Attribute titleAttribute = netcdfFile.findGlobalAttribute("Data Bins");

        try {
            if (titleAttribute.toString() != null) {
                return DecodeQualification.INTENDED;
            }
        } finally {
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // ok
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for an NASA OBPG Binned Level-3 product reader.
     * <p/>
     * <p> Instances of the classes returned in this array are valid objects for the <code>readProductNodes</code>
     * method of the <code>AbstractProductReader</code> class (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid input types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductReader#readProductNodes
     */
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual NASA OBPG Binned Level-3 product reader class.
     *
     * @return a new instance of the <code>OBPGL3BinProductReader</code> class
     */
    public ProductReader createReaderInstance() {
        return new ObpgL3BinProductReader(this);
    }

   @Override
    public BeamFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new BeamFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Returns a string array containing the single entry <code>&quot;L3_ENV_MER&quot;</code>.
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return new String[]{FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return FORMAT_DESCRIPTION;
    }

}
