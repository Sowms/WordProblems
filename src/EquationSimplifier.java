import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class EquationSimplifier {
	
	public static Equation simplify(String s) {
		Equation sEqn = null;
		String[] eqnComponents = s.split(" ");
		ArrayList<Double> coefficients = new ArrayList<Double>();
		TreeMap <String,Double> varCoeffMap = new TreeMap<String, Double>();
		varCoeffMap.put("const", 0.0);
		boolean signFlag = false, flipFlag = false;
		for (String component : eqnComponents) {
			if (component.equals("=")) {
				flipFlag = true;
				continue;
			}
			if (component.equals("-")) {
					signFlag = true;
					continue;
			}
			if (component.equals("+"))
				continue;
			Pattern coeffPattern = Pattern.compile("^\\d+(?:\\.\\d+)?");
			Pattern varPattern = Pattern.compile("[a-zA-Z]+");
		    Matcher matcher = coeffPattern.matcher(component);
		    Double singleCoeff = null;
		    if(!matcher.find()) {
		    	if (signFlag && !flipFlag || !signFlag && flipFlag)
		    		singleCoeff = -1.0; 
		    	else
		    		singleCoeff = 1.0;
		    	signFlag = false;
		    }
		    else {
		    	singleCoeff =  Double.parseDouble(matcher.group());
		    	if (signFlag && !flipFlag || !signFlag && flipFlag) {
		    		singleCoeff = -singleCoeff;
		    		signFlag = false;
		    	}
		    }
		    matcher = varPattern.matcher(component);
	    	if(!matcher.find()) {
	    		double constant = varCoeffMap.get("const"); 
	    		constant += singleCoeff;
	    		varCoeffMap.put("const", constant);
	    	}
	    	else {
	    		String var = matcher.group();
	    		double curCoeff = 0;
	    		if (varCoeffMap.containsKey(var))
	    			curCoeff += varCoeffMap.get(var);
	    		curCoeff += singleCoeff;
	    		varCoeffMap.put(var, curCoeff);
	    	}
		}
		//http://stackoverflow.com/questions/1066589/java-iterate-through-hashmap
		Iterator<Map.Entry<String,Double>> it = varCoeffMap.entrySet().iterator();
		while (it.hasNext()) {
		     Map.Entry<String,Double> pairs = it.next();
		     if (!pairs.getKey().equals("const"))
		    	 coefficients.add(pairs.getValue());
		}
		coefficients.add(varCoeffMap.get("const"));
		sEqn = new Equation(coefficients.size()-1,coefficients.toArray(new Double[coefficients.size()]));
		return sEqn;
	}

}
