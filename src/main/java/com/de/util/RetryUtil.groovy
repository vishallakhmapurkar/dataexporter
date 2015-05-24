package com.de.util

import org.apache.log4j.Logger
import org.springframework.stereotype.Service;
@Service
class RetryUtil {
	private def log  = Logger.getLogger(RetryUtil.class)
	def retry(def times, def sleeptime,def methodName, Closure c){
		Throwable catchedThrowable = null
		for(int i=0; i<times; i++){
			try {
				return c.call()
			} catch(Throwable t){
				catchedThrowable = t
				log.warn("failed to call $methodName. ${i+1} of $times runs.")
				Thread.sleep(sleeptime)
			}
		}
		log.error("finally failed to call $methodName after $times tries.")
		throw catchedThrowable
	}
}
