package gov.nasa.obpg.seadas.dataio.obpg;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import ucar.ma2.Array;
import ucar.nc2.Attribute;

/**
 * Created by IntelliJ IDEA.
 * User: seadas
 * Date: 11/14/11
 * Time: 2:23 PM
 * To change this template use File | Settings | File Templates.
 */
public class SeadasMappedFileReader extends SeadasFileReader {


    SeadasMappedFileReader(SeadasProductReader productReader) {
        super(productReader);
    }


    @Override
    public Product createProduct() throws ProductIOException {
        //todo figure out if we even need these...
        addGlobalAttributeSeadasMapped();

        int sceneWidth = getIntAttribute("Scene Pixels");
        int sceneHeight = getIntAttribute("Scene Lines");
        String productName = getStringAttribute("Product Name");

        SeadasProductReader.ProductType productType = productReader.getProductType();

        Product product = new Product(productName, productType.toString(), sceneWidth, sceneHeight);
        product.setDescription(productName);

        product.setFileLocation(productReader.getInputFile());
        product.setProductReader(productReader);

        addGlobalMetadata(product);
        variableMap = addBands(product, ncFile.getVariables());

        addGeocoding(product);
        return product;

    }

    public void addGeocoding(Product product) {
        //todo: figure out how to handle NON-CYLINDRICAL projections...
        //float pixelX = 0.0f;
        //float pixelY = 0.0f;
        // Changed after conversation w/ Sean, Norman F., et al.
        float pixelX = 0.5f;
        float pixelY = 0.5f;

        float easting = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("Easternmost Longitude").getData().getElemDouble();
        float westing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("Westernmost Longitude").getData().getElemDouble();
        float pixelSizeX = (easting - westing) / product.getSceneRasterWidth();
        float northing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("Northernmost Latitude").getData().getElemDouble();
        float southing = (float) product.getMetadataRoot().getElement("Global_Attributes").getAttribute("Southernmost Latitude").getData().getElemDouble();
        float pixelSizeY = (northing - southing) / product.getSceneRasterHeight();

        try {
            product.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84,
                    product.getSceneRasterWidth(),
                    product.getSceneRasterHeight(),
                    westing, northing,
                    pixelSizeX, pixelSizeY,
                    pixelX, pixelY));
        } catch (FactoryException e) {
            throw new IllegalStateException(e);
        } catch (TransformException e) {
            throw new IllegalStateException(e);
        }
    }

    public void addGlobalAttributeSeadasMapped(){
        int [] dims = ncFile.getVariables().get(0).getShape();
        String [] prodname = ncFile.getLocation().split("/");
        String projname = ncFile.getVariables().get(0).findAttribute("Projection Name").getStringValue();
        Array projlimits = ncFile.getVariables().get(0).findAttribute("Limit").getValues();
        double north = projlimits.getDouble(2);
        double south = projlimits.getDouble(0);
        double east = projlimits.getDouble(3);
        double west = projlimits.getDouble(1);

        Attribute rasterWidth = new Attribute("Scene Pixels",dims[1]);
        Attribute rasterHeight = new Attribute("Scene Lines",dims[0]);
        Attribute productName = new Attribute("Product Name",prodname[prodname.length-1]);
        Attribute projection = new Attribute("Projection Name",projname);
        Attribute northing = new Attribute("Northernmost Latitude",north);
        Attribute southing = new Attribute("Southernmost Latitude",south);
        Attribute easting = new Attribute("Easternmost Longitude",east);
        Attribute westing = new Attribute("Westernmost Longitude",west);
        globalAttributes.add(rasterHeight);
        globalAttributes.add(rasterWidth);
        globalAttributes.add(productName);
        globalAttributes.add(projection);
        globalAttributes.add(northing);
        globalAttributes.add(southing);
        globalAttributes.add(easting);
        globalAttributes.add(westing);
    }

}
