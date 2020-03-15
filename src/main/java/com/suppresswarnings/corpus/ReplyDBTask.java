package com.suppresswarnings.corpus;

import com.suppresswarnings.corpus.engine.CorpusEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.function.BiConsumer;

/**
 * just save the QA into DB
 */
public class ReplyDBTask implements ReplyTask {
    Log logger = LogFactory.getLog(ReplyDBTask.class);
    private ReplyDBTask(){
        throw new RuntimeException("you know");
    }

    public ReplyDBTask(CorpusEngine corpusEngine, String openid, String question) {
        this.corpusEngine = corpusEngine;
        this.openid = openid;
        this.question = question;
        this.finished = false;
    }

    CorpusEngine corpusEngine;
    String openid;
    String question;
    String answer;
    transient boolean finished;

    @Override
    public String openid() {
        return openid;
    }

    @Override
    public String question() {
        return question;
    }

    @Override
    public String answer() {
        return answer;
    }

    @Override
    public boolean finished() {
        return finished;
    }

    @Override
    public void done() {
        this.finished = true;
    }

    public CorpusEngine engine() {
        return corpusEngine;
    }

    @Override
    public void reply(String userid, String answer) {
        this.answer = answer;
        logger.info("It doesn't matter whether if it was " + finished());
        corpusEngine.saveQA(userid, question(), answer);
    }

    @Override
    public String toString() {
        return "ReplyDBTask{" +
                "openid='" + openid + '\'' +
                ", question='" + question + '\'' +
                ", answer='" + answer + '\'' +
                ", finished=" + finished +
                '}';
    }
}
