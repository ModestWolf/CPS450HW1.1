import java.util.Stack;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;

public class Main {
	public static void main(String[] args) throws FileNotFoundException {
		// use the following statement if your input file is in src folder
		// File fp = new File(System.getProperty("user.dir") + "\\src\\" + "rules");
		// use the following one if your input file is under the project
		File fp = new File("rules");
    	Scanner sc = new Scanner(fp);    	
    	String rules = "";
    	while(sc.hasNextLine()) {
    		rules += sc.nextLine() + "\n";
    	}
    	String[] rulesCollections = rules.split("\n");
    	// create S'->S
    	String r0 = rulesCollections[0].charAt(0) + "'->" + rulesCollections[0].charAt(0);
    	System.out.println("r0: " + r0);
    	for(int i = 0; i < rulesCollections.length; i++) {
    		System.out.println("r" + (i + 1) + ": " + rulesCollections[i]);
    	}
    	String nonTerminals = findNonTerminals(rulesCollections);
    	System.out.println("NonTerminals: " + nonTerminals);
    	String terminals = findTerminals(rulesCollections);
    	System.out.println("Terminals: " + terminals);
    	
    	// generate the augmented rule S'->.S
    	String I0 = r0.substring(0, r0.indexOf("->") + 2) + "." + r0.substring(r0.indexOf("->") + 2);
    	
	    I0 = Closure(I0, nonTerminals, rulesCollections);
	    
	    Hashtable <Integer, String> states = new Hashtable <Integer, String> ();
	    states.put(0, I0);
	    String Gotos = constructStateDiagram(states, nonTerminals, rulesCollections);
	    displayStates(states);
	    
	    System.out.println("Gotos: " + Gotos);
	    Hashtable <String, String> parsingTable = parsingTable(states, Gotos, terminals, nonTerminals, rulesCollections);
	    
	    displayParsingTable(states, parsingTable, terminals, nonTerminals);
	    
	    String input = "aabb";
	    parsing(parsingTable, input, rulesCollections);
	    
    	sc.close();
    }
	
	public static String findNonTerminals(String[] rulesCollections) {
		String nonTerminals = "";
		char sep = ',';
		for(int i = 0; i < rulesCollections.length; i++) {
			char symbol = rulesCollections[i].charAt(0);
			// add the new symbol into nonTerminals 
			if(nonTerminals.indexOf(symbol) == -1) {
				nonTerminals += Character.toString(symbol) + sep;
			}
		}
		return nonTerminals.substring(0, nonTerminals.length() - 1);
	}
	
	public static String findTerminals(String[] rulesCollections) {
		String terminals = "";
		String nonTerminals = findNonTerminals(rulesCollections);
		char sep = ',';
		for(int i = 0; i < rulesCollections.length; i++) {
			String rhs = rulesCollections[i].substring(rulesCollections[i].indexOf("->") + 2);
			for(int j = 0; j < rhs.length(); j++) {
				char symbol = rhs.charAt(j);
				// add the new symbol into terminals when it's not a nonTerminal
				if(terminals.indexOf(symbol) == -1 && nonTerminals.indexOf(symbol) == -1) {
					terminals += Character.toString(symbol) + sep;
				}
			}
		}
		return terminals.substring(0, terminals.length() - 1);
	}
	
	public static String Closure (String state, String nonTerminals, String[] rulesCollections) {
		// boolean array used to identify if the symbol has been added
		boolean[] tags = new boolean[(nonTerminals.length() + 1) / 2];
		for(int i = 0; i < tags.length; i++)
			tags[i] = false;
		for(int i = 0; i < tags.length; i++) {
			String[] items = state.split(",");
			for(int j = 0; j < items.length; j++) {
				// fetch the symbol after dot 
				if(items[j].indexOf(".") + 1 == items[j].length()) {
					return state;
				}else {
					char symbol = items[j].charAt(items[j].indexOf(".") + 1);
					// if the symbol is a nonTerminal and not added so far
					if(nonTerminals.indexOf(symbol) != -1) {
						int k = (nonTerminals.indexOf(symbol) + 1) / 2;
						if(tags[k]) continue;
						// look for rules that begin with the symbol
						for(int l = 0; l < rulesCollections.length; l++) {
							char lhs = rulesCollections[l].charAt(0);
							if(lhs == symbol) {
								// add rules into the state with dot on rhs
								int idx = rulesCollections[l].indexOf("->");
								state += "," + rulesCollections[l].substring(0, idx + 2) + "." + rulesCollections[l].substring(idx + 2);
							}						
						}				
						// mark the symbol as added
						tags[k] = true;
					}
				}
			}			
		}
		return state;
	}	
	
