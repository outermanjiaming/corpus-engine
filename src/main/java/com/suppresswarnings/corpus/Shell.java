package com.suppresswarnings.corpus;

import com.suppresswarnings.corpus.engine.CorpusEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * Shell to control CorpusEngine
 */
public class Shell {
    Log logger = LogFactory.getLog(Shell.class);
    Selector selector;
    CorpusEngine handler;
    StringBuffer sb=new StringBuffer();
    transient boolean running = true;
    public void stop() {
        running = false;
    }
    public void start(int port, CorpusEngine consumer) {
        try {
            // 创建通道管理器(Selector)
            selector = Selector.open();
            handler  = consumer;
            // 创建通道ServerSocketChannel
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            // 将通道设置为非阻塞
            serverSocketChannel.configureBlocking(false);

            // 将ServerSocketChannel对应的ServerSocket绑定到指定端口(port)
            ServerSocket serverSocket = serverSocketChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));

            /**
             * 将通道(Channel)注册到通道管理器(Selector)，并为该通道注册selectionKey.OP_ACCEPT事件
             * 注册该事件后，当事件到达的时候，selector.select()会返回，
             * 如果事件没有到达selector.select()会一直阻塞。
             */
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 循环处理
            while (running) {
                // 当注册事件到达时，方法返回，否则该方法会一直阻塞
                selector.select();
                // 获取监听事件
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectionKeys.iterator();
                // 迭代处理
                while (iterator.hasNext()) {
                    // 获取事件
                    SelectionKey key = iterator.next();
                    // 移除事件，避免重复处理
                    iterator.remove();
                    if (key.isAcceptable()) {
                        // 检查是否是一个就绪的可以被接受的客户端请求连接
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        // 检查套接字是否已经准备好读数据
                        handleRead(key);
                    } else {
                        System.out.println("what ? ");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void handleAccept(SelectionKey key) throws Exception {
        // 获取客户端连接通道
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = server.accept();
        socketChannel.configureBlocking(false);
        // 信息通过通道发送给客户端
        write("Welcome to Corpus Engine!", socketChannel);
        // 给通道设置读事件，客户端监听到读事件后，进行读取操作
        socketChannel.register(selector, SelectionKey.OP_READ, "Shell:" + ((InetSocketAddress)socketChannel.getRemoteAddress()).getPort());
    }

    void handleRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        String openid = (String)key.attachment();
        logger.info("[Shell] message from " + openid);
        // 从通道读取数据到缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int count = channel.read(buffer);
        if(count < 0){
            channel.close();
        } else {
            // 输出客户端发送过来的消息
            byte[] data = buffer.array();
            String input = new String(data);
            logger.info("[Shell] received message " + input);
            if(input.contains("\n") || sb.length() > 140) {
                sb.append(input.trim());
                String content = sb.toString();
                sb.setLength(0);
                if(handler != null && content.length() > 0) {
                    ReplyTask task = new ReplyShellTask(this, handler, openid, content);
                    String reply = handler.getWorkFlow().ask(task);
                    write(reply, channel);
                }
            } else {
                sb.append(input.trim());
            }
        }
    }

    void write(String content, SocketChannel channel) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((content + "\n").getBytes());
            channel.write(buffer);
        } catch (Exception e) {
            logger.error("fail to write", e);
        }
    }

    public void write(String content, String openid) {
        selector.keys()
                .stream()
                .filter(key->openid.equals(key.attachment()))
                .map(key->(SocketChannel) key.channel())
                .forEach(channel->{ write(content, channel); });
    }
}
