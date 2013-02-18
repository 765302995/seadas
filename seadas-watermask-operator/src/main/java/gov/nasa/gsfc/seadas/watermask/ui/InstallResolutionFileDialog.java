package gov.nasa.gsfc.seadas.watermask.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: knowles
 * Date: 1/16/13
 * Time: 1:01 PM
 * To change this template use File | Settings | File Templates.
 */


class InstallResolutionFileDialog extends JDialog {

    public static enum Step {
        INSTALLATION,
        CONFIRMATION
    }

    public SourceFileInfo sourceFileInfo;
    private JLabel jLabel;
    private LandMasksData landMasksData;


    public InstallResolutionFileDialog(LandMasksData landMasksData, SourceFileInfo sourceFileInfo, Step step) {
        this.landMasksData = landMasksData;
        this.sourceFileInfo = sourceFileInfo;


        if (step == Step.INSTALLATION) {
            installationUI();
        } else if (step == Step.CONFIRMATION) {
            confirmationUI();
        }
    }

//    private static class InstallationThread
//            implements   Runnable {
//
//        public void run() {
//
//
//            ResourceInstallationUtils.installAuxdata(sourceUrl, filename);
//
//        }
//    }


    public final void installationUI() {
        JButton installButton = new JButton("Install File");
        installButton.setPreferredSize(installButton.getPreferredSize());
        installButton.setMinimumSize(installButton.getPreferredSize());
        installButton.setMaximumSize(installButton.getPreferredSize());


        installButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {


                dispose();

                //  acquire in example: "http://oceandata.sci.gsfc.nasa.gov/SeaDAS/installer/landmask/50m.zip"
                try {
                    landMasksData.fireEvent(LandMasksData.CONFIRMED_REQUEST_TO_INSTALL_FILE_EVENT);

                    final String filename = sourceFileInfo.getFile().getName().toString();
                    final URL sourceUrl = new URL(LandMasksData.LANDMASK_URL + "/" + filename);

                    Thread t = new Thread(new FileInstallRunnable(sourceUrl, filename, landMasksData));
                    t.start();

//                    File targetDir = ResourceInstallationUtils.getTargetDir();
//                    ProcessBuilder pb = new ProcessBuilder("wget.py", sourceUrl.toString(), targetDir.getAbsolutePath());
//                    pb.start();



                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        landMasksData.addPropertyChangeListener(LandMasksData.FILE_INSTALLED_EVENT, new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                InstallResolutionFileDialog dialog = new InstallResolutionFileDialog(landMasksData, sourceFileInfo, Step.CONFIRMATION);
                dialog.setVisible(true);
                dialog.setEnabled(true);

                if (sourceFileInfo.isEnabled()) {
                    jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " has been installed");
                    landMasksData.fireEvent(LandMasksData.FILE_INSTALLED_EVENT2);
                } else {
                    jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " installation failure");
                }

                landMasksData.removePropertyChangeListener(LandMasksData.FILE_INSTALLED_EVENT, this);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setPreferredSize(cancelButton.getPreferredSize());
        cancelButton.setMinimumSize(cancelButton.getPreferredSize());
        cancelButton.setMaximumSize(cancelButton.getPreferredSize());

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        JLabel filler = new JLabel("                            ");


        JPanel buttonsJPanel = new JPanel(new GridBagLayout());
        buttonsJPanel.add(cancelButton,
                new ExGridBagConstraints(0, 0, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE));
        buttonsJPanel.add(filler,
                new ExGridBagConstraints(1, 0, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL));
        buttonsJPanel.add(installButton,
                new ExGridBagConstraints(2, 0, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));
//        buttonsJPanel.add(helpButton,
//                new ExGridBagConstraints(3, 0, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE));


        jLabel = new JLabel("Do you want to install file " + sourceFileInfo.getFile().getName().toString() + " ?");

        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.add(jLabel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        jPanel.add(buttonsJPanel,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));


        add(jPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);


        setTitle("File Installation");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();


        setPreferredSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setSize(getPreferredSize());

    }


//    private class FileInstallRunnable
//            implements Runnable {
//        URL sourceUrl;
//        String filename;
//        LandMasksData landMasksData;
//
//        public FileInstallRunnable(URL sourceUrl, String filename, LandMasksData landMasksData) {
//            this.sourceUrl = sourceUrl;
//            this.filename = filename;
//            this.landMasksData = landMasksData;
//        }
//
//        public void run() {
//            try {
//                ResourceInstallationUtils.installAuxdata(sourceUrl, filename);
//                landMasksData.fireEvent(LandMasksData.FILE_INSTALLED_EVENT);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }


    public final void confirmationUI() {
        JButton okayButton = new JButton("Okay");
        okayButton.setPreferredSize(okayButton.getPreferredSize());
        okayButton.setMinimumSize(okayButton.getPreferredSize());
        okayButton.setMaximumSize(okayButton.getPreferredSize());


        okayButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();


            }
        });


        if (sourceFileInfo.isEnabled()) {
            jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " has been installed");
        } else {
            jLabel = new JLabel("File " + sourceFileInfo.getFile().getName().toString() + " installation failure");
        }


        JPanel jPanel = new JPanel(new GridBagLayout());
        jPanel.add(jLabel,
                new ExGridBagConstraints(0, 0, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));
        jPanel.add(okayButton,
                new ExGridBagConstraints(0, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE));


        add(jPanel);

        setModalityType(ModalityType.APPLICATION_MODAL);


        setTitle("File Installation");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();


        setPreferredSize(getPreferredSize());
        setMinimumSize(getPreferredSize());
        setMaximumSize(getPreferredSize());
        setSize(getPreferredSize());

    }
}

