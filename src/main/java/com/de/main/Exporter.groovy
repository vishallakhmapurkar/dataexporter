package com.de.main

import org.springframework.context.ApplicationContext
import org.springframework.context.support.ClassPathXmlApplicationContext

import com.de.service.IReportService

class Exporter {
	static main(def args){
		def cntxt= new ClassPathXmlApplicationContext("de-context.xml")
		def iReportService=(IReportService) cntxt.getBean("iReportService")
		iReportService.intiDB("emp")
		iReportService.generateReport()
		
	}
}
