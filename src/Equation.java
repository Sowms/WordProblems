import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Equation {
	private int noVars;
	private Double[] coeff;
	public Equation() {
		this.noVars = 0;
		this.coeff = null;
	}
	public Equation(int noVars, Double[] coeff) {
		this.noVars = noVars;
		this.coeff = coeff;
	}
	public int getNoVars() {
		return noVars;
	}
	public Double[] getCoeff() {
		return coeff;
	}
	public static Equation parse(String s) {
		Equation sEqn = null;
		int count = 0;
		String[] eqnComponents = s.split(" ");
		ArrayList<Double> coefficients = new ArrayList<Double>();
		for (String component : eqnComponents) {
			if (component.equals("="))
				break;
			if (component.equals("+") || component.equals("-")) {
				count++;
				continue;
			}
			Pattern pattern = Pattern.compile("^\\d+(?:\\.\\d+)?");
		    Matcher matcher = pattern.matcher(component);
		    if(!matcher.find()) {
		    	coefficients.add(new Double(1));
		    	continue;
		    }
		    coefficients.add(Double.parseDouble(matcher.group()));
		}
		sEqn = new Equation(count,coefficients.toArray(new Double[coefficients.size()]));
		System.out.println(count+"|"+coefficients);
		return sEqn;
	}

}
