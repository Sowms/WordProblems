
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefChain.CorefMention;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations.CorefChainAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

class Number {
	int value;
	int unit;
	int pos;
	String entity;
}
public class EquationFramer {
	static HashMap<String,String> singPlural;
	public static void buildLookup() {
		singPlural = new HashMap<String,String>();
		BufferedReader br = null;	 
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader("singularplural"));
			while ((sCurrentLine = br.readLine()) != null) {
				String[] temp = sCurrentLine.split("\t");
				String value = temp[0], key;
				if(temp.length==3) 
					key = temp[2];
				else
					key = temp[1];
				singPlural.put(key,value);
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	public static ArrayList<String> getAllHypernyms(String word) {
		NounSynset nounSynset;
	    NounSynset[] hypernyms;
	    ArrayList<String> parents = new ArrayList<String>();
	    WordNetDatabase database = WordNetDatabase.getFileInstance();
	    String current = word;
	    while (!current.equals("entity")) {
	    	Synset[] synsets = database.getSynsets(current, SynsetType.NOUN); 
	        nounSynset = (NounSynset)(synsets[0]);
	        hypernyms = nounSynset.getHypernyms();
	        current = hypernyms[0].getWordForms()[0];
	    //    System.err.println(current);
	        int index = 1;
	        while (!parents.contains(current) && index < hypernyms.length) {
	        	current = hypernyms[index].getWordForms()[0];
	        	index++;
	        }
	        if (parents.contains(current))
	        	break;
	        parents.add(current);
	    }  
	    return parents;
	}
	public static void main(String[] args) {
		buildLookup();
		String question = "Aftab tells his daughter, Seven years ago, I was seven times as old as you were then. Also, three years from now, I shall be three times as old as you will be. How old is Aftab?";
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse, dcoref");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String text = question; 
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ArrayList<String> allWords = new ArrayList<String>();
	    
	    for(CoreMap sentence: sentences) {
	    	for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
	    		String word = token.get(TextAnnotation.class);
	    		String lemma;
	    		if (singPlural.containsKey(word))
	    			lemma = singPlural.get(word);
	    		else
	    			lemma = token.get(LemmaAnnotation.class);
	    		String pos = token.get(PartOfSpeechAnnotation.class);
	    		System.out.println(word+"|"+pos+"|"+lemma+"|"+token.get(NamedEntityTagAnnotation.class));
	    		allWords.add(lemma);
	    	}
	    	// this is the parse tree of the current sentence
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	System.out.println(tree);
	    	// this is the Stanford dependency graph of the current sentence
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    }
	    Map<Integer, CorefChain> graph = document.get(CorefChainAnnotation.class);
	    System.out.println(graph);
	    //http://stackoverflow.com/questions/6572207/stanford-core-nlp-understanding-coreference-resolution
	    for(Map.Entry<Integer, CorefChain> entry : graph.entrySet()) {
            CorefChain c = entry.getValue();
            //this is because it prints out a lot of self references which aren't that useful
            if(c.getMentionsInTextualOrder().size() <= 1)
                continue;
            CorefMention cm = c.getRepresentativeMention();
            String clust = "";
            List<CoreLabel> tks = document.get(SentencesAnnotation.class).get(cm.sentNum-1).get(TokensAnnotation.class);
            for(int i = cm.startIndex-1; i < cm.endIndex-1; i++)
                clust += tks.get(i).get(TextAnnotation.class) + " ";
            clust = clust.trim();
            System.out.println("representative mention: \"" + clust + "\" is mentioned by:");
            for(CorefMention m : c.getMentionsInTextualOrder()){
                String clust2 = "";
                tks = document.get(SentencesAnnotation.class).get(m.sentNum-1).get(TokensAnnotation.class);
                for(int i = m.startIndex-1; i < m.endIndex-1; i++)
                    clust2 += tks.get(i).get(TextAnnotation.class) + " ";
                clust2 = clust2.trim();
                //don't need the self mention
                if(clust.equals(clust2))
                    continue;
                System.out.println("\t" + clust2);
            }
        }
	    
	    ArrayList<Number> numbers = new ArrayList<Number>();
	    numbers = getNumbers(sentences);
	    for (Number n : numbers) {
	    	System.out.println(n.entity+"|"+n.value+"|"+n.unit);
	    }
	    
	    System.out.println(getAllHypernyms("novelist"));
	    System.out.println(getAllHypernyms("poet"));
	    System.out.println(getAllHypernyms("seat"));
	    System.out.println(getAllHypernyms("ticket"));
	    Equation oneqn = Equation.parse("x + y - 278 = 0");
	    Equation twoqn = Equation.parse("1.5x + 4y - 792 = 0");
	    ArrayList<Equation> sys = new ArrayList<Equation>();
	    sys.add(oneqn);
	    sys.add(twoqn);
	    EquationSolver.solve(sys);
	    oneqn = EquationSimplifier.simplify("5x+7y=50");
	    twoqn = EquationSimplifier.simplify("7x+5y=46");
	    sys = new ArrayList<Equation>();
	    sys.add(oneqn);
	    sys.add(twoqn);
	    EquationSolver.solve(sys);
	}
	private static ArrayList<Number> getNumbers(List<CoreMap> sentences) {
		
		ArrayList<Number> numbers = new ArrayList<Number>();
		for(CoreMap sentence: sentences) {
			int noTokens = sentence.get(TokensAnnotation.class).size();
			String[] candidates = new String[noTokens];
			for (int i=0; i<noTokens; i++) 
				candidates[i] = "";
			ArrayList<Number> sentNumbers = new ArrayList<Number>();
			int loc = 0;
			for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
		        String word = token.get(TextAnnotation.class);
		        String lemma;
		        if (singPlural.containsKey(word))
		        	lemma = singPlural.get(word);
		        else
		        	lemma = token.get(LemmaAnnotation.class);
		        String pos = token.get(PartOfSpeechAnnotation.class);
		      //  System.out.println(word+"|"+pos+"|"+lemma);
		        if (pos.contains("NN"))
		        	candidates[loc] = lemma;
		        if (pos.equals("CD")) {
		        	Number curNum = new Number();
		        	try {
		        		curNum.value = Integer.parseInt(word);
		        	}
		        	catch (Exception e) {
		        		curNum.value=Integer.parseInt(NumberNameToNumber.convert(word));
		        	}
		        	curNum.pos = loc;
		        	sentNumbers.add(curNum);
		        }
		        loc++;
		    }
			ArrayList<Number> copy = new ArrayList<Number>();
			for (Number n : sentNumbers) {
				int min = Integer.MAX_VALUE, p=-1;
				for (int i=0; i<loc; i++) {
					//System.err.println(n.value+"|"+n.pos+"|"+i+"|"+candidates[i]);
					if (!candidates[i].endsWith("ratio") && !candidates[i].equals("") && Math.abs(i-n.pos) < min) {
						min = Math.abs(i - n.pos);
						p = i;
						n.entity = candidates[i];
					}
				}
				candidates[p]="";
				copy.add(n);
			}
			numbers.addAll(copy);
		}
		return numbers;
	}
}
