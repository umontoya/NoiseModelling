package org.noisemap.core;

/***********************************
 * ANR EvalPDU
 * IFSTTAR 11_05_2011
 * @author Nicolas FORTIN, Judicaël PICAUT
 ***********************************/

import java.util.HashMap;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.sql.function.AbstractScalarFunction;
import org.gdms.sql.function.BasicFunctionSignature;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;

public class BTW_SpectrumRepartition extends AbstractScalarFunction {

	private HashMap<Integer, Integer> freqToIndex = new HashMap<Integer, Integer>();
	private final static double[] non_pervious_att = { -11.3, -11.3, -11.3, -11.3, -11.3 ,-11.3,-11.3,
			-11.3, -11.3, -11.3, -11.3, -11.3, -16.3, -16.3, -16.3, -21.3, -21.3, -21.3 };


	public BTW_SpectrumRepartition() {
		super();
		freqToIndex.put(100, 0);
		freqToIndex.put(125, 1);
		freqToIndex.put(160, 2);
		freqToIndex.put(200, 3);
		freqToIndex.put(250, 4);
		freqToIndex.put(315, 5);
		freqToIndex.put(400, 6);
		freqToIndex.put(500, 7);
		freqToIndex.put(630, 8);
		freqToIndex.put(800, 9);
		freqToIndex.put(1000, 10);
		freqToIndex.put(1250, 11);
		freqToIndex.put(1600, 12);
		freqToIndex.put(2000, 13);
		freqToIndex.put(2500, 14);
		freqToIndex.put(3150, 15);
		freqToIndex.put(4000, 16);
		freqToIndex.put(5000, 17);
	}

	public double getAttenuatedValue(int freq) throws FunctionException {
		if (freqToIndex.containsKey(freq)) {
			return non_pervious_att[freqToIndex.get(freq)];
		} else {
			throw new FunctionException("The frequency " + freq
					+ " Hz is unknown !");
		}
	}

	@Override
	public Value evaluate(SQLDataSourceFactory dsf, Value... args) throws FunctionException {
		if (args.length < 2) {
			throw new FunctionException("Not enough parameters !");
		} else if (args.length > 2) {
			throw new FunctionException("Too many parameters !");
		} else {
			return ValueFactory.createValue(args[1].getAsDouble()
					+ getAttenuatedValue(args[0].getAsInt()));
		}
	}

	@Override
	public String getName() {
		return "BTW_SpectrumRepartition";
	}


	@Override
	public Type getType(Type[] types) {
		return TypeFactory.createType(Type.DOUBLE);
	}

	   
    @Override
    public FunctionSignature[] getFunctionSignatures() {
            return new FunctionSignature[] {
                    new BasicFunctionSignature(getType(null),
                    		ScalarArgument.INT,  // Frequency
							// [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000]
                    		ScalarArgument.DOUBLE // Global SPL value (dBA)
                    		)
            };
    }

	@Override
	public String getDescription() {
		return "Return the dB(A) value corresponding to the the third octave frequency band. First parameter is Frequency band one of [100,125,160,200,250,315,400,500,630,800,1000,1250,1600,2000,2500,3150,4000,5000] third parameter is the global dB(A) Spl Value.";
	}

	@Override
	public String getSqlOrder() {
		return "select BTW_SpectrumRepartition(100,dbA) as dbA_100 from myTable;";
	}

}