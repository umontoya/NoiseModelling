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

title = 'Compute voice emission noise map from pedestrians table.'
description = '&#10145;&#65039; -----------etails). </br>' +
              '<hr>' +
              '&#x2705; The output table is called: <b>LW_VOICES </b> '

inputs = [
        tablePedestrian: [
                name       : 'Pedestrians table name',
                title      : 'Pedestrians table name',
                description: "<b>Name of the Pedestrians table.</b>  </br>  " +
                        "<br>  This function recognize the following columns (* mandatory) : </br><ul>" +
                        "<li><b> PK </b>* : an identifier. It shall be a primary key (INTEGER, PRIMARY KEY)</li>" +
                            "<li><b> LV_D </b><b>LV_E </b><b>LV_N </b> : Hourly average light vehicle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> MV_D </b><b>MV_E </b><b>MV_N </b> : Hourly average medium heavy vehicles, delivery vans > 3.5 tons,  buses, touring cars, etc. with two axles and twin tyre mounting on rear axle count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> HGV_D </b><b> HGV_E </b><b> HGV_N </b> :  Hourly average heavy duty vehicles, touring cars, buses, with three or more axles (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WAV_D </b><b> WAV_E </b><b> WAV_N </b> :  Hourly average mopeds, tricycles or quads &le; 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> WBV_D </b><b> WBV_E </b><b> WBV_N </b> :  Hourly average motorcycles, tricycles or quads > 50 cc count (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> LV_SPD_D </b><b> LV_SPD_E </b><b>LV_SPD_N </b> :  Hourly average light vehicle speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> MV_SPD_D </b><b> MV_SPD_E </b><b>MV_SPD_N </b> :  Hourly average medium heavy vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                        "<li><b> HGV_SPD_D </b><b> HGV_SPD_E </b><b> HGV_SPD_N </b> :  Hourly average heavy duty vehicles speed (6-18h)(18-22h)(22-6h) (DOUBLE)</li>" +
                       "<li><b> JUNC_TYPE </b> : Type of junction (k=0 none, k = 1 for a crossing with traffic lights ; k = 2 for a roundabout) (INTEGER)</li>" +
                        "<li><b> SLOPE </b> : Slope (in %) of the road section. If the field is not filled in, the LINESTRING z-values will be used to calculate the slope and the traffic direction (way field) will be force to 3 (bidirectional). (DOUBLE)</li>" +
                        "<li><b> WAY </b> : Define the way of the road section. 1 = one way road section and the traffic goes in the same way that the slope definition you have used, 2 = one way road section and the traffic goes in the inverse way that the slope definition you have used, 3 = bi-directional traffic flow, the flow is split into two components and correct half for uphill and half for downhill (INTEGER)</li>" +
                        "</ul></br><b> This table can be generated from the WPS Block 'Import_OSM'. </b>.",
                type       : String.class
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

    // drop table LW_ROADS if exists and the create and prepare the table
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

    // Get Class to compute LW
    LDENConfig ldenConfig = new LDENConfig(LDENConfig.INPUT_MODE.INPUT_MODE_TRAFFIC_FLOW)
    ldenConfig.setCoefficientVersion(2)
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.DAY, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.EVENING, new PropagationProcessPathData(false));
    ldenConfig.setPropagationProcessPathData(LDENConfig.TIME_PERIOD.NIGHT, new PropagationProcessPathData(false));

    LDENPropagationProcessData ldenData = new LDENPropagationProcessData(null, ldenConfig)


    // Get size of the table (number of road segments
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
            // Compute emission sound level for each road segment
            def results = ldenData.computeLw(rs)
            // fill the LW_PEDESTRIAN table
            ps.addBatch(rs.getLong(pkIndex) as Integer, geo as Geometry,
                    70 + 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70 + 10*Math.log10(nbPedestrianOnPoint) as Double,
                    70+ 10*Math.log10(nbPedestrianOnPoint) as Double, 70+ 10*Math.log10(nbPedestrianOnPoint) as Double, "DAY")
        }
    }

    // Add Z dimension to the road segments
    sql.execute("UPDATE LW_PEDESTRIAN SET THE_GEOM = ST_UPDATEZ(The_geom,1.5);")

    // Add primary key to the road table
    sql.execute("ALTER TABLE LW_PEDESTRIAN ALTER COLUMN PK INT NOT NULL;")
    sql.execute("ALTER TABLE LW_PEDESTRIAN ADD PRIMARY KEY (PK);  ")

    resultString = "Calculation Done ! The table LW_PEDESTRIAN has been created."

    // print to command window
    logger.info('\nResult : ' + resultString)
    logger.info('End : LW_PEDESTRIAN from Emission')

    // print to WPS Builder
    return resultString

}


