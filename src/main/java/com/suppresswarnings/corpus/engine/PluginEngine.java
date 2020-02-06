package com.suppresswarnings.corpus.engine;

import java.util.HashMap;
import java.util.Map;

import com.suppresswarnings.corpus.Context;
import com.suppresswarnings.corpus.ContextFactory;

/**
 * 加载外部jar包，找到对应的ContextFactory并存储到map中，考虑是否需要热加载。
 * @author lijiaming
 *
 */
public class PluginEngine implements ContextFactory<CorpusEngine>{
	Map<String, ContextFactory<CorpusEngine>> map = new HashMap<>();

	@Override
	public Context<CorpusEngine> getInstance(String userid, String text) {
		if(map.isEmpty()) {
			//load Class from external jars and put them into map, key is the name of it?
		}
		ContextFactory<CorpusEngine> pluginContextFactory = map.get(text);
		if(pluginContextFactory != null) return pluginContextFactory.getInstance(userid, text);
		return null;
	}

}
