package com.suppresswarnings.corpus;

import java.util.function.BiConsumer;

public interface ReplyTask {
    //openid implies a wxid
    String openid();
    String question();
    String answer();
    long timestamp();
    boolean expired();
    boolean finished();
    void done();
    void reply(String userid, String answer);
}
