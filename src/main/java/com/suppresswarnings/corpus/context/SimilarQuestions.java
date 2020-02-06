package com.suppresswarnings.corpus.context;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.State;
import com.suppresswarnings.corpus.engine.CorpusEngine;

/**
 * 用户同义句特定的问题，进入该状态后必须按以下流程输入：
 * 1.输入问题
 * 2.输入同义句
 * @author lijiaming
 *
 */
public class SimilarQuestions extends Context<CorpusEngine> {
	String userid;
	String q;

	public SimilarQuestions(CorpusEngine handler, String userid) {
		super(handler);
		this.userid = userid;
		this.state(init);
	}

	State<Context<CorpusEngine>> question = (t,  u) ->{
		q = t;
		u.output(q);
		return this.similar;
	};

	State<Context<CorpusEngine>> similar = (t,  u) ->{
		u.handler().saveQS(userid, q,t);
		return this.question;
	};
	
	State<Context<CorpusEngine>> init = (t,  u) -> {
		log(" init SimilarQuestions for " + userid);
		u.output("（你进入了同义句问题状态）");
		return this.question;
	};
	
}
