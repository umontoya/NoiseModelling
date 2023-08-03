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
 * @Author Paul Chapron, IGN
 */


package org.noise_planet.noisemodelling.wps.Experimental

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.h2gis.utilities.GeometryTableUtilities
import org.h2gis.utilities.TableLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.noise_planet.noisemodelling.pathfinder.RootProgressVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.sql.Connection

title = 'Pedestrian localisation '
description = 'Locate some pedestrian in the city thanks to a walkable area polygon and a PointsOfInterests layer.'

inputs = [
        walkableArea : [
                name : 'walkableArea',
                title: 'walkableArea',
                type : String.class
        ],
        cellSize             : [
                name       : 'cellSize',
                title      : 'cellSize',
                description: 'cellSize',
                type       : Double.class
        ],
        pointsOfInterests: [
                name       : 'PointsOfInterests',
                description: 'PointsOfInterests',
                title      : 'PointsOfInterests',
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

static Connection openGeoserverDataStoreConnection(String dbName) {
    if (dbName == null || dbName.isEmpty()) {
        dbName = new GeoServer().catalog.getStoreNames().get(0)
    }
    Store store = new GeoServer().catalog.getStore(dbName)
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}


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



def exec(connection, input) {

    // output string, the information given back to the user
    String resultString = null


    // Create a logger to display messages in the geoserver logs and in the command prompt.
    Logger logger = LoggerFactory.getLogger("org.noise_planet.noisemodelling")

    // print to command window
    logger.info('Start : Sampling Pietons')
    logger.info("inputs {}", input) // log inputs of the run


    String walkableArea = "PEDESTRIAN_AREA"
    if (input['walkableArea']) {
        walkableArea = input['walkableArea']
    }
    walkableArea = walkableArea.toUpperCase()


    Double cellSize = 2
    if (input['cellSize']) {
        cellSize = input['cellSize']
    }

    String pointsOfInterests = "PEDESTRIAN_POIS"
    if (input['pointsOfInterests']) {
        pointsOfInterests = input['pointsOfInterests']
    }
    pointsOfInterests = pointsOfInterests.toUpperCase()


    Sql sql = new Sql(connection)


    int srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(pointsOfInterests))
    logger.info("SRID de la couche de lines" +  srid)

    int poly_srid = GeometryTableUtilities.getSRID(connection, TableLocation.parse(walkableArea))
    logger.info("SRID de la couche de polygones" +  poly_srid)

    //Delete previous PEDESTRIAN.
    sql.execute(String.format("DROP TABLE IF EXISTS PEDESTRIANS"))

    /** Centroids in cells upon a polygon collection **/
    logger.info("#########################")
    logger.info("RASTERIZE...")

    sql.execute("DROP TABLE CELLGRID IF EXISTS;")
    sql.execute( "CREATE TABLE CELLGRID AS SELECT * FROM ST_MakeGrid(\'"+walkableArea+"\' , "+cellSize+" , "+ cellSize + ");")
    sql.execute("CREATE SPATIAL INDEX ON CELLGRID(the_geom);")
    sql.execute("CREATE SPATIAL INDEX ON "+walkableArea+"(the_geom);")

    sql.execute("DROP TABLE CELLGRID_ON_AREA IF EXISTS ;")
    sql.execute("CREATE TABLE CELLGRID_ON_AREA AS SELECT c.id pk,  ST_ACCUM(ST_Intersection(zem.the_geom,c.the_geom)) the_geom FROM "+walkableArea+" zem , CELLGRID c WHERE ST_intersects(c.the_geom,zem.the_geom) AND c.the_geom && zem.the_geom GROUP BY c.id ;")

    sql.execute("DROP TABLE CELLGRID IF EXISTS ;")
     /** KDE computation**/

    logger.info("#########################")
    logger.info("POINTS OF INTERESTS")

    int food_drink_count = sql.firstRow('SELECT COUNT(*) FROM ' + pointsOfInterests + ' WHERE \'TYPE\' = \'food_drink\'')[0] as Integer
    List<Point> poi_food_drink = new ArrayList<Point>()
    RootProgressVisitor KDEprogressLogger = new RootProgressVisitor(food_drink_count, true, 1)
    sql.eachRow("SELECT pk, the_geom from " + pointsOfInterests) { row ->
      def geom = row[1] as Geometry
      if (geom instanceof Point) {
          poi_food_drink.add(geom)
      }
      KDEprogressLogger.endStep()
    }

    logger.info("taille de la liste de points d'intérêt" + poi_food_drink.size())


    sql.execute("DROP TABLE POIS_DENSITY IF EXISTS;")
    sql.execute("CREATE TABLE POIS_DENSITY (pk INTEGER , the_geom GEOMETRY, density FLOAT) ;")


    logger.info("(empty) Density table POIS_DENSITY created")
    RootProgressVisitor densityprogressLogger = new RootProgressVisitor(food_drink_count, true, 1)

    def qry_add_density_values = 'INSERT INTO POIS_DENSITY (pk , the_geom, density ) VALUES (?,?,?);'
    sql.withBatch(3, qry_add_density_values) { ps ->
        sql.eachRow("SELECT pk, the_geom FROM  CELLGRID_ON_AREA ") { row ->
            int pk = row[0] as Integer
            Geometry the_poly = row[1] as Geometry
            Point centroid = the_poly.getEnvelope().getCentroid()
            double density = densityChatGPT(150,centroid, poi_food_drink)
            logger.info("here density is " + density)
            ps.addBatch(pk, the_poly, density)
        }
        densityprogressLogger.endStep()
    }

    double sum_densities =  sql.firstRow('SELECT SUM(density) FROM POIS_DENSITY')[0] as Double

    sql.execute("DROP TABLE POIS_DENSITY_POLYGONS IF EXISTS;")
    sql.execute("CREATE TABLE POIS_DENSITY_POLYGONS AS SELECT * FROM ST_EXPLODE(\'POIS_DENSITY\');")

    sql.execute("DROP TABLE PEDESTRIANS_PROBABILITY IF EXISTS;")
    sql.execute("CREATE TABLE PEDESTRIANS_PROBABILITY AS SELECT ST_POINTONSURFACE(the_geom) the_geom , density * ST_AREA(the_geom) probability FROM POIS_DENSITY_POLYGONS;")

    sql.execute("DROP TABLE POIS_DENSITY_POLYGONS,POIS_DENSITY,CELLGRID_ON_AREA IF EXISTS;")


    logger.info("somme des densités"+ sum_densities )

    logger.info("#########################")
    logger.info("It is the time to sample")

    sql.execute("DROP TABLE PEDESTRIANS IF EXISTS;")
    sql.execute("CREATE TABLE PEDESTRIANS AS SELECT the_geom the_geom, GREATEST(FLOOR(probability*10)+  CASE WHEN RAND() < (probability*10 - FLOOR(probability*10)) THEN 1 ELSE 0 END,1) AS nbPedestrian FROM PEDESTRIANS_PROBABILITY WHERE RAND() < probability*10;")
    sql.execute("DROP TABLE PEDESTRIANS_PROBABILITY IF EXISTS;")

    sql.execute("ALTER TABLE PEDESTRIANS ADD PK INT AUTO_INCREMENT PRIMARY KEY;")

    return ["Process done. Table of outputs created !"]
}



//compute the density estimate of sample points at a given location X(x,y)

/**
 * Compute density using KDE
 * Thank you to my new friend ChatGPT
 * @param bandwidth
 * @param location
 * @param poi
 * @return
 */
double densityChatGPT(double bandwidth , Point location, List<Point> poi ){
    // Compute the distances between the target point and all input objects
    List<Double> distances = poi.collect { object ->
       // new DistanceToPoint(object).computeDistance(location,object,)
        location.distance(object)
    }

// Compute the kernel density estimate for the target point
    double kernelSum = distances.collect { distance ->
        Math.exp(-0.5 * Math.pow( (distance / bandwidth) , 2))
    }.sum()

    //double density = kernelSum / (Math.sqrt(2 * Math.PI) * bandwidth * poi.size())
    double density = kernelSum / (Math.PI * 2 * bandwidth*bandwidth)
    return density
}



double density_2(Point location, List<Point> poi ){



    List<Double> poiX = poi.collect{p-> p.x}
    List<Double> poiY = poi.collect{p-> p.y}

    Double wX = Math.sqrt(variance(vecX))
    Double wY = Math.sqrt(variance(vecY))


    Double locx = location.x
    Double locy = location.y

    List<Double> terms = []

    for (p in poi) {
        Double Xi = p.x
        Double Yi = p.y
        Double kernelTerm = exp( - (Math.pow((locx-Xi),2) / (2*Math.pow(wX,2))) -  (Math.pow((locy -Yi),2) / (2*Math.pow(wY,2)))  )
        terms.add(kernelTerm)
    }

    Double kernelSum = terms.sum()

    int n = poi.size()
    Double result = kernelSum / (n * 2 * Math.PI * wX *wY)
    return( result)
    }




double densityEstimate(double[]sample_x, double[] sample_y, double [] X){
    assert(sample_x.size()==sample_y.size())

    int n=sample_x.size()
    double[] bandwidth = bandwidthMatrixSelectionScott(sample_x, sample_y, 2)
    double[] X_minus_Xi = [0]
    //double res= (1/n)  *   X_minus_Xi.collect(i -> gaussianKernelwithDiagonalBandwidth(X_minus_Xi bandwidth ) )
    double res= 0.0
    return res

}



/**
Compute the value of the density estimate at point oocation(x,y)
full formula oif the 2D multivariate kernel density estimate is
 density = {\textstyle K_{\mathbf {H} }(\mathbf {x} )={(2\pi )^{-d/2}}\mathbf {|H|} ^{-1/2}e^{-{\frac {1}{2}}\mathbf {x^{T}} \mathbf {H^{-1}} \mathbf {x} }}
Here H is diagonal
**/
double gaussianKernelwithDiagonalBandwidth(Double[] X , Double poi_x, Double poi_y ,  Double[] bandwidth_matrix_diagonal_values){

    // coordinates of the X vector
    double x =X[1]
    double y= X[2]

    // determinant of the diagonal matrix
    double Hdet = bandwidth_matrix_diagonal_values[1] * bandwidth_matrix_diagonal_values[2]
    // H inverse matrix
    double[] H_inverse = [ 1/ bandwidth_matrix_diagonal_values[1] , 1/ bandwidth_matrix_diagonal_values[2]]
    // argument of the exponential : Xtransposed . H^-1 .X =  x * Hinverse11 * x + y * Hinverse22 * y  since H is diagonal
    double x_THinverse_x = x*H_inverse[1]*x +y*H_inverse[2]*y

    // kernel value for vector X=[x,y]
    Double density= 1/(2*Math.PI)*  1 /(sqrt(Hdet)) * exp( -0.5 *  x_THinverse_x)
    return(density)
}


double variance(List<Double> x) {
    int n= x.size()
    double mu = x.sum() / n
    double variance = x.collect {i -> return Math.pow(i - mu, 2) }.sum() * (1/(n-1))    // variance empirique debiaisée
    return (variance)
}


// Based on Scott's rule of thumb  : sqrt(Hii) =  n ^( -1 / d+4) * sigma_j,
// with n the number of data points
// with d the number of spatial dimension
// j is the index of these dimension
// 2D case ; j=1 sigma_1 = std(x) , j=2sigma_2=std(y) coordinates of the sample
// return the two diagonal term of the bandwidth Matrix H
double[] bandwidthMatrixSelectionScott(double[] x , double[] y, d =2){
    assert(x.size() == y.size())
    int n= x.size() //number of sample points
    double sigma_x = Math.sqrt(variance(x))
    double sigma_y = Math.sqrt(variance(y))
    double H11 = Math.pow(Math.pow(n, (-1 / d+4))* sigma_x , 2)   //first diagonal term
    double H22 = Math.pow(Math.pow(n, (-1 / d+4))* sigma_y , 2)   //first diagonal term
    double[] res = [H11,H22]
    return res
}