	public static String Goto (String state, char symbol, String nonTerminals, String[] rulesCollections) {
		String[] items = state.split(",");
		String newState = "", toUpdate = "";
		for(int i = 0; i < items.length; i++) {
			// find all items that need to be updated
			String rhs = items[i].substring(items[i].indexOf("->") + 2);
			if(rhs.indexOf("." + symbol) != -1) {
				toUpdate += items[i] + ",";
			}
		}
		if(toUpdate.equals("")) {
			return "";
		}else {
			// get rid of the comma at the end of line
			toUpdate = toUpdate.substring(0, toUpdate.length() - 1);
			// split the items that need to be updated into s
			String[] s = toUpdate.split(",");
			for(int i = 0; i < s.length; i++) {
				// shift the dot one digit to its right
				if(s[i].indexOf(".") + 2 == s[i].length()) {
					newState += s[i].substring(0, s[i].indexOf(".")) + symbol + ".";
				}else {
					newState += s[i].substring(0, s[i].indexOf(".")) + symbol + "." + s[i].substring(s[i].indexOf(symbol, s[i].indexOf(".")) + 1);	
				}
			}		
			return Closure(newState, nonTerminals, rulesCollections);
		}
	}
	
	public static String constructStateDiagram(Hashtable <Integer, String> states, String nonTerminals, String[] rulesCollections){
		String Gotos = "";
	    int n = 1, stateSize = states.size(), gotoSize = Gotos.split(",").length;
	    while(true) {
	    	for(int i = 0; i < states.size(); i++) {
	    		// for every item in each state
	    		for(int j = 0; j < states.get(i).split(",").length; j++) {
	    			String item = states.get(i).split(",")[j];
	    			if(item.indexOf(".") + 1 == item.length()) {
	    				continue;
	    			} else {
		    			char symbol = item.charAt(item.indexOf(".") + 1);
		    			// check which state the current item is going to
		    			String dest = Goto(item, symbol, nonTerminals, rulesCollections);
		    			if(!states.contains(dest)) {
		    				states.put(n++, dest);
		    				Gotos += i + "" + symbol + "" + findKey(states, dest) + ",";		    				
		    			} else {
		    				String str = i + "" + symbol + "" + findKey(states, dest) + ",";
		    				if(Gotos.indexOf(str) == -1) {
		    					Gotos += str;
		    				}
		    			}
	    			}
	    		}
	    	}	    	
	    	// if there is no change made into either state or goto, terminate the loop
	    	if (stateSize != states.size() || gotoSize != Gotos.split(",").length) {
	    		stateSize = states.size();
	    		gotoSize = Gotos.split(",").length;
	    	} else {
	    		break;
	    	}
	    }
	    // get rid of the final comma
	    return Gotos.substring(0, Gotos.length() - 1);
	}
	
	public static Hashtable <String, String> parsingTable(Hashtable <Integer, String> states, String Gotos, String terminals, String nonTerminals, String[] rulesCollections){
		// Gotos: 0S1,0A2,0a3,0b4,2A5,2a3,2b4,3A6,3a3,3b4
		Hashtable <String, String> pT = new Hashtable <String, String>();
		String[] actions = Gotos.split(",");
		// adding shift actions
		for(int i = 0; i < actions.length; i++) {
			char symbol = actions[i].charAt(1);
			if(terminals.indexOf(symbol) != -1) {
				pT.put(actions[i].substring(0,2), "s" + actions[i].charAt(2));
			} else if (nonTerminals.indexOf(symbol) != -1) {
				pT.put(actions[i].substring(0,2), "" + actions[i].charAt(2));
			}
		}
		// adding reduce actions
		String I0 = rulesCollections[0].charAt(0) + "'->" + rulesCollections[0].charAt(0) + ".";
		// add <"1$", "accept">
		for(int i = 0; i < states.size(); i++){
			if(states.get(i).indexOf(I0) != -1) {
				pT.put(i + "$", "accept");
				break;
			} 
		}
		for(int i = 0; i < states.size(); i++) {
			for(int j = 0; j < rulesCollections.length; j++) {
				if(states.get(i).indexOf(rulesCollections[j] + ".") != -1) {
					// i: state number, j: rule number
					for(int k = 0; k < terminals.split(",").length; k++) {
						pT.put(i + "" + terminals.split(",")[k], "r" + (j + 1));
					}
					// add i$rj
					pT.put(i + "$", "r" + (j + 1));
				}
			}
		}
		return pT;
	}	
	
