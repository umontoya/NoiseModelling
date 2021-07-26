package org.noise_planet.noisemodelling.pathfinder;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class ComputeCnossosRaysTest {

    //Error for coordinates
    private static final double DELTA_COORDS = 1e-8;

    //Error for G path value
    private static final double DELTA_G_PATH = 1e-4;

    /**
     * Test TC01 -- Reflecting ground (G = 0)
     */
    @Test
    public void TC01() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.0)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.0} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC02 -- Mixed ground (G = 0,5)
     */
    @Test
    public void TC02() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(0.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.5} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC03 -- Mixed ground (G = 0,5)
     */
    @Test
    public void TC03() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .setGs(1.0)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {1.0} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     */
    @Test
    public void TC04() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        //Ground effects
        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.2);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.9);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .horizontalDiff(true)
                .verticalDiff(true)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {0.2*(40.88/194.16) + 0.5*(102.19/194.16) + 0.9*(51.09/194.16)} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC05 -- Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC05() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        //Ground effects
        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2);
        //Topography
        profileBuilder.addTopographicLine(0, 80, 0, 225, 80, 0);
        profileBuilder.addTopographicLine(225, 80, 0, 225, -20, 0);
        profileBuilder.addTopographicLine(225, -20, 0, 0, -20, 0);
        profileBuilder.addTopographicLine(0, -20, 0, 0, 80, 0);
        profileBuilder.addTopographicLine(120, -20, 0, 120, 80, 0);
        profileBuilder.addTopographicLine(185, -5, 10, 205, -5, 10);
        profileBuilder.addTopographicLine(205, -5, 10, 205, 75, 0);
        profileBuilder.addTopographicLine(205, 75, 0, 185, 75, 0);
        profileBuilder.addTopographicLine(185, 75, 0, 185, -5, 0);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();
        //Ground effects
        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2);
        //Topography
        profileBuilder.addTopographicLine(0, 80, 0, 225, 80, 0);
        profileBuilder.addTopographicLine(225, 80, 0, 225, -20, 0);
        profileBuilder.addTopographicLine(225, -20, 0, 0, -20, 0);
        profileBuilder.addTopographicLine(0, -20, 0, 0, 80, 0);
        profileBuilder.addTopographicLine(120, -20, 0, 120, 80, 0);
        profileBuilder.addTopographicLine(185, -5, 10, 205, -5, 10);
        profileBuilder.addTopographicLine(205, -5, 10, 205, 75, 0);
        profileBuilder.addTopographicLine(205, 75, 0, 185, 75, 0);
        profileBuilder.addTopographicLine(185, 75, 0, 185, -5, 0);
        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .build();

        //Out and computation settings
        ComputeCnossosRaysOut propDataOut = new ComputeCnossosRaysOut(true);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[][][] pts = new double[][][]{
                {{10.0, 10.0, 1.0}, {200.0, 50.0, 4.0}} //Path 1 : direct
        };
        double[][] gPaths = new double[][]{
                {(0.9*40.88 + 0.5*102.19 + 0.2*51.09)/194.16} //Path 1 : direct
        };

        //Assertion
        assertPaths(pts, gPaths, propDataOut.getPropagationPaths());
    }

    private static void assertPaths(double[][][] expectedPts, double[][] expectedGPaths, List<PropagationPath> actualPaths) {
        assertEquals("Expected path count is different than actual path count.", expectedPts.length, actualPaths.size());
        for(int i=0; i<expectedPts.length; i++) {
            PropagationPath path = actualPaths.get(i);
            for(int j=0; j<expectedPts.length; j++){
                PointPath point = path.getPointList().get(j);
                assertEquals("Path "+i+" point "+j+" coord X", expectedPts[i][j][0], point.coordinate.x, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Y", expectedPts[i][j][1], point.coordinate.y, DELTA_COORDS);
                assertEquals("Path "+i+" point "+j+" coord Z", expectedPts[i][j][2], point.coordinate.z, DELTA_COORDS);
            }
            assertEquals("Expected path segments count is different than actual path segment count.", expectedGPaths[i].length, path.getSegmentList().size());
            for(int j=0; j<expectedGPaths[i].length; j++) {
                assertEquals("Path " + i + " g path " + j, expectedGPaths[i][j], path.getSegmentList().get(j).gPath, DELTA_G_PATH);
            }
        }
    }
}