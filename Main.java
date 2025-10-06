import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

public class Main {

    private static class Production {
        private final String LHS;
        private final String RHS;

        Production(String lhs, String rhs) {
            this.LHS = lhs;
            this.RHS = rhs;
        }

        String getLHS() {
            return LHS;
        }

        String getRHS() {
            return RHS;
        }

        String toRuleString() {
            return LHS + "->" + RHS;
        }

        static Production fromRule(String rule) {
            int idx = rule.indexOf("->");
            String lhs = rule.substring(0, idx);
            String rhs = rule.substring(idx + 2);
            return new Production(lhs, rhs);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        File fp = new File("rules");
        List<Production> productions = loadProductions(fp);
        if (productions.isEmpty()) {
            System.out.println("No grammar rules found.");
            return;
        }

        String[] rulesCollections = new String[productions.size()];
        for (int i = 0; i < productions.size(); i++) {
            rulesCollections[i] = productions.get(i).toRuleString();
        }

        String augmentedRule = productions.get(0).getLHS() + "'->" + productions.get(0).getLHS();
        System.out.println("r0: " + augmentedRule);
        for (int i = 0; i < productions.size(); i++) {
            System.out.println("r" + (i + 1) + ": " + productions.get(i).toRuleString());
        }

        String nonTerminals = findNonTerminals(productions);
        System.out.println("NonTerminals: " + nonTerminals);
        String terminals = findTerminals(productions, nonTerminals);
        System.out.println("Terminals: " + terminals);

        String I0 = augmentedRule.substring(0, augmentedRule.indexOf("->") + 2) + "." + augmentedRule.substring(augmentedRule.indexOf("->") + 2);
        I0 = Closure(I0, nonTerminals, rulesCollections);

        Hashtable<Integer, String> states = new Hashtable<>();
        states.put(0, I0);
        String gotos = constructStateDiagram(states, nonTerminals, rulesCollections);
        displayStates(states);

        System.out.println("Gotos: " + gotos);
        Hashtable<String, String> parsingTable = parsingTable(states, gotos, terminals, nonTerminals, rulesCollections, productions.get(0));

        displayParsingTable(states, parsingTable, terminals, nonTerminals);

        String input = "aabb";
        parsing(parsingTable, input, productions);
    }

    private static List<Production> loadProductions(File file) throws FileNotFoundException {
        List<Production> productions = new ArrayList<>();
        try (Scanner sc = new Scanner(file)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (!line.isEmpty()) {
                    productions.add(Production.fromRule(line));
                }
            }
        }
        return productions;
    }

    public static String findNonTerminals(List<Production> productions) {
        Set<Character> symbols = new LinkedHashSet<>();
        for (Production production : productions) {
            if (!production.getLHS().isEmpty()) {
                symbols.add(production.getLHS().charAt(0));
            }
        }
        return joinCharacters(symbols);
    }

    public static String findTerminals(List<Production> productions, String nonTerminals) {
        Set<Character> nonTerminalSet = toCharacterSet(nonTerminals);
        Set<Character> terminals = new LinkedHashSet<>();
        for (Production production : productions) {
            String rhs = production.getRHS();
            for (int i = 0; i < rhs.length(); i++) {
                char symbol = rhs.charAt(i);
                if (!nonTerminalSet.contains(symbol)) {
                    terminals.add(symbol);
                }
            }
        }
        return joinCharacters(terminals);
    }

