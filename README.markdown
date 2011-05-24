SeaDAS
======

Installation Guide
------------------

1. Download and install build tools:
    *   Install J2SE 1.6 and set `JAVA_HOME` accordingly.
    *   Install Maven and set `MAVEN_HOME` accordingly.
    *   Install GIT and set `GIT_HOME` accordingly.
        *   On Windows we recommend the `msysGit` package.
        *   Make sure Git is configured correctly: type `git config -l` at your console; the value `core.autocrlf` has to be set to `input`
        *   If it is not, open `$GIT_HOME/etc/gitconfig` and set `core.autocrlf` to `input`
    *   Create a directory for SeaDAS and set `SEADAS` to this directory.

2.  Add `$JAVA_HOME/bin`, `$MAVEN_HOME/bin` and `$GIT_HOME/bin` to your `PATH`. (Windows:  `%JAVA_HOME%\bin`, `%MAVEN_HOME%\bin` and `%GIT_HOME%\bin`)

3.  Checkout Ceres using `git`:

        git clone https://github.com/bcdev/ceres.git $SEADAS/ceres
        git clone https://github.com/seadas/seadas7.git $SEADAS/beam

4.  Build Ceres from source and install in local Maven repository: 

        cd $SEADAS/ceres
        mvn install

5.  Build BEAM from source and install in local Maven repository:

        cd $SEADAS/beam
        mvn install

6.  Open up the project in your IDE:
    *   Netbeans:
        +   *Menu File* -> *Open Project* and select ceres
        +   Check the *Open Required Projects* box
        +   *Menu File* -> *Open Project* and select beam.  
        +   Check the *Open Required Projects* box
        +   Set the *SeaDAS Application* as the main project
    *   IntelliJ IDEA:
        +   *Main Menu* -> *File* -> *New Project* -> *Import Project from External Model*
        +   Choose Maven
        +   Specify your root directory: `$SEADAS` (Note: put your actual path)
        +   Check the box: *Search for Directories Recursively*
        +   Check the box: *default tools*
        +   Click *Next* 
        +   Click *Finish*
    *   Eclipse:
        +   Build Eclipse project files for BEAM:

                cd $SEADAS/beam
                mvn eclipse:eclipse
        +   Delete the created `.project` file in the main project folder.
        +   Make sure that `M2_REPO` classpath variable is set:
            -   Open *Window* -> *Preferences...* then select *Java* -> *Build Path* -> *Classpath Variables*
            -   Select *New...* and add variable `M2_REPO`
            -   Select *Folder...* and choose the location of your Maven local repository, e.g `~/.m2/repository`. On Windows Vista the default Maven repository is `C:\Users\<Username>\.m2\repository`
        +   Click *Main Menu* -> *File* -> *Import*
        +   Select *General* -> *Existing Project into Workspace*
        +   Select *Root Directory* `$SEADAS/beam`
        +   Click *Finish*
        
7. Use the following configuration to run BEAM/VISAT:
    *   Main class: `com.bc.ceres.launcher.Launcher`
    *   VM parameters: `-Xmx1024M -Dceres.context=seadas`
    *   Program parameters: `none`
    *   Working directory: `$SEADAS/beam` (replace $SEADAS with your actual path)
    *   Use classpath of module (project in Eclipse): `beam-bootstrap`

8. Once you have all the configuration done, hit *Make Project*. Let it rebuild and then *Run*

Original instructions from [Brockmann Consult][bc].
    
  [bc]: http://www.brockmann-consult.de/beam-wiki/display/BEAM/Build+from+Source

