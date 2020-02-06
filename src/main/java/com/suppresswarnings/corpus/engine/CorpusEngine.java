package com.suppresswarnings.corpus.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.ContextFactory;
import com.suppresswarnings.corpus.State;
import com.suppresswarnings.corpus.context.AnswerQuestions;
import com.suppresswarnings.corpus.context.ServerFirstQAs;
import com.suppresswarnings.corpus.context.SimilarQuestions;
import com.suppresswarnings.osgi.leveldb.LevelDB;
import com.suppresswarnings.osgi.leveldb.LevelDBImpl;

public class CorpusEngine implements ContextFactory<CorpusEngine> {
	String delimiter = "/";
	Log logger = LogFactory.getLog(CorpusEngine.class);
	
	/**
	 * Action is limited characteristics.
	 * @author lijiaming
	 *
	 */
	public static interface Action {
		int ActionLength = 3;
		String ServerFirstQAs   = "/S1";
		String AnswerQuestions  = "/AQ";
		String SimilarQuestions = "/SQ";
		String Plugins          = "/PL";
	}
	
	Map<String, State<Context<?>>> stateMachine = new HashMap<>();
	Map<String, Context<?>> contexts = new ConcurrentHashMap<>();
	PluginEngine pluginEngine = new PluginEngine();
	AtomicInteger increment = new AtomicInteger(0);
	String index = "Index";
	Random random = new Random();
	LevelDB userDB = new LevelDBImpl("user");
	LevelDB answerDB = new LevelDBImpl("answer");
	LevelDB similarDB = new LevelDBImpl("similar");
	LevelDB questionDB = new LevelDBImpl("question");
	
	public Context<CorpusEngine> getInstance(String userid, String text) {
		String action = text.length() < Action.ActionLength ? text : text.substring(0, Action.ActionLength);
		if(action.startsWith(delimiter)) {
			switch(action) {
				case Action.ServerFirstQAs:
					return new ServerFirstQAs(this, userid);
				case Action.AnswerQuestions:
					return new AnswerQuestions(this, userid);
				case Action.SimilarQuestions:
					return new SimilarQuestions(this, userid);
				case Action.Plugins:
					return pluginEngine.getInstance(userid, text);
				default:
					return null;
			}
		} else {
			return null;
		}
	}
	
	public String input(String id, String text) {
		Context<?> trans = this.getInstance(id, text);
		if(trans != null) {
			contexts.put(id, trans);
		}
		Context<?> ctx = contexts.get(id);
		String output = ctx.apply(text);
		return output;
	}

	public void log(String info) {
		//logger.info(info);
		System.out.println(info);
	}
	public int random(int bound) {
		return random.nextInt(bound);
	}
	/**
	 * 保存一对问答
	 * @param userid
	 * @param question
	 * @param answer
	 */
	public void saveQA(String userid, String question, String answer) {
		log(userid+ " Q:" + question);
		log(userid+ " A:" + answer);
		answerDB.put(question, answer);
		String answerK = joinKey("Question", question, "Answer", answer);
		String exist = answerDB.get(answerK);
		String value = "1";
		if(!isNull(exist)) {
			value = String.valueOf(Long.valueOf(exist) + 1);
		}
		//for statistic
		answerDB.put(answerK, value);
		answerDB.put(serialKey(question), answer);
		saveQ(userid, answer);
	}
	
	public String getAnswer(String question) {
		return answerDB.get(question);
	}
	
	public String getSimilar(String question) {
		return similarDB.get(question);
	}
	
	public String countQuestion(String question) {
		return questionDB.get(question);
	}
	
	/**
	 * 保存同义句
	 * @param userid
	 * @param question
	 * @param similar
	 */
	public void saveQS(String userid, String question, String similar) {
		log(userid+ " Q:" + question);
		log(userid+ " S:" + similar);
		similarDB.put(question, similar);
		String similarK = joinKey("Question", question, "Similar", similar);
		String exist = similarDB.get(similarK);
		String value = "1";
		if(!isNull(exist)) {
			value = String.valueOf(Long.valueOf(exist) + 1);
		}
		//for statistic
		similarDB.put(similarK, value);
		similarDB.put(serialKey(question), similar);
		saveQ(userid, similar);
	}
	
	/**
	 * 保存提问
	 * @param userid
	 * @param question
	 */
	public void saveQ(String userid, String question) {
		String exist = questionDB.get(question);
		String value = "1";
		//for nextQ
		questionDB.put(indexKey(), question);
		//for serial
		questionDB.put(serialKey(question), question);
		if(!isNull(exist)) {
			value = String.valueOf(Long.valueOf(exist) + 1);
		}
		//for statistic
		questionDB.put(question, value);
		//for user
		userDB.put(joinKey("User", userid, indexKey()), question);
	}

	public String indexKey() {
		return String.join(delimiter, "Index", String.valueOf(System.currentTimeMillis()), String.valueOf(increment.incrementAndGet()));
	}
	
	public String serialKey(String key) {
		return String.join(delimiter, "Serial", key, "When", String.valueOf(System.currentTimeMillis()));
	}
	
	public String joinKey(String ...args) {
		return String.join(delimiter, args);
	}
	
	public boolean isNull(String value) {
		return value == null || value.trim().length() < 1;
	}
	/**
	 * 获取本人下一题
	 * @param userid
	 * @return
	 */
	public String nextQ(String userid) {
		String nextK = joinKey("Next", userid);
		String next = userDB.get(nextK);
		if(isNull(next)) {
			next = index;
		}
		String head = next;
		log("using head: " + head);
		AtomicReference<String> key = new AtomicReference<>();
		AtomicReference<String> value = new AtomicReference<>();
		questionDB.page(index, head, null, 2, (k,v)->{
			key.set(k);
			value.set(v);
		});
		if(value.get() != null) {
			userDB.put(nextK, key.get());
			return value.get();
		} else {
			return "None";
		}
	}
	
	public void display() {
		questionDB.list("0", Integer.MAX_VALUE, (k,v)->{
			log("questionDB : " + k + " = " + v);
		});
		
		answerDB.list("0", Integer.MAX_VALUE, (k,v)->{
			log("answerDB : " + k + " = " + v);
		});
		
		similarDB.list("0", Integer.MAX_VALUE, (k,v)->{
			log("similarDB : " + k + " = " + v);
		});
		
		userDB.list("0", Integer.MAX_VALUE, (k,v)->{
			log("userDB : " + k + " = " + v);
		});
	}

	public static void main(String[] args) {
		CorpusEngine engine = new CorpusEngine();
		engine.input("12", CorpusEngine.Action.ServerFirstQAs);
		engine.input("12", "不知道");
		engine.input("12", "我母鸡");
		engine.input("12", "干啥呢");
		engine.input("12", "是谁啊");
		engine.input("21", CorpusEngine.Action.AnswerQuestions);
		engine.input("21", "你叫什么名字？");
		engine.input("21", "我叫李嘉铭");
		engine.input("21", "你来自哪里？");
		engine.input("21", "中国");
		engine.input("33", CorpusEngine.Action.SimilarQuestions);
		engine.input("33", "你叫什么名字？");
		engine.input("33", "你的名字是什么？");
		engine.display();
	}
}
