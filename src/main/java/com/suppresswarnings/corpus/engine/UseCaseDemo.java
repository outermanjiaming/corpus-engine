package com.suppresswarnings.corpus.engine;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

public class UseCaseDemo {
//	public static void main(String[] args) {
//		feed();
//	}
	
	public static void auto() {
		Scanner scanner = new Scanner(System.in);
		CorpusEngine engine = new CorpusEngine();
		String userid = "lijiaming";
		String hi = engine.input(userid, CorpusEngine.Action.ServerFirstQAs);
		System.out.println(hi);
		while(scanner.hasNext()) {
			String input = scanner.nextLine();
			String output = engine.input(userid, input);
			System.out.println(output);
		}
		scanner.close();
	}

	public static void feed() {
		Scanner scanner = new Scanner(System.in);
		CorpusEngine engine = new CorpusEngine();
		String userid = "lijiaming";
//		String hi = engine.input(userid, CorpusEngine.Action.SimilarQuestions);
		String hi = engine.input(userid, CorpusEngine.Action.AnswerQuestions);
		System.out.println(hi);
		List<String> questions = Arrays.asList("你是谁？","你在哪里？","你在做什么？","为什么？");
		Iterator<String> iterator = questions.iterator();
		//1.先输入问题
		String question = iterator.next();
		String output = engine.input(userid, question);
		System.out.println(output);
		while(scanner.hasNext()) {
			//2.然后输入回答
			String input = scanner.nextLine();
			String out = engine.input(userid, input);
			System.out.println(out);//none
			if(!iterator.hasNext()) {
				break;
			}
			//3.输入问题
			question = iterator.next();
			output = engine.input(userid, question);
			System.out.println(output);
		}
		scanner.close();
	}
	
}
