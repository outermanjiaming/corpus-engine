package com.suppresswarnings.corpus.context;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.State;
import com.suppresswarnings.corpus.engine.CorpusEngine;

public class ReplyContext extends Context<CorpusEngine> {
    private String userid;
    public ReplyContext(CorpusEngine handler, String userid) {
        super(handler);
        this.userid = userid;
        this.state(init);
    }

    State<Context<CorpusEngine>> question = (t,  u) ->{
        u.handler().saveQ(userid, t);
        u.output(u.handler().getAnswer(t));
        return this.question;
    };

    State<Context<CorpusEngine>> init = (t, u) -> {
        u.output("ReplyContext");
        return this.question;
    };
}
