/**
 * NoiseModelling is an open-source tool designed to produce environmental noise maps on very large urban areas. It can be used as a Java library or be controlled through a user friendly web interface.
 *
 * This version is developed by the DECIDE team from the Lab-STICC (CNRS) and by the Mixt Research Unit in Environmental Acoustics (Université Gustave Eiffel).
 * <http://noise-planet.org/noisemodelling.html>
 *
 * NoiseModelling is distributed under GPL 3 license. You can read a copy of this License in the file LICENCE provided with this software.
 *
 * Contact: contact@noise-planet.org
 *
 */

/**
 * @Author Pierre Aumond, Université Gustave Eiffel
 */

package org.noise_planet.noisemodelling.wps.NoiseModelling

import crosby.binary.osmosis.OsmosisReader
import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.JDBCUtilities
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.SpatialResultSet
import org.h2gis.utilities.TableLocation
import org.h2gis.utilities.wrapper.ConnectionWrapper
import org.locationtech.jts.geom.Geometry

import org.noise_planet.noisemodelling.emission.*
import org.noise_planet.noisemodelling.pathfinder.*
import org.noise_planet.noisemodelling.propagation.*
import org.noise_planet.noisemodelling.jdbc.*
import org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import groovy.json.JsonSlurper

title = 'Compute voice emission noise map from pedestrians table.'
description = '&#10145;&#65039; -----------details). </br>' +
              '<hr>' +
              '&#x2705; The output table is called: <b>LW_PEDESTRIAN </b> '

inputs = [
        tablePedestrian: [
                name       : 'Pedestrians table name',
                title      : 'Pedestrians table name',
                description: "<b>Name of the Pedestrians table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'PedestrianLocalisation'. </b>.",
                type       : String.class
        ],
        populationDistribution: [
                name       : 'Population distribution',
                title      : 'Population distribution',
                description: "<b>This parameter allows the user to populate the area in terms of 3 different types of voice:</b>  </br>  " +
                        "<br>  Male, Female, Children </br><ul>" +
                        "<li><b> This is an optional input parameter</li>" +
                        "<li><b> This variable takes the percentage (%) of male, female and children in the study area</li>" +
                        "<li><b> The percentages should be separated by a coma (H,F,C)</li>",
                type       : Double.class,
                min        : 0, max: 1
        ]
]

outputs = [
        result: [
                name       : 'Result output string',
                title      : 'Result output string',
                description: 'This type of result does not allow the blocks to be linked together.',
                type       : String.class
        ]
]
// Open Connection to Geoserver
static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

// run the script
def run(input) {

    // Get name of the database
    // by default an embedded h2gis database is created
    // Advanced user can replace this database for a postGis or h2Gis server database.
    String dbName = "h2gisdb"

    // Open connection
    openGeoserverDataStoreConnection(dbName).withCloseable {
        Connection connection ->
            return [result: exec(connection, input)]
    }
}

