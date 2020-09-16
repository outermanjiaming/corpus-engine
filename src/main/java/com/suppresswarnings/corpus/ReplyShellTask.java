package com.suppresswarnings.corpus;

import com.suppresswarnings.corpus.engine.CorpusEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;

/**
 * 回复shell任务
 */
public class ReplyShellTask extends ReplyDBTask {
    Log logger = LogFactory.getLog(ReplyDBTask.class);
    Shell service;
    public ReplyShellTask(Shell shell, CorpusEngine corpusEngine, String openid, String question) {
        super(corpusEngine, openid, question);
        this.service = shell;
    }

    @Override
    public boolean expired() {
        return System.currentTimeMillis() - timestamp() > TimeUnit.MINUTES.toMillis(5);
    }

    @Override
    public void reply(String userid, String answer) {
        super.reply(userid, answer);
        if(expired()) {
            logger.info("task expired: " + this.toString());
        } else {
            service.write(formatAnswer(answer), openid());
        }
    }

    public String formatAnswer(String answer) {
        return String.format("「%s」=> %s", question(), answer);
    }

    @Override
    public String toString() {
        return "ReplyShellTask=" + question() + " => " + answer() + ", time = " + timestamp();
    }
}
