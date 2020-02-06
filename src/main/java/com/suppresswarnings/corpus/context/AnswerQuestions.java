package com.suppresswarnings.corpus.context;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.State;
import com.suppresswarnings.corpus.engine.CorpusEngine;

/**
 * 用户回答特定的问题，进入该状态后必须按以下流程输入：
 * 1.输入问题
 * 2.输入回答
 * @author lijiaming
 *
 */
public class AnswerQuestions extends Context<CorpusEngine> {
	String userid;
	String q;

	public AnswerQuestions(CorpusEngine handler, String userid) {
		super(handler);
		this.userid = userid;
		this.state(init);
	}

	State<Context<CorpusEngine>> question = (t,  u) ->{
		q = t;
		u.output(q);
		return this.answer;
	};

	State<Context<CorpusEngine>> answer = (t,  u) ->{
		u.handler().saveQA(userid, q,t);
		return this.question;
	};
	
	State<Context<CorpusEngine>> init = (t,  u) -> {
		log(" init AnswerQuestions for " + userid);
		u.output("（你进入了回答问题状态）");
		return this.question;
	};
	
}
