package com.de.service

import org.apache.log4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.de.db.DBConstant
import com.de.db.IDBDAO
import com.de.util.RetryUtil

// Use annotation to inject log field into the class.
@Service
class ReportService implements IReportService {

	@Autowired
	private IDBDAO idbdao
	@Autowired
	private RetryUtil retryUtil
	@Autowired
	private IEmailer iemailer
	def fileName =null
	private @Value('${db.retry.count}') def dbRetryCount
	private @Value('${db.retry.sleep}') def dbRetrySleep
	private @Value('${email.retry.count}') def emailRetryCount
	private @Value('${email.retry.sleep}') def emailRetrySleep
	private @Value('${ftp.retry.count}') def ftpRetryCount
	private @Value('${ftp.retry.sleep}') def ftpRetrySleep

	private def log  = Logger.getLogger(IReportService.class)
    void setReportFileName(def fileName){
		this.fileName =fileName
	}
	
	def getReportFileName(){return fileName}

	@Override
	public void generateReport() {
		log.debug("Starting generating report...")
		log.debug("Loading report data..")
		def reportData = idbdao.getReportData()
		log.debug("Loading report done..")
		def finlaData =idbdao.getData()
		log.debug("Final data is .."+finlaData.toString())
		def fileName =null
		if(reportData!=null){
			fileName= reportData.reportfilenameprefix + DBConstant.CSV_FILE_EXT
			log.debug("Generating file .."+fileName)
			new File(fileName).withWriter{ it << finlaData.toString()}
		}


	}


	@Override
	public void intiDB(def reportCode) {
		if(idbdao!=null){
			log.debug("Loading db configuration for report code :"+reportCode)
			try{
				retryUtil.retry(dbRetryCount,dbRetrySleep,"DB Initialization"){ idbdao.initDB(reportCode) }
			}catch(all){
				log.error("DB Retry failure :"+all.message,all)
			}
			log.debug("Db Loaded ..")
		}
		else
		{
			log.warn("No Db initialization done ,hence exiting ")
			System.exit(1)
		}

	}


	@Override
	public def sendEmail() {
		if(iemailer!=null){
			try{
				
				if(idbdao!=null){
					def reportData = idbdao.getReportData()
					if(reportData!=null){
						def file = getReportFileName()
						def to = reportData.emailTo
						def from  = reportData.emailFrom
						def sub  = reportData.emailSubPrefix
						def body  = reportData.emailBody
						retryUtil.retry(emailRetryCount,emailRetrySleep,"Email Sending"){ iemailer.sendEmail(to,from,sub,body,file) }
						
					}
					
				}
			}catch(all){
				log.error("Email Sending failure :"+all.message,all)
			}
			log.debug("Email Sending Done ..")

		}else
		{
			log.warn("No email service registered ")
		}
		return null;
	}



}
