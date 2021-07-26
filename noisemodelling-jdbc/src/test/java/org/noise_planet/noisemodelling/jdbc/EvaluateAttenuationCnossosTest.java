package org.noise_planet.noisemodelling.jdbc;

import org.junit.Test;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.noise_planet.noisemodelling.pathfinder.*;
import org.noise_planet.noisemodelling.propagation.ComputeRaysOutAttenuation;
import org.noise_planet.noisemodelling.propagation.EvaluateAttenuationCnossos;
import org.noise_planet.noisemodelling.propagation.PropagationProcessPathData;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.noise_planet.noisemodelling.jdbc.Utils.aWeighting;
import static org.noise_planet.noisemodelling.jdbc.Utils.addArray;
import static org.noise_planet.noisemodelling.pathfinder.utils.PowerUtils.*;

// TODO reduce error epsilon
public class EvaluateAttenuationCnossosTest {

    private static final GeometryFactory FACTORY = new GeometryFactory();

    private static final double ERROR_EPSILON_HIGHEST = 1e5;
    private static final double ERROR_EPSILON_VERY_HIGH = 15;
    private static final double ERROR_EPSILON_HIGH = 3;
    private static final double ERROR_EPSILON_MEDIUM = 1;
    private static final double ERROR_EPSILON_LOW = 0.5;
    private static final double ERROR_EPSILON_VERY_LOW = 0.2;
    private static final double ERROR_EPSILON_LOWEST = 0.02;

