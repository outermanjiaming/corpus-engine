package com.suppresswarnings.corpus.context;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.State;
import com.suppresswarnings.corpus.engine.CorpusEngine;

/**
 * 由服务端主动随机提问，用户回答。
 * 
 * @author lijiaming
 *
 */
public class ServerFirstQAs extends Context<CorpusEngine> {
	String userid;
	String q;

	public ServerFirstQAs(CorpusEngine handler, String userid) {
		super(handler);
		this.userid = userid;
		this.state(init);
	}

	State<Context<CorpusEngine>> sqa = (t, u) -> {
		u.handler().saveQA(userid, q, t);

		q = u.handler().nextQ(userid);
		u.output(q);
		return this.sqa;
	};

	State<Context<CorpusEngine>> init = (t, u) -> {
		u.output("（你进入了服务端提问状态）");
		q = u.handler().nextQ(userid);
		u.output(q);
		return this.sqa;
	};

}