// main function of the script
def exec(Connection connection, input) {

    //Need to change the ConnectionWrapper to WpsConnectionWrapper to work under postGIS database
    connection = new ConnectionWrapper(connection)

    // output string, the information given back to the user
    String resultString = null

    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Pedestrian Emission')
    logger.info("inputs {}", input) // log inputs of the run


    // -------------------
    // Get every inputs
    // -------------------

    // Read the database info table. This table contains the information of the audio database
    def BDD_Info = new JsonSlurper().parse(new File('C:/Users/siliezar-montoya/Documents/GitHub/NoiseModellingPieton/wps_scripts/src/test/resources/org/noise_planet/noisemodelling/wps/BDD_Info.json'))

    // Read the spectrum table. This table contains the spectrum of the voice database
    def spectrumDB = new JsonSlurper().parse(new File( 'C:/Users/siliezar-montoya/Documents/GitHub/NoiseModellingPieton/wps_scripts/src/test/resources/org/noise_planet/noisemodelling/wps/Spectrums_500ms2.json'))

    String sources_table_name = input['tablePedestrian']
    // do it case-insensitive
    sources_table_name = sources_table_name.toUpperCase()
    // Check if srid are in metric projection.
    int sridSources = GeometryTableUtilities.getSRID(connection, TableLocation.parse(sources_table_name))
    if (sridSources == 3785 || sridSources == 4326) throw new IllegalArgumentException("Error : Please use a metric projection for "+sources_table_name+".")
    if (sridSources == 0) throw new IllegalArgumentException("Error : The table "+sources_table_name+" does not have an associated SRID.")

    //Get the geometry field of the source table
    TableLocation sourceTableIdentifier = TableLocation.parse(sources_table_name)
    List<String> geomFields = GeometryTableUtilities.getGeometryColumnNames(connection, sourceTableIdentifier)
    if (geomFields.isEmpty()) {
        throw new SQLException(String.format("The table %s does not exists or does not contain a geometry field", sourceTableIdentifier))
    }

    //Get the primary key field of the source table
    int pkIndex = JDBCUtilities.getIntegerPrimaryKey(connection, TableLocation.parse( sources_table_name))
    if (pkIndex < 1) {
        throw new IllegalArgumentException(String.format("Source table %s does not contain a primary key", sourceTableIdentifier))
    }


    // -------------------
    // Init table LW_ROADS
    // -------------------

    // Create a sql connection to interact with the database in SQL
    Sql sql = new Sql(connection)

    // drop table LW_PEDESTRIAN if exists and then create and prepare the table
    sql.execute("drop table if exists LW_PEDESTRIAN;")
    sql.execute("create table LW_PEDESTRIAN (pk integer, the_geom Geometry, " +
            "LWD63 double precision, LWD125 double precision, LWD250 double precision, LWD500 double precision, LWD1000 double precision, LWD2000 double precision, LWD4000 double precision, LWD8000 double precision," +
            "TIMESTEP VARCHAR);")

    def qry = 'INSERT INTO LW_PEDESTRIAN(pk,the_geom, ' +
            'LWD63, LWD125, LWD250, LWD500, LWD1000,LWD2000, LWD4000, LWD8000,' +
            'TIMESTEP) ' +
            'VALUES (?,?,?,?,?,?,?,?,?,?,?);'


    // --------------------------------------
    // Start calculation and fill the table
    // --------------------------------------


    // We create a new table BDD_INFO in SQL that we are going to populate next
    sql.execute("DROP TABLE IF EXISTS BDD_INFO;")
    sql.execute("CREATE TABLE BDD_INFO (ID integer, Nb_Pers integer);")

    // We convert the Spectrum object into SQL in order to perform basic operations (T4)
    BDD_Info.each { bdd ->
        sql.execute("""
        INSERT INTO BDD_INFO (ID, Nb_Pers)
        VALUES (?, ?)
    """, [bdd.ID, bdd.Nb_Pers])
    }

    def dataType = BDD_Info.getClass().getName()
    println("data type: $dataType")

    // We create a new table Spectrum in SQL that we are going to populate next
    sql.execute("DROP TABLE IF EXISTS SPECTRUM;")
    sql.execute("CREATE TABLE SPECTRUM (ID_g integer, ID_File integer, LWD63 double, LWD125 double, LWD250 double, LWD500 double, LWD1000 double, LWD2000 double, LWD4000 double, LWD8000 double," +
            "Alpha double, T integer);")

    // We convert the Spectrum object into SQL in order to perform basic operations (T5)
    spectrumDB.each { spect ->
        sql.execute("""
        INSERT INTO SPECTRUM (ID_g, ID_File, LWD63, LWD125, LWD250, LWD500, LWD1000, LWD2000, LWD4000, LWD8000, Alpha, T)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, [spect.ID_g, spect.ID_File, spect.LWD63, spect.LWD125, spect.LWD250, spect.LWD500, spect.LWD1000, spect.LWD2000, spect.LWD4000, spect.LWD8000, spect.Alpha, spect.T])
    }

    // We load the PEDESTRIANS table into a list/object
    def query = 'SELECT * from PEDESTRIANS'
    def pedestriansTable = sql.rows(query)

    def resultTable = []

    // Join between BDD_INFO and PEDESTRIANS in order to obtain T4. This table contains a PK, the_geom, ID (subject id in BDD_INFO) and NBPEDESTRIAN
    // I'M NOT CURRENTLY USING THIS ONE BUT I LEAVE IT JUST IN CASE
    sql.execute("CREATE TABLE T4 AS SELECT PEDESTRIANS.PK, PEDESTRIANS.the_geom, PEDESTRIANS.NBPEDESTRIAN, BDD_INFO.ID " +
            "FROM PEDESTRIANS " +
            "INNER JOIN BDD_INFO ON PEDESTRIANS.NBPEDESTRIAN = BDD_INFO.Nb_Pers;")

    // Attribution TEST

    // I've decided to perform this step (Obtention of T4) in SQL for simplicity

    // We add a new column AudioFileID to our PEDESTRIANS table. This column stores the id of the audio file corresponding to the number of pedestrians calculated in PEDESTRIANS and BDD_INFO
    sql.execute("ALTER TABLE PEDESTRIANS ADD COLUMN AudioFileID INT;")
    // We store the unique audio file identifiers from BDD_INFO. The point is that, even though we have many audio files for a single number of pedestrians, the assignment will only take one of them randomly
    sql.execute("SELECT DISTINCT Nb_Pers FROM BDD_INFO;")
    // This is the equivalent of the AudioChooser function. We assign an audio file identifier to our PEDESTRIANS table where it corresponds
    sql.execute("UPDATE PEDESTRIANS AS spc SET AudioFileID = (SELECT ID FROM BDD_INFO AS bi WHERE bi.Nb_Pers = spc.NBPEDESTRIAN ORDER BY RAND() LIMIT 1) WHERE spc.NBPEDESTRIAN IN (SELECT DISTINCT Nb_Pers FROM BDD_INFO);")
    // Since not all of the points in PEDESTRIANS correspond to a particular number of pedestrians in BDD_INFO, we'll take them out and only work with those who correspond
    sql.execute("DELETE FROM PEDESTRIANS WHERE AudioFileID IS NULL;")

    // Now we assign the spectrum values to PEDESTRIANS in function of the AudioFileID
    sql.execute("CREATE TABLE T5 AS SELECT * FROM PEDESTRIANS INNER JOIN SPECTRUM ON PEDESTRIANS.AudioFileID = SPECTRUM.ID_File")
    // Add PK
    String queryPK = '''
                    ALTER TABLE T5 DROP COLUMN PK;
                    ALTER TABLE T5 ADD PK INT AUTO_INCREMENT PRIMARY KEY;
                    '''
    sql.execute(queryPK)

    // Get Class to compute LW
    LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW)
    ldenConfig.setCoefficientVersion(2)
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.DAY, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.EVENING, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.NIGHT, new PropagationProcessPathData(false));

    LDENPropagationProcessData ldenData = new LDENPropagationProcessData(null, ldenConfig)

    // At this step, the LW_PEDESTRIAN table is filled using the class computation and the number of pedestrian in each cell
    // We can actually skip this since the T5 would be the correct/equivalent table to use here. I'm not going to touch this for the moment. Discuss with Pierre about optimizing implementation

    // Get size of the table (number of pedestrians points)
    PreparedStatement st = connection.prepareStatement("SELECT COUNT(*) AS total FROM " + sources_table_name)
    ResultSet rs1 = st.executeQuery().unwrap(ResultSet.class)
    int nbPedestrianPoints = 0
    while (rs1.next()) {
        nbRoads = rs1.getInt("total")
        logger.info('The table Pedestrian has ' + nbPedestrianPoints + ' pedestrian positions.')
    }

    int k = 0
    sql.withBatch(100, qry) { ps ->
        st = connection.prepareStatement("SELECT * FROM " + sources_table_name)
        SpatialResultSet rs = st.executeQuery().unwrap(SpatialResultSet.class)

        while (rs.next()) {
            k++
            //logger.info(rs)
            Geometry geo = rs.getGeometry()
            int nbPedestrianOnPoint = rs.getDouble("NBPEDESTRIAN")
            // Compute emission sound level for each point source
            def results = ldenData.computeLw(rs)
            // fill the LW_PEDESTRIAN table
            ps.addBatch(rs.getLong(pkIndex) as Integer, geo as Geometry,
                    70 + 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70 + 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, "DAY")
        }
    }

    // Add Z dimension to the pedestrian points
    sql.execute("UPDATE T5 SET THE_GEOM = ST_UPDATEZ(The_geom,1.5);")

    // Add primary key to the pedestrian table
    sql.execute("ALTER TABLE LW_PEDESTRIAN ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE LW_PEDESTRIAN ADD PRIMARY KEY (PK);  ")

    resultString = "Calculation Done ! The table LW_PEDESTRIAN has been created."

    // print to command window
    logger.info('\nResult : ' + resultString)
    logger.info('End : LW_PEDESTRIAN from Emission')

    // print to WPS Builder
    return resultString

}


    // AudioChooser function

    /**
     * AudioChooser
     * @param ped_number
     * @param ped_available
     * @param BDD_info
     * @return
     */

def audioChooser = { ped_number, ped_available, BDD_info ->
    if (ped_number in ped_available) {
        // Filter and select a random ID if a match is found
        def matchingData = BDD_info.findAll { it.NbPers == ped_number }
        def randomRow = matchingData.sample()
        return randomRow?.ID ?: null
    } else {
        return null
    }
}

/* THIS IS UNUSED CODE. MAYBE WE WILL USE IT LATER
def nb_dispo = BDD_Info.Nb_Pers.unique()

// Define the audioChooser function that we will be using to assign the audio file ID from BDD_Info to PEDESTRIANS as a function of the number of pedestrians
def audioChooser = { sourceNbPers, nbPersDisponibles, bddInfo ->
    if (sourceNbPers in nbPersDisponibles) {
        def matchingRows = bddInfo.findAll { row ->
            row.Nb_Pers == sourceNbPers
        }

        if (matchingRows) {
            // Randomly select one row from matchingRows
            def randomRow = matchingRows[(int)(Math.random() * matchingRows.size())]
            return randomRow.PK
        } else {
            return null
        }
    } else {
        return null
    }
}


// Create a list to store the results
def audioFileIDs = pedestriansTable.NBPEDESTRIAN.collect { sourceNbPers ->
    audioChooser(sourceNbPers, nb_dispo, BDD_Info)
}

// Add the audioFileIDs list as a new column to PEDESTRIANS
// Create a new map with the additional column
def newPedestriansTable = pedestriansTable.collect { row ->
    row + [AudioFileID: audioFileIDs[pedestriansTable.NBPEDESTRIAN.indexOf(row.NBPEDESTRIAN)]]
}


// Apply the audioChooser function to pedestriansTable and create a new column audioFileID
pedestriansTable.each { row ->
    def sourceNbPers = row.NBPEDESTRIAN
    row.audioFileID = audioChooser(sourceNbPers, nb_dispo, BDD_Info)
}

// PRINT TO CHECK
newPedestriansTable.each { row ->
    println("audioFileID: ${row.AudioFileID}, NbPed: ${row.NBPEDESTRIAN}, the_geom: ${row.the_geom}, PK: ${row.PK}")
}
*/