    private static final double[] HOM_WIND_ROSE = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private static final double[] FAV_WIND_ROSE = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};

    private static final double HUMIDITY = 70;
    private static final double TEMPERATURE = 10;
    private static final double[] SOUND_POWER_LEVELS = new double[]{93, 93, 93, 93, 93, 93, 93, 93};


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

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00};
        //double[] expectedCfH = new double[]{194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16};
        double[] expectedAGroundH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00, 0.00};
        //double[] expectedCfF = new double[]{194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16, 194.16};
        double[] expectedAGroundF = new double[]{-4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36};
        
        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00, -3.00};
        double[] expectedABoundaryF = new double[]{-4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36, -4.36};
        double[] expectedLH = new double[]{39.21, 39.16, 39.03, 38.86, 38.53, 37.36, 32.87, 16.54};
        double[] expectedLF = new double[]{40.58, 40.52, 40.40, 40.23, 39.89, 38.72, 34.24, 17.90};
        double[] expectedL = new double[]{39.95, 39.89, 39.77, 39.60, 39.26, 38.09, 33.61, 17.27};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        //double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        //double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_LOWEST);
    }

    /**
     * Test TC02 -- Mixed ground (G = 0.5)
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

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{8.2e-05, 4.5e-04, 2.5e-03, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfH = new double[]{199.17, 213.44, 225.43, 134.05, 23.76, 2.49, 0.47, 0.10};
        double[] expectedAGroundH = new double[]{-1.50, -1.50, -1.50, 0.85, 5.71, -1.50, -1.50, -1.50};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.41, 2.10, 10.13};
        double[] expectedCfF = new double[]{199.17, 213.44, 225.43, 134.05, 23.76, 2.49, 0.47, 0.10};
        double[] expectedAGroundF = new double[]{-2.18, -2.18, -2.18, -2.18, -0.93, -2.18, -2.18, -2.18};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-1.50, -1.50, -1.50, 0.85, 5.71, -1.50, -1.50, -1.50};
        double[] expectedABoundaryF = new double[]{-2.18, -2.18, -2.18, -2.18, -0.93, -2.18, -2.18, -2.18};
        double[] expectedLH = new double[]{37.71, 37.66, 37.53, 35.01, 29.82, 35.86, 31.37, 15.04};
        double[] expectedLF = new double[]{38.39, 38.34, 38.22, 38.04, 36.45, 36.54, 32.05, 15.72};
        double[] expectedL = new double[]{38.07, 38.01, 37.89, 36.79, 34.29, 36.21, 31.73, 15.39};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_VERY_LOW);
    }

    /**
     * Test TC03 -- Porous ground (G = 1)
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

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{4.9e-04, 2.7e-03, 1.5e-02, 0.08, 0.41, 2.02, 9.06, 35.59};
        double[] expectedCfH = new double[]{214.47, 224.67, 130.15, 22.76, 2.48, 0.49, 0.11, 0.03};
        double[] expectedAGroundH = new double[]{0.00, 0.00, 1.59, 9.67, 5.03, 0.00, 0.00, 0.00};
        double[] expectedWF = new double[]{0.00, 0.00, 0.01, 0.08, 0.41, 2.02, 9.06, 35.59};
        double[] expectedCfF = new double[]{214.47, 224.67, 130.15, 22.76, 2.48, 0.49, 0.11, 0.03};
        double[] expectedAGroundF = new double[]{0.00, 0.00, 0.00, 4.23, 0.00, 0.00, 0.00, 0.00};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{0.00, 0.00, 1.59, 9.67, 5.03, 0.00, 0.00, 0.00};
        double[] expectedABoundaryF = new double[]{0.00, 0.00, 0.00, 4.23, 0.00, 0.00, 0.00, 0.00};
        double[] expectedLH = new double[]{36.21, 36.16, 34.45, 26.19, 30.49, 34.36, 29.87, 13.54};
        double[] expectedLF = new double[]{36.21, 36.16, 36.03, 31.63, 35.53, 34.36, 29.87, 13.54};
        double[] expectedL = new double[]{36.21, 36.16, 35.31, 29.71, 33.70, 34.36, 29.87, 13.54};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_VERY_LOW);
    }
    
    /**
     * Test TC04 -- Flat ground with spatially varying acoustic properties
     */
    @Test
    public void TC04() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();

        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.2);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.9);

        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 4)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{1.0e-04, 5.6e-04, 3.1e-03, 0.02, 0.09, 0.50, 2.53, 11.96};
        double[] expectedCfH = new double[]{200.18, 216.12, 221.91, 116.87, 17.87, 2.02, 0.39, 0.08};
        double[] expectedAGroundH = new double[]{-1.37, -1.37, -1.37, 1.77, 6.23, -1.37, -1.37, -1.37};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.02, 0.09, 0.50, 2.53, 11.96};
        double[] expectedCfF = new double[]{200.18, 216.12, 221.91, 116.87, 17.87, 2.02, 0.39, 0.08};
        double[] expectedAGroundF = new double[]{-2.00, -2.00, -2.00, -2.00, -0.95, -2.00, -2.00, -2.00};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.36, 22.70};
        double[] expectedADiv = new double[]{56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76, 56.76};
        double[] expectedABoundaryH = new double[]{-1.37, -1.37, -1.37, 1.77, 6.23, -1.37, -1.37, -1.37};
        double[] expectedABoundaryF = new double[]{-2.00, -2.00, -2.00, -2.00, -0.95, -2.00, -2.00, -2.00};
        double[] expectedLH = new double[]{37.59, 37.53, 37.41, 34.10, 29.29, 35.73, 31.25, 14.91};
        double[] expectedLF = new double[]{38.21, 38.15, 38.03, 37.86, 36.48, 36.36, 31.87, 15.54};
        double[] expectedL = new double[]{37.91, 37.85, 37.73, 36.37, 34.23, 36.06, 31.57, 15.24};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_VERY_LOW);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_VERY_LOW);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_VERY_LOW);
     }

    /**
     * Test TC05 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC05() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();

        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2);

        profileBuilder.addTopographicLine(0, 80, 0, 255, 80, 0);
        profileBuilder.addTopographicLine(225, 80, 0, 225, -20, 0);
        profileBuilder.addTopographicLine(225, -20, 0, 0, -20, 0);
        profileBuilder.addTopographicLine(0, -20, 0, 0, 80, 0);
        profileBuilder.addTopographicLine(120, -20, 0, 120, 80, 0);
        profileBuilder.addTopographicLine(185, -5, 10, 205, -5, 10);
        profileBuilder.addTopographicLine(205, -5, 10, 205, 75, 10);
        profileBuilder.addTopographicLine(205, 74, 10, 185, 75, 10);
        profileBuilder.addTopographicLine(185, 75, 10, 185, -5, 10);

        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 14)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{1.6e-04, 8.7e-04, 4.8e-03, 0.03, 0.14, 0.75, 3.70, 16.77};
        double[] expectedCfH = new double[]{203.37, 222.35, 207.73, 82.09, 9.63, 1.33, 0.27, 0.06};
        double[] expectedAGroundH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.73, 214.27, 225.54, 131.93, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.38, 22.75};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedABoundaryF = new double[]{-1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07, -1.07};
        double[] expectedLH = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedLF = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};
        double[] expectedL = new double[]{37.26, 37.21, 37.08, 36.91, 36.57, 35.41, 30.91, 14.54};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOWEST);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_LOWEST);
    }


    /**
     * Test TC06 -- Reduced receiver height to include diffraction in some frequency bands
     */
    @Test
    public void TC06() {
        //Profile building
        ProfileBuilder profileBuilder = new ProfileBuilder();

        profileBuilder.addGroundEffect(0.0, 50.0, -20.0, 80.0, 0.9);
        profileBuilder.addGroundEffect(50.0, 150.0, -20.0, 80.0, 0.5);
        profileBuilder.addGroundEffect(150.0, 225.0, -20.0, 80.0, 0.2);

        profileBuilder.addTopographicLine(0, 80, 0, 255, 80, 0);
        profileBuilder.addTopographicLine(225, 80, 0, 225, -20, 0);
        profileBuilder.addTopographicLine(225, -20, 0, 0, -20, 0);
        profileBuilder.addTopographicLine(0, -20, 0, 0, 80, 0);
        profileBuilder.addTopographicLine(120, -20, 0, 120, 80, 0);
        profileBuilder.addTopographicLine(185, -5, 10, 205, -5, 10);
        profileBuilder.addTopographicLine(205, -5, 10, 205, 75, 10);
        profileBuilder.addTopographicLine(205, 74, 10, 185, 75, 10);
        profileBuilder.addTopographicLine(185, 75, 10, 185, -5, 10);

        profileBuilder.finishFeeding();

        //Propagation data building
        CnossosPropagationData rayData = new PropagationDataBuilder(profileBuilder)
                .addSource(10, 10, 1)
                .addReceiver(200, 50, 11.5)
                .verticalDiff(true)
                .horizontalDiff(true)
                .build();

        //Propagation process path data building
        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(HUMIDITY);
        attData.setTemperature(TEMPERATURE);

        //Out and computation settings
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);

        //Run computation
        computeRays.run(propDataOut);

        //Expected values
        double[] expectedWH = new double[]{1.1e-04, 6.0e-04, 3.4e-03, 0.018481174387754963, 0.10058027255548177, 0.53, 2.70, 12.70};
        double[] expectedCfH = new double[]{200.89, 217.45, 220.41, 110.88133821759506, 16.121253137811642, 1.88, 0.37, 0.08};
        double[] expectedAGroundH = new double[]{-1.32, -1.32, -1.32, 2.7069005842106226, -1.3265007781962908, -1.32, -1.32, -1.32};
        double[] expectedWF = new double[]{0.00, 0.00, 0.00, 0.01, 0.08, 0.42, 2.16, 10.35};
        double[] expectedCfF = new double[]{199.59, 214.11, 225.39, 131.90, 22.89, 2.42, 0.46, 0.10};
        double[] expectedAGroundF = new double[]{-1.32, -1.32, -1.29, -1.05, -1.32, -1.32, -1.32, -1.32};

        double[] expectedAlphaAtm = new double[]{0.12, 0.41, 1.04, 1.93, 3.66, 9.66, 32.77, 116.88};
        double[] expectedAAtm = new double[]{0.02, 0.08, 0.20, 0.37, 0.71, 1.88, 6.37, 22.73};
        double[] expectedADiv = new double[]{56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78, 56.78};
        double[] expectedABoundaryH = new double[]{-1.32, -1.32, -1.32, 4.31, -0.83, -1.32, -1.32, -1.32};
        double[] expectedABoundaryF = new double[]{-1.32, -1.32, -1.29, -1.05, -1.32, -1.32, -1.32, -1.32};
        double[] expectedLH = new double[]{37.53, 37.47, 37.35, 36.91, 36.34, 35.67, 31.18, 14.82};
        double[] expectedLF = new double[]{37.53, 37.47, 37.31, 36.89, 36.84, 35.67, 31.18, 14.82};
        double[] expectedL = new double[]{37.53, 37.47, 37.33, 34.99, 36.60, 35.67, 31.18, 14.82};

        //Actual values
        double[] actualWH = propDataOut.propagationPaths.get(0).groundAttenuation.wH;
        double[] actualCfH = propDataOut.propagationPaths.get(0).groundAttenuation.cfH;
        double[] actualAGroundH = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundH;
        double[] actualWF = propDataOut.propagationPaths.get(0).groundAttenuation.wF;
        double[] actualCfF = propDataOut.propagationPaths.get(0).groundAttenuation.cfF;
        double[] actualAGroundF = propDataOut.propagationPaths.get(0).groundAttenuation.aGroundF;

        double[] actualAlphaAtm = propDataOut.genericMeteoData.getAlpha_atmo();
        double[] actualAAtm = propDataOut.propagationPaths.get(0).absorptionData.aAtm;
        double[] actualADiv = propDataOut.propagationPaths.get(0).absorptionData.aDiv;
        double[] actualABoundaryH = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryH;
        double[] actualABoundaryF = propDataOut.propagationPaths.get(0).absorptionData.aBoundaryF;
        double[] actualLH = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalH, SOUND_POWER_LEVELS);
        double[] actualLF = addArray(propDataOut.propagationPaths.get(0).absorptionData.aGlobalF, SOUND_POWER_LEVELS);
        double[] actualL = addArray(propDataOut.getVerticesSoundLevel().get(0).value, SOUND_POWER_LEVELS);

        //Assertions
        assertArrayEquals(expectedWH, actualWH, ERROR_EPSILON_LOW);
        assertArrayEquals(expectedCfH, actualCfH, ERROR_EPSILON_LOW);
        assertArrayEquals(expectedAGroundH, actualAGroundH, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedWF, actualWF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedCfF, actualCfF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAGroundF, actualAGroundF, ERROR_EPSILON_LOW);

        assertArrayEquals(expectedAlphaAtm, actualAlphaAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedAAtm, actualAAtm, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedADiv, actualADiv, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedABoundaryH, actualABoundaryH, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedABoundaryF, actualABoundaryF, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedLH, actualLH, ERROR_EPSILON_LOWEST);
        //assertArrayEquals(expectedLF, actualLF, ERROR_EPSILON_LOWEST);
        assertArrayEquals(expectedL, actualL, ERROR_EPSILON_LOW);
    }

    /**
     * Test TC07h -- Flat ground with spatially varying acoustic properties and long barrier - METEO HOM
     */
    @Test
    public void TC07h()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.1, 240, 0),
                new Coordinate(265.1, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6, -1);
        
        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setWindRose(HOM_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals( new double[]{32.54,31.32,29.60,27.37,22.22,20.76,13.44,-5.81},L, ERROR_EPSILON_VERY_LOW);//HOM
    }

    /**
     * Test TC07f -- Flat ground with spatially varying acoustic properties and long barrier -  METEO FAV
     */
    @Test
    public void TC07f()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.001, 240, 0),
                new Coordinate(265.001, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setWindRose(FAV_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
         assertArrayEquals(  new double[]{32.85,31.83,30.35,28.36,25.78,22.06,14.81,-4.41},L, ERROR_EPSILON_VERY_LOW);//FAV

    }

    /**
     * Test TC07 -- Flat ground with spatially varying acoustic properties and long barrier
     */
    @Test
    public void TC07()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 240, 0),
                new Coordinate(100.001, 240, 0),
                new Coordinate(265.001, -180, 0),
                new Coordinate(265, -180, 0),
                new Coordinate(100, 240, 0)}), 6, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        //attData.setWindRose(FAV_WIND_ROSE);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{32.70,31.58,29.99,27.89,24.36,21.46,14.18,-5.05},L, ERROR_EPSILON_VERY_LOW);//p=0.5

    }

    /**
     * Test TC08_vp -- Flat ground with spatially varying acoustic properties and short barrier - vertical plane
     */
    @Test
    public void TC08_vp()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{6.49,15.47,21.37,24.67,24.32,22.62,15.14,-6.19},L, ERROR_EPSILON_VERY_LOW);//p=0.5
    }
