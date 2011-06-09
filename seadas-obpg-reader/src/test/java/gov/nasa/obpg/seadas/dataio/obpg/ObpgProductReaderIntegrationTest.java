package gov.nasa.obpg.seadas.dataio.obpg;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * A test that will test the OBPG product reader against a number (all?) of
 * possible OPBG file formats that it shall support.
 * <p/>
 * Note: This tests runs with the {@link SeadasOcsswTestDirRunner}.
 *
 * @author Norman Fomferra
 * @since SeaDAS 7.0
 */
@RunWith(SeadasOcsswTestDirRunner.class)
public class ObpgProductReaderIntegrationTest {

    @Test
    public void testThatPluginIdentifiesAllProductTypes() throws Exception {
        List<File> fileList = SeadasOcsswTestDirRunner.getFileList();
        ObpgProductReaderPlugIn plugIn = new ObpgProductReaderPlugIn();
        for (File file : fileList) {
            System.out.println("ObpgProductReaderIntegrationTest: Now processing test file: " + file);
            assertEquals("Failed to identify file " + file.getName() + ":",
                         DecodeQualification.INTENDED,
                         plugIn.getDecodeQualification(file));
        }
    }
/*
    @Test
    public void testThatReaderCanReadAllProductTypes() throws Exception {
        List<File> fileList = SeadasOcsswTestDirRunner.getFileList();
        ObpgProductReaderPlugIn plugIn = new ObpgProductReaderPlugIn();
        for (File file : fileList) {
            ProductReader reader = plugIn.createReaderInstance();
            try {
                Product product = reader.readProductNodes(file, null);
                assertNotNull(product);
                assertNotNull(product.getName());
            } finally {
                reader.close();
            }
        }
    }
    */
}
