package org.example.utils;

import org.example.problemMetaData.BinPosRoRunner;
import org.example.problemMetaData.ProblemMetaData;
import org.example.problemMetaData.Sentence;
import org.example.problemMetaData.Word;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PhraseSplitterOnVerbs {

    private ProblemMetaData problemMetaData;

    private String problem;

    public PhraseSplitterOnVerbs(ProblemMetaData problemMetaData){
        this.problemMetaData = problemMetaData;
        this.problem = problemMetaData.problem;
    }

    public ArrayList<String> getSentences() {

        ArrayList<String> sentences = new ArrayList<>();

        //Getting the Junction Points
        Path junctionPointsPath = Paths.get("src/main/java/org/example/files/junctionPoints.txt");
        List<String> junctionPointsList;
        try {
            junctionPointsList = Files.readAllLines(junctionPointsPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Removing the sentence
//        String question = QuestionFinder.getQuestion(problem);
//
//        if(question != null){
//            problem = problem.replace(question, "");
//        }

        //Tokenization
        //POS Tags extraction
        List<String> posTags = problemMetaData.getAllPOSTags();
        List<String> tokens = problemMetaData.getAllTokens();

        ArrayList<Integer> tokenIndices = getTokenIndices(problem, tokens);

        int startIndex = 0;
        int endIndex = problem.length();
        int lastEndIndex = 0;
        boolean verbWasPresent = false;

        for(int i = 0; i < tokens.size(); i++){

            if(posTags.get(i) != null && posTags.get(i).equals("VERB")){
                verbWasPresent = true;
            }

            if(tokens.get(i).equals(".") || tokens.get(i).equals("!") || tokens.get(i).equals("?") || (tokens.get(i).equals(",") && verbWasPresent) || i == tokens.size() - 1){

                endIndex = tokenIndices.get(i);

                if(i == tokens.size() - 1 && !tokens.get(i).equals(".") && !tokens.get(i).equals("!") && !tokens.get(i).equals("?") && !tokens.get(i).equals(",")){
                    endIndex = problem.length();
                }

                String subString = problem.substring(startIndex, endIndex);

                if(verbWasPresent || sentences.isEmpty() || tokens.get(lastEndIndex).equals(".") || tokens.get(lastEndIndex).equals("!") || tokens.get(lastEndIndex).equals("?")){
                    sentences.add(subString);
                }else{
                        String lastSent = sentences.get(sentences.size() - 1);
                        String concatSent = lastSent + ", " + subString;
                        sentences.set(sentences.size() - 1, concatSent);
                }

                lastEndIndex = i;
                verbWasPresent = false;

                //Setting the startIndex after the junctionPoint
                if (i + 1 < tokens.size()) {
                    String subStringToCheckJunctionPoint = problem.substring(tokenIndices.get(i + 1)).toLowerCase();
                    boolean nextWordIsJunctionPoint = false;
                    String[] foundJunctionPointWords = null;

                    for (String junctionPoint : junctionPointsList) {
                        String junctionPointLower = junctionPoint.toLowerCase();
                        if (subStringToCheckJunctionPoint.startsWith(junctionPointLower)) {
                            ProblemMetaData jp = BinPosRoRunner.runTextAnalysis(junctionPoint);
                            List<Sentence> jpSentences = jp.getSentences();
                            List<String> jpTokens = new ArrayList<>();
                            for(Sentence s : jpSentences){
                                List<Word> words = s.getWords();
                                for(Word w : words){
                                    jpTokens.add(w.getContent());
                                }
                            }
                            foundJunctionPointWords = jpTokens.toArray(new String[0]);
                            nextWordIsJunctionPoint = true;
                            break;
                        }
                    }

                    if (!nextWordIsJunctionPoint) {
                        startIndex = tokenIndices.get(i + 1);
                        if (",".equals(tokens.get(i + 1)) && i + 2 < tokens.size()) {
                            startIndex = tokenIndices.get(i + 2);
                        }
                    } else {
                        int junctionPointEndIndex = i + foundJunctionPointWords.length + 1;
                        if (junctionPointEndIndex < tokenIndices.size()) {
                            startIndex = tokenIndices.get(junctionPointEndIndex);
                            if (",".equals(tokens.get(junctionPointEndIndex)) && junctionPointEndIndex + 1 < tokens.size()) {
                                startIndex = tokenIndices.get(junctionPointEndIndex + 1);
                            }
                        }
                    }
                }

            }

        }

        if(sentences.isEmpty()){
            sentences.add(problem.substring(startIndex, endIndex));
        }


        for(int i = 0; i < sentences.size(); i++){
            sentences.set(i, capitalizeAndAddPoint(sentences.get(i)));
        }

        return sentences;

    }

    public static String capitalizeAndAddPoint(String input) {
        // check if input is null or empty
        if (input == null || input.isEmpty()) {
            return input;
        }
        // get the first character and convert it to upper case
        char first = Character.toUpperCase(input.charAt(0));
        // get the rest of the string
        String rest = input.substring(1);
        // return the concatenated string with a point at the end
        return first + rest + ".";
    }

    @NotNull
    private static ArrayList<Integer> getTokenIndices(String problem, List<String> tokens) {
        ArrayList<Integer> tokenIndices = new ArrayList<>();

        StringBuilder tempProblem = new StringBuilder();
        tempProblem.append(problem);
        StringBuilder replacement = new StringBuilder();

        for(String token : tokens){
            tokenIndices.add(tempProblem.indexOf(token));
            replacement.setLength(0);
            replacement.append("~".repeat(token.length()));
            tempProblem.replace(tempProblem.indexOf(token), tempProblem.indexOf(token) + token.length(), replacement.toString());
        }
        return tokenIndices;
    }

    public static void main(String[] args) {
        PhraseSplitterOnVerbs phraseSplitter = new PhraseSplitterOnVerbs(BinPosRoRunner.runTextAnalysis("Ana are multe mere, pere, banane, protocale, și mai are 10 cai."));
        ArrayList<String> sentences = phraseSplitter.getSentences();
        for(String sentence : sentences){
            System.out.println(sentence);
        }
    }

}
//Betty economisește bani pentru un portofel nou care costă 100 de dolari. Betty are doar jumătate din banii de care are nevoie. Părinții ei au decis să-i dea 15 dolari în acest scop, iar bunicii ei de două ori mai mult decât părinții ei. De câți bani mai are nevoie Betty pentru a cumpăra portofelul?
//Ken a creat un pachet de îngrijire pentru a-l trimite fratelui său, care era plecat la internat.  Ken a așezat o cutie pe un cântar, apoi a turnat în cutie suficiente jeleuri pentru a aduce greutatea la 2 kilograme.  Apoi, a adăugat suficiente negrese pentru ca greutatea să se tripleze.  Apoi, a adăugat încă 2 kilograme de jeleuri.  Și, în cele din urmă, a adăugat suficienți viermi gumați pentru a dubla din nou greutatea.  Care a fost greutatea finală a cutiei de bunătăți, în kilograme?
//Un monstru de mare adâncime iese din ape o dată la o sută de ani pentru a se hrăni cu o navă și a-și potoli foamea. În trei sute de ani, a consumat 847 de oameni. Navele au fost construite mai mari de-a lungul timpului, astfel încât fiecare navă nouă are de două ori mai mulți oameni decât ultima navă. Câți oameni se aflau pe nava pe care a mâncat-o monstrul în prima sută de ani?
//Într-un camion, există 26 de căști de protecție roz, 15 căști de protecție verzi și 24 de căști de protecție galbene.  Dacă Carl ia 4 căști de protecție roz, iar John ia 6 căști de protecție roz și de două ori mai multe căști de protecție verzi decât numărul de căști de protecție roz pe care le-a luat, calculați numărul total de căști de protecție care au rămas în camion.