//
//    /**
//     * Test TC08_lph -- Flat ground with spatially varying acoustic properties and short barrier - lateral paths (homogeneous)
//     */
//    @Test
//    public void TC08_lph()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        ProfileBuilder builder = new ProfileBuilder();
//
//        // Add building
//        builder.addBuilding(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 50, 0),
//                new Coordinate(175.01, 50, 0),
//                new Coordinate(190.01, 10, 0),
//                new Coordinate(190, 10, 0),
//                new Coordinate(175, 50, 0)}), 6, -1);
//
//        builder.finishFeeding();
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(builder.getPolygonWithHeight(), builder.getTriangles(),
//                builder.getTriNeighbors(), builder.getVertices());
//
//        CnossosPropagationData rayData = new CnossosPropagationData(builder);
//        rayData.addReceiver(new Coordinate(200, 50, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);
//        rayData.setComputeVerticalDiffraction(false);
//        rayData.setGs(0.9);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//        attData.setWindRose(HOM_WIND_ROSE);
//        attData.setPrime2520(true);
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
//        assertArrayEquals(  new double[]{8.17,16.86,22.51,25.46,24.87,23.44,15.93,-5.43},L, ERROR_EPSILON_low);//p=0.5
//       // double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
//       // assertArrayEquals(  new double[]{27.91,25.83,23.28,17.92,9.92,13.14,5.68,-13.7},L, ERROR_EPSILON_low);//p=0.5
//        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions
//    }
//
//    /**
//     * Test TC08_lpf -- Flat ground with spatially varying acoustic properties and short barrier - lateral paths (favorable)
//     */
//    @Test
//    public void TC08_lpf()  throws LayerDelaunayError , IOException {
//        GeometryFactory factory = new GeometryFactory();
//        //Scene dimension
//        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));
//
//        //Create obstruction test object
//        ProfileBuilder builder = new ProfileBuilder();
//
//        // Add building
//        builder.addBuilding(factory.createPolygon(new Coordinate[]{
//                new Coordinate(175, 50, 0),
//                new Coordinate(175.01, 50, 0),
//                new Coordinate(190.01, 10, 0),
//                new Coordinate(190, 10, 0),
//                new Coordinate(175, 50, 0)}), 6, -1);
//
//        builder.finishFeeding();
//
//        //Retrieve Delaunay triangulation of scene
//        FastObstructionTest manager = new FastObstructionTest(builder.getPolygonWithHeight(), builder.getTriangles(),
//                builder.getTriNeighbors(), builder.getVertices());
//
//        CnossosPropagationData rayData = new CnossosPropagationData(builder);
//        rayData.addReceiver(new Coordinate(200, 50, 4));
//        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
//        rayData.setComputeHorizontalDiffraction(true);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
//        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);
//        rayData.setComputeVerticalDiffraction(false);
//        rayData.setGs(0.9);
//
//        PropagationProcessPathData attData = new PropagationProcessPathData();
//        attData.setHumidity(70);
//        attData.setTemperature(10);
//        attData.setWindRose(FAV_WIND_ROSE);
//        attData.setPrime2520(true);
//        ComputeRaysOut propDataOut = new ComputeRaysOut(true, attData);
//        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
//        computeRays.setThreadCount(1);
//        computeRays.run(propDataOut);
//
//        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
//        assertArrayEquals(new double[]{28.59,26.51,23.96,21.09,16.68,12.82,6.36,-12.02},L, ERROR_EPSILON_medium);//p=0.5
//        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions
//
//    }



    /**
     * Test TC08 -- Flat ground with spatially varying acoustic properties and short barrier
     */
    @Test
    public void TC08()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{8.17,16.86,22.51,25.46,24.87,23.44,15.93,-5.43},L, ERROR_EPSILON_VERY_LOW);//p=0.5
        // Here we decided to define one different Gpath for each segment of each ray. In reference document only the GpathSR is used for lateral diffractions

    }

    /**
     * Test TC09 -- Ground with spatially varying heights and and acoustic properties and short barrier
     */
    @Test
    public void TC09()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 50, 0),
                new Coordinate(175.01, 50, 0),
                new Coordinate(190.01, 10, 0),
                new Coordinate(190, 10, 0),
                new Coordinate(175, 50, 0)}), 6.63, -1);

        // Add topographic points
        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        // impossible geometry in NoiseModelling
        assertArrayEquals(  new double[]{6.41,14.50,19.52,22.09,22.16,19.28,11.62,-9.31},L, ERROR_EPSILON_HIGH);//p=0.5
    }


    /**
     * Test TC10 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at low height
     */
    @Test
    public void TC10()  throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5, 0),
                new Coordinate(65, 5, 0),
                new Coordinate(65, 15, 0),
                new Coordinate(55, 15, 0),
                new Coordinate(55, 5, 0)}), 10, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 100, -100, 100)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(70, 10, 4));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(true);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93,93,93,93,93,93,93,93});
        assertArrayEquals(  new double[]{46.09,42.49,38.44,35.97,34.67,33.90,33.09,31.20},L, ERROR_EPSILON_VERY_LOW);//p=0.5
    }

    /**
     * Test TC11 -- Flat ground with homogeneous acoustic properties and cubic building – receiver
     * at large height
     */
    @Test
    public void TC11() throws LayerDelaunayError , IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(55, 5,0),
                new Coordinate(65, 5,0),
                new Coordinate(65, 15,0),
                new Coordinate(55, 15,0),
                new Coordinate(55, 5,0)}), 10, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(70, 10, 15));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{21.28,28.39,32.47,34.51,34.54,33.37,32.14,27.73},L, ERROR_EPSILON_LOW);//p=0.5

    }


    /**
     * Test TC12 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at low height
     */
    @Test
    public void TC12() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(30, 20, 6));
        rayData.addSource(factory.createPoint(new Coordinate(0, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{21.81,29.66,34.31,36.14,35.57,33.72,31.12,25.37},L, ERROR_EPSILON_VERY_LOW);//p=0.5

    }

    /**
     * Test TC13 -- Ground with spatially varying heights and acoustic properties and polygonal
     * building
     */
    @Test
    public void TC13() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(169.4, 41.0, 0),
                new Coordinate(172.5, 33.5, 0),
                new Coordinate(180.0, 30.4, 0),
                new Coordinate(187.5, 33.5, 0),
                new Coordinate(190.6, 41.0, 0),
                new Coordinate(187.5, 48.5, 0),
                new Coordinate(180.0, 51.6, 0),
                new Coordinate(172.5, 48.5, 0),
                new Coordinate(169.4, 41.0, 0)}), 20, -1);

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 28.5));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
       // attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{5.14,12.29,16.39,18.47,18.31,15.97,9.72,-9.92},L, ERROR_EPSILON_HIGH);//p=0.5

    }

    /**
     * Test TC14 -- Flat ground with homogeneous acoustic properties and polygonal building –
     * receiver at large height
     */
    @Test
    public void TC14() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(11., 15.5, 0),
                new Coordinate(12., 13, 0),
                new Coordinate(14.5, 12, 0),
                new Coordinate(17.0, 13, 0),
                new Coordinate(18.0, 15.5, 0),
                new Coordinate(17.0, 18, 0),
                new Coordinate(14.5, 19, 0),
                new Coordinate(12.0, 18, 0),
                new Coordinate(11, 15.5, 0)}), 10, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(-300, 300, -300, 300)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(25, 20, 23));
        rayData.addSource(factory.createPoint(new Coordinate(8, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.2);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{25.61,34.06,39.39,42.04,41.86,39.42,35.26,27.57},L, ERROR_EPSILON_VERY_LOW);//p=0.5
    }

    /**
     * Test TC15 -- Flat ground with homogeneous acoustic properties and four buildings
     */
    @Test
    public void TC15() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(55.0, 5.0, 0),
                new Coordinate(65.0, 5.0, 0),
                new Coordinate(65.0, 15.0, 0),
                new Coordinate(55.0, 15.0, 0),
                new Coordinate(55.0, 5.0, 0)}), 8, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(70, 14.5, 0),
                new Coordinate(80.0, 10.2, 0),
                new Coordinate(80.0, 20.2, 0),
                new Coordinate(70, 14.5, 0)}), 12, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(90.1, 19.5, 0),
                new Coordinate(93.3, 17.8, 0),
                new Coordinate(87.3, 6.6, 0),
                new Coordinate(84.1, 8.3, 0),
                new Coordinate(90.1, 19.5, 0)}), 10, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(94.9, 14.1, 0),
                new Coordinate(98.02, 12.37, 0),
                new Coordinate(92.03, 1.2, 0),
                new Coordinate(88.86, 2.9, 0),
                new Coordinate(94.9, 14.1, 0)}), 10, -1);

        builder.addGroundEffect(factory.toGeometry(new Envelope(-250, 250, -250, 250)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(100, 15, 5));
        rayData.addSource(factory.createPoint(new Coordinate(50, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{10.75,16.57,20.81,24.51,26.55,26.78,25.04,18.50},L, ERROR_EPSILON_MEDIUM);
    }

    /**
     * Reflecting barrier on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC16() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15, Arrays.asList(0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.5));


        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{13.62,23.58,30.71,35.68,38.27,38.01,32.98,15.00},L, ERROR_EPSILON_HIGH);//p=0.5
    }

    /**
     * TC17 - Reflecting barrier on ground with spatially varying heights and acoustic properties
     * reduced receiver height
     */
    @Test
    public void TC17() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52, 0),
                new Coordinate(170, 60, 0),
                new Coordinate(170, 62, 0),
                new Coordinate(114, 54, 0),
                new Coordinate(114, 52, 0)}), 15, Arrays.asList(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.5));

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 11.5));

        PropagationProcessPathData attData = new PropagationProcessPathData();
        // Push source with sound level
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), dbaToW(aWeighting(Collections.nCopies(attData.freq_lvl.size(), 93d))));

        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);
        rayData.setGs(0.9);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertArrayEquals(  new double[]{14.02,23.84,30.95,33.86,38.37,38.27,33.25,15.28}, propDataOut.getVerticesSoundLevel().get(0).value, ERROR_EPSILON_MEDIUM);//p=0.5
    }


    /**
     * TC18 - Screening and reflecting barrier on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC18() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(114, 52),
                new Coordinate(170, 60),
                new Coordinate(170, 61),
                new Coordinate(114, 53),
                new Coordinate(114, 52)}), 15, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(87, 50),
                new Coordinate(92, 32),
                new Coordinate(92, 33),
                new Coordinate(87, 51),
                new Coordinate(87, 50)}), 12, -1);

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 12));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{11.69,21.77,28.93,32.71,36.83,36.83,32.12,13.66},L, ERROR_EPSILON_LOW);//p=0.5


    }


    /**
     * TC19 - Complex object and 2 barriers on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC19() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(156, 28),
                new Coordinate(145, 7),
                new Coordinate(145, 8),
                new Coordinate(156, 29),
                new Coordinate(156, 28)}), 14, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(175, 35),
                new Coordinate(188, 19),
                new Coordinate(188, 20),
                new Coordinate(175, 36),
                new Coordinate(175, 35)}), 14.5, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 24),
                new Coordinate(118, 24),
                new Coordinate(118, 30),
                new Coordinate(100, 30),
                new Coordinate(100, 24)}), 12, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 15.1),
                new Coordinate(118, 15.1),
                new Coordinate(118, 23.9),
                new Coordinate(100, 23.9),
                new Coordinate(100, 15.1)}), 7, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(100, 9),
                new Coordinate(118, 9),
                new Coordinate(118, 15),
                new Coordinate(100, 15),
                new Coordinate(100, 9)}), 12, -1);


        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 30, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{6.72,14.66,19.34,21.58,21.84,19.00,11.42,-9.38},L, ERROR_EPSILON_VERY_HIGH);//p=0.5
    }

    /**
     * TC20 - Ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC20() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 0, -1);

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));
        rayData.setComputeHorizontalDiffraction(false);

        rayData.setComputeVerticalDiffraction(false);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{11.21,21.25,28.63,33.86,36.73,36.79,32.17,14},L, ERROR_EPSILON_VERY_LOW);//p=0.5
    }

    /**
     * TC21 - Building on ground with spatially varying heights and acoustic properties
     */
    @Test
    public void TC21() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(167.2, 39.5),
                new Coordinate(151.6, 48.5),
                new Coordinate(141.1, 30.3),
                new Coordinate(156.7, 21.3),
                new Coordinate(159.7, 26.5),
                new Coordinate(151.0, 31.5),
                new Coordinate(155.5, 39.3),
                new Coordinate(164.2, 34.3),
                new Coordinate(167.2, 39.5)}), 11.5, -1);

        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));

        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setReflexionOrder(0);
        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{10.44,20.58,27.78,33.09,35.84,35.73,30.91,12.48},L, ERROR_EPSILON_VERY_HIGH);// Because building height definition is not in accordance with ISO

    }

    /**
     * TC22 - Building with receiver backside on ground with spatially varying heights and
     * acoustic properties
     */
    @Test
    public void TC22() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(197, 36.0, 0),
                new Coordinate(179, 36, 0),
                new Coordinate(179, 15, 0),
                new Coordinate(197, 15, 0),
                new Coordinate(197, 21, 0),
                new Coordinate(187, 21, 0),
                new Coordinate(187, 30, 0),
                new Coordinate(197, 30, 0),
                new Coordinate(197, 36, 0)}), 20, -1);


        //x1
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(120, -20, 0));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        //x2
        builder.addTopographicPoint(new Coordinate(225, 80, 0));
        builder.addTopographicPoint(new Coordinate(225, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, -20, 0));
        builder.addTopographicPoint(new Coordinate(0, 80, 0));
        builder.addTopographicPoint(new Coordinate(120, 80, 0));
        builder.addTopographicPoint(new Coordinate(205, -5, 10));
        builder.addTopographicPoint(new Coordinate(205, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, 75, 10));
        builder.addTopographicPoint(new Coordinate(185, -5, 10));

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -100, 100)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -100, 100)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -100, 100)), 0.2);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(187.05, 25, 14));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)));

        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setGs(0.9);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        assertArrayEquals(  new double[]{-2.96,3.56,6.73,11.17,13.85,13.86,9.48,-7.64},L, ERROR_EPSILON_VERY_HIGH); //because we don't take into account this rays

    }


    /**
     * TC23 – Two buildings behind an earth-berm on flat ground with homogeneous acoustic
     * properties
     */
    @Test
    public void TC23() throws LayerDelaunayError, IOException {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9, buildingsAbs);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8, buildingsAbs);

        // Ground Surface

        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(30, -14, 0), // 1
                new Coordinate(122, -14, 0),// 1 - 2
                new Coordinate(122, 45, 0), // 2 - 3
                new Coordinate(30, 45, 0),  // 3 - 4
                new Coordinate(30, -14, 0) // 4
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(46.27, 36.28, 0), // 9
                new Coordinate(54.68, 37.59, 5), // 9-10
                new Coordinate(55.93, 37.93, 5), // 10-11
                new Coordinate(63.71, 41.16, 0) // 11
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 12
                new Coordinate(67.35, -6.83, 5), // 12-13
                new Coordinate(68.68, -6.49, 5), // 13-14
                new Coordinate(76.84, -5.28, 0) // 14
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(54.68, 37.59, 5), //15
                new Coordinate(67.35, -6.83, 5)
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(55.93, 37.93, 5), //16
                new Coordinate(68.68, -6.49, 5)
        }));
        builder.addGroundEffect(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }), 1.);


        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(107, 25.95, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(false);
        // Create porous surface as defined by the test:
        // The surface of the earth berm is porous (G = 1).

        rayData.setComputeVerticalDiffraction(true);
        rayData.setReflexionOrder(0);

        rayData.setGs(0.);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        //KMLDocument.exportScene("target/tc23.kml", manager, propDataOut);
        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93 - 26.2, 93 - 16.1,
                93 - 8.6, 93 - 3.2, 93, 93 + 1.2, 93 + 1.0, 93 - 1.1});
        assertArrayEquals(new double[]{12.7, 21.07, 27.66, 31.48, 31.42, 28.74, 23.75, 13.92}, L, ERROR_EPSILON_HIGH);//p=0.5

    }

    /**
     * – Two buildings behind an earth-berm on flat ground with homogeneous acoustic properties – receiver position modified
     * @throws LayerDelaunayError
     * @throws IOException
     */
    @Test
    public void TC24() throws LayerDelaunayError, IOException {
        PropagationProcessPathData attData = new PropagationProcessPathData();
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building 20% abs
        List<Double> buildingsAbs = Collections.nCopies(attData.freq_lvl.size(), 0.2);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9, buildingsAbs);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8, buildingsAbs);

        // Ground Surface

        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(30, -14, 0), // 1
                new Coordinate(122, -14, 0),// 1 - 2
                new Coordinate(122, 45, 0), // 2 - 3
                new Coordinate(30, 45, 0),  // 3 - 4
                new Coordinate(30, -14, 0) // 4
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(46.27, 36.28, 0), // 9
                new Coordinate(54.68, 37.59, 5), // 9-10
                new Coordinate(55.93, 37.93, 5), // 10-11
                new Coordinate(63.71, 41.16, 0) // 11
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 12
                new Coordinate(67.35, -6.83, 5), // 12-13
                new Coordinate(68.68, -6.49, 5), // 13-14
                new Coordinate(76.84, -5.28, 0) // 14
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(54.68, 37.59, 5), //15
                new Coordinate(67.35, -6.83, 5)
        }));
        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(55.93, 37.93, 5), //16
                new Coordinate(68.68, -6.49, 5)
        }));

        builder.addGroundEffect(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.6, -9.87, 0), // 5
                new Coordinate(76.84, -5.28, 0), // 5-6
                new Coordinate(63.71, 41.16, 0), // 6-7
                new Coordinate(46.27, 36.28, 0), // 7-8
                new Coordinate(59.6, -9.87, 0) // 8
        }), 1.);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(106, 18.5, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(true);
        // Create porus surface as defined by the test:
        // The surface of the earth berm is porous (G = 1).

        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        //KMLDocument.exportScene("target/tc24.kml", manager, propDataOut);
        assertEquals(1, propDataOut.getVerticesSoundLevel().size());
        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93 - 26.2, 93 - 16.1,
                93 - 8.6, 93 - 3.2, 93, 93 + 1.2, 93 + 1.0, 93 - 1.1});
        //todo IL Y A UNE ERREUR DANS LA NORME AVEC LE BATIMENT 2, SI ON LE SUPPRIME LES RESULTATS SONT EQUIVALENTS
        assertArrayEquals(new double[]{14.31, 21.69, 27.76, 31.52, 31.49, 29.18, 25.39, 16.58}, L, ERROR_EPSILON_VERY_HIGH);

    }

    /**
     * – Replacement of the earth-berm by a barrier
     * @throws LayerDelaunayError
     * @throws IOException
     */
    @Test
    public void TC25() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-250., -250., 0.), new Coordinate(250, 250, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();


        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(75, 34, 0),
                new Coordinate(110, 34, 0),
                new Coordinate(110, 26, 0),
                new Coordinate(75, 26, 0),
                new Coordinate(75, 34, 0)}), 9, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(83, 18, 0),
                new Coordinate(118, 18, 0),
                new Coordinate(118, 10, 0),
                new Coordinate(83, 10, 0),
                new Coordinate(83, 18, 0)}), 8, -1);

        // screen
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(59.19, 24.47, 0),
                new Coordinate(64.17, 6.95, 0),
                new Coordinate(64.171, 6.951, 0),
                new Coordinate(59.191, 24.471, 0),
                new Coordinate(59.19, 24.47, 0)}), 5, -1);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(106, 18.5, 4));
        rayData.addSource(factory.createPoint(new Coordinate(38, 14, 1)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});
        //MANQUE DIFFRACTIONS HORIZONTALES
        assertArrayEquals(  new double[]{17.50,25.65,30.56,33.22,33.48,31.52,27.51,17.80},L, ERROR_EPSILON_VERY_HIGH);//p=0.5
    }


    /**
     * TC26 – Road source with influence of retrodiffraction
     * @throws LayerDelaunayError
     * @throws IOException
     * */
    @Test
    public void TC26() throws LayerDelaunayError, IOException {


        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        // screen
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(74.0, 52.0, 0),
                new Coordinate(130.0, 60.0, 0),
                new Coordinate(130.01, 60.01, 0),
                new Coordinate(74.01, 52.01, 0),
                new Coordinate(74.0, 52.0, 0)}), 7, -1); // not exacly the same


        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -10, 100)), 0.0);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -10, 100)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(120, 50, 8));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 0.05)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});


        assertArrayEquals(  new double[]{17.50,27.52,34.89,40.14,43.10,43.59,40.55,29.15},L, ERROR_EPSILON_HIGH);//p=0.5
    }


    /**
     * TC27 – Road source with influence of retrodiffraction
     * @throws LayerDelaunayError
     * @throws IOException
     * */
    @Test
    public void TC27() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-300., -300., 0.), new Coordinate(300, 300, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        // screen
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(114.0, 52.0, 0),
                new Coordinate(170.0, 60.0, 0),
                new Coordinate(170.01, 60.01, 0),
                new Coordinate(114.01, 52.01, 0),
                new Coordinate(114.0, 52.0, 0)}), 4, -1); // not exacly the same


        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(-200, -200, -0.5), // 5
                new Coordinate(110, -200, -0.5), // 5-6
                new Coordinate(110, 200, -0.5), // 6-7
                new Coordinate(-200, 200, -0.5), // 7-8
                new Coordinate(-200, -200, -0.5) // 8
        }));

        builder.addTopographicLine(factory.createLineString(new Coordinate[]{
                new Coordinate(111, -200, 0), // 5
                new Coordinate(200, -200, 0), // 5-6
                new Coordinate(200, 200, 0), // 6-7
                new Coordinate(111, 200, 0), // 7-8
                new Coordinate(111, -200, 0) // 8
        }));


        builder.addGroundEffect(factory.toGeometry(new Envelope(80, 110, 20, 80)), 0.0);
        builder.addGroundEffect(factory.toGeometry(new Envelope(110, 215, 20, 80)), 1.0);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(200, 50, 4));
        rayData.addSource(factory.createPoint(new Coordinate(105, 35, -0.45)));
        rayData.setComputeHorizontalDiffraction(true);

        rayData.setComputeVerticalDiffraction(true);

        rayData.setReflexionOrder(1);

        rayData.setGs(0.);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{93-26.2,93-16.1,93-8.6,93-3.2,93,93+1.2,93+1.0,93-1.1});

        assertArrayEquals(  new double[]{16.84,26.97,34.79,40.23,38.57,38.58,39.36,29.60},L, ERROR_EPSILON_VERY_HIGH);// we don't take into account retrodiffraction

    }

    /**
     * TC28 Propagation over a large distance with many buildings between source and
     * receiver
     */
    @Test
    public void TC28() throws LayerDelaunayError, IOException {
        GeometryFactory factory = new GeometryFactory();

        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1500., -1500., 0.), new Coordinate(1500, 1500, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        // Add building
        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(113, 10, 0),
                new Coordinate(127, 16, 0),
                new Coordinate(102, 70, 0),
                new Coordinate(88, 64, 0),
                new Coordinate(113, 10, 0)}), 6, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(176, 19, 0),
                new Coordinate(164, 88, 0),
                new Coordinate(184, 91, 0),
                new Coordinate(196, 22, 0),
                new Coordinate(176, 19, 0)}), 10, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(250, 70, 0),
                new Coordinate(250, 180, 0),
                new Coordinate(270, 180, 0),
                new Coordinate(270, 70, 0),
                new Coordinate(250, 70, 0)}), 14, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(332, 32, 0),
                new Coordinate(348, 126, 0),
                new Coordinate(361, 108, 0),
                new Coordinate(349, 44, 0),
                new Coordinate(332, 32, 0)}), 10, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(400, 5, 0),
                new Coordinate(400, 85, 0),
                new Coordinate(415, 85, 0),
                new Coordinate(415, 5, 0),
                new Coordinate(400, 5, 0)}), 9, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(444, 47, 0),
                new Coordinate(436, 136, 0),
                new Coordinate(516, 143, 0),
                new Coordinate(521, 89, 0),
                new Coordinate(506, 87, 0),
                new Coordinate(502, 127, 0),
                new Coordinate(452, 123, 0),
                new Coordinate(459, 48, 0),
                new Coordinate(444, 47, 0)}), 12, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(773, 12, 0),
                new Coordinate(728, 90, 0),
                new Coordinate(741, 98, 0),
                new Coordinate(786, 20, 0),
                new Coordinate(773, 12, 0)}), 14, -1);

        builder.addBuilding(factory.createPolygon(new Coordinate[]{
                new Coordinate(972, 82, 0),
                new Coordinate(979, 121, 0),
                new Coordinate(993, 118, 0),
                new Coordinate(986, 79, 0),
                new Coordinate(972, 82, 0)}), 8, -1);

        //x2
        builder.addTopographicPoint(new Coordinate(-1300, -1300, 0));
        builder.addTopographicPoint(new Coordinate(1300, 1300, 0));
        builder.addTopographicPoint(new Coordinate(-1300, 1300, 0));
        builder.addTopographicPoint(new Coordinate(1300, -1300, 0));

        builder.addGroundEffect(factory.toGeometry(new Envelope(-11, 1011, -300, 300)), 0.5);

        builder.finishFeeding();

        CnossosPropagationData rayData = new CnossosPropagationData(builder);
        rayData.addReceiver(new Coordinate(1000, 100, 1));
        rayData.addSource(factory.createPoint(new Coordinate(0, 50, 4)));
        rayData.setComputeHorizontalDiffraction(true);
        rayData.maxSrcDist = 1500;
        rayData.setComputeVerticalDiffraction(true);

        rayData.setGs(0.5);

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        attData.setPrime2520(false);
        //attData.setWindRose(HOM_WIND_ROSE);
        ComputeRaysOutAttenuation propDataOut = new ComputeRaysOutAttenuation(true, attData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        double[] L = addArray(propDataOut.getVerticesSoundLevel().get(0).value, new double[]{150-26.2,150-16.1,150-8.6,150-3.2,150,150+1.2,150+1.0,150-1.1});
        assertArrayEquals(  new double[]{43.56,50.59,54.49,56.14,55.31,49.77,23.37,-59.98},L, ERROR_EPSILON_VERY_HIGH);//p=0.5


    }


    /**
     * Test optimisation feature {@link CnossosPropagationData#maximumError}
     */
    @Test
    public void testIgnoreNonSignificantSources() throws LayerDelaunayError {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(0, 0, 4));
        rayData.addSource(factory.createPoint(new Coordinate(10, 10, 1)), roadLvl);
        rayData.addSource(factory.createPoint(new Coordinate(1100, 1100, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;
        rayData.maximumError = 3; // 3 dB error max

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        // Second source has not been computed because at best it would only increase the received level of only 0.0004 dB
        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        assertEquals(44.07, wToDba(sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
    }

    @Test
    public void testRoseIndex() {
        double angle_section = (2 * Math.PI) / PropagationProcessPathData.DEFAULT_WIND_ROSE.length;
        double angleStart = Math.PI / 2 - angle_section / 2;
        for(int i = 0; i < PropagationProcessPathData.DEFAULT_WIND_ROSE.length; i++) {
            double angle = angleStart - angle_section * i - angle_section / 3;
            int index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);angle = angleStart - angle_section * i - angle_section * 2.0/3.0;
            index = ComputeRaysOutAttenuation.getRoseIndex(new Coordinate(0, 0), new Coordinate(Math.cos(angle), Math.sin(angle)));
            assertEquals(i, index);
        }
    }

    /**
     * Check if Li coefficient computation and line source subdivision are correctly done
     * @throws LayerDelaunayError
     */
    @Test
    public void testSourceLines()  throws LayerDelaunayError, IOException, ParseException {

        // First Compute the scene with only point sources at 1m each
        GeometryFactory factory = new GeometryFactory();
        WKTReader wktReader = new WKTReader(factory);
        LineString geomSource = (LineString)wktReader.read("LINESTRING (51 40.5 0.05, 51 55.5 0.05)");
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(50, 50, 0.05));
        rayData.addReceiver(new Coordinate(48, 50, 4));
        rayData.addReceiver(new Coordinate(44, 50, 4));
        rayData.addReceiver(new Coordinate(40, 50, 4));
        rayData.addReceiver(new Coordinate(20, 50, 4));
        rayData.addReceiver(new Coordinate(0, 50, 4));

        List<Coordinate> srcPtsRef = new ArrayList<>();
        ComputeCnossosRays.splitLineStringIntoPoints(geomSource, 1.0, srcPtsRef);
        for(Coordinate srcPtRef : srcPtsRef) {
            rayData.addSource(factory.createPoint(srcPtRef), roadLvl);
        }

        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);
        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);

        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.makeRelativeZToAbsolute();
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);


        // Second compute the same scene but with a line source
        rayData.clearSources();
        rayData.addSource(geomSource, roadLvl);
        RayOut propDataOutTest = new RayOut(true, attData, rayData);
        computeRays.run(propDataOutTest);

        // Merge levels for each receiver for point sources
        Map<Long, double[]> levelsPerReceiver = new HashMap<>();
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOut.receiversAttenuationLevels) {
            if(!levelsPerReceiver.containsKey(lvl.receiverId)) {
                levelsPerReceiver.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiver.put(lvl.receiverId, sumDbArray(levelsPerReceiver.get(lvl.receiverId),
                        lvl.value));
            }
        }


        // Merge levels for each receiver for lines sources
        Map<Long, double[]> levelsPerReceiverLines = new HashMap<>();
        for(ComputeRaysOutAttenuation.VerticeSL lvl : propDataOutTest.receiversAttenuationLevels) {
            if(!levelsPerReceiverLines.containsKey(lvl.receiverId)) {
                levelsPerReceiverLines.put(lvl.receiverId, lvl.value);
            } else {
                // merge
                levelsPerReceiverLines.put(lvl.receiverId, sumDbArray(levelsPerReceiverLines.get(lvl.receiverId),
                        lvl.value));
            }
        }

        assertEquals(6, levelsPerReceiverLines.size());
        assertEquals(6, levelsPerReceiver.size());

