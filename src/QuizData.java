package src;

import java.util.ArrayList;
import java.util.List;

public class QuizData {
    public static List<src.Question> getQuestions() {
        List<src.Question> list = new ArrayList<>();

        list.add(new src.Question("Which language runs in a web browser?",
                new String[]{"Java", "C", "Python", "JavaScript"}, 3));
        list.add(new src.Question("What does JVM stand for?",
                new String[]{"Java Virtual Machine", "Java Vendor Machine", "Joint Virtual Memory", "Java Variable Method"}, 0));
        list.add(new src.Question("Which company developed the Java language?",
                new String[]{"Microsoft", "Sun Microsystems", "Apple", "IBM"}, 1));
        list.add(new src.Question("Which keyword is used to inherit a class in Java?",
                new String[]{"implements", "extends", "inherits", "super"}, 1));
        list.add(new src.Question("Which collection class allows random access by index?",
                new String[]{"HashSet", "LinkedList", "ArrayList", "HashMap"}, 2));
        list.add(new src.Question("Which operator is used for concatenation in Java?",
                new String[]{"+", "&", "+=", "concat"}, 0));
        list.add(new src.Question("Which access modifier makes a member visible only within its class?",
                new String[]{"public", "private", "protected", "default"}, 1));
        list.add(new src.Question("What is the default value of a boolean in Java?",
                new String[]{"true", "false", "0", "null"}, 1));
        list.add(new src.Question("Which package contains the Scanner class?",
                new String[]{"java.io", "java.util", "java.lang", "java.net"}, 1));
        list.add(new src.Question("Which method is the entry point of a Java program?",
                new String[]{"main", "start", "init", "run"}, 0));

        return list;
    }
}
