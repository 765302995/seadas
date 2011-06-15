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
package gov.nasa.obpg.seadas.dataio.obpg;

import gov.nasa.obpg.seadas.dataio.obpg.ObpgUtils;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.Product;

import ucar.nc2.Attribute;

import java.util.ArrayList;

import junit.framework.TestCase;

public class ObpgUtilsCreateProductBodyTest extends TestCase {

    private ObpgUtils obpgUtils;

    @Override
    protected void setUp() throws Exception {
        obpgUtils = new ObpgUtils();
    }

    public void testAcceptableL2Product() throws ProductIOException {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-2 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-2 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));
        
        final Product product = obpgUtils.createProductBody(globalAttributes);

        assertNotNull(product);
        assertEquals("Level-2 ProductName", product.getName());
        assertEquals("OBPG Level-2 ProductType", product.getProductType());
        assertEquals(2030, product.getSceneRasterWidth());
        assertEquals(1354, product.getSceneRasterHeight());
    }

    public void testL2WithoutHeightAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-2 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-2 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        // Since this is a test without the height Attribute, don't put one in here.

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_HEIGHT));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL2WithoutNameAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-2 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_NAME));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL2WithoutTypeAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-2 ProductName"));
        //globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("Should not get here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_TYPE));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL2WithoutWidthAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-2 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-2 ProductType"));
        // Since this is a test without the width Attribute, don't put one in here.
        globalAttributes.add(new Attribute(ObpgUtils.KEY_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_WIDTH));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testAcceptableL3SmiProduct() throws ProductIOException {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-3 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-3 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_HEIGHT, 1354));

        final Product product = obpgUtils.createProductBody(globalAttributes);

        assertNotNull(product);
        assertEquals("Level-3 ProductName", product.getName());
        assertEquals("OBPG Level-3 ProductType", product.getProductType());
        assertEquals(2030, product.getSceneRasterWidth());
        assertEquals(1354, product.getSceneRasterHeight());
    }

    public void testL3SmiWithoutHeightAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-3 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-3 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_WIDTH, 2030));
        // Since this is a test without the height Attribute, don't put one in here.

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_L3SMI_HEIGHT));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL3SmiWithoutNameAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-3 ProductType"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_NAME));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL3SmiWithoutTypeAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-3 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_WIDTH, 2030));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("Should not get here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_TYPE));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

    public void testL3SmiWithoutWidthAttribute() {
        final ArrayList<Attribute> globalAttributes = new ArrayList<Attribute>();
        globalAttributes.add(new Attribute(ObpgUtils.KEY_NAME, "Level-3 ProductName"));
        globalAttributes.add(new Attribute(ObpgUtils.KEY_TYPE, "Level-3 ProductType"));
        // Since this is a test without the width Attribute, don't put one in here.
        globalAttributes.add(new Attribute(ObpgUtils.KEY_L3SMI_HEIGHT, 1354));

        try {
            obpgUtils.createProductBody(globalAttributes);
            fail("should not come here");
        } catch (ProductIOException e) {
            assertTrue(e.getMessage().contains(ObpgUtils.KEY_L3SMI_WIDTH));
            assertTrue(e.getMessage().contains("is missing"));
        }
    }

}