//        KMLDocument.exportScene("target/testSourceLines.kml", manager, propDataOutTest);
//        KMLDocument.exportScene("target/testSourceLinesPt.kml", manager, propDataOut);
//        // Uncomment for printing maximum error
//        for(int i = 0; i < levelsPerReceiver.size(); i++) {
//            LOGGER.info(String.format("%d error %.2f", i,  getMaxError(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i))));
//        }

        for(int i = 0; i < levelsPerReceiver.size(); i++) {
            assertArrayEquals(levelsPerReceiver.get(i), levelsPerReceiverLines.get(i), 0.2);
        }
    }





    /**
     * Test reported issue with receiver over building
     */
    @Test
    public void testReceiverOverBuilding() throws LayerDelaunayError, ParseException {

        GeometryFactory factory = new GeometryFactory();
        //Scene dimension
        Envelope cellEnvelope = new Envelope(new Coordinate(-1200, -1200, 0.), new Coordinate(1200, 1200, 0.));

        WKTReader wktReader = new WKTReader();
        //Create obstruction test object
        ProfileBuilder builder = new ProfileBuilder();

        builder.addGroundEffect(factory.toGeometry(new Envelope(0, 50, -250, 250)), 0.9);
        builder.addGroundEffect(factory.toGeometry(new Envelope(50, 150, -250, 250)), 0.5);
        builder.addGroundEffect(factory.toGeometry(new Envelope(150, 225, -250, 250)), 0.2);

        builder.addBuilding(wktReader.read("POLYGON ((-111 -35, -111 82, 70 82, 70 285, 282 285, 282 -35, -111 -35))"), 10, -1);

        builder.finishFeeding();

        double[] roadLvl = new double[]{25.65, 38.15, 54.35, 60.35, 74.65, 66.75, 59.25, 53.95};
        for(int i = 0; i < roadLvl.length; i++) {
            roadLvl[i] = dbaToW(roadLvl[i]);
        }

        DirectPropagationProcessData rayData = new DirectPropagationProcessData(builder);
        rayData.addReceiver(new Coordinate(162, 80, 150));
        rayData.addSource(factory.createPoint(new Coordinate(-150, 200, 1)), roadLvl);
        rayData.setComputeHorizontalDiffraction(true);
        rayData.setComputeVerticalDiffraction(true);

        rayData.maxSrcDist = 2000;

        PropagationProcessPathData attData = new PropagationProcessPathData();
        attData.setHumidity(70);
        attData.setTemperature(10);
        RayOut propDataOut = new RayOut(true, attData, rayData);
        ComputeCnossosRays computeRays = new ComputeCnossosRays(rayData);
        computeRays.setThreadCount(1);
        computeRays.run(propDataOut);

        assertEquals(1, propDataOut.receiversAttenuationLevels.size());

        assertEquals(14.6, wToDba(sumArray(roadLvl.length, dbaToW(propDataOut.getVerticesSoundLevel().get(0).value))), 0.1);
    }






    private static double getMaxError(double[] ref, double[] result) {
        assertEquals(ref.length, result.length);
        double max = Double.MIN_VALUE;
        for(int i=0; i < ref.length; i++) {
            max = Math.max(max, Math.abs(ref[i] - result[i]));
        }
        return max;
    }

    private static final class RayOut extends ComputeRaysOutAttenuation {
        private DirectPropagationProcessData processData;

        public RayOut(boolean keepRays, PropagationProcessPathData pathData, DirectPropagationProcessData processData) {
            super(keepRays, pathData);
            this.processData = processData;
        }

        @Override
        public double[] computeAttenuation(PropagationProcessPathData pathData, long sourceId, double sourceLi, long receiverId, List<PropagationPath> propagationPath) {
            double[] attenuation = super.computeAttenuation(pathData, sourceId, sourceLi, receiverId, propagationPath);
            double[] soundLevel = wToDba(multArray(processData.wjSources.get((int)sourceId), dbaToW(attenuation)));
            return soundLevel;
        }
    }

    private static final class DirectPropagationProcessData extends CnossosPropagationData {
        private List<double[]> wjSources = new ArrayList<>();

        public DirectPropagationProcessData(ProfileBuilder builder) {
            super(builder);
        }

        public void addSource(Geometry geom, double[] spectrum) {
            super.addSource(geom);
            wjSources.add(spectrum);
        }

        public void addSource(Geometry geom, List<Double> spectrum) {
            super.addSource(geom);
            double[] wj = new double[spectrum.size()];
            for(int i=0; i < spectrum.size(); i++) {
                wj[i] = spectrum.get(i);
            }
            wjSources.add(wj);
        }

        public void clearSources() {
            wjSources.clear();
            sourceGeometries.clear();
            sourcesIndex = new QueryRTree();
        }

        @Override
        public double[] getMaximalSourcePower(int sourceId) {
            return wjSources.get(sourceId);
        }
    }


    /**
     * Test NaN regression issue
     * ByteArrayOutputStream bos = new ByteArrayOutputStream();
     * propath.writeStream(new DataOutputStream(bos));
     * new String(Base64.getEncoder().encode(bos.toByteArray()));
     */
    @Test
    public void TestRegressionNaN() throws LayerDelaunayError, IOException {
        String path = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABkELTp9wo7AcQVnI2rXCgfo/qZmZmZmZmgAAAAAAAAAAAAAAAAAAAAAACH/4" +
                "AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAAEELUD" +
                "JSoUA3QVnItqDcGhJAJdiQBvXwS0AVTjoMf9fiAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gA" +
                "AAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELUoGFTOGrQVnIga50fzdANmqD/Me4pUActzMeCMRaAAAAAA" +
                "AAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////" +
                "AAAAA0ELUo/NRf1KQVnIgGcH8SZANmqD/Me4pUAe4TEhnNY1AAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AA" +
                "AAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAAA0ELU1RrgqjDQVnIbssqD85AMNkgsNSQIkAlRqCv" +
                "boWkAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAA" +
                "AAAAD/////AAAAA0ELU3djM9QGQVnIa6l2eGhALdnXzMMRgUAl2dfMwxGBAAAAAAAAAAAACH/4AAAAAAAAf/gAAAAAAAB/+AAAA" +
                "AAAAH/4AAAAAAAAf/gAAAAAAAB/+AAAAAAAAH/4AAAAAAAAf/gAAAAAAAD/////AAAABAAAAAU/wBvxnrf6hkBJLxOOJzAAwGIL" +
                "Ic/cAABAJRdmLHGkLkELTp9xTapHQVnI2rWzSOi/jTelQ3f5WD/hNUTOrUdwQFJ6WqIZaADAanpOelWAAD/odGaWtL+wQQtQMkh" +
                "BG1pBWci2ocn7h0ASDrQig+tyAAAAAAAAAAA/+uzqKo4AAMATSo/AwAAAP/Qml6qEHQRBC1J9yb/w6kFZyIIECGuLQBb5xfFciCQ" +
                "AAAAAAAAAAEA4e506acAAwFGKjYvwAABABNd1zN/3IEELUo8N2d3pQVnIgHgsr3lAIC98kZ14SQAAAAAAAAAAQBFrZiGsAADAKP" +
                "YLaTAAAD/SoWVrORNgQQtTU81vkgNBWchu2VI9a0AlRuFfAqJXAAAABT/TBsY8SUi2QGNg7UkPZADAe8S0CsKAAEAVMZUczyA2Q" +
                "QtOn1fPWpRBWcjat/vFwUAKJWO95msGP9MGxjxJSLZAY2CN2WJwAMB7xCtJq4AAQCDgvd4PLeBBC06fP717akFZyNq6I58WQBny" +
                "M91nx0E/0wbGPElItkBjYUy4vFgAwHvFPMvZgABAAUNc+v/JNkELTp9Xz1qUQVnI2rf7xcFACiVjveZrBj/TBsY8SUi2QGNfsm" +
                "l4cADAe8Lw2QoAAEAuAc8nHC4hQQtOn3aZdepBWcjatTnckL+z+60sjk/gP9MGxjxJSLZAY2GkR76YAMB7xbpDLYAAQBuZiTof" +
                "xetBC06fV89alEFZyNq3+8XBQAolY73mawY=";

        PropagationPath propPath = new PropagationPath();
        propPath.readStream(new DataInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(path))));
        propPath.initPropagationPath();

//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        propPath.writeStream(new DataOutputStream(bos));
//        String newVersion  = new String(Base64.getEncoder().encode(bos.toByteArray()));
//        System.out.println(newVersion);

        EvaluateAttenuationCnossos evaluateAttenuationCnossos = new EvaluateAttenuationCnossos();
        PropagationProcessPathData pathData = new PropagationProcessPathData();
        evaluateAttenuationCnossos.evaluate(propPath, pathData);
        double[] aGlobalMeteoHom = evaluateAttenuationCnossos.getaGlobal();
        for (int i = 0; i < aGlobalMeteoHom.length; i++) {
            assertFalse(String.format("freq %d Hz with nan value", pathData.freq_lvl.get(i)),
                    Double.isNaN(aGlobalMeteoHom[i]));
        }

    }
}