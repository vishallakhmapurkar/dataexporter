package com.de.db

import groovy.sql.Sql

import javax.sql.DataSource

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.stereotype.Service

import com.de.beans.ReportBean


@Service
class DBDAO implements IDBDAO {

	@Autowired
	private DataSource defaultDataSource
	def db1Ins=null
	def db2Ins=null
	def defaultINS
	def isDb2Required =false
	def reportData =null
	def getDefaultINS(){
		return (defaultINS==null)?(new Sql(defaultDataSource)):defaultINS
	}
	void setDB1Ins(driver,url,user,pwd){
		def dataSOurce= getDataSource driver,url,user,pwd
		db1Ins =new Sql(dataSOurce)
	}

	void setDB2Ins(driver,url,user,pwd){
		def dataSOurce= getDataSource driver,url,user,pwd
		db2Ins =new Sql(dataSOurce)
	}

	def getDB1Ins(){
		return db1Ins
	}

	def getDB2Ins(){
		return db2Ins
	}
	def getDataSource(def driver,def url,def user,def pwd){
		def datasource =new DriverManagerDataSource();
		datasource.setDriverClassName(driver)
		datasource.setUrl(url)
		datasource.setUsername(user)
		datasource.setPassword(pwd)

		return datasource
	}
	void setReportdata(def reportData){
		this.reportData =reportData
	}
	def getReportData(){
		return reportData
	}
	@Override
	public void initDB(def reportCode) {
		def reportdata =new ReportBean()
		def sql=DBConstant.REPORT_SQL.replaceAll("<COLMS>", reportdata.getReportCols().toString())
		if(getDefaultINS()!=null){
			getDefaultINS().eachRow(sql,['reportcode':reportCode]){
				reportdata.reportCode = "${it.reportCode}"
				reportdata.reportSQl = "${it.reportSQl}"
				reportdata.isparamrequired = "${it.isparamrequired}"
				reportdata.paramname = "${it.paramname}"
				reportdata.isProc = "${it.isProc}"
				reportdata.reportfilenameprefix = "${it.reportfilenameprefix}"
				reportdata.reportheadersname = "${it.reportheadersname}"
				reportdata.isInsNameRequired = "${it.isInsNameRequired}"
				reportdata.isFTPRequired = "${it.isFTPRequired}"
				reportdata.ftphost = "${it.ftphost}"
				reportdata.ftpuser = "${it.ftpuser}"
				reportdata.ftppwd = "${it.ftppwd}"
				reportdata.isEmailRequired = "${it.isEmailRequired}"
				reportdata.emailTo = "${it.emailTo}"
				reportdata.emailFrom = "${it.emailFrom}"
				reportdata.emailSubPrefix = "${it.emailSubPrefix}"
				reportdata.emailBody = "${it.emailBody}"
				reportdata.isEmailAttachmentRequired = "${it.isEmailAttachmentRequired}"
				reportdata.dbType = "${it.dbType}"
				reportdata.db1host = "${it.db1host}"
				reportdata.db1port = "${it.db1port}"
				reportdata.db1dbname = "${it.db1dbname}"
				reportdata.db1user = "${it.db1user}"
				reportdata.db1pwd = "${it.db1pwd}"
				reportdata.isDb2Required = "${it.isDb2Required}"
				reportdata.db2host = "${it.db2host}"
				reportdata.db2port = "${it.db2port}"
				reportdata.db2dbname = "${it.db2dbname}"
				reportdata.db2user = "${it.db2user}"
				reportdata.db2pwd = "${it.db2pwd}"
			}
			setReportdata reportdata

			setDB1Ins getDriver("mysql") , "jdbc:mysql://localhost:3307/dataexporter", "root", "root"

			if(reportdata.isDb2Required.toString().equalsIgnoreCase("Y")){
				setDB2Ins getDriver("mysql") , "jdbc:mysql://localhost:3307/dataexporter", "root", "root"
			}
		}
	}

	def getDriver(def dbType){
		return "com.mysql.jdbc.Driver"
	}

	def getData(){
		def finalStr =new StringBuilder()
		def reportdata = getReportData()
		if(reportdata!=null){
			finalStr.append(reportdata.reportheadersname.toString())


			def isInsNameRequired = reportdata.isInsNameRequired.toString().equalsIgnoreCase("Y")?true:false
			def isparamrequired =reportdata.isparamrequired.toString().equalsIgnoreCase("Y")?true:false
			def isDb2Required =reportdata.isDb2Required.toString().equalsIgnoreCase("Y")?true:false

			if(isInsNameRequired) {
				finalStr.append(",")
				finalStr.append(DBConstant.INSNAME)
			}
			finalStr.append("\n")

			def row=null
			def records =new ArrayList<String>()
			def paramsArray=null

			if(isparamrequired) {
				paramsArray= getParamAray paramname
				getDB1Ins().eachRow(reportdata.reportSQl.toString(),paramsArray){
					row="${it}"
					row=(isInsNameRequired)?row.toString()+","+"INS:"+DBConstant.PBS1INSNAME:row
					records << row.toString()
				}
				if (isDb2Required){
					row=null
					getDB2Ins().eachRow(reportdata.reportSQl.toString(),paramsArray){
						row="${it}"
						row=(isInsNameRequired)?row.toString()+","+"INS:"+DBConstant.PBS2INSNAME:row
						records << row.toString()
					}
				}
			}else{
				getDB1Ins().eachRow(reportdata.reportSQl.toString()){
					row="${it}"
					row=(isInsNameRequired)?row.toString()+","+"INS:"+DBConstant.PBS1INSNAME:row
					records << row.toString()
				}
				if (isDb2Required){
					row=null
					getDB2Ins().eachRow(reportdata.reportSQl.toString()){
						row="${it}"
						row=(isInsNameRequired)?row.toString()+","+"INS:"+DBConstant.PBS2INSNAME:row
						records << row.toString()
					}
				}
			}
			if(records.size>0){
				for(rw in records){
					def str=rw.toString()

					def arryRow= str.toString().split(",")
					arryRow.each { data->
						data=data.toString().replaceAll("\\s","").replaceAll("\\[", "").replaceAll("\\]","");
						def dataAry=data.toString().split(":")
						if(dataAry.length==2){
							def dataval=dataAry[1]
							finalStr.append(dataval.toString()+ "\t");
						}else{
							finalStr.append("NULL"+ "\t");
						}
						if(!dataAry[0].toString().equalsIgnoreCase("INS"))
							finalStr.append(",")
					}
					finalStr.append("\n")
				}
			}
			finalStr.append("\n")
		}
		return finalStr.toString()
	}
	def getStartDate(){

		def cStartDate= Calendar.getInstance();
		def startDate = cStartDate.getTime();
		return SDF.format(startDate);
	}

	def getEndDate(){
		def cEndDate = Calendar.getInstance();

		if (cEndDate.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
			cEndDate.add(Calendar.DATE, -3);
		}
		else {
			cEndDate.add(Calendar.DATE, -1);
		}

		def endDate = cEndDate.getTime();

		return SDF.format(endDate);
	}
	def getParamAray(def params){
		def keys=params.toString().replaceAll("\\}","").replaceAll("\\{","").split(",")
		def valus = new ArrayList<String>();
		keys.eachWithIndex { a, i ->
			if(i%2==0){
				valus.add(getEndDate())
			}
			else{

				valus.add(getStartDate())
			}
		}
		def vauls = valus.toArray(valus.size())
		return zip(keys, vauls)
	}
	static def zip(keys, values) {
		keys.inject([:]) { m, k ->
			m[k] = values[m.size()]; m
		}
	}
}