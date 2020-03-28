package com.suppresswarnings.curpus.engine;

import java.util.HashMap;
import java.util.Map;

import com.suppresswarnings.corpus.Shell;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.suppresswarnings.corpus.engine.CorpusEngine;

public class DemoTest {
	
	@Test	
	public void testDemo() {
		Map<String,String> map = new HashMap<>();
		map.put("hello", "world");
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		String json = gson.toJson(map);
		System.out.println(json);
	}

	@Test
	public void testNull() {
		StringBuffer sb = new StringBuffer();
		String str = null;
		sb.append(str);
		System.out.println(sb.toString());
	}
	@Test
	public void testEngine() {
		CorpusEngine engine = new CorpusEngine();
		engine.input("12", CorpusEngine.Action.ServerFirstQAs);
		engine.input("12", "不知道");
		engine.input("12", "我母鸡");
		engine.input("21", CorpusEngine.Action.AnswerQuestions);
		engine.input("21", "你叫什么名字？");
		engine.input("21", "我叫李嘉铭");
		engine.input("21", "你来自哪里？");
		engine.input("21", "中国");
		engine.input("33", CorpusEngine.Action.SimilarQuestions);
		engine.input("33", "你叫什么名字？");
		engine.input("33", "你的名字是什么？");
		engine.shutdown();
	}

	@Test
	public void testShell() {
		//Shell shell = new Shell();
		//new Thread(()->shell.start(9091, null)).start();
	}
	
}
