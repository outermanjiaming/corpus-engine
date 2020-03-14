package com.suppresswarnings.corpus;

import com.suppresswarnings.corpus.engine.CorpusEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class WorkFlow {
    String delimiter = "/";
    CorpusEngine corpusEngine;
    ConcurrentLinkedQueue<ReplyTask> queue;
    ConcurrentHashMap<String, ReplyTask> cache;
    ConcurrentHashMap<String, ReentrantLock> locks;
    ConcurrentHashMap<String, Condition> conditions;
    Log logger = LogFactory.getLog(WorkFlow.class);
    private WorkFlow(){
    }
    public WorkFlow(CorpusEngine corpusEngine) {
        this.corpusEngine = corpusEngine;
        this.queue = new ConcurrentLinkedQueue<>();
        this.locks = new ConcurrentHashMap<>();
        this.conditions = new ConcurrentHashMap<>();
    }

    /**
     * ask a question and get an answer if there is one, or wait for 400ms to get the online answer
     * @param replyTask
     * @return
     */
    public String ask(ReplyTask replyTask) {
        String answer = corpusEngine.getAnswer(replyTask.question());
        if(!corpusEngine.isNull(answer)) {
            return answer;
        }
        String id = id(replyTask);
        logger.info("id = " + id);
        queue.add(replyTask);
        logger.info(id + " added to queue");
        cache.putIfAbsent(id, replyTask);
        logger.info(id + " put to cache");
        ReentrantLock lock = locks.computeIfAbsent(id, key -> new ReentrantLock());
        logger.info("create lock for " + id);
        try {
            logger.info("try to lock for " + id);
            lock.tryLock(400, TimeUnit.MILLISECONDS);
            logger.info(id + " locked");
            Condition condition = conditions.computeIfAbsent(id, key -> lock.newCondition());
            logger.info("create condition for " + id);
            logger.info("await 400ms for " + id);
            condition.await(400, TimeUnit.MILLISECONDS);
            logger.info(id + " wakes up, and remove from cache");
            cache.remove(id);
            logger.info(id + " removed");
            //ok lets go
        } catch (InterruptedException ex){
            logger.info(id + " was waked up, and remove from cache");
            cache.remove(id);
            logger.info(id + " removed");
        } finally {
            lock.unlock();
        }
        logger.info("answering the question " + id);
        if(replyTask.finished()) {
            logger.info(id + " was finished");
            return replyTask.answer();
        }
        replyTask.done();
        logger.info(id + " was not finished yet, try to get answer from DB");
        return corpusEngine.getAnswer(replyTask.question());
    }

    public ReplyTask take(){
        return queue.poll();
    }

    /**
     * reply the task, tell the waiter if it is not too late
     * @param replyTask
     * @param userid
     * @param answer
     */
    public void reply(ReplyTask replyTask, String userid, String answer) {
        replyTask.reply(userid, answer);
        String id = id(replyTask);
        ReplyTask task = cache.remove(id);
        ReentrantLock lock = locks.remove(id);
        Condition condition = conditions.remove(id);
        if(task != null && lock != null && condition != null) {
            try {
                lock.lock();
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public String id(ReplyTask replyTask) {
        return String.join(delimiter, replyTask.openid(), replyTask.question());
    }
}