	public static int findKey(Hashtable<Integer, String> states, String value) {
		Enumeration <Integer> keys = states.keys();
		while(keys.hasMoreElements()) {
			int key = keys.nextElement();
			if(states.get(key).equals(value)) {
				return key;
			}
		}
		return -1;
	}
	
	public static void displayStates(Hashtable <Integer, String> states) {
    	for(int i = 0; i < states.size(); i++) {
    		System.out.println("I" + i + ": " + states.get(i));
    	}
	}
	
	public static void displayParsingTable(Hashtable <Integer, String> states, Hashtable <String, String> pT, String terminals, String nonTerminals) {
		// display the first two rows
		// 1: action & goto
		System.out.printf("The parsing table:\n%24s%18s\n", "action", "goto");
		// 2. state terminals $ nonTerminals
		System.out.print("state");
		String firstRow  = terminals.replace(",", "") + "$" + nonTerminals.replace(",", "");
		for(int i = 0; i < firstRow.substring(0, firstRow.indexOf("$")).length(); i++) {
			System.out.printf("%8c", firstRow.charAt(i));
		}
		System.out.printf("%9c", '$');
		for(int i = firstRow.indexOf("$") + 1; i < firstRow.length(); i++) {
			System.out.printf("%8c", firstRow.charAt(i));
		}
		System.out.println();
		// state and corresponding actions
		for(int i = 0; i < states.size(); i++) {
			System.out.printf("%-6d", i);
			for(int j = 0; j < firstRow.length(); j++) {
				String result = pT.get(i + "" + firstRow.charAt(j));
				if(result == null) {
					System.out.printf("%8c", ' ');
				}else {
					System.out.printf("%8s", result);
				}			
			}
			System.out.println();
		}
		System.out.println();
	}
	
	public static void parsing(Hashtable<String, String> pT, String input, String[] rulesCollections) {
		Stack <String> s = new Stack <String> ();
		s.push("0");
		input += "$";
		System.out.printf("Input: %s\n", input);
		System.out.printf("%-12s%8s%14s\n", "stack", "input", "action");
		String action = "";
		while (true) {
			String st = s.toString().replaceAll(",| ", "");
			System.out.printf("%-12s", st.substring(1, st.length() - 1));
			System.out.printf("%8s", input);
			if(s.size() == 1) {
				action = pT.get(s.peek() + input.charAt(0));	
			} else {
				action = pT.get(s.peek().charAt(1) + "" + input.charAt(0));
			}
			if (action == null) {
				// no available action -> syntax error
				System.out.printf("%14s\n", "error");
				break;
			} else {
				System.out.printf("%14s\n", action);
				if (action.equals("accept")) {
					// successfully parsed
					break;
				} else if (action.charAt(0) == 's') {
					// shift
					s.push(input.charAt(0) + "" + action.charAt(1));
					input = input.substring(1);				
				} else if (action.charAt(0) == 'r') {
					// reduce				
					int idx = action.charAt(1) - 49;
					String lhs = rulesCollections[idx].substring(0, 1);
					String rhs = rulesCollections[idx].substring(rulesCollections[idx].indexOf("->") + 2);
					String stackContent = s.pop().charAt(0) + "";
					while(!stackContent.equals(rhs)){
						stackContent = s.pop().charAt(0) + stackContent;
					}
					char snum = ' ';
					if(s.size() == 1) {
						snum = s.peek().charAt(0);
					}else {
						snum = s.peek().charAt(1);
					}
					s.push(lhs + pT.get(snum + lhs));
				}
			}		
		}
	}
}
