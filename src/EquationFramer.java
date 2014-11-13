
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
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
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
		String question = "A company can make 5 bikes in one minute. How many bikes can it make in 6 minutes?";
		Properties props = new Properties();
	    props.put("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
	    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
	    String text = question; 
	    Annotation document = new Annotation(text);
	    pipeline.annotate(document);
	    List<CoreMap> sentences = document.get(SentencesAnnotation.class);
	    ArrayList<String> allWords = new ArrayList<String>();
	    ArrayList<Number> numbers = new ArrayList<Number>();
	    ArrayList<String> equations = new ArrayList<String>();
	    for(CoreMap sentence: sentences) {
	    	ArrayList<Integer> values = new ArrayList<Integer>();
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
	    		if (pos.equals("CD")) {
	    			
	    			values.add(Integer.parseInt(NumberNameToNumber.convert(word)));
	    		}
	    	}
	    	// this is the parse tree of the current sentence
	    	Tree tree = sentence.get(TreeAnnotation.class);
	    	System.out.println(tree);
	    	// this is the Stanford dependency graph of the current sentence
	    	SemanticGraph dependencies = sentence.get(CollapsedCCProcessedDependenciesAnnotation.class);
	    	System.out.println(dependencies);
	    	//Assumes every sentence has equation
	    	String equation = "";
	    	ArrayList<SemanticGraphEdge> edges = (ArrayList<SemanticGraphEdge>) dependencies.edgeListSorted();
	    	boolean isEqn = false;
	    	for (SemanticGraphEdge edge : edges) {
	    		System.out.println(edge.getSource()+"|"+edge.getTarget()+"|"+edge.getRelation());
	    		if (edge.getRelation().equals("num")) {
	    			//need to resolve complex numbers
	    			Number n = new Number();
	    			if (!edge.getSource().lemma().matches("[a-zA-Z]+"))
	    				continue;
	    			n.entity = edge.getSource().lemma();
	    			//resolve number name
	    			n.value = Integer.parseInt(NumberNameToNumber.convert(edge.getTarget().originalText()));
	    			values.remove(new Integer(n.value));
	    			numbers.add(n);
	    			equation = equation + n.value + n.entity;
	    			isEqn = true;
	    			break;
	    		}
	    	}
	    	if (isEqn) {
	    		if (!values.isEmpty())
	    			equation = equation + "+0ans"+"=" + values.get(0);
	    		else
	    			equation = equation + "=ans";
	    		System.out.println("aaaaaaaaaaaaaa"+equation);
	    		equations.add(equation);
	    	}
	    }/*
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
        }*/
	    
	    /*ArrayList<Number> numbers = new ArrayList<Number>();
	    numbers = getNumbers(sentences);
	    for (Number n : numbers) {
	    	System.out.println(n.entity+"|"+n.value+"|"+n.unit);
	    }*/
	    /*
	    System.out.println(getAllHypernyms("novelist"));
	    System.out.println(getAllHypernyms("poet"));
	    System.out.println(getAllHypernyms("seat"));
	    System.out.println(getAllHypernyms("ticket"));*/
	    
	    ArrayList<Equation> sys = new ArrayList<Equation>();
	    for (String equation : equations) {
	    	sys.add(EquationSimplifier.simplify(equation));
	    }
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
