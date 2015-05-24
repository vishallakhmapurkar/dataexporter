package com.de.beans

class ReportBean {
private def reportCode
private def reportSQl
private def isparamrequired
private def paramname
private def isProc
private def reportfilenameprefix
private def reportheadersname
private def isInsNameRequired
private def isFTPRequired
private def ftphost
private def ftpuser
private def ftppwd
private def isEmailRequired
private def emailTo
private def emailFrom
private def emailSubPrefix
private def emailBody
private def isEmailAttachmentRequired
private def dbType
private def db1host
private def db1port
private def db1dbname
private def db1user
private def db1pwd
private def isDb2Required
private def db2host
private def db2port
private def db2dbname
private def db2user
private def db2pwd


 def getReportCols() {
	return "reportCode, reportSQl, isparamrequired,paramname, isProc, reportfilenameprefix, reportheadersname, isInsNameRequired, isFTPRequired,ftphost, ftpuser, ftppwd, isEmailRequired	, emailTo,emailFrom, emailSubPrefix, emailBody, isEmailAttachmentRequired"+
	        ",dbType,db1host,db1port,db1dbname,db1user,db1pwd,isDb2Required,db2host,db2port,db2dbname,db2user,db2pwd"
 }

}