    private static String joinCharacters(Iterable<Character> characters) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (char ch : characters) {
            if (!first) {
                builder.append(',');
            }
            builder.append(ch);
            first = false;
        }
        return builder.toString();
    }

    private static Set<Character> toCharacterSet(String csv) {
        Set<Character> characters = new LinkedHashSet<>();
        if (csv == null || csv.isEmpty()) {
            return characters;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            if (!part.isEmpty()) {
                characters.add(part.charAt(0));
            }
        }
        return characters;
    }

    public static String Closure(String state, String nonTerminals, String[] rulesCollections) {
        boolean[] tags = new boolean[(nonTerminals.length() + 1) / 2];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = false;
        }
        for (int i = 0; i < tags.length; i++) {
            String[] items = state.split(",");
            for (String item : items) {
                if (item.indexOf('.') + 1 == item.length()) {
                    return state;
                } else {
                    char symbol = item.charAt(item.indexOf('.') + 1);
                    if (nonTerminals.indexOf(symbol) != -1) {
                        int k = (nonTerminals.indexOf(symbol) + 1) / 2;
                        if (tags[k]) {
                            continue;
                        }
                        for (String rule : rulesCollections) {
                            char lhs = rule.charAt(0);
                            if (lhs == symbol) {
                                int idx = rule.indexOf("->");
                                state += "," + rule.substring(0, idx + 2) + "." + rule.substring(idx + 2);
                            }
                        }
                        tags[k] = true;
                    }
                }
            }
        }
        return state;
    }

    public static String Goto(String state, char symbol, String nonTerminals, String[] rulesCollections) {
        String[] items = state.split(",");
        String newState = "";
        String toUpdate = "";
        for (String item : items) {
            String rhs = item.substring(item.indexOf("->") + 2);
            if (rhs.contains("." + symbol)) {
                toUpdate += item + ",";
            }
        }
        if (toUpdate.equals("")) {
            return "";
        }
        toUpdate = toUpdate.substring(0, toUpdate.length() - 1);
        String[] s = toUpdate.split(",");
        for (String value : s) {
            int dotIndex = value.indexOf('.');
            if (dotIndex + 2 == value.length()) {
                newState += value.substring(0, dotIndex) + symbol + ".";
            } else {
                newState += value.substring(0, dotIndex) + symbol + "." + value.substring(value.indexOf(symbol, dotIndex) + 1);
            }
            newState += ",";
        }
        newState = newState.substring(0, newState.length() - 1);
        return Closure(newState, nonTerminals, rulesCollections);
    }

    public static String constructStateDiagram(Hashtable<Integer, String> states, String nonTerminals, String[] rulesCollections) {
        LinkedHashSet<String> transitions = new LinkedHashSet<>();
        int nextStateId = states.size();
        boolean updated;
        do {
            updated = false;
            int stateCount = states.size();
            for (int i = 0; i < stateCount; i++) {
                String state = states.get(i);
                if (state == null || state.isEmpty()) {
                    continue;
                }
                String[] items = state.split(",");
                Set<Character> seenSymbols = new LinkedHashSet<>();
                for (String item : items) {
                    int dotIndex = item.indexOf('.');
                    if (dotIndex + 1 == item.length()) {
                        continue;
                    }
                    char symbol = item.charAt(dotIndex + 1);
                    if (!seenSymbols.add(symbol)) {
                        continue;
                    }
                    String dest = Goto(state, symbol, nonTerminals, rulesCollections);
                    if (dest.isEmpty()) {
                        continue;
                    }
                    int targetState = findKey(states, dest);
                    if (targetState == -1) {
                        states.put(nextStateId, dest);
                        targetState = nextStateId;
                        nextStateId++;
                        updated = true;
                    }
                    String transition = i + "" + symbol + targetState;
                    if (transitions.add(transition)) {
                        updated = true;
                    }
                }
            }
        } while (updated);
        return String.join(",", transitions);
    }

    public static Hashtable<String, String> parsingTable(Hashtable<Integer, String> states, String gotos, String terminals, String nonTerminals,
            String[] rulesCollections, Production startProduction) {
        Hashtable<String, String> pT = new Hashtable<>();
        String[] actions = gotos.isEmpty() ? new String[0] : gotos.split(",");
        Set<Character> terminalSet = toCharacterSet(terminals);
        Set<Character> nonTerminalSet = toCharacterSet(nonTerminals);
        for (String action : actions) {
            if (action.isEmpty()) {
                continue;
            }
            int index = 0;
            while (index < action.length() && Character.isDigit(action.charAt(index))) {
                index++;
            }
            String fromState = action.substring(0, index);
            char symbol = action.charAt(index);
            String toState = action.substring(index + 1);
            if (terminalSet.contains(symbol)) {
                pT.put(fromState + symbol, "s" + toState);
            } else if (nonTerminalSet.contains(symbol)) {
                pT.put(fromState + symbol, toState);
            }
        }

        String acceptItem = startProduction.getLHS() + "'->" + startProduction.getLHS() + ".";
        for (int i = 0; i < states.size(); i++) {
            String state = states.get(i);
            if (state != null && state.contains(acceptItem)) {
                pT.put(i + "$", "accept");
                break;
            }
        }

        for (int i = 0; i < states.size(); i++) {
            String state = states.get(i);
            if (state == null) {
                continue;
            }
            for (int j = 0; j < rulesCollections.length; j++) {
                String rule = rulesCollections[j] + ".";
                if (state.contains(rule)) {
                    for (char terminal : terminalSet) {
                        pT.put(i + "" + terminal, "r" + (j + 1));
                    }
                    pT.put(i + "$", "r" + (j + 1));
                }
            }
        }
        return pT;
    }

    public static int findKey(Hashtable<Integer, String> states, String value) {
        Enumeration<Integer> keys = states.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            if (value.equals(states.get(key))) {
                return key;
            }
        }
        return -1;
    }

    public static void displayStates(Hashtable<Integer, String> states) {
        for (int i = 0; i < states.size(); i++) {
            System.out.println("I" + i + ": " + states.get(i));
        }
    }

    public static void displayParsingTable(Hashtable<Integer, String> states, Hashtable<String, String> pT, String terminals, String nonTerminals) {
        System.out.printf("The parsing table:\n%24s%18s\n", "action", "goto");
        System.out.print("state");
        String firstRow = terminals.replace(",", "") + "$" + nonTerminals.replace(",", "");
        for (int i = 0; i < firstRow.substring(0, firstRow.indexOf('$')).length(); i++) {
            System.out.printf("%8c", firstRow.charAt(i));
        }
        System.out.printf("%9c", '$');
        for (int i = firstRow.indexOf('$') + 1; i < firstRow.length(); i++) {
            System.out.printf("%8c", firstRow.charAt(i));
        }
        System.out.println();
        for (int i = 0; i < states.size(); i++) {
            System.out.printf("%-6d", i);
            for (int j = 0; j < firstRow.length(); j++) {
                String result = pT.get(i + "" + firstRow.charAt(j));
                if (result == null) {
                    System.out.printf("%8c", ' ');
                } else {
                    System.out.printf("%8s", result);
                }
            }
            System.out.println();
        }
        System.out.println();
    }

    public static void parsing(Hashtable<String, String> pT, String input, List<Production> productions) {
        Stack<String> stack = new Stack<>();
        stack.push("0");
        input += "$";
        System.out.printf("Input: %s\n", input);
        System.out.printf("%-12s%8s%14s\n", "stack", "input", "action");
        while (true) {
            String stackString = stack.toString().replaceAll(",| ", "");
            System.out.printf("%-12s", stackString.substring(1, stackString.length() - 1));
            System.out.printf("%8s", input);
            String stateId = extractStateId(stack);
            String action = pT.get(stateId + input.charAt(0));
            if (action == null) {
                System.out.printf("%14s\n", "error");
                break;
            }
            System.out.printf("%14s\n", action);
            if (action.equals("accept")) {
                break;
            }
            if (action.charAt(0) == 's') {
                String nextState = action.substring(1);
                stack.push(input.charAt(0) + nextState);
                input = input.substring(1);
                continue;
            }
            if (action.charAt(0) == 'r') {
                int ruleIndex = Integer.parseInt(action.substring(1)) - 1;
                Production rule = productions.get(ruleIndex);
                String lhs = rule.getLHS();
                String rhs = rule.getRHS();
                StringBuilder stackContent = new StringBuilder();
                stackContent.append(stack.pop().charAt(0));
                while (!stackContent.toString().equals(rhs)) {
                    stackContent.insert(0, stack.pop().charAt(0));
                }
                String nextStateId = extractStateId(stack);
                String gotoState = pT.get(nextStateId + lhs);
                if (gotoState == null) {
                    System.out.printf("%14s\n", "error");
                    break;
                }
                stack.push(lhs + gotoState);
            }
        }
    }

    private static String extractStateId(Stack<String> stack) {
        String topEntry = stack.peek();
        if (stack.size() == 1) {
            return topEntry;
        }
        return topEntry.substring(1);
    }
}
