package com.suppresswarnings.corpus.engine;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.suppresswarnings.corpus.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.suppresswarnings.corpus.context.AnswerQuestions;
import com.suppresswarnings.corpus.context.ServerFirstQAs;
import com.suppresswarnings.corpus.context.SimilarQuestions;
import com.leveldb.LevelDBImpl;

public class CorpusEngine implements ContextFactory<CorpusEngine> {
	String delimiter = "/";
	Log logger = LogFactory.getLog(CorpusEngine.class);
	
	/**
	 * Action is limited characteristics.
	 * @author lijiaming
	 *
	 */
	public interface Action {
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
	String ID = "Index";
	String QA = "Answer";
	String SM = "Similar";
	String CNT = "Count";

	Random random = new Random();
	WorkFlow workFlow = new WorkFlow(this);
	AnyDB userDB = new LevelDBImpl("user");
	AnyDB answerDB = new LevelDBImpl("answer");
	AnyDB similarDB = new LevelDBImpl("similar");
	AnyDB questionDB = new LevelDBImpl("question");
	AnyDB logsDB = new LevelDBImpl("logs");

	public WorkFlow getWorkFlow() {
		return workFlow;
	}
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

	public void info(String info) {
		logger.info(info);
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
		info(userid+ " Q:" + question);
		info(userid+ " A:" + answer);
		answerDB.put(joinKey(QA, question), answer);
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
		return answerDB.get(joinKey(QA, question));
	}
	
	public String getSimilar(String question) {
		return similarDB.get(joinKey(SM, question));
	}
	
	public String countQuestion(String question) {
		return questionDB.get(joinKey(CNT, question));
	}
	
	/**
	 * 保存同义句
	 * @param userid
	 * @param question
	 * @param similar
	 */
	public void saveQS(String userid, String question, String similar) {
		info(userid+ " Q:" + question);
		info(userid+ " S:" + similar);
		similarDB.put(joinKey(SM, question), similar);
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
		questionDB.put(joinKey(CNT, question), value);
		//for user
		userDB.put(joinKey(userid, question), question);
		userDB.put(serialKey(userid), question);
	}

	public void log(String info) {
		logsDB.put(indexKey(), info);
	}

	/**
	 * Index/1581393663212/9
	 * @return
	 */
	public String indexKey() {
		return String.join(delimiter, "Index", String.valueOf(System.currentTimeMillis()), String.valueOf(increment.incrementAndGet()));
	}

	/**
	 * Serial/你叫什么名字？/When/1581393454760
	 * @param key
	 * @return
	 */
	public String serialKey(String key) {
		return String.join(delimiter, "Serial", key, "When", String.valueOf(System.currentTimeMillis()));
	}

	/**
	 * 组合多个key
	 * @param args
	 * @return
	 */
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
			next = ID;
		}
		String head = next;
		AtomicReference<String> key = new AtomicReference<>();
		AtomicReference<String> value = new AtomicReference<>();
		questionDB.page(ID, head, null, 2, (k, v)->{
			key.set(k);
			value.set(v);
		});
		if(value.get() != null) {
			userDB.put(nextK, key.get());
			return value.get();
		} else {
			return "";
		}
	}

	/**
	 * 返回用户的所有问题数据
	 * @param pages
	 * @param userid
	 * @return
	 */
	public Pages<String> userQ(Pages<String> pages, String userid) {
		List<String> data = new ArrayList<>();
		AtomicReference<String> key = new AtomicReference<>();
		AtomicReference<String> value = new AtomicReference<>();
		String head = userid;
		if(isNull(pages.getCurrent())) {
			pages.setCurrent(head);
		}
		userDB.page(head, pages.getCurrent(), null, pages.getSize(), (k,v)->{
			key.set(k);
			value.set(v);
			data.add(v);
		});
		return pages.update(data, key.get());
	}

	/**
	 * 释放资源
	 */
	public void shutdown() {
		questionDB.close();
		similarDB.close();
		answerDB.close();
		userDB.close();
		workFlow.close();
	}

	/**
	 * 展示所有数据 TODO 图形化展示分页数据
	 */
	public void display() {
		questionDB.list("0", Integer.MAX_VALUE, (k,v)->{
			info("questionDB : " + k + " = " + v);
		});
		
		answerDB.list("0", Integer.MAX_VALUE, (k,v)->{
			info("answerDB : " + k + " = " + v);
		});
		
		similarDB.list("0", Integer.MAX_VALUE, (k,v)->{
			info("similarDB : " + k + " = " + v);
		});
		
		userDB.list("0", Integer.MAX_VALUE, (k,v)->{
			info("userDB : " + k + " = " + v);
		});
	}

//	public static void main(String[] args) {
//		final String format = "%1$tY %1$tm %1$td %1$tH:%1$tM:%1$tS|%2$s %5$s%6$s%n";
//		final String key = "java.util.logging.SimpleFormatter.format";
//		System.setProperty(key, format);
//		CorpusEngine engine = new CorpusEngine();
//		String output = engine.input("12", CorpusEngine.Action.ServerFirstQAs);
//		System.out.println("////////"+output);
//		engine.input("12", "不知道");
//		engine.input("12", "我母鸡");
//		engine.input("12", "干啥呢");
//		engine.input("12", "是谁啊");
//		engine.input("21", CorpusEngine.Action.AnswerQuestions);
//		engine.input("21", "你叫什么名字？");
//		engine.input("21", "我叫李嘉铭");
//		engine.input("21", "你来自哪里？");
//		engine.input("21", "中国");
//		engine.input("33", CorpusEngine.Action.SimilarQuestions);
//		engine.input("33", "你叫什么名字？");
//		engine.input("33", "你的名字是什么？");
//		Pages<String> pages = new Pages<>();
//		pages.setSize(2);
//		Pages<String> userData = engine.userQ(pages, "21");
//		System.out.println(userData.toString());
//		System.out.println(userData.getData());
//
//		ReplyTask replyDBTask = new ReplyDBTask(engine, "21", "你在干啥呢");
//		System.out.println(replyDBTask);
//		String question = engine.getWorkFlow().ask(replyDBTask);
//		System.out.println(question);
//		ReplyTask task = engine.getWorkFlow().pollOrNull();
//		engine.getWorkFlow().reply(task, "33", "正在吃饭");
//
//		engine.display();
//		engine.shutdown();
//	}
}
